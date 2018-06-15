package org.example.sbt

import sbt.{Logger, _}
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
    processSources(sourceFiles, log)
    None
  }

  private def processSources(sourceFiles: List[File], log: Logger): Unit = {
    log.info(s"Here are the file${ if (sourceFiles.nonEmpty) "s" else "" }")
    sourceFiles.foreach { f =>
      log.info("Name " + f.getName)
      processFileContent(IO.read(f), log)
    }
  }

  private def processFileContent(file: String, log: Logger): Unit = {
    log.info("Content: ")
    log.info(file)
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

  val apiParser: Parser[(Option[String], String, String)] = P( ("{" ~ (!"}" ~ AnyChar).rep.! ~ "}").? ~ " " ~ (!" " ~ AnyChar).rep.! ~ " " ~ AnyChar.rep.!)
  private[sbt] def apiParser(content: String): Option[Js.Obj] = {
    apiParser.parse(content).fold(
      (_, _, _) => None,
      (result, _) => {
        val json: Js.Obj = Js.Obj("local" -> Js.Obj("type" -> result._1.getOrElse(""), "url" -> result._2, "title" -> result._3))
        Option(json)
      }
    )
  }
}