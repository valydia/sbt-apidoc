package com.culpin.team.sbt

import com.culpin.team.sbt.parser.Parser
import com.culpin.team.sbt.worker.Worker
import sbt.Keys.{name, version, _}
import sbt.plugins.JvmPlugin
import sbt.{IO, Logger, _}
import ujson.Js

import scala.collection.mutable

case class Config(name: String,
                  title: Option[String],
                  description: String,
                  defaultVersion: String,
                  url: Option[String],
                  sampleUrl: Option[String],
                  headerTitle: Option[String],
                  headerFile: Option[File],
                  footerTitle: Option[String],
                  footerFile: Option[File]
                 )

object SbtApidoc extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport extends SbtApidocKeys

  import autoImport._

  lazy val defaultSettings = List(
    apidocName := name.value,
    apidocTitle := None,
    apidocVersion := Option(version.value),
    apidocOutputDir := target.value / "apidoc",
    apidocDescription := "",
    apidocURL := None,
    apidocSampleURL := None,
    apidocHeaderTitle := None,
    apidocHeaderFile := None,
    apidocFooterTitle := None,
    apidocFooterFile := None
  )

  override lazy val projectSettings: Seq[Setting[_]] = defaultSettings ++ Seq(
    apidocSetting)

  type RelativeFilename = String

  def apidocSetting: Setting[_] = apidoc := {

    val log = streams.value.log

    val config = Config(
      apidocName.value,
      apidocTitle.value,
      apidocDescription.value,
      Option(version.value).getOrElse("0.0.0"), // TODO should it be apidocVersion ??
      apidocURL.value,
      apidocSampleURL.value,
      apidocHeaderTitle.value,
      apidocHeaderFile.value,
      apidocFooterTitle.value,
      apidocFooterFile.value
    )

    val projectDirectory = baseDirectory.value.getAbsolutePath
    val sourceFileAndName =
      (sources in Compile).value.toList map { f =>
       f -> f.getAbsolutePath.replaceFirst(projectDirectory, ".")
      }
    val result =
      run(sourceFileAndName, config, log) map {
        case (apiData, apiProject) =>
          generateApidoc(apiData, apiProject, apidocOutputDir.value, log)
      }
    result.fold(log.info("Nothing to do."))(_ => log.info("Done."))
    result
  }

  def run(sourceFileAndName: List[(File, RelativeFilename)],
          apidocConfig: Config,
          log: Logger): Option[(String, String)] = {
    val (parsedFiles, filenames) = Parser(sourceFileAndName, log)
    val processedFiles = Worker(parsedFiles, filenames, apidocConfig.sampleUrl)
    val sortedFiles = Util.sortBlocks(filter(processedFiles))
    if (sortedFiles.arr.isEmpty || sortedFiles.arr.forall(_ == Js.Null)) None
    else {
      //For some reason, the default value is set to false
      val sampleUrl: Js.Value =
        apidocConfig.sampleUrl.fold(Js.Bool(false): Js.Value)(Js.Str.apply)

      val generator = Js.Obj(
        "name" -> "sbt-apidoc",
        "time" -> java.time.LocalDateTime.now().toString,
        "url" -> "https://github.com/valydia/sbt-apidoc",
        "version" -> "0.17.6"
      )

      val map =
        mutable.LinkedHashMap[String, Js.Value](
          "name" -> Js.Str(apidocConfig.name),
          "version" -> Js.Str(apidocConfig.defaultVersion),
          "description" -> Js.Str(apidocConfig.description),
          "sampleUrl" -> sampleUrl,
          "defaultVersion" -> Js.Str(apidocConfig.defaultVersion),
          "apidoc" -> Js.Str("0.3.0") // see SPECIFICATION_VERSION
        )

      apidocConfig.title.foreach(t => map.put("title", Js.Str(t)))
      apidocConfig.url.foreach(v => map.put("url", Js.Str(v)))
      apidocConfig.headerFile.filter(_.exists()).foreach { h =>
        map.put("header",
          apidocConfig.headerTitle.fold(
            Js.Obj("content" -> Util.renderMarkDown(IO.read(h)))
          ){ title =>
            Js.Obj(
              "content" -> Util.renderMarkDown(IO.read(h)),
              "title" -> title
            )
          }
        )
      }
      apidocConfig.footerFile.filter(_.exists()).foreach { f =>
        map.put("footer",
          apidocConfig.footerTitle.fold(
            Js.Obj("content" -> Util.renderMarkDown(IO.read(f)))
          ){ title =>
            Js.Obj(
              "content" -> Util.renderMarkDown(IO.read(f)),
              "title" -> title
            )
          }
        )
      }
      map.put("generator", generator)
      val config = Js.Obj(map)

      Some((sortedFiles.render(2), config.render(2)))
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
                     log: Logger): File = {

    val relativePath = apidocOutput.getParentFile.getName + "/" + apidocOutput.getName

    log.info(s"copy template to $relativePath")

    val templateStream =
      getClass.getClassLoader.getResourceAsStream("template.zip")

    val tmp = IO.createTemporaryDirectory
    IO.unzipStream(templateStream, tmp)
    val template = tmp / "template"
    val files = template.listFiles() zip template.list().map(apidocOutput / _)
    IO.move(files)
    IO.delete(tmp)

    log.info(s"write json file: $relativePath/api_data.json")
    IO.write(apidocOutput / "api_data.json", apiData + "\n")

    log.info(s"write js file: $relativePath/api_data.js")
    IO.write(apidocOutput / "api_data.js",
             "define({ \"api\":  " + apiData + "  });" + "\n")

    log.info(s"write json file: $relativePath/api_project.json")
    IO.write(apidocOutput / "api_project.json", apiProject + "\n")

    log.info(s"write js file: $relativePath/api_project.js")
    IO.write(apidocOutput / "api_project.js",
             "define( " + apiProject + " );" + "\n")

    log.info("Generated output into folder " + relativePath)
    apidocOutput
  }

}
