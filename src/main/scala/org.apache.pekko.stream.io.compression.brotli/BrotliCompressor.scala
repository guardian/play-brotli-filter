/*
 * Copyright 2023 Mariot Chauvin
 *
 * Mariot Chauvin licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pekko.stream.io.compression.brotli


import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.impl.io.compression.Compressor
import com.aayushatharva.brotli4j.encoder.Encoder


object BrotliCompressor {
    val MinQuality: Int = 0
    val MaxQuality: Int = 11
    val DefaultQuality: Int = MaxQuality
}

class BrotliCompressor(level: Int = BrotliCompressor.DefaultQuality) extends Compressor {

  /**
   * Compresses the given input and returns compressed data. The implementation
   * can and will choose to buffer output data to improve compression. Use
   * `flush` or `compressAndFlush` to make sure that all input data has been
   * compressed and pending output data has been returned.
   */
  override final def compress(input: ByteString): ByteString = {
     val output = Encoder.compress(input.toArray, new Encoder.Parameters().setQuality(level))
     ByteString(output)
  }

  /**
   * Flushes any output data and returns the currently remaining compressed data.
   */
  override final def flush(): ByteString = ByteString.empty

  /**
   * Closes this compressed stream and return the remaining compressed data. After
   * calling this method, this Compressor cannot be used any further.
   */
  override final def finish(): ByteString = ByteString.empty

  /** Combines `compress` + `flush` */
  override final def compressAndFlush(input: ByteString): ByteString = compress(input)

  /** Combines `compress` + `finish` */
  override final def compressAndFinish(input: ByteString): ByteString = compress(input)

  /** Make sure any resources have been released */
  override final def close(): Unit = {}

}


/*



 public BrotliOutputStream(OutputStream destination, Encoder.Parameters params)





 */



/*
public BrotliEncoderChannel(WritableByteChannel destination, Encoder.Parameters params)
            throws IOException {

*/


/*

protected def flushWithBuffer(buffer: Array[Byte]): ByteString = {
    val written = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH)
    ByteString.fromArray(buffer, 0, written)
  }

   private def newTempBuffer(size: Int = 65536): Array[Byte] = {
    // The default size is somewhat arbitrary, we'd like to guess a better value but Deflater/zlib
    // is buffering in an unpredictable manner.
    // `compress` will only return any data if the buffered compressed data has some size in
    // the region of 10000-50000 bytes.
    // `flush` and `finish` will return any size depending on the previous input.
    // This value will hopefully provide a good compromise between memory churn and
    // excessive fragmentation of ByteStrings.
    // We also make sure that buffer size stays within a reasonable range, to avoid
    // draining deflator with too small buffer.
    new Array[Byte](math.max(size, MinBufferSize))
  }
}

*/



