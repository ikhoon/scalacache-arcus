import sbtrelease.ReleaseStateTransformations._

import scala.language.postfixOps

val ScalaVersion = "2.11.8"

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
  "com.navercorp.arcus" % "arcus-java-client" % "1.9.7" excludeAll(
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
    organization := "com.github.ikhoon",
    crossScalaVersions := Seq("2.11.9", "2.12.3"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers += Resolver.typesafeRepo("releases"),
    libraryDependencies ++= commonDeps,
    parallelExecution in Test := false,
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
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
//      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
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
    val nexus = "TODO-make-a-repo"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "some-snapshot")
    else
      Some("releases"  at nexus + "some-release")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false }
)

def scala211OnlyDeps(moduleIDs: ModuleID*) =
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.11" => moduleIDs
    case other => Nil
  })
