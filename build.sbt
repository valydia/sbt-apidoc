import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._


sbtPlugin := true

organization := "com.culpin.team"

name := """sbt-apidoc"""

version := "0.5-SNAPSHOT"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

ivyLoggingLevel := UpdateLogging.Quiet

scalacOptions ++= Seq("-deprecation", "-feature")

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))


val json4s_version = "3.3.0"
//val json4s_version = "3.2.9"


libraryDependencies ++= Seq(
  "org.json4s"         %% "json4s-native"    % json4s_version,
  "com.gilt"           %% "gfc-semver"       % "0.1.0",
  "org.scalatest"      %% "scalatest"        % "2.2.4"   % "test",
  "org.mockito"        %  "mockito-core"     % "1.8.5"   % "test"
)

dependencyOverrides += "org.json4s"  %% "json4s-native" % json4s_version


resolvers += Resolver.bintrayRepo("giltgroupe", "maven")

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value, "-Dproject.version=" + version.value)
}

//scriptedBufferLog := false

//Scalariform
scalariformSettings









