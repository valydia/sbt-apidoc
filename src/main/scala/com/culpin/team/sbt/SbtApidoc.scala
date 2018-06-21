package com.culpin.team.sbt

import com.culpin.team.sbt.parser.Parser

import sbt.Keys.{name, version, _}
import sbt.plugins.JvmPlugin
import sbt.{IO, Logger, _}


object SbtApidoc extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport extends SbtApidocKeys

  import autoImport._

  lazy val defaultSettings = List(
    apidocName := name.value,
    apidocTitle := apidocName.value,
    apidocVersion := Option(version.value),
    apidocOutputDir := target.value / "apidoc",
    apidocDescription := "",
    apidocURL := None,
    apidocSampleURL := None
  )

  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(apidocSetting)

  def apidocSetting: Setting[_] = apidoc := {
    //getting the source files
    val log = streams.value.log
    val sourceFiles = (sources in Compile).value.toList
    val result = run(sourceFiles, log) match {
      case Some((apiData, apiProject)) => Some(generateApidoc(apiData, apiProject, apidocOutputDir.value, log))
      case _ => None
    }
    log.info("Done.")
    result
  }

  def run(sourceFiles: List[File], log: Logger): Option[(String, String)] = {
     val (blocks, filename) = Parser(sourceFiles, log)

//    blocks map { b =>
//
//    }
    Some(("",""))
  }

  def generateApidoc(apiData: String, apiProject: String, apidocOutput: File, log: Logger): File = {

    val relativePath = apidocOutput.getParentFile.getName + "/" + apidocOutput.getName

    log.info(s"copy template to $relativePath")

    val templateStream = getClass.getClassLoader.getResourceAsStream("template.zip")

    val tmp = IO.createTemporaryDirectory
    IO.unzipStream(templateStream, tmp)
    val template = tmp / "template"
    val files = template.listFiles() zip template.list().map(apidocOutput / _)
    IO.move(files)
    IO.delete(tmp)

    log.info(s"write json file: $relativePath/api_data.json")
    IO.write(apidocOutput / "api_data.json", apiData)

    log.info(s"write js file: $relativePath/api_data.js")
    IO.write(apidocOutput / "api_data.js", "define({ \"api\":  " + apiData + "  });")

    log.info(s"write json file: $relativePath/api_project.json")
    IO.write(apidocOutput / "api_project.json", apiProject)

    log.info(s"write js file: $relativePath/api_project.js")
    IO.write(apidocOutput / "api_project.js", "define( " + apiProject + " );")

    log.info("Generated output into folder " + relativePath)
    apidocOutput
  }



}