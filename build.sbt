name := """sbt-apidoc"""
organization := "com.culpin.team"
version := "0.1-SNAPSHOT"

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.lihaoyi"         %%     "upickle"        %    "0.6.6",
  "org.scalatest"       %%     "scalatest"      %    "3.0.5"   % "test"
)

bintrayPackageLabels := Seq("sbt","plugin")
bintrayVcsUrl := Some("""git@github.com:org.example/sbt-apidocjs.git""")

initialCommands in console := """import org.example.sbt._"""

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scriptedBufferLog := false
