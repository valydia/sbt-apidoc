package com.culpin.team

import com.culpin.team.core.Apidoc
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

  /**
   * Provide default settings
   */
  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(apidocSetting)

  def apidocSetting: Setting[_] = apidoc := {

    val log = streams.value.log

    //getting the source files
    val sourcesFiles = (sources in Compile).value

    val config = SbtApidocConfiguration(apidocName.value, apidocDescription.value, apidocSampleURL.value.isDefined, apidocVersion.value.getOrElse("1.0.0"))

    val parseResult = Apidoc(sourcesFiles, config, log)
    //log.error(parseResult.toString)
    //println("%%%%%%%%%%%%%%%%%%%%%%%" + parseResult)
    val maybeFolder = parseResult match {
      case Success(Some((apiData, apiProject))) => Some(generateApidoc(apiData, apiProject, apidocOutputDir.value, log))
      case Success(None) => None
      case Failure(ex) => None
    }

    log.info("Done.")
    maybeFolder
  }

  def generateApidoc(apiData: String, apiProject: String, target: File, log: Logger): File = {

    val folderName = target.getName
    log.info(s"create dir: $folderName")
    IO.createDirectory(target)
    //FIXME scripted test doesn't seem to get resourceDirectory

    log.info(s"copy template to $folderName")
    val maybeTemplateFolder = Option(new File(getClass.getResource("/template").getFile))
    maybeTemplateFolder.map(IO.copyDirectory(_, target))

    log.info(s"write json file: ${target.getName}/api_data.json")
    IO.write(target / "api_data.json", apiData)

    log.info(s"write js file: ${target.getName}/api_data.js")
    IO.write(target / "api_data.js", "define({ \"api\":  " + apiData + "  })")

    log.info(s"write json file: ${target.getName}/api_project.json")
    IO.write(target / "api_project.json", apiProject)

    log.info(s"write js file: ${target.getName}/api_project.js")
    IO.write(target / "api_project.js", "define({ \"api\":  " + apiProject + "  })")

    target
  }

}

case class SbtApidocConfiguration(name: String, description: String, sampleUrl: Boolean, version: String)