import sbtrelease.ReleaseStateTransformations._

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"


val Scala212 = "2.12.18"
val Scala213 = "2.13.12"
val Scala3 = "3.3.1"


ThisBuild / scalaVersion := Scala213

ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val sharedSettings = Seq(
  scalacOptions ++= Seq("-feature", "-deprecation"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.17" % Test
  ),
  Test / publishArtifact := false,
  publishConfiguration := publishConfiguration.value.withOverwrite(true)
)

val Brotli4jVersion = "1.15.0"
lazy val common = project
  .settings(
    name:="brotli4s",
    description := "A scala wrapper for brotli4j",
    sharedSettings,
    crossScalaVersions := Seq(Scala212, Scala213, Scala3),
    libraryDependencies ++= Seq(
      "com.aayushatharva.brotli4j" % "brotli4j" % Brotli4jVersion
    ),

    artifactPomMetadataSettings,

    assemblySettings
  )

val AkkaVersion = "2.6.21"
lazy val akka = project.dependsOn(common)
  .settings(
    name:="akka-stream-brotli",
    description := "A brotli stream extension for Akka 2.6",
    sharedSettings,
    crossScalaVersions := Seq(Scala212, Scala213, Scala3),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion % Provided,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
    ),

    artifactPomMetadataSettings


  )

val PekkoVersion = "1.0.2"
lazy val pekko = project.dependsOn(common)
  .settings(
    name:="pekko-stream-brotli",
    description := "A brotli stream extension for Pekko 1.0",
    sharedSettings,
    crossScalaVersions := Seq(Scala213, Scala3),
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion % Provided,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
    ),

    artifactPomMetadataSettings

  )


val artifactPomMetadataSettings = Seq(
  organization := "com.gu",
  licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/play-brotli-filter"),
    "scm:git:git@github.com:guardian/play-brotli-filter.git"
  )),
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
)

val assemblySettings = Seq( 
    /* Use assembly output as packaged jar */
    Compile / packageBin := assembly.value,


    /* Use same packaged jar name that packageBin task */
    assembly / assemblyJarName :=  s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar",

    /* Exclude the scala library from being included in assembled jar*/
    assembly / assemblyOption ~= {
          _.withIncludeScala(false)
    },

    /* Exclude brotli4j library from being included in assembled jar*/
    assembly / assemblyExcludedJars := {
          val cp = (assembly / fullClasspath).value
          cp filter {_.data.getName == s"brotli4j-${Brotli4jVersion}.jar"}
    },


    assembly / assemblyMergeStrategy := {
        case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
        case x =>
            val oldStrategy = (assembly / assemblyMergeStrategy).value
            oldStrategy(x)
    }
  )

lazy val `play-v28` = project
  .dependsOn(akka)
  .settings(
    sharedSettings,
    name:="play-v28-brotli-filter",
    description := "A brotli filter module for Play 2.8",
    crossScalaVersions := Seq(Scala212, Scala213),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % "2.8.21" % Provided,
      "com.typesafe.play" %% "filters-helpers" % "2.8.21" % Test,
      "com.typesafe.play" %% "play-specs2" % "2.8.21" % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.14"  % Test
    ),

    artifactPomMetadataSettings
  )

lazy val `play-v29` = project
  .dependsOn(akka)
  .settings(
    sharedSettings,
    name:="play-v29-brotli-filter",
    description := "A brotli filter module for Play 2.9",
    crossScalaVersions := Seq(Scala213, Scala3),
    libraryDependencies ++= Seq( 
      "com.typesafe.play" %% "play" % "2.9.1" % Provided,
      "com.typesafe.play" %% "filters-helpers" % "2.9.0-M6" % Test,
      "com.typesafe.play" %% "play-specs2" % "2.9.1" % Test,
    ),

    artifactPomMetadataSettings
  )

  lazy val `play-v30` = project
  .dependsOn(pekko)
  .settings(
    sharedSettings,
    name:="play-v30-brotli-filter",
    description := "A brotli filter module for Play 3",
    crossScalaVersions := Seq(Scala213, Scala3),
    libraryDependencies ++= Seq( 
      "org.playframework" %% "play" % "3.0.1" % Provided,
      "org.playframework" %% "play-filters-helpers" % "3.0.1" % Test,
      "org.playframework" %% "play-specs2" % "3.0.1" % Test,
    ),

    artifactPomMetadataSettings
  )


lazy val `play-brotli-filter-root` = (project in file("."))
  .aggregate(common, akka, pekko, `play-v28`,`play-v29` ,`play-v30`)
  .settings(
    publish / skip := true,
    crossScalaVersions := Nil,

    organization := "com.gu",
    /* deactivate cross-building and use `+` on `test` and `publishSigned` see https://www.scala-sbt.org/1.x/docs/Cross-Build.html */
    releaseCrossBuild := false,
    releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
    )
  )