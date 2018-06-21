package com.culpin.team.sbt.parser

import sbt.{File, IO, Logger}
import ujson.Js
import fastparse.all._

object Parser {

  def apply(sourceFiles: List[File], log: Logger): (Js.Arr, List[String]) = {
    (sourceFiles.map { f =>
      log.info("parse file: " + f.getName)
      processFileContent(f, log)
    }, sourceFiles map (_.getName))
  }

  private def processFileContent(file: File, log: Logger): Js.Arr = {
    log.debug(s"inspect file ${file.getName}")
    log.debug(s"size: ${file.length} bytes")
    val commentBlocks = parseCommentBlocks(IO.read(file))
    if (commentBlocks.nonEmpty)
      log.debug(s"count blocks: ${commentBlocks.size}")

    val elements =
      commentBlocks.zipWithIndex map { case(block, index) =>
        val result = parseElement(block)
        log.debug(s"count elements in block $index : ${result.size}")
        result
      }
    processElements(elements, log)
  }

  private val parserMap: Map[String, String => Option[Js.Obj]] =
    Map(
      "api" -> api,
      "apidefine" -> apiDefine,
      "apidescription" -> apiDescription,
      "apierror" -> apiError,
      "apierrorexample" -> apiErrorExample,
      "apigroup" -> apiGroup,
      "apiheader" -> apiHeader,
      "apiheaderexample" -> apiHeaderExample,
      "apiname" -> apiName,
      "apiparam" -> apiParam,
      "apiparamexample" -> apiParamExample,
      "apipermission" -> apiPermission,
      "apisamplerequest" -> apiSampleRequest,
      "apisuccessexample" -> apiSuccessExample,
      "apisuccess" -> apiSuccess,
      "apiuse" -> apiUse,
      "apiversion" -> apiVersion,
    )

  def processElements(detectedElements: Seq[Seq[Element]], log: Logger): Js.Arr = {

    def isApiBlock(elements: Seq[Element]): Boolean = {
      val apiIgnore = elements.exists { elem =>
        elem.name.toLowerCase.startsWith("apiignore")
      }
      val apiElem = elements.exists { elem =>
        elem.name.toLowerCase.startsWith("api")
      }
      !apiIgnore && apiElem
    }

    detectedElements.zipWithIndex collect { case (elems, index) if isApiBlock(elems) =>
      val initialResult: Js.Value = Js.Obj("global" -> Js.Obj(), "local" -> Js.Obj())
      elems.foldLeft(initialResult){ case (acc, element) =>
        parserMap.get(element.name.toLowerCase) map {
          ep =>
            log.debug(s"found @${element.sourceName} in block: $index")
            ep(element.content) map { values =>

              val version = if (element.name.toLowerCase == "apiversion") values("local")("version") else Js.Null
              val jsIndex= Js.Obj("index" -> (index + 1), "version" -> version)
              merge(acc, merge(values, jsIndex))
            } getOrElse {
              //TODO what to log?????
              acc
            }
        } getOrElse {
          log.warn(s"parser plugin '${element.name}' not found in block: $index")
          acc
        }
      }
    }
  }

  private def merge(val1: Js.Value, val2: Js.Value): Js.Value = (val1, val2) match {
    case (Js.Obj(xs), Js.Obj(ys)) =>  Js.Obj.from(mergeFields(xs.toList, ys.toList))
    case (Js.Arr(xs), Js.Arr(ys)) =>  Js.Arr(mergeVals(xs.toList, ys.toList))
    case (Js.Null, x) => x
    case (x, Js.Null) => x
    case (_, y) => y
  }

