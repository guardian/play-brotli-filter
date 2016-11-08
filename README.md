# Brotli filter for play

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/play-brotli-filter_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/play-brotli-filter_2.11) [![Build Status](https://travis-ci.org/guardian/play-brotli-filter.svg?branch=master)](https://travis-ci.org/guardian/play-brotli-filter) [![License](https://img.shields.io/:license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

A [Brotli](https://opensource.googleblog.com/2015/09/introducing-brotli-new-compression.html) filter for the [playframework](https://www.playframework.com/)


## Install

```scala
libraryDependencies ++= Seq(
 "com.gu" %% "play-brotli-filter" % "0.1",
 "org.meteogroup.jbrotli" % brotliNativeArtefact % "0.5.0",
)
```

`brotliNativeArtefact` is dependent of your target platform and can not be resolved transparently as
sbt does not support activating maven profile (as far as I am aware).

Below is a workaround that should allow to have the correct artefact name depending on your platform:

```scala
val brotliNativeArtefact = {

  val osName = System.getProperty("os.name").toLowerCase
  val osArch = System.getProperty("os.arch").toLowerCase
  
  val family = if (osName.startsWith("linux")) {
    "linux"
    } else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
      "darwin"
    } else {
      "win32"
  }

  val arch = if (family == "darwin") {
      "x86-amd64"
    } else if (osArch == "i386" || osArch == "i486" || osArch == "i586" || osArch == "i686") {
      "x86"
    } else if (osArch == "amd64" || osArch == "x86-64" || osArch == "x64") {
      "x86-amd64"
    } else if (family == "linux" && osArch.startsWith("arm")) {
      "arm32-vfp-hflt"
  }

  s"jbrotli-native-$family-$arch"
}
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