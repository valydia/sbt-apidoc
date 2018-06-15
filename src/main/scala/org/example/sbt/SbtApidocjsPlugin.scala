package org.example.sbt

import sbt.{IO, Logger, _}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import fastparse.all._

object SbtApidocjsPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport extends SbtApidocjsKeys {

  }

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
    val result = processSources(sourceFiles, log) match {
      case Some((apiData, apiProject)) => Some(generateApidoc(apiData, apiProject, apidocOutputDir.value, log))
      case _ => None
    }
    log.info("Done.")
    result
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

  private def processSources(sourceFiles: List[File], log: Logger): Option[(String, String)] = {
    log.info(s"Here are the file${ if (sourceFiles.nonEmpty) "s" else "" }")
    sourceFiles.foreach { f =>
      log.info("Name " + f.getName)
      processFileContent(IO.read(f), log)
    }
    Some("", "")
  }

  private def processFileContent(file: String, log: Logger): Unit = {
    log.debug("Content: ")
    val comment = parseCommentBlocks(file)
    log.info(s"Comment: $comment")
  }

  val commentChunk: Parser[Any] = P( CharsWhile(c => c != '/' && c != '*') | multilineComment | !"*/" ~ AnyChar )
  lazy val multilineComment: Parser[String] = P( "/**" ~ "\n".? ~ " ".rep() ~ "* " ~/ commentChunk.rep.! ~ "*/" )
  val commentBlock: Parser[String] = P( (!"/**" ~ AnyChar).rep ~ multilineComment ~ (!"/**" ~ AnyChar).rep )
  val commentBlocks: Parser[Seq[String]] = commentBlock.rep

  private[sbt] def parseCommentBlocks(file: String): Seq[String] = {

    def onSuccess(comments: Seq[String], index: Int): Seq[String] = {
      //TODO use another parser?
      comments.map(_.dropRight(3).replace("\n  * ","\n"))
    }

    commentBlocks.parse(file).fold(
      (_, _, _) => Seq(),
      onSuccess
    )
  }

  val identifier: Parser[String]  = P( (!" " ~ AnyChar).rep.! )
  val argument: Parser[String]  = P( (!"\n" ~ AnyChar).rep.! )
  val prefix: Parser[Unit] = P( (!"@" ~ AnyChar).rep )
  val elementParser: Parser[Element] = P( prefix ~ ("@" ~ identifier ~ " " ~ argument)).map{case (id, arg) => Element(s"@$id $arg", id.toLowerCase, id, arg) }
  val elements: Parser[Seq[Element]] = elementParser.rep

  case class Element(source: String, name: String, sourceName: String, content: String)

  private[sbt] def parseElement(block: String): Seq[Element] = {
    elements.parse(block).fold(
      (_, _, _) => Seq(),
        (elements, _) => elements
    )
  }
  import ujson.Js

  val api: Parser[(Option[String], String, Option[String])] = P( ("{" ~ (!"}" ~ AnyChar).rep.! ~ "}").? ~ " " ~ (!" " ~ AnyChar).rep.! ~ (" " ~ AnyChar.rep.!).?)
  private[sbt] def apiParse(content: String): Option[Js.Obj] = {
    api.parse(content).fold(
      (_, _, _) => None,
      (result, _) => {
        val json: Js.Obj = Js.Obj("local" -> Js.Obj("type" -> result._1.getOrElse(""), "url" -> result._2, "title" -> result._3.fold(Js.Null: Js.Value)(Js.Str.apply)))
        Option(json)
      }
    )
  }

  val apiDefine: Parser[(String, Option[String], Option[String])] = P( (!" " ~ AnyChar).rep.! ~ (" " ~ (!"\n" ~ AnyChar).rep.!).? ~ ("\n" ~ AnyChar.rep.!).? )
  private[sbt] def apiDefineParse(content: String): Option[Js.Obj] = {
    apiDefine.parse(content).fold(
      (_, _, _) => None,
      (result, _) => {
          val json = Js.Obj( "global" -> Js.Obj( "define" -> Js.Obj( "name" -> result._1, "title" -> result._2.fold(Js.Null: Js.Value)(Js.Str.apply), "description" -> result._3.fold(Js.Null: Js.Value)(Js.Str.apply))))
          Option(json)
      }
    )
  }


  private[sbt] def apiDescription(content: String): Option[Js.Obj] = {
    val description = trim(content)
    println("hello -- " + description)
    if (description.isEmpty)
      None
    else
      Option(Js.Obj("local" -> Js.Obj("description" -> description)))
  }

  val trim: Parser[String] = P( CharIn("\r\n\t\f ").rep.? ~ (!(CharIn("\r\n\t\f ").rep ~ End) ~ AnyChar).rep.!)
  def trim(str: String): String = {
    trim.parse(str).fold(
      (_, _, _) => str,
      (result, _) => result
    )
  }

}