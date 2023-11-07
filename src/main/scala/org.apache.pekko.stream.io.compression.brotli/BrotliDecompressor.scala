/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 *   - Mariot Chauvin
 */

package org.apache.pekko.stream.io.compression.brotli

import java.nio.ByteBuffer
import java.util.zip.Inflater
import java.util.zip.ZipException

import com.aayushatharva.brotli4j.decoder.DirectDecompress
import com.aayushatharva.brotli4j.decoder.DecoderJNI

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.impl.io.ByteStringParser
import org.apache.pekko.stream.impl.io.ByteStringParser.{ ParseResult, ParseStep }
import org.apache.pekko.util.ByteString

class BrotliDecompressor extends ByteStringParser[ByteString] {

  class DecompressorParsingLogic extends ParsingLogic {

    case object DecompressStep extends ParseStep[ByteString] {

      private def fail(msg: String) = throw new ZipException(msg)

      override def onTruncation(): Unit = failStage(new ZipException("Truncated Brotli stream"))

      override def parse(reader: ByteStringParser.ByteReader): ParseResult[ByteString] = {
        if (!reader.hasRemaining) {
          ParseResult(None, ByteStringParser.FinishedParser, true)
        } else {
          val data = reader.remainingData.toArrayUnsafe()
          val directDecompress = DirectDecompress.decompress(data)
          reader.skip(reader.remainingSize)

          val status = directDecompress.getResultStatus()
          if (status == DecoderJNI.Status.DONE) {
            val outcome = directDecompress.getDecompressedData()
            ParseResult(Some(ByteString(outcome)), this, true)
          } else if (status == DecoderJNI.Status.NEEDS_MORE_INPUT) {
            throw ByteStringParser.NeedMoreData
          } else {
            fail(" Brotli decompression failed - status: " + status)
          }
        }
      }
    }

    override def postStop(): Unit = {}

  }

  override def createLogic(attr: Attributes) = new DecompressorParsingLogic {
      startWith(DecompressStep)
  }

}