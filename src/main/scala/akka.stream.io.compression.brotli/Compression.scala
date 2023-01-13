/*
 * Copyright (C) 2016-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.io.compression.brotli

import akka.NotUsed
import akka.stream.impl.io.compression._
import akka.stream.scaladsl.Flow
import akka.util.ByteString

object Compression {

  /**
   * Creates a flow that gzip-compresses a stream of ByteStrings. Note that the compressor
   * will SYNC_FLUSH after every [[ByteString]] so that it is guaranteed that every [[ByteString]]
   * coming out of the flow can be fully decompressed without waiting for additional data. This may
   * come at a compression performance cost for very small chunks.
   *
   * FIXME: should strategy / flush mode be configurable? See https://github.com/akka/akka/issues/21849
   */
  def brotli: Flow[ByteString, ByteString, NotUsed] = brotli(9) //TODO use constant

  /**
   * Same as [[brotli]] with a custom level.
   *
   * @param level Compression level (0-9)
   */
  def brotli(level: Int): Flow[ByteString, ByteString, NotUsed] =
    CompressionUtils.compressorFlow(() => new BrotliCompressor(level))

  /**
   * Creates a Flow that decompresses a brotli-compressed stream of data.
   */
  def unbrotli(): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].via(new BrotliDecompressor()).named("unbrotli")

}