package com.culpin.team.parser

import java.io.File

import com.culpin.team.core._
import com.culpin.team.util.Util
import sbt.Logger
import scala.util.matching.Regex

import org.json4s.JsonAST.{ JArray, JString, JObject }
import org.json4s.JsonDSL._

trait Parser {

  val name: String

  val regex: Regex

  protected def parse(input: String): List[Option[String]] = Parser.parse(regex)(input)

  def parseBlock(content: String): Option[JObject] = None
}

class ApiParser extends Parser {

  override val name = "api"

  override val regex = """^(?:(?:\{(.+?)\})?\s*)?(.+?)(?:\s+(.+?))?$""".r

  override def parseBlock(content: String): Option[JObject] = {
    val matches = parse(content)
    if (matches.isEmpty)
      None
    else {
      //TODO
      // List("type", "url", "title") zip matches map {case (key,value) => key -> value}
      Some(("local" -> ("type" -> matches(0)) ~ ("url" -> matches(1)) ~ ("title" -> matches(2))))
    }
  }

}

class ApiDescriptionParser extends Parser {

  override val name = "apidescription"

  override val regex = """""".r

  override def parseBlock(content: String): Option[JObject] = {
    val description = Util.trim(content)
    if (description.isEmpty)
      None
    else
      Some(("local" -> ("description" -> description)))
  }
}

class ApiErrorExampleParser extends ApiExampleParser {

  override val name = "apierrorexample"

  override def buildPath(content: String): Option[JObject] = {

    Some(("local" ->
      ("error" ->
        ("examples" -> jsonProcessBlock(content))
      )
    ))
  }

}
//
class ApiErrorParser extends ApiParamParser("Error 4xx") {

  override val name = "apierror"

  override def buildPath(content: String): Option[JObject] = {
    Some(("local" ->
      ("error" ->
        ("fields" -> jsonProcessBlock(content))
      )
    ))
  }

}

class ApiExampleParser extends Parser {

  override val name = "apiexample"

  override val regex = """(@\w*)?(?:(?:\s*\{\s*([a-zA-Z0-9\.\/\\\[\]_-]+)\s*\}\s*)?\s*(.*)?)""".r

  protected def jsonProcessBlock(content: String): JArray = {
    val trimmedSource = Util.trim(content)

    val matches = parse(trimmedSource)
    val `type` = matches(1) orElse Some("json")
    val title = matches(2)

    val regexFollowingLines = """(?m)(^.*\s?)""".r
    val matches2 = Parser.parse(regexFollowingLines)(trimmedSource)
    val text = if (matches2.length <= 1) None
    else matches2.tail.foldLeft(Option("")) {
      case (acc, elem) =>
        acc.map(s => s + elem.getOrElse(""))
    }.map(Util.unindent)
    JArray(List(("title" -> title) ~ ("content" -> text) ~ ("type" -> `type`)))
  }

  override def parseBlock(content: String): Option[JObject] =
    buildPath(content)

  def buildPath(content: String): Option[JObject] =
    Some(("local" ->
      ("examples" ->
        jsonProcessBlock(content)
      )
    ))

}

class ApiGroupParser extends Parser {

  override val name = "apigroup"

  override val regex = """(\s+)""".r

  override def parseBlock(content: String): Option[JObject] = {
    val group = Util.trim(content)
    if (group.isEmpty)
      None
    else
      Some(("local" -> ("group" -> group.replaceAll("\\s+", "_"))))
  }
}

class ApiNameParser extends Parser {

  override val name = "apiname"

  override val regex = """(\s+)""".r

  override def parseBlock(content: String): Option[JObject] = {
    val name = Util.trim(content)
    if (name.isEmpty)
      None
    else
      Some(("local" -> ("name" -> name.replaceAll("\\s+", "_"))))
  }
}

class ApiParamParser(val defaultGroup: String = "Parameter") extends Parser {

  override val name = "apiparam"

