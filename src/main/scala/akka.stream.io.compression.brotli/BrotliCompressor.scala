/*
 * Copyright (C) 2022 Mariot Chauvin
 *    
 */


package akka.stream.io.compression.brotli


import akka.util.ByteString
import akka.stream.impl.io.compression.Compressor
import com.aayushatharva.brotli4j.encoder.Encoder


class BrotliCompressor(level: Int = 9) extends Compressor {

  //TODO manage paramters

  /**
   * Compresses the given input and returns compressed data. The implementation
   * can and will choose to buffer output data to improve compression. Use
   * `flush` or `compressAndFlush` to make sure that all input data has been
   * compressed and pending output data has been returned.
   */
  override final def compress(input: ByteString): ByteString = {
     val output = Encoder.compress(input.toArray) //TODO manage parameters
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