  private def mergeFields(vs1: List[(String, Js.Value)], vs2: List[(String, Js.Value)]): List[(String, Js.Value)] = {
    def mergeRec(xleft: List[(String, Js.Value)], yleft: List[(String, Js.Value)]): List[(String, Js.Value)] = xleft match {
      case Nil => yleft
      case (xn, xv) :: xs => yleft find (_._1 == xn) match {
        case Some(y @ (_, yv)) =>
          (xn, merge(xv, yv)) :: mergeRec(xs, yleft filterNot (_ == y))
        case None => (xn, xv) :: mergeRec(xs, yleft)
      }
    }

    mergeRec(vs1, vs2)
  }
  private def mergeVals(vs1: List[Js.Value], vs2: List[Js.Value]): List[Js.Value] = {
    def mergeRec(xleft: List[Js.Value], yleft: List[Js.Value]): List[Js.Value] = xleft match {
      case Nil => yleft
      case x :: xs => yleft find (_ == x) match {
        case Some(y) => merge(x, y) :: mergeRec(xs, yleft filterNot (_ == y))
        case None => x :: mergeRec(xs, yleft)
      }
    }

    mergeRec(vs1, vs2)
  }

  private val commentChunk: Parser[Any] = P( CharsWhile(c => c != '/' && c != '*') | multilineComment | !"*/" ~ AnyChar )
  private lazy val multilineComment: Parser[String] = P( "/**" ~ "\n".? ~ " ".rep() ~ "* " ~/ commentChunk.rep.! ~ "*/" )
  private val commentBlock: Parser[String] = P( (!"/**" ~ AnyChar).rep ~ multilineComment ~ (!"/**" ~ AnyChar).rep )
  private val commentBlocks: Parser[Seq[String]] = commentBlock.rep

  private[parser] def parseCommentBlocks(file: String): Seq[String] = {

    def onSuccess(comments: Seq[String], index: Int): Seq[String] = {
      //TODO use another parser?
      comments.map(_.dropRight(3).replace("\n  * ","\n"))
    }

    commentBlocks.parse(file).fold((_, _, _) => Seq(), onSuccess)
  }

  private val identifier: Parser[String]  = P( (!" " ~ AnyChar).rep.! )
  private val argument: Parser[String]  = P( (!"\n" ~ AnyChar).rep.! )
  private val prefix: Parser[Unit] = P( (!"@" ~ AnyChar).rep )
  private val elementParser: Parser[Element] = P( prefix ~ ("@" ~ identifier ~ " " ~ argument)).map{case (id, arg) => Element(s"@$id $arg", id.toLowerCase, id, arg) }
  private val elements: Parser[Seq[Element]] = elementParser.rep

  case class Element(source: String, name: String, sourceName: String, content: String)

  private[parser] def parseElement(block: String): Seq[Element] = {
    elements.parse(block).fold(
      (_, _, _) => Seq(),
      (elements, _) => elements
    )
  }

  private val apiParser: Parser[Js.Obj] =
    P( ("{" ~ (!"}" ~ AnyChar).rep.! ~ "}").? ~ " " ~ (!" " ~ AnyChar).rep.! ~ (" " ~ AnyChar.rep.!).?) map {
      case (t, url, title) =>
        Js.Obj("type" -> t.getOrElse(""), "url" -> url, "title" -> title.fold(Js.Null: Js.Value)(Js.Str.apply))
    }
  private[parser] def api(content: String): Option[Js.Obj] =
    apiParser.parse(content).toOption("local")


  private val apiDefineParser: Parser[Js.Obj] =
    P( (!" " ~ AnyChar).rep.! ~ (" " ~ (!"\n" ~ AnyChar).rep.!).? ~ ("\n" ~ AnyChar.rep.!).? ) map {
      case (name, title, description) =>
        Js.Obj("name" -> name, "title" -> title.fold(Js.Null: Js.Value)(Js.Str.apply), "description" -> description.fold(Js.Null: Js.Value)(s => Js.Str(unindent(s))))
    }

  private[parser] def apiDefine(content: String): Option[Js.Obj] =
    apiDefineParser.parse(content).toOption("global", "define")