  override val regex = """^\s*(?:\(\s*(.+?)\s*\)\s*)?\s*(?:\{\s*([a-zA-Z0-9()#:\.\/\\\[\]_-]+)\s*(?:\{\s*(.+?)\s*\}\s*)?\s*(?:=\s*(.+?)(?=\s*\}\s*))?\s*\}\s*)?(\[?\s*([a-zA-Z0-9\.\/\\_-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|(.*?)(?:\s|\]|$)))?\s*\]?\s*)(.*)?$|@""".r

  val allowedValuesWithDoubleQuoteRegExp = """\"[^\"]*[^\"]\"""".r
  val allowedValuesWithQuoteRegExp = """\'[^\']*[^\']\'""".r
  val allowedValuesRegExp = """[^,\s]+""".r

  protected def jsonProcessBlock(content: String): JObject = {
    val c = Util.trim(content)
    val contentNoLineBreak = Parser.replaceLineWithUnicode(c)
    val matches = parse(contentNoLineBreak)
    if (matches.isEmpty)
      JObject()
    else {
      val maybeAllowedValues = matches(3)
      val allowedValue = maybeAllowedValues.map { av =>
        val regex = av.charAt(0) match {
          case '\"' => allowedValuesWithDoubleQuoteRegExp
          case '\'' => allowedValuesWithQuoteRegExp
          case _ => allowedValuesRegExp
        }
        regex.findAllIn(av).toList.map(JString(_))
      }
      val group = matches(0).getOrElse(defaultGroup)
      val description = matches(9).map(Parser.reverseUnicodeLinebreak(_))
      val optional = matches(4).map(s => (s.charAt(0) == '[').toString)
      val defaultValue = matches(6).orElse(matches(7)).orElse(matches(8))
      (group ->
        JArray(List(("group" -> group) ~ ("type" -> matches(1)) ~ ("size" -> matches(2)) ~
          ("optional" -> optional) ~ ("field" -> matches(5)) ~ ("defaultValue" -> defaultValue) ~
          ("allowedValue" -> allowedValue) ~ ("description" -> description))))
    }
  }

  override def parseBlock(content: String): Option[JObject] = buildPath(content)

  def buildPath(content: String): Option[JObject] = {
    Some(("local" ->
      ("parameter" ->
        ("fields" -> jsonProcessBlock(content))
      )
    ))
  }

}

class ApiPermissionParser extends ApiUseParser {

  override val name = "apipermission"

  override def parseBlock(content: String): Option[JObject] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      Some(("local" ->
        ("permission" ->
          ("name" -> c)
        )
      ))
  }
}

class ApiSuccessParser extends ApiParamParser("Success 200") {

  override val name = "apisuccess"

  override def buildPath(content: String): Option[JObject] = {
    Some(("local" ->
      ("success" ->
        ("fields" -> jsonProcessBlock(content))
      )
    ))
  }
}

class ApiSuccessExampleParser extends ApiExampleParser {

  override val name = "apisuccessexample"

  override def buildPath(content: String): Option[JObject] = {

    Some(("local" ->
      ("success" ->
        ("examples" -> jsonProcessBlock(content))
      )
    ))
  }

}

class ApiUseParser extends Parser {

  override val name = "apiuse"

  override val regex: Regex = new Regex("")

  override def parseBlock(content: String): Option[JObject] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      Some(("local" ->
        ("use" ->
          ("name" -> c)
        )
      ))
  }
}

class ApiVersionParser extends Parser {

  override val name = "apiversion"

  override val regex: Regex = new Regex("")

  override def parseBlock(content: String): Option[JObject] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      Some(("local" ->
        ("version" -> c)
      ))
  }
}

object Parser {

  def apply(sources: Seq[File], log: Logger): JArray =
    sources.map { s => parseFile(s) }

  def parseFile(file: File): JArray = {
    val rawBlocks = findBlocks(file)
    val elements = rawBlocks.map { b =>
      findElements(b)
    }
    parseBlockElementJson(elements, file.getName)
  }

