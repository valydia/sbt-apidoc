package com.github.valydia.sbt

import sbt._

trait SbtApidocKeys {
  lazy val apidoc =
    taskKey[Option[File]]("Generates APIDOC plugin documentation")
  lazy val apidocOutputDir =
    settingKey[File]("Location where to put the generated documentation")
  lazy val apidocName = settingKey[String]("Name of your project, by default uses the name setting key")
  lazy val apidocTitle = settingKey[Option[String]]("Browser title text.")
  lazy val apidocDescription = settingKey[String]("Introduction of your project")
  lazy val apidocURL = settingKey[Option[String]]("If set, a form to test an api method (send a request) will be visible.")
  lazy val apidocSampleURL = settingKey[Option[String]](
    "If set, a form to test an api method (send a request) will be visible")
  lazy val apidocVersion =
    settingKey[Option[String]]("Version of your project.")
  lazy val apidocVersionFile =
    settingKey[File]("File/Folder to keep track of the old api")
  lazy val apidocHeaderTitle =
    settingKey[Option[String]]("Navigation text for the included Header file")
  lazy val apidocHeaderFile =
    settingKey[Option[File]]("Filename (markdown-file) for the included Header file.")
  lazy val apidocFooterTitle =
    settingKey[Option[String]]("Navigation text for the included Footer file")
  lazy val apidocFooterFile =
    settingKey[Option[File]]("Filename (markdown-file) for the included Footer file")
  lazy val apidocOrder = settingKey[List[String]]("A list of api-names / group-names for ordering the output. Not defined names are automatically displayed last.")
  lazy val apidocTemplateCompare = settingKey[Option[Boolean]]("Enable comparison with older api versions.")
  lazy val apidocTemplateGenerator = settingKey[Option[Boolean]]("Output the generator information at the footer.")
}
