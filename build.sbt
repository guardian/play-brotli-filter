import sbtrelease.ReleaseStateTransformations._

name:="play-brotli-filter"

organization := "com.gu"

description := "A brotli filter for the play framework"

licenses := Seq("Apache v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.13.12"

crossScalaVersions := Seq("2.13.12", "3.3.1")

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
    Opts.resolver.sonatypeOssSnapshots.head /* Take first repo */
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

val Brotli4jVersion = "1.12.0"

val PlayVersion = "2.9.0-RC2"
val AkkaVersion = "2.6.21"

libraryDependencies ++= Seq(
  "com.aayushatharva.brotli4j" % "brotli4j" % Brotli4jVersion,
  "com.typesafe.play" %% "play" % PlayVersion % Provided,
  "com.typesafe.play" %% "filters-helpers" % "2.9.0-M6" % Test,
  "com.typesafe.play" %% "play-specs2" % PlayVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Provided,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)


/* Use assembly output as packaged jar */
Compile / packageBin := assembly.value


/* Use same packaged jar name that packageBin task */
assembly / assemblyJarName :=  s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"

/* Exclude the scala library from being included in assembled jar*/
assembly / assemblyOption ~= {
      _.withIncludeScala(false)
}

/* Exclude brotli4j library from being included in assembled jar*/
assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      cp filter {_.data.getName == s"brotli4j-${Brotli4jVersion}.jar"}
}


assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
    case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
}


