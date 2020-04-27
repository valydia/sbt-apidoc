package com.culpin.team.sbt

import com.culpin.team.sbt.parser.Parser
import com.culpin.team.sbt.worker.Worker
import sbt.Keys.{name, version, _}
import sbt.plugins.JvmPlugin
import sbt.{IO, Logger, _}
import ujson.Js

case class Config(name: String,
                  title: Option[String],
                  description: String,
                  version: String,
                  projectVersion: String,
                  versionFile: File,
                  url: Option[String],
                  sampleUrl: Option[String],
                  headerTitle: Option[String],
                  headerFile: Option[File],
                  footerTitle: Option[String],
                  footerFile: Option[File],
                  order: List[String],
                  templateCompare: Option[Boolean],
                  templateGenerator: Option[Boolean]
                 )

object SbtApidoc extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport extends SbtApidocKeys

  import autoImport._

  lazy val defaultSettings = List(
    apidocName := name.value,
    apidocTitle := None,
    apidocVersion := None,
    apidocVersionFile := (resourceDirectory in Compile).value / "apidoc",
    apidocOutputDir := crossTarget.value / "apidoc",
    apidocDescription := description.value,
    apidocURL := None,
    apidocSampleURL := None,
    apidocHeaderTitle := None,
    apidocHeaderFile := None,
    apidocFooterTitle := None,
    apidocFooterFile := None,
    apidocOrder := List(),
    apidocTemplateCompare := None,
    apidocTemplateGenerator := None
  )

  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(
    apidocSetting)

  type RelativeFilename = String

  def apidocSetting: Setting[_] = apidoc := {

    val log = streams.value.log
    // This project version
    val projectVersion = "0.5.4"
    val config = Config(
      apidocName.value,
      apidocTitle.value,
      apidocDescription.value,
      Util.defaultVersion(apidocVersion.value, version.value, log),
      projectVersion,
      apidocVersionFile.value,
      apidocURL.value,
      apidocSampleURL.value,
      apidocHeaderTitle.value,
      apidocHeaderFile.value,
      apidocFooterTitle.value,
      apidocFooterFile.value,
      apidocOrder.value,
      apidocTemplateCompare.value,
      apidocTemplateGenerator.value
    )

    val projectDirectory = baseDirectory.value
    val apidocFile = apidocVersionFile.value
    val versionFiles =
      if (apidocFile.exists()) {
        if (apidocFile.isDirectory)
          IO.listFiles(apidocFile).toList
        else
          List(apidocFile)
      } else Nil
    val inputFileAndName =
      ((sources in Compile).value.toList ++ versionFiles) map { f =>
       f -> relativePath(projectDirectory, f)
      }
    val result =
      run(inputFileAndName, config, log) map {
        case (apiData, apiProject) =>
          generateApidoc(apiData, apiProject, apidocOutputDir.value, relativePath(projectDirectory, apidocOutputDir.value), log)
      }
    result.fold(log.info("Nothing to do."))(_ => log.info("Done."))
    result
  }

  private def relativePath(baseDirectory: File, file: File): RelativeFilename = {
    file.getAbsolutePath.replaceFirst(baseDirectory.getAbsolutePath, ".")
  }

  def run(sourceFileAndName: List[(File, RelativeFilename)],
          apidocConfig: Config,
          log: Logger): Option[(String, String)] = {
    val (parsedFiles, filenames) = Parser(sourceFileAndName, log)
    val processedFiles = Worker(parsedFiles, filenames, apidocConfig.sampleUrl)
    val sortedFiles = Util.sortBlocks(filter(processedFiles))
    if (sortedFiles.arr.isEmpty || sortedFiles.arr.forall(_ == Js.Null)) None
    else {
      val config = buildConfig(apidocConfig, log)
      Some((sortedFiles.render(2), config.render(2)))
    }
  }

  private def buildConfig(apidocConfig: Config, log: Logger): Js.Obj = {

    // This strange behaviour is due to the original library (in JS)
    val sampleUrl: Js.Value =
      apidocConfig.sampleUrl.fold(Js.Bool(false): Js.Value)(Js.Str.apply)

    val template = Option(Util.buildObj(
      "withCompare" -> apidocConfig.templateCompare.map(Js.Bool.apply),
      "withGenerator" -> apidocConfig.templateGenerator.map(Js.Bool.apply)
    )).filter(_.value.nonEmpty)

    val header = buildHeaderFooter(apidocConfig.headerFile, apidocConfig.headerTitle, log)

    val footer = buildHeaderFooter(apidocConfig.footerFile, apidocConfig.footerTitle, log)
    val order =
      if (apidocConfig.order.nonEmpty)
        Some(Js.Arr.from(apidocConfig.order))
      else
        None

    val generator = Js.Obj(
      "name" -> "sbt-apidoc",
      "time" -> java.time.LocalDateTime.now().toString,
      "url" -> "https://github.com/valydia/sbt-apidoc",
      "version" -> apidocConfig.projectVersion
    )

    Util.buildObj(
      "name" -> Some(Js.Str(apidocConfig.name)),
      "version" -> Some(Js.Str(apidocConfig.version)),
      "description" -> Some(Js.Str(apidocConfig.description)),
      "title" -> apidocConfig.title.map(Js.Str.apply),
      "template" -> template,
      "url" -> apidocConfig.url.map(Js.Str.apply),
      "sampleUrl" -> Some(sampleUrl),
      "header" -> header,
      "footer" -> footer,
      "order" -> order,
      "generator" -> Some(generator)
    )
  }

  private def buildHeaderFooter(file: Option[File], title: Option[String], log: Logger): Option[Js.Obj] = {
    file.flatMap { file =>
      if (file.exists()){
        Some(
          Util.buildObj(
            "title" -> title.map(Js.Str.apply),
            "content" -> Some(Js.Str(Util.renderMarkDown(IO.read(file))))
          )
        )
      }
      else {
        log.warn(s"The file ${file.getAbsoluteFile} doesn't exist")
        None
      }
    }
  }


  def filter(parsedFiles: Js.Arr): Js.Arr = {
    import scala.collection.mutable
    Js.Arr(parsedFiles.arr flatMap {
      case Js.Arr(parsedFile) =>
        parsedFile.collect {
          case block
              if block("global").obj.isEmpty && block("local").obj.nonEmpty =>
            block("local")
        }
      case _ => mutable.ArrayBuffer[Js.Value]()
    })
  }

  def generateApidoc(apiData: String,
                     apiProject: String,
                     apidocOutput: File,
                     apidocOutputRelativePath: String,
                     log: Logger): File = {
    log.info(s"copy template to $apidocOutputRelativePath")

    val templateStream =
      getClass.getClassLoader.getResourceAsStream("template.zip")

    val tmp = IO.createTemporaryDirectory
    IO.unzipStream(templateStream, tmp)
    val template = tmp / "template"
    val files = template.listFiles() zip template.list().map(apidocOutput / _)
    IO.move(files)
    IO.delete(tmp)

    log.info(s"write json file: $apidocOutputRelativePath/api_data.json")
    IO.write(apidocOutput / "api_data.json", apiData + "\n")

    log.info(s"write js file: $apidocOutputRelativePath/api_data.js")
    IO.write(apidocOutput / "api_data.js",
             "define({ \"api\":  " + apiData + "  });" + "\n")

    log.info(s"write json file: $apidocOutputRelativePath/api_project.json")
    IO.write(apidocOutput / "api_project.json", apiProject + "\n")

    log.info(s"write js file: $apidocOutputRelativePath/api_project.js")
    IO.write(apidocOutput / "api_project.js",
             "define( " + apiProject + " );" + "\n")

    log.info("Generated output into folder " + apidocOutputRelativePath)
    apidocOutput
  }

}
