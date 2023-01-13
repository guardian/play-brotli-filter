/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> and Mariot Chauvin <mariot.chauvin@gmail.com>
 */

package akka.stream.io.compression.brotli

import java.io.{ InputStream, OutputStream }

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import java.util.zip.ZipException

import akka.stream.io.compression.brotli.CoderSpec
import akka.stream.impl.io.compression.{ Compressor, GzipCompressor }
import akka.stream.scaladsl.Flow
import akka.util.ByteString

class BrotliSpec extends CoderSpec("brotli") {
  import CompressionTestingTools._


  {
    Brotli4jLoader.ensureAvailability();
  }

  override protected def newCompressor(): Compressor = new BrotliCompressor
  override protected val encoderFlow: Flow[ByteString, ByteString, Any] = Compression.brotli
  override protected def decoderFlow(maxBytesPerChunk: Int): Flow[ByteString, ByteString, Any] = Compression.unbrotli()

  protected def newDecodedInputStream(underlying: InputStream): InputStream =
    new BrotliInputStream(underlying)

  override protected def newEncodedOutputStream(underlying: OutputStream): OutputStream =
    new BrotliOutputStream(underlying)

  /* There is no CRC in Brotli */
  override def corruptInputCheck = false

  override def extraTests(): Unit = {
    "decode concatenated compressions" in {
      pending //TODO is that something we could support? 
      ourDecode(Seq(encode("Hello, "), encode("dear "), encode("User!")).join) should readAs("Hello, dear User!")
    }
    "provide a similar compression ratio than the standard Brotli/Unbortli streams" in {
      ourEncode(largeTextBytes).length should be equals streamEncode(largeTextBytes).length
    }
    "throw an error on truncated input" in {
      val ex = the[RuntimeException] thrownBy ourDecode(streamEncode(smallTextBytes).dropRight(5))
      ex.ultimateCause.getMessage should equal(" Brotli decompression failed - status: NEEDS_MORE_INPUT")
    }

    /* TODO check if we can reinstate

    "throw an error if compressed data is just missing the trailer at the end" in {
      def brokenCompress(payload: String) = newCompressor().compress(ByteString(payload, "UTF-8"))
      val ex = the[RuntimeException] thrownBy ourDecode(brokenCompress("abcdefghijkl"))
      ex.ultimateCause.getMessage should equal("Truncated GZIP stream")
    }
    "throw early if header is corrupt" in {
      val cause = (the[RuntimeException] thrownBy ourDecode(ByteString(0, 1, 2, 3, 4))).ultimateCause
      cause should ((be(a[ZipException]) and have).message("Not in GZIP format"))
    }
    */
  }
}