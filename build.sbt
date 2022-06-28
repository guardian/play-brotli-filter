import sbtrelease.ReleaseStateTransformations._

name:="play-brotli-filter"

organization := "com.gu"

description := "A brotli filter for the play framework"

licenses := Seq("Apache v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.13.8"

crossScalaVersions := Seq("2.13.8", "2.12.15")

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

Test / publishArtifact := false

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


libraryDependencies ++= Seq(
  "com.aayushatharva.brotli4j" % "brotli4j" % "1.7.1",
  "com.typesafe.play" %% "play" % "2.8.15" % "provided",
  "com.typesafe.play" %% "filters-helpers" % "2.8.15" % "test",
  "com.typesafe.play" %% "play-specs2" % "2.8.15" % "test"
)