lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization in ThisBuild := "com.culpin.team",
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/valydia/sbt-apidoc"),
      "scm:git:git@github.com:valydia/sbt-apidoc.git"
    )
  ),
  developers := List(
    Developer(
      "valydia",
      "Valy Dia",
      "v.diarrassouba@gmail.com",
      url("https://github.com/valydia")
    )
  ),
  homepage := scmInfo.value.map(_.browseUrl)
)

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(commonSettings)
  .settings(
    sbtPlugin := true,
    name := "sbt-apidoc",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "com.lihaoyi"           %%     "ujson"                      %    "0.6.6",
      "com.lihaoyi"           %%     "fastparse"                  %    "0.4.2",
      "com.gilt"              %%     "gfc-semver"                 %    "0.0.5",
      "com.vladsch.flexmark"  %      "flexmark-html-parser"       %    "0.34.48",
      "com.vladsch.flexmark"  %      "flexmark-ext-typographic"   %    "0.34.48",
      "org.scalatest"         %%     "scalatest"                  %    "3.0.5"   % "test"
    )
  )
