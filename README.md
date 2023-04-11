# Brotli filter for play

[![Maven Central](https://index.scala-lang.org/guardian/play-brotli-filter/play-brotli-filter/latest-by-scala-version.svg)](https://index.scala-lang.org/guardian/play-brotli-filter/play-brotli-filter)
[![License](https://img.shields.io/:license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) filter for the [playframework](https://www.playframework.com/)


## Install

Add `play-brotli-filter` as a dependency:

```scala
libraryDependencies ++= Seq(
 "com.gu" %% "play-brotli-filter" % "0.9",
)
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
