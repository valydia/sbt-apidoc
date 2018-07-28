package com.culpin.team.sbt

import com.culpin.team.sbt.parser.Parser
import com.culpin.team.sbt.worker.Worker
import sbt.Keys.{ name, version, _ }
import sbt.plugins.JvmPlugin
import sbt.{ IO, Logger, _ }
import ujson.Js

case class Config(
  name: String,
  title: String,
  description: String,
  version: String,
  url: Option[String],
  sampleUrl: Option[String])

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
    apidocSampleURL := None)

  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(apidocSetting)

  def apidocSetting: Setting[_] = apidoc := {

    val log = streams.value.log

    val config = Config(
      apidocName.value,
      apidocTitle.value,
      apidocDescription.value,
      apidocVersion.value.getOrElse("0.0.0"),
      apidocURL.value.map(_.toString),
      apidocSampleURL.value.map(_.toString))

    val sourceFiles = (sources in Compile).value.toList
    val result =
      run(sourceFiles, config, log) map {
        case (apiData, apiProject) =>
          generateApidoc(apiData, apiProject, apidocOutputDir.value, log)
      }
    result.fold(log.info("Nothing to do."))(_ => log.info("Done."))
    result
  }

  def run(sourceFiles: List[File], apidocConfig: Config, log: Logger): Option[(String, String)] = {
    val (parsedFiles, filenames) = Parser(sourceFiles, log)
    val processedFiles = Worker(parsedFiles, filenames, apidocConfig.sampleUrl)
    val sortedFiles = Util.sortBlocks(filter(processedFiles))
    if (sortedFiles.arr.isEmpty || sortedFiles.arr.forall(_ == Js.Null)) None
    else {

      val config = Js.Obj(
        "name" -> apidocConfig.name,
        "title" -> apidocConfig.title,
        "description" -> apidocConfig.description,
        "version" -> apidocConfig.version,
        "url" -> apidocConfig.url.fold("")(identity),
        "sampleUrl" -> apidocConfig.sampleUrl.fold("")(identity),
        "template" -> Js.Obj(
          "withCompare" -> true,
          "withGenerator" -> true))
      Some((sortedFiles.render(2), config.render(2)))
    }
  }

  def filter(parsedFiles: Js.Arr): Js.Arr = {
    import scala.collection.mutable
    Js.Arr(parsedFiles.arr flatMap {
      case Js.Arr(parsedFile) =>
        parsedFile.collect {
          case block if block("global").obj.isEmpty && block("local").obj.nonEmpty => block("local")
        }

      case _ => mutable.ArrayBuffer[Js.Value]()
    })
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
    IO.write(apidocOutput / "api_data.json", apiData + "\n")

    log.info(s"write js file: $relativePath/api_data.js")
    IO.write(apidocOutput / "api_data.js", "define({ \"api\":  " + apiData + "  });" + "\n")

    log.info(s"write json file: $relativePath/api_project.json")
    IO.write(apidocOutput / "api_project.json", apiProject + "\n")

    log.info(s"write js file: $relativePath/api_project.js")
    IO.write(apidocOutput / "api_project.js", "define( " + apiProject + " );" + "\n")

    log.info("Generated output into folder " + relativePath)
    apidocOutput
  }

}