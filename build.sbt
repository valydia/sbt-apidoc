import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._


sbtPlugin := true

//Change to your organization
organization := "com.culpin.team"

//Change to your plugin name
name := """sbt-apidoc"""

//Change to the version
version := "0.5-SNAPSHOT"

scalaVersion := "2.10.4"
//scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.9.2", "2.10.0", "2.11.7")

scalacOptions ++= Seq("-deprecation", "-feature")

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))

resolvers += Resolver.sonatypeRepo("snapshots")

val json4s_version = "3.2.11"

libraryDependencies ++= Seq(
  "org.json4s"         %% "json4s-native"    % json4s_version,
  "org.json4s"         %% "json4s-jackson"   % json4s_version,
  "org.scalatest"      %% "scalatest"        % "2.2.4"   % "test",
  "org.mockito"        %  "mockito-core"     % "1.8.5"   % "test"
)

// Scripted - sbt plugin tests
scriptedSettings

//scriptedLaunchOpts += "-Dproject.version=" + version.value
ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value, "-Dproject.version=" + version.value)
}

scriptedBufferLog := false

//Scalariform
scalariformSettings









