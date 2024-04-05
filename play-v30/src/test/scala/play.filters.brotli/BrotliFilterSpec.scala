/*
 * Copyright (C) 2009-2023 Lightbend Inc. <https://www.lightbend.com> and Mariot Chauvin <mariot.chauvin@gmail.com>
 */
package play.filters.brotli

import javax.inject.Inject

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Application
import play.api.http.{ HttpEntity, HttpFilters }
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.{Router, SimpleRouterImpl}
import play.api.test._
import play.api.mvc.{ AnyContentAsEmpty, Action, DefaultActionBuilder, Result }
import play.api.mvc.Results._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Files
import org.apache.pekko.stream.scaladsl.FileIO
import org.apache.commons.io.IOUtils
import scala.concurrent.Future
import scala.util.Random
import org.specs2.matcher.{DataTables, MatchResult}

import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import play.api.mvc.Cookie

object BrotliFilterSpec extends PlaySpecification with DataTables {

  sequential

  "The BrotliFilter" should {

    "compress responses with brotli" in withApplication(Ok("hello")) { implicit app =>
      checkCompressedBody(makeBrotliRequest(app), "hello")(app.materializer)
    }

    """compress a response with brotli if (and only if) it is accepted and preferred by the request.
      |Although not explicitly mentioned in RFC 2616 sect. 14.3, the default qvalue
      |is assumed to be 1 for all mentioned codings. If no "*" is present, unmentioned
      |codings are assigned a qvalue of 0, except the identity coding which gets q=0.001,
      |which is the lowest possible acceptable qvalue.
      |This seems to be the most consistent behaviour with respect to the other "accept"
      |header fields described in sect 14.1-5.""".stripMargin in withApplication(Ok("meep")) { implicit app =>

      val (plain, bred) = (None, Some("br"))

      "Accept-Encoding of request" || "Response" |
        //------------------------------------++------------+
        "br" !! bred |
        "compress,br" !! bred |
        "compress, br" !! bred |
        "br,compress" !! bred |
        "deflate, br,compress" !! bred |
        "br, compress" !! bred |
        "identity, br, compress" !! bred |
        "*" !! bred |
        "*;q=0" !! plain |
        "*; q=0" !! plain |
        "*;q=0.000" !! plain |
        "br;q=0" !! plain |
        "br; q=0.00" !! plain |
        "*;q=0, br" !! bred |
        "compress;q=0.1, *;q=0, br" !! bred |
        "compress;q=0.1, *;q=0, br;q=0.005" !! bred |
        "compress, br;q=0.001" !! bred |
        "compress, br;q=0.002" !! bred |
        "compress;q=1, *;q=0, br;q=0.000" !! plain |
        "compress;q=1, *;q=0" !! plain |
        "identity" !! plain |
        "br;q=0.5, identity" !! plain |
        "br;q=0.5, identity;q=1" !! plain |
        "br;q=0.6, identity;q=0.5" !! bred |
        "*;q=0.7, br;q=0.6, identity;q=0.4" !! bred |
        "" !! plain |> {

          (codings, expectedEncoding) =>
            header(CONTENT_ENCODING, requestAccepting(app, codings)) must be equalTo (expectedEncoding)
        }
    }

    "not brotli empty responses" in withApplication(Ok) { implicit app =>
      checkNotCompressed(makeBrotliRequest(app), "")(app.materializer)
    }

    "not brotli responses when not requested" in withApplication(Ok("hello")) { implicit app =>
      checkNotCompressed(route(app, FakeRequest()).get, "hello")(app.materializer)
    }

    "not brotli HEAD requests" in withApplication(Ok) { implicit app =>
      checkNotCompressed(route(app, FakeRequest("HEAD", "/").withHeaders(ACCEPT_ENCODING -> "br")).get, "")(app.materializer)
    }

    "not brotli no content responses" in withApplication(NoContent) { implicit app =>
      checkNotCompressed(makeBrotliRequest(app), "")(app.materializer)
    }

    "not brotli not modified responses" in withApplication(NotModified) { implicit app =>
      checkNotCompressed(makeBrotliRequest(app), "")(app.materializer)
    }

    "brotli chunked responses" in withApplication(Ok.chunked(Source(List("foo", "bar")))) { implicit app =>
      val result = makeBrotliRequest(app)

      checkCompressedBody(result, "foobar")(app.materializer)
      await(result).body must beAnInstanceOf[HttpEntity.Chunked]
    }

    val body = Random.nextString(1000)

    "not buffer more than the configured threshold" in withApplication(
      Ok.sendEntity(HttpEntity.Streamed(Source.single(ByteString(body)), Some(1000), None)), chunkedThreshold = 512) { implicit app =>
        val result = makeBrotliRequest(app)
        checkCompressedBody(result, body)(app.materializer)
        await(result).body must beAnInstanceOf[HttpEntity.Chunked]
      }
  

    "brotli a strict body even if it exceeds the threshold" in withApplication(Ok(body), chunkedThreshold = 512) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressedBody(result, body)(app.materializer)
      await(result).body must beAnInstanceOf[HttpEntity.Strict]
    }