  private[parser] def apiDescription(content: String): Option[Js.Obj] = {
    val description = trim(content)
    if (description.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("description" -> unindent(description))))
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
  private[parser] def apiExample(content: String): Option[Js.Obj] =
    apiExample.parse(trim(content)).toOption("local", "examples")

  private[parser] def apiErrorExample(content: String): Option[Js.Obj] =
    apiExample.parse(trim(content)).toOption("local", "error", "examples")

  private[parser] def apiHeaderExample(content: String): Option[Js.Obj] =
    apiExample.parse(trim(content)).toOption("local", "header", "examples")

  private[parser] def apiParamExample(content: String): Option[Js.Obj] = {
    apiExample.parse(trim(content)).toOption("local", "parameter", "examples")
  }

  private[parser] def apiSuccessExample(content: String): Option[Js.Obj] = {
    apiExample.parse(trim(content)).toOption("local", "success", "examples")
  }

  private[parser] def apiGroup(content: String): Option[Js.Obj] = {
    val group = trim(content)
    if (group.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("group" -> group.replaceAll("\\s+", "_"))))
  }

  private[parser] def apiName(content: String): Option[Js.Obj] = {
    val name = trim(content)
    if (name.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("name" -> name.replaceAll("\\s+", "_"))))
  }

  private val whiteSpace: Parser[String] = P( CharIn("\r\n\t\f ").rep(min = 1).!)
  private val nonWhiteSpace: Parser[String] = P((!whiteSpace ~ AnyChar).rep.!)

  private[parser] def unindent(content: String): String = {

    def matches(p: Parsed[_]): Boolean = p.fold((_, _, _) => false,(_, _) => true)

    val lines = content.split("\n")
    val nonWhiteSpaceLines = lines.filter(l => matches(nonWhiteSpace.parse(l))).sorted
    if (nonWhiteSpaceLines.isEmpty) content
    else {
      val (head, last) = (nonWhiteSpaceLines.head, nonWhiteSpaceLines.last)
      val i = (head zip last).takeWhile{ case (headChar, lastChar) => matches(whiteSpace.parse(headChar.toString)) && headChar == lastChar }.length
      lines.map(_.substring(i)).mkString("\n")
    }
  }

  private[parser] def group(defaultGroup: String): Parser[String] =
    P( ("(" ~ " ".rep ~ (!CharIn(") ") ~ AnyChar).rep.! ~ " ".rep ~ ")" ~ " ".rep).? ) map { _.getOrElse(defaultGroup) }

  private val doubleQuoteEnum: Parser[Seq[String]] =
    P(("\"" ~ CharsWhile(_ != '\"').! ~ "\"" ~ ",".? ~ " ".rep ).map("\"" + _ + "\"").rep(min = 1))
  private val singleQuoteEnum: Parser[Seq[String]] =
    P(("\'" ~ CharsWhile(_ != '\'').! ~ "\'" ~ ",".? ~ " ".rep).map("\'" + _ + "\'").rep(min = 1))
  private val noQuoteEnum: Parser[Seq[String]] = P(((!"," ~ AnyChar).rep.! ~ ",".? ~ " ".rep).rep)
  private val enum: Parser[Seq[String]] = doubleQuoteEnum | singleQuoteEnum | noQuoteEnum
  private val typeSizeAllowedValues: Parser[Js.Obj] =
    P( ("{" ~ " ".rep ~ (!CharIn("{} ") ~ AnyChar).rep.! ~ " ".rep ~ ("{" ~ " ".rep ~ (!CharIn("} ") ~ AnyChar).rep.! ~ " ".rep ~ "}" ~ " ".rep).? ~  " ".rep ~ ("=" ~ " ".rep ~ enum).? ~ " ".rep ~ "}"  ~ " ".rep).? ) map {
      case Some((_type, size, allowedValues)) =>
        Js.Obj("type" -> _type, "size" -> size.fold(Js.Null: Js.Value)(Js.Str.apply), "allowedValue" -> allowedValues.fold(Js.Null: Js.Value)( av => av.map(Js.Str.apply)))
      case _ => Js.Obj("type" -> Js.Null, "size" -> Js.Null, "allowedValue" -> Js.Null)
    }
  private val fieldCharacter: Seq[Char] =
    Seq('.', '-', '/') ++ ('A' to 'z').filterNot(c => c == '`' || c == '^' || c == '[' || c == ']')
  private val doubleQuotedDefaultValue =
    P( "\"" ~ (!CharIn("]\"") ~ AnyChar).rep.! ~ "\"")
  private val singleQuotedDefaultValue =
    P( "\'" ~ (!CharIn("]\'") ~ AnyChar).rep.! ~ "\'")
  private val noQuotedDefaultValue =
    P( (!CharIn("] ") ~ AnyChar).rep.!)
  private val defaultValue: Parser[String] = P( " ".rep ~ "=" ~ " ".rep ~ (doubleQuotedDefaultValue | singleQuotedDefaultValue | noQuotedDefaultValue))
  private val field: Parser[Js.Obj] = P( "[".!.? ~ " ".rep ~ CharIn(fieldCharacter).rep.! ~ defaultValue.? ~ " ".rep ~ "]".? ~ " ".rep) map {
    case (o, f, dv) =>
      Js.Obj("field" -> f, "optional" -> o.isDefined, "defaultValue" -> dv.fold(Js.Null: Js.Value)(Js.Str.apply))
  }
  private val description: Parser[Js.Obj] = P( AnyChar.rep.!.? ).map(description => Js.Obj("description" -> description.fold(Js.Null: Js.Value)(d => if (d.isEmpty) Js.Null else Js.Str(d))))
  private def apiParamParser(defaultGroup: String): Parser[Js.Obj] =
    P( group(defaultGroup) ~ typeSizeAllowedValues ~ field ~ description) map {
      case (g, tsav, f, d) =>
        Js.Obj(g -> Js.Obj.from( Js.Obj("group" -> g).value ++ tsav.value ++ f.value ++ d.value))
    }

  private[parser] def apiParam(content: String): Option[Js.Obj] =
    apiParamParser("Parameter").parse(trim(content)).toOption("local", "parameter", "fields")

  private[parser] def apiSuccess(content: String): Option[Js.Obj] =
    apiParamParser("Success 200").parse(trim(content)).toOption("local", "success", "fields")

  private[parser] def apiError(content: String): Option[Js.Obj] =
    apiParamParser("Error 4xx").parse(trim(content)).toOption("local", "error", "fields")

  private[parser] def apiHeader(content: String): Option[Js.Obj] =
    apiParamParser("Header").parse(trim(content)).toOption("local", "header", "fields")

  private[parser] def apiUse(content: String): Option[Js.Obj] = {
    val name = trim(content)
    if (name.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("use" -> Js.Obj("name" -> name))))
  }

  private[parser] def apiPermission(content: String): Option[Js.Obj] = {
    val name = trim(content)
    if (name.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("permission" -> Js.Obj("name" -> name))))
  }

  private[parser] def apiSampleRequest(content: String): Option[Js.Obj] = {
    val url = trim(content)
    if (url.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("sampleRequest" -> Js.Obj("url" -> url))))
  }

  private[parser] def apiVersion(content: String): Option[Js.Obj] = {
    val version = trim(content)
    if (version.isEmpty) None
    else
      Option(Js.Obj("local" -> Js.Obj("version" -> version)))
  }

  private def pathToObject(jsObj: Js.Obj, path: String*): Js.Obj =
    path.foldRight(jsObj){ case (key, acc) => Js.Obj(key -> acc) }

  implicit class ParsedWrapper(val parsed: Parsed[Js.Obj]) extends AnyVal {
    def toOption(path: String*): Option[Js.Obj] = {
      parsed.fold(
        (_, _, _) => None,
        (jsObj, _) => Option(pathToObject(jsObj, path:_*))
      )
    }
  }

}
