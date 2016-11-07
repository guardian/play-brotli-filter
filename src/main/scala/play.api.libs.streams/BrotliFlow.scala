/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> and Mariot Chauvin <mariot.chauvin@gmail.com>
 */

package play.api.libs.streams

import java.util.zip.GZIPOutputStream

import akka.stream.scaladsl.Flow
import akka.stream.stage.{ Context, PushPullStage }
import akka.util.ByteString

/**
 * A simple Brotli Flow
 *
 * Brotlis each chunk separately using the JDK7 syncFlush feature.
 */
object BrotliFlow {

  /**
   * Create a Brotli Flow with the given buffer size.
   */
  def brotli(bufferSize: Int = 512): Flow[ByteString, ByteString, _] = {
    Flow[ByteString].transform(() => new BrotliStage(bufferSize))
  }

  private class BrotliStage(bufferSize: Int) extends PushPullStage[ByteString, ByteString] {

    val builder = ByteString.newBuilder
    // Uses syncFlush mode
    //TODO modify
    val gzipOs = new GZIPOutputStream(builder.asOutputStream, bufferSize, true)

    def onPush(elem: ByteString, ctx: Context[ByteString]) = {
      // For each chunk, we write it to the gzip output stream, flush which forces it to be entirely written to the
      // underlying ByteString builder, then we create the ByteString and clear the builder.
      gzipOs.write(elem.toArray)
      gzipOs.flush()
      val result = builder.result()
      builder.clear()
      ctx.push(result)
    }

    def onPull(ctx: Context[ByteString]) = {
      // If finished, push the last ByteString
      if (ctx.isFinishing) {
        ctx.pushAndFinish(builder.result())
      } else {
        // Otherwise request more demand from upstream
        ctx.pull()
      }
    }

    override def onUpstreamFinish(ctx: Context[ByteString]) = {
      // Absorb termination, so we can send the last chunk out of the gzip output stream on the next pull
      gzipOs.close()
      ctx.absorbTermination()
    }

    override def postStop() = {
      // Close in case it's not already closed to release native deflate resources
      gzipOs.close()
      builder.clear()
    }
  }

}