package play.filters.brotli

import java.util.function.BiFunction
import javax.inject.{ Provider, Inject, Singleton }

import akka.stream.{ OverflowStrategy, FlowShape, Materializer }
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api.inject.Module
import play.api.{ Environment, Configuration }
import play.api.mvc._
import play.core.j
import scala.concurrent.Future
import play.api.http.{ HttpChunk, HttpEntity, Status }
import scala.compat.java8.FunctionConverters._

import com.nixxcode.jvmbrotli.enc.Encoder
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream

import com.nixxcode.jvmbrotli.common.BrotliLoader

/**
 * A brotli filter.
 *
 * This filter may compress with brotli the responses for any requests that aren't HEAD requests and specify an accept encoding of brotli.
 *
 * It won't compresss under the following conditions:
 *
 * - The response code is 204 or 304 (these codes MUST NOT contain a body, and an empty compressed response is 20 bytes
 * long)
 * - The response already defines a Content-Encoding header
 * - A custom shouldBrotli function is supplied and it returns false
 *
 * Since compressing changes the content length of the response, this filter may do some buffering - it will buffer any
 * streamed responses that define a content length less than the configured chunked threshold.  Responses that are
 * greater in length, or that don't define a content length, will not be buffered, but will be sent as chunked
 * responses.
 */
@Singleton
class BrotliFilter @Inject() (config: BrotliFilterConfig)(implicit mat: Materializer) extends EssentialFilter {


  {
    BrotliLoader.isBrotliAvailable()
  }

  import play.api.http.HeaderNames._

  def this(quality: Int = 5,
    chunkedThreshold: Int = 102400,
    shouldBrotli: (RequestHeader, Result) => Boolean = (_, _) => true)(implicit mat: Materializer) = {
      this(BrotliFilterConfig(quality, chunkedThreshold, shouldBrotli))
    }


  def apply(next: EssentialAction) = new EssentialAction {
    implicit val ec = mat.executionContext
    def apply(request: RequestHeader) = {
      if (mayCompress(request)) {
        next(request).mapFuture(result => handleResult(request, result))
      } else {
        next(request)
      }
    }
  }

  private def handleResult(request: RequestHeader, result: Result): Future[Result] = {
    if (shouldCompress(result) && config.shouldBrotli(request, result)) {
      implicit val ec = mat.executionContext
      val header = result.header.copy(headers = setupHeader(result.header.headers))

      result.body match {

        case HttpEntity.Strict(data, contentType) =>
          Future.successful(Result(header, compressStrictEntity(data, contentType)))


        case entity @ HttpEntity.Streamed(_, Some(contentLength), contentType) /*if contentLength <= config.chunkedThreshold*/ =>
          // It's below the chunked threshold, so buffer then compress and send
          entity.consumeData.map { data =>
            Result(header, compressStrictEntity(data, contentType))
          }

        /*

        We do not support chunked response yet.

        case HttpEntity.Streamed(data, _, contentType) if request.version == HttpProtocol.HTTP_1_0 =>
          // It's above the chunked threshold, but we can't chunk it because we're using HTTP 1.0.
          // Instead, we use a close delimited body (ie, regular body with no content length)
          val gzipped = data.via(createGzipFlow)
          Future.successful(
            result.copy(header = header, body = HttpEntity.Streamed(gzipped, None, contentType))

        case HttpEntity.Streamed(data, _, contentType) =>
          // It's above the chunked threshold, compress through the brotli flow, and send as chunked
          val compressed = data via BrotliFlow.brotli(config.quality) map (d => HttpChunk.Chunk(d))
          Future.successful(Result(header, HttpEntity.Chunked(compressed, contentType)))

        case HttpEntity.Chunked(chunks, contentType) =>
          val brotliFlow = Flow.fromGraph(GraphDSL.create[FlowShape[HttpChunk, HttpChunk]]() { implicit builder =>
            import GraphDSL.Implicits._

            val extractChunks = Flow[HttpChunk] collect { case HttpChunk.Chunk(data) => data }
            val createChunks = Flow[ByteString].map[HttpChunk](HttpChunk.Chunk.apply)
            val filterLastChunk = Flow[HttpChunk]
              .filter(_.isInstanceOf[HttpChunk.LastChunk])
              // Since we're doing a merge by concatenating, the filter last chunk won't receive demand until the brotli
              // flow is finished. But the broadcast won't start broadcasting until both flows start demanding. So we
              // put a buffer of one in to ensure the filter last chunk flow demands from the broadcast.
              .buffer(1, OverflowStrategy.backpressure)

            val broadcast = builder.add(Broadcast[HttpChunk](2))
            val concat = builder.add(Concat[HttpChunk]())

            // Broadcast the stream through two separate flows, one that collects chunks and turns them into
            // ByteStrings, sends those ByteStrings through the Brotli flow, and then turns them back into chunks,
            // the other that just allows the last chunk through. Then concat those two flows together.
            broadcast.out(0) ~> extractChunks ~> BrotliFlow.brotli(config.bufferSize) ~> createChunks ~> concat.in(0)
            broadcast.out(1) ~> filterLastChunk ~> concat.in(1)

            new FlowShape(broadcast.in, concat.out)
          })
          Future.successful(Result(header, HttpEntity.Chunked(chunks via brotliFlow, contentType)))*/
      }
    } else {
      Future.successful(result)
    }
  }

