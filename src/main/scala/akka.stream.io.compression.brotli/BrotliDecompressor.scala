/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 *   - Mariot Chauvin
 */

package akka.stream.io.compression.brotli

import java.nio.ByteBuffer
import java.util.zip.Inflater
import java.util.zip.ZipException

import com.aayushatharva.brotli4j.decoder.DirectDecompress
import com.aayushatharva.brotli4j.decoder.DecoderJNI

import akka.annotation.InternalApi
import akka.stream.Attributes
import akka.stream.impl.io.ByteStringParser
import akka.stream.impl.io.ByteStringParser.{ ParseResult, ParseStep }
import akka.util.ByteString

class BrotliDecompressor extends ByteStringParser[ByteString] {

  class DecompressorParsingLogic extends ParsingLogic {

    case object DecompressStep extends ParseStep[ByteString] {

      private def fail(msg: String) = throw new ZipException(msg)

      override def parse(reader: ByteStringParser.ByteReader): ParseResult[ByteString] = {
        if (!reader.hasRemaining) {
          ParseResult(None, ByteStringParser.FinishedParser, true) 
        } else {
          val directDecompress = DirectDecompress.decompress(reader.remainingData.toArrayUnsafe())
          reader.skip(reader.remainingSize)

          val status = directDecompress.getResultStatus()
          if (status == DecoderJNI.Status.DONE) {
            val outcome = directDecompress.getDecompressedData()
            ParseResult(Some(ByteString(outcome)), this, true)
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