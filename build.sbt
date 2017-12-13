import sbtrelease.ReleaseStateTransformations._

import scala.language.postfixOps

scalafmtOnCompile in ThisBuild := true

lazy val `scalacache-arcus` = Project(id = "scalacache-arcus", base = file("."))
  .settings(commonSettings: _*)
//  .settings(sonatypeSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.6" % Test
    )
  )

lazy val scalacache = Seq(
  "com.github.cb372" %% "scalacache-core" % "0.21.0",
  "com.github.cb372" %% "scalacache-circe" % "0.21.0",
  "com.github.cb372" %% "scalacache-caffeine" % "0.21.0" % Test
)

lazy val arcus = Seq(
  "com.navercorp.arcus" % "arcus-java-client" % "1.9.7" excludeAll (
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j")
  )
)

lazy val scalaTest = Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)
lazy val slf4j = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25"
)

// Dependencies common to all projects
lazy val commonDeps =
  scalacache ++
    arcus ++
    slf4j ++
    scalaTest

lazy val commonSettings =
  Defaults.coreDefaultSettings ++
    mavenSettings ++
    Seq(
      scalaVersion := "2.12.4",
      organization := "com.github.ikhoon",
      crossScalaVersions := Seq("2.11.11", "2.12.4"),
      scalacOptions ++= Seq(
//      "-Xfatal-warnings",
        "-deprecation",
        "-encoding",
        "UTF-8",
        "-feature",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:existentials",
        "-target:jvm-1.8",
        "-Xfuture"
      ),
      resolvers += Resolver.typesafeRepo("releases"),
      libraryDependencies ++= commonDeps,
      parallelExecution in Test := false,
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseCrossBuild := true,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
//        releaseStepCommand("publishSigned"),
        publishArtifacts,
        setNextVersion,
        commitNextVersion,
        releaseStepCommand("sonatypeReleaseAll"),
        pushChanges
      )
//    commands += Command.command("update-version-in-readme")(updateVersionInReadme)
    )

lazy val implProjectSettings = commonSettings

lazy val mavenSettings = Seq(
  pomExtra :=
    <url>https://github.com/ikhoon/scalacache-arcus</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:ikhoon/scalacache-arcus.git</url>
      <connection>scm:git:git@github.com:ikhoon/scalacache-arcus.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ikhoon</id>
        <name>Ikhoon Eom</name>
        <url>https://github.com/ikhoon</url>
      </developer>
    </developers>,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)

def scala211OnlyDeps(moduleIDs: ModuleID*) =
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.11" => moduleIDs
    case other  => Nil
  })
