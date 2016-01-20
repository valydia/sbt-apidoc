import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._



sbtPlugin := true

organization := "com.culpin.team"

name := """sbt-apidoc"""

version := "0.5"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

scalacOptions ++= Seq("-deprecation", "-feature")

licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))




val json4s_version = "3.3.0"


libraryDependencies ++= Seq(
  "org.json4s"         %% "json4s-native"    % json4s_version,
  "com.gilt"           %% "gfc-semver"       % "0.1.0",
  "org.scalatest"      %% "scalatest"        % "2.2.4"   % "test",
  "org.mockito"        %  "mockito-core"     % "1.8.5"   % "test"
)

dependencyOverrides += "org.json4s"  %% "json4s-native" % json4s_version


resolvers += Resolver.bintrayRepo("giltgroupe", "maven")



//Scalariform
scalariformSettings









