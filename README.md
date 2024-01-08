# Brotli filter for play

[![License](https://img.shields.io/:license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)


 * A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) filter for the [Play framework](https://www.playframework.com/)
 * A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) compression `Operator` and a decompression `Operator` for [Akka streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html) 
 * A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) compression `Operator` and a decompression `Operator` for [Apache Pekko](https://pekko.apache.org/) streams 


## Install

Add as a dependency:

* Play **3.0** use [![play-v30-brotli-filter](https://index.scala-lang.org/guardian/play-brotli-filter/play-v30-brotli-filter/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/play-v30-brotli-filter/)
  ```scala
  libraryDependencies += "com.gu" %% "play-v30-brotli-filter" % "[latest version number]"
  ```
* Play **2.9** use [![play-v29-brotli-filter](https://index.scala-lang.org/guardian/play-brotli-filter/play-v29-brotli-filter/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/play-v29-brotli-filter/)
  ```scala
  libraryDependencies += "com.gu" %% "play-v29-brotli-filter" % "[latest version number]"
  ```
* Play **2.8** use [![play-v28-brotli-filter](https://index.scala-lang.org/guardian/play-brotli-filter/play-v28-brotli-filter/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/play-v28-brotli-filter/)
  ```scala
  libraryDependencies += "com.gu" %% "play-v28-brotli-filter" % "[latest version number]"
  ```

* Akka use [![akka-stream-brotli](https://index.scala-lang.org/guardian/play-brotli-filter/akka-stream-brotli/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/akka-stream-brotli/)
  ```scala
  libraryDependencies += "com.gu" %% "akka-stream-brotli" % "[latest version number]"
  ```
  
* Pekko use [![pekko-stream-brotli](https://index.scala-lang.org/guardian/play-brotli-filter/pekko-stream-brotli/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/pekko-stream-brotli/)
  ```scala
  libraryDependencies += "com.gu" %% "pekko-stream-brotli" % "[latest version number]"
  ```

## Configure

Currently the only parameter you can configure is quality, which defaults to `11`.

```
play.filters {

  # Brotli filter configuration
  brotli {

    # The compression-speed vs compression-density tradeoffs. The higher the quality, the slower the compression. Range is 0 to 11
    quality = 11

  }
}
```