    val path: Path = Path.of(getClass.getResource("/bootstrap.min.css").toURI())
    val source = FileIO.fromPath(path)
    val contentLength = Files.size(path)

    "brotli entire content for large files" in withApplication(
      Ok.sendEntity(HttpEntity.Streamed(source, Some(contentLength), Some("text/css"))), chunkedThreshold = 512) { implicit app =>
        val result = makeBrotliRequest(app)
        checkCompressedBodyLength(result, contentLength)(app.materializer)
        await(result).body must beAnInstanceOf[HttpEntity.Chunked]
    }

    "preserve original headers" in withApplication(Ok("hello").withHeaders(SERVER -> "Play")) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressed(result)
      header(SERVER, result) must beSome("Play")
    }

    "preserve original cookies" in withApplication(Ok("hello").withCookies(Cookie("foo", "bar"))) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressed(result)
      cookies(result).get("foo") must beSome(Cookie("foo", "bar"))
    }

    "preserve original session" in withApplication(Ok("hello").withSession("foo" -> "bar")) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressed(result)
      session(result).get("foo") must beSome("bar")
    }

    "preserve original Vary header values" in withApplication(Ok("hello").withHeaders(VARY -> "original")) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressed(result)
      header(VARY, result) must beSome[String].which(header => header.contains("original,"))
    }

    "preserve original Vary header values and not duplicate case-insensitive ACCEPT-ENCODING" in withApplication(Ok("hello").withHeaders(VARY -> "original,ACCEPT-encoding")) { implicit app =>
      val result = makeBrotliRequest(app)
      checkCompressed(result)
      header(VARY, result) must beSome[String].which(header => header.split(",").filter(_.toLowerCase(java.util.Locale.ENGLISH) == ACCEPT_ENCODING.toLowerCase(java.util.Locale.ENGLISH)).size == 1)
    }
  }

  class Filters @Inject() (brotliFilter: BrotliFilter) extends HttpFilters {
    def filters = Seq(brotliFilter)
  }

  class ResultRouter @Inject() (action: DefaultActionBuilder, result: Result) extends SimpleRouterImpl({ case _ => action(result) })

  def withApplication[T](result: Result, quality: Int = 5, chunkedThreshold: Int = 1024)(block: Application => T): T = {
    val application = new GuiceApplicationBuilder()
      .configure(
        "play.filters.brotli.quality" -> quality,
        "play.filters.brotli.chunkedThreshold" -> chunkedThreshold
      ).overrides(
          bind[Result].to(result),
          bind[Router].to[ResultRouter],
          bind[HttpFilters].to[Filters]
        ).build()
    running(application)(block(application))
  }

  def brotliRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(ACCEPT_ENCODING -> "br")

  def makeBrotliRequest(app: Application): Future[Result] = route(app, brotliRequest).get

  def requestAccepting(app: Application, codings: String): Future[Result] = route(app, FakeRequest().withHeaders(ACCEPT_ENCODING -> codings)).get

  def uncompress(bytes: ByteString): String = {
    val is = new BrotliInputStream(new ByteArrayInputStream(bytes.toArray))
    val result = IOUtils.toString(is, "UTF-8")
    is.close()
    result
  }

  def uncompressBytes(bytes: ByteString): Array[Byte] = {
    val is = new BrotliInputStream(new ByteArrayInputStream(bytes.toArray))
    val result = IOUtils.toByteArray(is)
    is.close()
    result
  }

  def checkCompressed(result: Future[Result]): MatchResult[Option[String]] = {
    header(CONTENT_ENCODING, result) aka "Content encoding header" must beSome("br")
  }

  def checkCompressedBodyLength(result: Future[Result], contentLength: Long)(implicit mat: Materializer): MatchResult[Any] = {
    checkCompressed(result)
    val resultBody = contentAsBytes(result)
    await(result).body.contentLength.foreach { cl =>
      resultBody.length must_== cl
    }
    uncompressBytes(resultBody).length must_== contentLength
  }

  def checkCompressedBody(result: Future[Result], body: String)(implicit mat: Materializer): MatchResult[Any] = {
    checkCompressed(result)
    val resultBody = contentAsBytes(result)
    await(result).body.contentLength.foreach { cl =>
      resultBody.length must_== cl
    }
    uncompress(resultBody) must_== body
  }

  def checkNotCompressed(result: Future[Result], body: String)(implicit mat: Materializer): MatchResult[Any] = {
    header(CONTENT_ENCODING, result) must beNone
    contentAsString(result) must_== body
  }
}
