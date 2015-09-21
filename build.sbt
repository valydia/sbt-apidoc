import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._


lazy val buildSettings = Seq(
  version := "0.4-SNAPSHOT",
  organization := "com.culpin.team",
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/")),
  scalaVersion := "2.10.4",
  scalacOptions := Seq("-deprecation", "-unchecked", "-feature"),
  crossScalaVersions := Seq("2.9.2", "2.10.0", "2.11.7"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)



lazy val root = (project in file(".")).aggregate(core, sbtapidocjs).
  settings(buildSettings: _*).
  settings(scalariformSettings: _*).
  settings(
    name := "root"
  )

lazy val core = (project in file("core")).
  settings(buildSettings: _*).
  settings(
    name := "core",
    libraryDependencies ++= coreDependencies
  )

lazy val sbtapidocjs = (project in file("sbt-apidocjs")).
  settings(buildSettings: _*).
  settings(scriptedSettings:_*).
  settings(
    name := "sbt-apidocjs",
    sbtPlugin := true,
    scriptedLaunchOpts += "-Dproject.version=" + version.value
  ).
  dependsOn(core)




lazy val json4s_version = "3.2.11"

lazy val coreDependencies = Seq (
 "org.json4s" %% "json4s-native" % json4s_version,
 "org.json4s" %% "json4s-jackson" % json4s_version,
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test"
)







