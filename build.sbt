sbtPlugin := true

//Change to your organization
organization := "com.culpin.team"

//Change to your plugin name
name := """sbt-apidoc"""

//Change to the version
version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.json4s"         %% "json4s-native"    % "3.2.11",
  "org.json4s"         %% "json4s-jackson"   % "3.2.11",
  "org.scalatest"      %% "scalatest"        % "2.2.4"   % "test",
  "org.mockito"        %  "mockito-core"     % "1.8.5"   % "test"
)


// Scripted - sbt plugin tests
scriptedSettings

scriptedLaunchOpts += "-Dproject.version=" + version.value

//Scalariform
scalariformSettings

