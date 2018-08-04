import scalariform.formatter.preferences._

name := """sbt-apidoc"""
organization := "com.culpin.team"
version := "0.5.3-SNAPSHOT"

sbtPlugin := true

scalacOptions ++= Seq("-deprecation", "-feature")
licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))


libraryDependencies ++= Seq(
  "com.lihaoyi"           %%     "ujson"                  %    "0.6.6",
  "com.gilt"              %%     "gfc-semver"             %    "0.0.5",
  "com.vladsch.flexmark"  %     "flexmark-html-parser"    %    "0.34.12",
  "org.scalatest"         %%     "scalatest"              %    "3.0.5"   % "test"
)

//bintrayPackageLabels := Seq("sbt","plugin")
//bintrayVcsUrl := Some("""git@github.com:valydia/sbt-apidoc.git""")

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scriptedBufferLog := false

//Scalariform
scalariformPreferences := scalariformPreferences.value
