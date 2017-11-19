import sbtrelease.ReleaseStateTransformations._

import scala.language.postfixOps
import scalariform.formatter.preferences._


val ScalaVersion = "2.11.8"

lazy val `scalacache-arcus` = Project(id = "scalacache-arcus", base = file("."))
  .settings(commonSettings: _*)
//  .settings(sonatypeSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.6" % Test
    )
  )

lazy val scalacache = Seq(
  "com.github.cb372" %% "scalacache-core" % "0.9.3",
  "com.github.cb372" %% "scalacache-caffeine" % "0.9.3" % Test
)

lazy val arcus = Seq(
  "com.navercorp.arcus" % "arcus-java-client" % "1.9.7" excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j")
  )
)

lazy val scalaTest = Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)
lazy val slf4j = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.21"
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
  scalariformSettings ++
  formatterPrefs ++
  Seq(
    organization := "com.github.ikhoon",
    scalaVersion := ScalaVersion,
    crossScalaVersions := Seq(ScalaVersion, "2.12.0"),
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
      updateVersionInReadme,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
//      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    commands += Command.command("update-version-in-readme")(updateVersionInReadme)
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

// Scalariform preferences
lazy val formatterPrefs = Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignParameters, true)
    .setPreference(DoubleIndentClassDeclaration, true)
)

lazy val updateVersionInReadme = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val projectVersion = extracted.get(Keys.version)

  println(s"Updating project version to $projectVersion in the README")
  Process(Seq("sed", "-i", "", "-E", "-e", s"""s/"scalacache-(.*)" % ".*"/"scalacache-\\1" % "$projectVersion"/g""", "README.md")).!
  println("Committing README.md")
  Process(Seq("git", "commit", "README.md", "-m", s"Update project version in README to $projectVersion")).!

  st
})

def scala211OnlyDeps(moduleIDs: ModuleID*) =
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.11" => moduleIDs
    case other => Nil
  })
