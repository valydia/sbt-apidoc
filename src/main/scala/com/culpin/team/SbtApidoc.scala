package com.culpin.team

import com.culpin.team.core.Apidoc
import com.culpin.team.core.SbtApidocConfiguration
import sbt.Keys._
import sbt._

import scala.util.{ Success, Failure }

/**
 * This plugin helps you which operating systems are awesome
 */
object SbtApidoc extends AutoPlugin {

  /**
   * Defines all settings/tasks that get automatically imported,
   * when the plugin is enabled
   */
  object autoImport extends SbtApidocKeys {

  }

  import com.culpin.team.SbtApidoc.autoImport._

  lazy val defaultSettings = List(
    apidocName := name.value,
    apidocTitle := apidocName.value,
    apidocVersion := Option(version.value),
    apidocOutputDir := target.value / "apidoc",
    apidocDescription := "",
    apidocURL := None,
    apidocSampleURL := None
  )

  override def trigger = allRequirements

  /**
   * Provide default settings
   */
  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(apidocSetting)

  def apidocSetting: Setting[_] = apidoc := {

    val log = streams.value.log

    //getting the source files
    val sourcesFiles = (sources in Compile).value.toList

    val config = SbtApidocConfiguration(
      apidocName.value,
      apidocDescription.value,
      apidocSampleURL.value.map(_.toString),
      apidocVersion.value.getOrElse("1.0.0")
    )

    val parseResult = Apidoc(sourcesFiles, config, log)
    val maybeFolder = parseResult match {
      case Success(Some((apiData, apiProject))) => Some(generateApidoc(apiData, apiProject, apidocOutputDir.value, log))
      case Success(None) => None
      case Failure(ex) => log.error(ex.getMessage); None
    }

    log.info("Done.")
    maybeFolder
  }

  def generateApidoc(apiData: String, apiProject: String, apidocOutput: File, log: Logger): File = {

    val relativePath = apidocOutput.getParentFile.getName + "/" + apidocOutput.getName

    log.info(s"copy template to $relativePath")

    val templateStream = getClass.getClassLoader.getResourceAsStream("template.zip")

    val tmp = IO.createTemporaryDirectory
    IO.unzipStream(templateStream, tmp)
    val template = (tmp / "template")
    val files = template.listFiles() zip template.list().map(apidocOutput / _)
    IO.move(files)
    IO.delete(tmp)

    log.info(s"write json file: $relativePath/api_data.json")
    IO.write(apidocOutput / "api_data.json", apiData)

    log.info(s"write js file: $relativePath/api_data.js")
    IO.write(apidocOutput / "api_data.js", "define({ \"api\":  " + apiData + "  })")

    log.info(s"write json file: $relativePath/api_project.json")
    IO.write(apidocOutput / "api_project.json", apiProject)

    log.info(s"write js file: $relativePath/api_project.js")
    IO.write(apidocOutput / "api_project.js", "define({ \"api\":  " + apiProject + "  })")

    log.info("Generated output into folder " + relativePath)
    apidocOutput
  }

}