  private def compressStrictEntity(data: ByteString, contentType: Option[String]) = {
    val builder = ByteString.newBuilder
    val brotliParameters = new Encoder.Parameters().setQuality(config.quality)
    val gzipOs = new BrotliOutputStream(builder.asOutputStream, brotliParameters)
    gzipOs.write(data.toArray)
    gzipOs.close()
    HttpEntity.Strict(builder.result(), contentType)
  }

  /**
   * Whether this request may be compressed.
   */
  private def mayCompress(request: RequestHeader) =
    request.method != "HEAD" && brotliIsAcceptedAndPreferredBy(request)

  // TODO modify
  private def brotliIsAcceptedAndPreferredBy(request: RequestHeader) = {
    val codings = play.api.mvc.RequestHeader.acceptHeader(request.headers, ACCEPT_ENCODING)
    def explicitQValue(coding: String) = codings collectFirst { case (q, c) if c equalsIgnoreCase coding => q }
    def defaultQValue(coding: String) = if (coding == "identity") 0.001d else 0d
    def qvalue(coding: String) = explicitQValue(coding) orElse explicitQValue("*") getOrElse defaultQValue(coding)

    qvalue("br") > 0d && qvalue("br") >= qvalue("identity")
  }

  /**
   * Whether this response should be compressed.  Responses that may not contain content won't be compressed, nor will
   * responses that already define a content encoding.  Empty responses also shouldn't be compressed, as they will
   * actually always get bigger.
   */
  private def shouldCompress(result: Result) = isAllowedContent(result.header) &&
    isNotAlreadyCompressed(result.header) &&
    !result.body.isKnownEmpty

  /**
   * Certain response codes are forbidden by the HTTP spec to contain content, but a gzipped response always contains
   * a minimum of 20 bytes, even for empty responses.
   */
  private def isAllowedContent(header: ResponseHeader) = header.status != Status.NO_CONTENT && header.status != Status.NOT_MODIFIED

  /**
   * Of course, we don't want to double compress responses
   */
  private def isNotAlreadyCompressed(header: ResponseHeader) = header.headers.get(CONTENT_ENCODING).isEmpty

  // TODO modify
  private def setupHeader(header: Map[String, String]): Map[String, String] = {
    header + (CONTENT_ENCODING -> "br") + addToVaryHeader(header, VARY, ACCEPT_ENCODING)
  }

  /**
   * There may be an existing Vary value, which we must add to (comma separated)
   */
  private def addToVaryHeader(existingHeaders: Map[String, String], headerName: String, headerValue: String): (String, String) = {
    existingHeaders.get(headerName) match {
      case None => (headerName, headerValue)
      case Some(existing) if existing.split(",").exists(_.trim.equalsIgnoreCase(headerValue)) => (headerName, existing)
      case Some(existing) => (headerName, s"$existing,$headerValue")
    }
  }
}

/**
 * Configuration for the brotli filter
 *
 * @param quality The compression-speed vs compression-density tradeoffs. The higher the quality, the slower the compression. Range is 0 to 11
 * @param chunkedThreshold The content length threshold, after which the filter will switch to chunking the result.
 * @param shouldBrotli Whether the given request/result should be compressed with brotli.  This can be used, for example, to implement
 *                   black/white lists for compressing by content type.
 */
case class BrotliFilterConfig(quality: Int = 5,
    chunkedThreshold: Int = 102400,
    shouldBrotli: (RequestHeader, Result) => Boolean = (_, _) => true) {

  // alternate constructor and builder methods for Java
  def this() = this(shouldBrotli = (_, _) => true)

  def withShouldBrotli(shouldBrotli: (RequestHeader, Result) => Boolean): BrotliFilterConfig = copy(shouldBrotli = shouldBrotli)

  def withShouldBrotli(shouldBrotli: BiFunction[play.mvc.Http.RequestHeader, play.mvc.Result, Boolean]): BrotliFilterConfig =
    withShouldBrotli((req: RequestHeader, res: Result) => shouldBrotli.asScala(new j.RequestHeaderImpl(req), res.asJava))

  def withChunkedThreshold(threshold: Int): BrotliFilterConfig = copy(chunkedThreshold = threshold)

  def withQuality(q: Int): BrotliFilterConfig = copy(quality = q)
}

object BrotliFilterConfig {

  def fromConfiguration(conf: Configuration) = {

    val config = conf.get[Configuration]("play.filters.brotli")

    BrotliFilterConfig(
      quality = config.get[Int]("quality")
      /*,chunkedThreshold = config.get[ConfigMemorySize]("chunkedThreshold").toBytes.toInt*/
    )
  }
}

/**
 * The brotli filter configuration provider.
 */
@Singleton
class BrotliFilterConfigProvider @Inject() (config: Configuration) extends Provider[BrotliFilterConfig] {
  lazy val get = BrotliFilterConfig.fromConfiguration(config)
}


/**
 * The brotli filter module.
 */
class BrotliFilterModule extends Module {

  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[BrotliFilterConfig].toProvider[BrotliFilterConfigProvider],
      bind[BrotliFilter].toSelf
    )
  }
}

/**
 * The brotli filter components.
 */
trait BrotliFilterComponents {
  def configuration: Configuration
  def materializer: Materializer

  lazy val brotliFilterConfig: BrotliFilterConfig = BrotliFilterConfig.fromConfiguration(configuration)
  lazy val brotliFilter: BrotliFilter = new BrotliFilter(brotliFilterConfig)(materializer)
}