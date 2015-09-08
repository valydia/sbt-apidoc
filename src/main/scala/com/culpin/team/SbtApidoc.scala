package com.culpin.team

import com.culpin.team.core.Apidoc
import sbt.Keys._
import sbt._

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
    // Sbt provided logger.
    val log = streams.value.log

    log.info("Creating APIDoc")

    //getting the source files
    val sourcesFiles = (sources in Compile).value

    val config = SbtApidocConfiguration(apidocName.value, apidocDescription.value, apidocSampleURL.value.isDefined, apidocVersion.value.getOrElse("1.0.0"))
    Apidoc(sourcesFiles, config, log)
    val outputDir = apidocOutputDir.value / "apidoc.html"
    IO.write(outputDir, "Apidoc")
    log.info("Done.")
    outputDir
  }

}

case class SbtApidocConfiguration(name: String, description: String, sampleUrl: Boolean, version: String)