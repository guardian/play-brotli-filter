# Brotli filter for play

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/play-brotli-filter_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/play-brotli-filter_2.13) [![Build Status](https://travis-ci.org/guardian/play-brotli-filter.svg?branch=master)](https://travis-ci.org/guardian/play-brotli-filter) [![License](https://img.shields.io/:license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) filter for the [playframework](https://www.playframework.com/)


## Install

Add `play-brotli-filter` as a dependency:

```scala
libraryDependencies ++= Seq(
 "com.gu" %% "play-brotli-filter" % "0.6",
)
```

## Configure

Currently the only parameter you can configure is quality, which defaults to `5`.

```
play.filters {

  # Brotli filter configuration
  brotli {

    # The compression-speed vs compression-density tradeoffs. The higher the quality, the slower the compression. Range is 0 to 11
    quality = 5

  }
}
```