package com.culpin.team.sbt

import sbt._

trait SbtApidocKeys {
  lazy val apidoc = taskKey[Option[File]]("Generates APIDOC plugin documentation")
  lazy val apidocOutputDir = settingKey[File]("The output directory of the apidoc")
  lazy val apidocName = settingKey[String]("The Name of the API")
  lazy val apidocTitle = settingKey[String]("Browser title text.")
  lazy val apidocDescription = settingKey[String]("The Description of the API")
  lazy val apidocURL = settingKey[Option[URL]]("The url of the API")
  lazy val apidocSampleURL = settingKey[Option[URL]]("If set, a form to test an api method (send a request) will be visible")
  lazy val apidocVersion = settingKey[Option[String]]("The default version of the API")
}
