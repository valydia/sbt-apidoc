package org.example.sbt

import sbt.{IO, Logger, _}
import sbt.Keys._
import sbt.plugins.JvmPlugin
import ujson.Js
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

  private val commentChunk: Parser[Any] = P( CharsWhile(c => c != '/' && c != '*') | multilineComment | !"*/" ~ AnyChar )
  private lazy val multilineComment: Parser[String] = P( "/**" ~ "\n".? ~ " ".rep() ~ "* " ~/ commentChunk.rep.! ~ "*/" )
  private val commentBlock: Parser[String] = P( (!"/**" ~ AnyChar).rep ~ multilineComment ~ (!"/**" ~ AnyChar).rep )
  private val commentBlocks: Parser[Seq[String]] = commentBlock.rep

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

  private val identifier: Parser[String]  = P( (!" " ~ AnyChar).rep.! )
  private val argument: Parser[String]  = P( (!"\n" ~ AnyChar).rep.! )
  private val prefix: Parser[Unit] = P( (!"@" ~ AnyChar).rep )
  private val elementParser: Parser[Element] = P( prefix ~ ("@" ~ identifier ~ " " ~ argument)).map{case (id, arg) => Element(s"@$id $arg", id.toLowerCase, id, arg) }
  private val elements: Parser[Seq[Element]] = elementParser.rep

  case class Element(source: String, name: String, sourceName: String, content: String)

  private[sbt] def parseElement(block: String): Seq[Element] = {
    elements.parse(block).fold(
      (_, _, _) => Seq(),
        (elements, _) => elements
    )
  }


  private def toOption(parsed: Parsed[Js.Obj], path: String*): Option[Js.Obj] = {
    parsed.fold(
      (_, _, _) => None,
      (jsObj, _) => Option(path.foldRight(jsObj){ case (key, acc) =>
        Js.Obj(key -> acc)
      })
    )
  }
  private val api: Parser[Js.Obj] =
    P( ("{" ~ (!"}" ~ AnyChar).rep.! ~ "}").? ~ " " ~ (!" " ~ AnyChar).rep.! ~ (" " ~ AnyChar.rep.!).?) map {
      case (t, url, title) =>
        Js.Obj("type" -> t.getOrElse(""), "url" -> url, "title" -> title.fold(Js.Null: Js.Value)(Js.Str.apply))
    }
  private[sbt] def apiParse(content: String): Option[Js.Obj] =
    toOption(api.parse(content),"local")


  private val apiDefine: Parser[Js.Obj] =
    P( (!" " ~ AnyChar).rep.! ~ (" " ~ (!"\n" ~ AnyChar).rep.!).? ~ ("\n" ~ AnyChar.rep.!).? ) map {
      case (name, title, description) =>
        Js.Obj("name" -> name, "title" -> title.fold(Js.Null: Js.Value)(Js.Str.apply), "description" -> description.fold(Js.Null: Js.Value)(s => Js.Str(unindent(s))))
    }
  private[sbt] def apiDefineParse(content: String): Option[Js.Obj] =
    toOption(apiDefine.parse(content), "global", "define")


  private[sbt] def apiDescription(content: String): Option[Js.Obj] = {
    val description = trim(content)
    if (description.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("description" -> description)))
  }

  private val trim: Parser[String] = P( CharIn("\r\n\t\f ").rep.? ~ (!(CharIn("\r\n\t\f ").rep ~ End) ~ AnyChar).rep.!)
  def trim(str: String): String = {
    trim.parse(str).fold(
      (_, _, _) => str,
      (result, _) => result
    )
  }

  private val apiExample: Parser[Js.Obj] =
    P( ("{" ~ (!"}" ~ AnyChar).rep.! ~ "}" ~ " " ).?  ~ (!"\n" ~ AnyChar).rep.! ~ "\n" ~ AnyChar.rep.!) map {
      case (t, title, content) =>
        Js.Obj("type" -> t.fold(Js.Str("json"))(Js.Str.apply), "title" -> title, "content" -> unindent(content))
    }
  private[sbt] def apiExample(content: String): Option[Js.Obj] =
    toOption(apiExample.parse(trim(content)), "local", "examples")


  private[sbt] def apiErrorExample(content: String): Option[Js.Obj] =
    toOption(apiExample.parse(trim(content)), "local", "error", "examples")

  private[sbt] def apiHeaderExample(content: String): Option[Js.Obj] =
    toOption(apiExample.parse(trim(content)), "local", "header", "examples")

  private[sbt] def apiGroup(content: String): Option[Js.Obj] = {
    val group = trim(content)
    if (group.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("group" -> group.replaceAll("\\s+", "_"))))
  }

  private val whiteSpace: Parser[String] = P( CharIn("\r\n\t\f ").rep(min = 1).!)
  private val nonWhiteSpace: Parser[String] = P((!whiteSpace ~ AnyChar).rep.!)

  private[sbt] def unindent(content: String): String = {

    def matches(p: Parsed[_]): Boolean = p.fold((_, _, _) => false,(_, _) => true)

    val lines = content.split("\n")
    val nonWhiteSpaceLines = lines.filter(l => matches(nonWhiteSpace.parse(l))).sorted
    if (nonWhiteSpaceLines.isEmpty) content
    else {
        val (head, last) = (nonWhiteSpaceLines.head, nonWhiteSpaceLines.last)
        val i = (head zip last).takeWhile{ case (aa, bb) => matches(whiteSpace.parse(aa.toString)) && aa == bb }.length
        lines.map(_.substring(i)).mkString("\n")
    }
  }

  private val group: Parser[String] =
    P( ("(" ~ " ".rep ~ (!CharIn(") ") ~ AnyChar).rep.! ~ " ".rep ~ ")" ~ " ".rep).? ) map {
      group =>
        group.getOrElse("Parameter")
    }

  private val doubleQuoteEnum: Parser[Seq[String]] = P(("\"" ~ CharsWhile(_ != '\"').! ~ "\"" ~ ",".? ~ " ".rep ).map("\"" + _ + "\"").rep(min = 1))
  private val singleQuoteEnum: Parser[Seq[String]] = P(("\'" ~ CharsWhile(_ != '\'').! ~ "\'" ~ ",".? ~ " ".rep).map("\'" + _ + "\'").rep(min = 1))
  private val noQuoteEnum: Parser[Seq[String]] = P(((!"," ~ AnyChar).rep.! ~ ",".? ~ " ".rep).rep)
  private val enum: Parser[Seq[String]] = doubleQuoteEnum | singleQuoteEnum | noQuoteEnum
  private val typeSizeAllowedValues: Parser[Js.Obj] =
    P("{" ~ " ".rep ~ (!CharIn("{} ") ~ AnyChar).rep.! ~ " ".rep ~ ("{" ~ " ".rep ~ (!CharIn("} ") ~ AnyChar).rep.! ~ " ".rep ~ "}" ~ " ".rep).? ~  " ".rep ~ ("=" ~ " ".rep ~ enum).? ~ " ".rep ~ "}"  ~ " ".rep) map {
      case (_type, size, allowedValues) =>
        Js.Obj("type" -> _type, "size" -> size.fold(Js.Null: Js.Value)(Js.Str.apply), "allowedValue" -> allowedValues.fold(Js.Null: Js.Value)( av => av.map(Js.Str.apply)))
    }
  private val fieldCharacter: Seq[Char] = Seq('.', '-', '/') ++ ('A' to 'z').filterNot(c => c == '`' || c == '^')
  private val doubleQuotedDefaultValue = P( "\"" ~ (!CharIn("] \"") ~ AnyChar).rep.! ~ "\"")
  private val singleQuotedDefaultValue = P( "\'" ~ (!CharIn("] \'") ~ AnyChar).rep.! ~ "\'")
  private val noQuotedDefaultValue = P( (!CharIn("] ") ~ AnyChar).rep.!)
  private val defaultValue: Parser[String] = P( " ".rep ~ "=" ~ " ".rep ~ (doubleQuotedDefaultValue | singleQuotedDefaultValue | noQuotedDefaultValue))
  private val field: Parser[Js.Obj] = P( CharIn(fieldCharacter).rep.! ~ defaultValue.? ~ " ".rep) map {
    case (f, dv) =>
      Js.Obj("field" -> f, "optional" -> f.startsWith("["), "defaultValue" -> dv.fold(Js.Null: Js.Value)(Js.Str.apply))
  }
  private val description: Parser[Js.Obj] = P( AnyChar.rep.!.? ).map(d => Js.Obj("description" -> d.fold(Js.Null: Js.Value)(Js.Str.apply)))
  private val apiParam: Parser[Js.Obj] =
    P( group ~ typeSizeAllowedValues ~ field ~ description) map {
      case (g, tsav, f, d) =>
        Js.Obj(g -> Js.Obj.from( Js.Obj("group" -> g).value ++ tsav.value ++ f.value ++ d.value))
    }

  private[sbt] def apiParam(content: String): Option[Js.Obj] =
    toOption(apiParam.parse(trim(content)), "local", "parameter", "fields")



//  TODO
//  implicit class ParsedWrapper[T](val parsed: Parsed[T]) extends AnyVal {
//
//    def toOption(path: String*): Option[Js.Obj] = {
//      parsed.fold(
//        (_, _, _) => None,
//        (jsObj, _) => Option(path.foldRight(jsObj){ case (key, acc) =>
//          Js.Obj(key -> acc)
//        })
//      )
//    }
//
//  }

}