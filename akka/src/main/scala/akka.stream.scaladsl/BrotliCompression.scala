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

package akka.stream.scaladsl

import akka.NotUsed
import akka.stream.impl.io.compression._
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import akka.stream.io.compression.brotli.{BrotliCompressor, BrotliDecompressor}

object BrotliCompression {

  val DefaultQuality: Int = BrotliCompressor.DefaultQuality

  //TODO improve comment

  def brotli: Flow[ByteString, ByteString, NotUsed] = 
    CompressionUtils.compressorFlow(() => new BrotliCompressor())

  /**
   * Creates a flow that gzip-compresses a stream of ByteStrings. Note that the compressor
   * will SYNC_FLUSH after every [[ByteString]] so that it is guaranteed that every [[ByteString]]
   * coming out of the flow can be fully decompressed without waiting for additional data. This may
   * come at a compression performance cost for very small chunks.
   *
   * FIXME: should strategy / flush mode be configurable? See https://github.com/akka/akka/issues/21849
   *
   * @param level Compression level (0-11)
   */
  def brotli(level: Int = BrotliCompressor.DefaultQuality): Flow[ByteString, ByteString, NotUsed] =
    CompressionUtils.compressorFlow(() => new BrotliCompressor(level))

  /**
   * Creates a Flow that decompresses a brotli-compressed stream of data.
   */
  def unbrotli(): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new BrotliDecompressor()).named("unbrotli")

}