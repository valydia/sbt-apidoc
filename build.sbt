name := """sbt-apidocjs"""
organization := "org.example"
version := "0.1-SNAPSHOT"

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.lihaoyi"         %%     "upickle"        %    "0.6.6",
  "org.scalatest"       %%     "scalatest"      %    "3.0.5"   % "test",
  "org.mockito"         %      "mockito-core"   %    "1.8.5"   % "test"
)

// choose a test framework

// utest
//libraryDependencies += "com.lihaoyi" %% "utest" % "0.4.8" % "test"
//testFrameworks += new TestFramework("utest.runner.Framework")

// ScalaTest
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1" % "test"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

// Specs2
//libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.9.1" % "test")
//scalacOptions in Test ++= Seq("-Yrangepos")

bintrayPackageLabels := Seq("sbt","plugin")
bintrayVcsUrl := Some("""git@github.com:org.example/sbt-apidocjs.git""")

initialCommands in console := """import org.example.sbt._"""

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scriptedBufferLog := false