  val parser = List(
    new ApiParser,
    new ApiDescriptionParser,
    new ApiErrorParser,
    new ApiErrorExampleParser,
    new ApiExampleParser,
    new ApiNameParser,
    new ApiGroupParser,
    new ApiParamParser,
    new ApiPermissionParser,
    new ApiSuccessExampleParser,
    new ApiSuccessParser,
    new ApiUseParser,
    new ApiVersionParser
  )

  def parseBlockElementJson(detectedElements: Seq[Seq[Element]], filename: String): JArray = {
    def isApiBlock(elements: Seq[Element]): Boolean = {
      val apiIgnore = elements.exists { elem =>
        val elementName = elem.name
        (elementName.length() >= 9 && elementName.substring(0, 9) == "apiignore")
      }
      val apiElem = elements.exists { elem =>
        val elementName = elem.name
        elementName.length() >= 3 && elementName.substring(0, 3) == "api"
      }
      !apiIgnore && apiElem
    }
    val parserMap = parser.map(p => (p.name, p)).toMap
    detectedElements.filter(isApiBlock).map { elements =>
      val initialResult: JObject = ("global" -> JObject()) ~ ("local" -> JObject())
      elements.foldLeft(initialResult) {
        case (result, element) =>

          //TODO handle non existing parser
          val Some(elementParser) = parserMap.get(element.name)

          //TODO handle empty block

          val Some(values) = elementParser.parseBlock(element.content)
          result merge values
      }
    }
  }
  /**
   * Determine Blocks
   */
  def findElements(block: String): Seq[Element] = {
    val blockUnicode = replaceLineWithUnicode(block)
    val elementRegex = """(?m)(@(\w*)\s?(.+?)(?=\uffff[\s\*]*@|$))""".r

    elementRegex.findAllIn(blockUnicode).matchData.map { m =>
      Element(m.group(1), m.group(2).toLowerCase, reverseUnicodeLinebreak(m.group(2)), reverseUnicodeLinebreak(m.group(3)))
    }.toSeq
  }

  /**
   * Find block in the file give as a parameter
   * @param file the input file
   * @return The list of blocks found
   */
  def findBlocks(file: File): Seq[String] = {
    val src: String = readFile(file)
    findBlocks(src)
  }

  def readFile(file: File): String = {
    val source = scala.io.Source.fromFile(file)
    val src = try source.mkString finally source.close()
    src
  }

  /**
   * Find block in the string give as a parameter
   * @param src the input string
   * @return The list of blocks found
   */
  def findBlocks(src: String): List[String] = {
    val srcNoLines = replaceLineWithUnicode(src)
    //TODO handle language

    val regexForFile = defaultLanguageBlockRegex

    val matches = parseAndFilterNullGroup(regexForFile)(srcNoLines)
    matches.map { block =>
      // Reverse Unicode Linebreaks
      val blockLine = reverseUnicodeLinebreak(block)
      //&   remove not needed ' * ' and tabs at the beginning
      inlineRegex.replaceAllIn(blockLine, "")
    }
  }

  /**
   * Look up for the match within the string
   * @param regex the regex to match against
   * @param input the input string
   * @return
   */
  def parse(regex: Regex)(input: String): List[Option[String]] = {
    val matchIt = regex.findAllIn(input).matchData
    val result = for {
      m <- matchIt
      i <- 1 to m.groupCount
    } yield m.group(i)
    result.map(Option(_)).toList
  }

  def parseAndFilterNullGroup(regex: Regex)(input: String): List[String] =
    for {
      maybeString <- parse(regex)(input) if (maybeString.isDefined)
    } yield maybeString.getOrElse("")

  val defaultLanguageBlockRegex = """\/\*\*\uffff?(.+?)\uffff?(?:\s*)?\*\/""".r

  //Regex Multiline
  //http://daily-scala.blogspot.co.uk/2010/01/regular-expression-2-rest-regex-class.html
  val inlineRegex = """(?m)^(\s*)?(\*)[ ]?""".r

  def reverseUnicodeLinebreak(block: String): String = {
    block.replaceAll("\uffff", "\n")
  }

  def replaceLineWithUnicode(src: String): String = {
    src.replaceAll("\n", "\uffff")
  }

}
