import sbtrelease.ReleaseStateTransformations._

name:="play-brotli-filter"

organization := "com.gu"

description := "A brotli filter for the play framework"

licenses := Seq("Apache v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.13.4"

crossScalaVersions := Seq("2.13.4", "2.12.12")

scalacOptions ++= Seq("-feature", "-deprecation")

scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/play-brotli-filter"),
  "scm:git:git@github.com:guardian/play-brotli-filter.git"
))

pomExtra := {
  <url>https://github.com/guardian/play-brotli-filter</url>
    <developers>
      <developer>
        <id>mchv</id>
        <name>Mariot Chauvin</name>
        <url>https://github.com/mchv</url>
      </developer>
    </developers>
  }

releaseCrossBuild := true

publishArtifact in Test := false

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
) 

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "JBrotli Bintray Repository" at "https://dl.bintray.com/nitram509/jbrotli/"


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

  s"jvmbrotli-$family-$arch"
}

libraryDependencies ++= Seq(
  "com.nixxcode.jvmbrotli" % "jvmbrotli" % "0.2.0",
  "com.nixxcode.jvmbrotli" % brotliNativeArtefact % "0.2.0" % "provided",
  "com.typesafe.play" %% "play" % "2.8.6" % "provided",
  "com.typesafe.play" %% "filters-helpers" % "2.8.6" % "test",
  "com.typesafe.play" %% "play-specs2" % "2.8.6" % "test"
)