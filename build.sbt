import sbtrelease.ReleaseStateTransformations._

name:="play-brotli-filter"

pomExtra := (
  <url>https://github.com/guardian/play-brotli-filter</url>
    <developers>
      <developer>
        <id>mchv</id>
        <name>Mariot Chauvin</name>
        <url>https://github.com/mchv</url>
      </developer>
    </developers>
  )
publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
description := ""
scalaVersion := "2.11.8"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
organization := "com.gu"
licenses := Seq("Apache v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/play-brotli-filter"),
  "scm:git:git@github.com:guardian/play-brotli-filter.git"
))
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-deprecation", "-unchecked")
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

resolvers += "JBrotli Bintray Repository" at "https://dl.bintray.com/nitram509/jbrotli/"

libraryDependencies ++= Seq(
  "org.meteogroup.jbrotli" % "jbrotli" % "0.5.0",
  "com.typesafe.play" %% "play" % "2.5.9",
  "com.typesafe.play" %% "filters-helpers" % "2.5.9" % "test",
  "com.typesafe.play" %% "play-specs2" % "2.5.9" % "test"
)