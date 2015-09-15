package com.culpin.team.parser

import java.io.File

import com.culpin.team.core.{ Block, Element }
import com.culpin.team.util.Util
import scala.util.matching.Regex

trait Parser {

  val regex: Regex

  val path: String

  protected def parse(input: String): List[Option[String]] = Parser.parse(regex)(input)

  def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block]
}

class ApiParser extends Parser {

  override val regex = """^(?:(?:\{(.+?)\})?\s*)?(.+?)(?:\s+(.+?))?$""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val matches = parse(content)
    if (matches.isEmpty)
      None
    else {
      Some(Block(`type` = matches(0), url = matches(1), title = matches(2)))
    }
  }

}

class ApiDescriptionParser extends Parser {

  override val regex = """""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val description = Util.trim(content)
    if (description.isEmpty)
      None
    else
      Some(Block(description = Some(description)))
  }
}

class ApiNameParser extends Parser {

  override val regex = """(\s+)""".r

  override val path = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val name = Util.trim(content)
    if (name.isEmpty)
      None
    else
      Some(Block(name = Some(name.replaceAll("\\s+", "_"))))
  }
}

class ApiParamParser(val defaultGroup: String = "Parameter") extends Parser {

  override val regex = """^\s*(?:\(\s*(.+?)\s*\)\s*)?\s*(?:\{\s*([a-zA-Z0-9()#:\.\/\\\[\]_-]+)\s*(?:\{\s*(.+?)\s*\}\s*)?\s*(?:=\s*(.+?)(?=\s*\}\s*))?\s*\}\s*)?(\[?\s*([a-zA-Z0-9\.\/\\_-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|(.*?)(?:\s|\]|$)))?\s*\]?\s*)(.*)?$|@""".r

  override val path = ""

  val allowedValuesWithDoubleQuoteRegExp = """\"[^\"]*[^\"]\"""".r
  val allowedValuesWithQuoteRegExp = """\'[^\']*[^\']\'""".r
  val allowedValuesRegExp = """[^,\s]+""".r

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content);
    val contentNoLineBreak = Parser.replaceLineWithUnicode(c)
    val matches = parse(contentNoLineBreak)
    if (matches.isEmpty)
      None
    else {
      val allowedValues = matches(3)
      val av = if (allowedValues.isDefined) {
        val regex = allowedValues.get.charAt(0) match {
          case '\"' => allowedValuesWithDoubleQuoteRegExp
          case '\'' => allowedValuesWithQuoteRegExp
          case _ => allowedValuesRegExp
        }
        Parser.parse(regex)(allowedValues.get)
      } else Nil
      val group = matches(0).orElse(Some(defaultGroup))
      val description = matches(9).map(Parser.reverseUnicodeLinebreak(_))
      Some(Block(group = group, `type` = matches(1), size = matches(2), optional = matches(4).map(s => (s.charAt(0) == '[').toString),
      field = matches(5), defaultValue = matches(6).orElse(matches(7)).orElse(matches(8)), description = description))

    }

  }
}

class ApiSuccessParser extends ApiParamParser("Success 200") {

  override val path: String = "local.use.success.fields." + defaultGroup //TODO improve

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] =
    super.parseBlock(content, source, messages)
}

class ApiUseParser extends Parser {
  override val regex: Regex = new Regex("")

  override val path: String = "local.use"

  override def parseBlock(content: String, source: Option[String], messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
      Some(Block(name = Some(c)))
  }
}

class ApiVersionParser extends Parser {
  override val regex: Regex = new Regex("")

  override val path: String = "local"

  override def parseBlock(content: String, source: Option[String] = None, messages: Map[String, String] = Map()): Option[Block] = {
    val c = Util.trim(content)

    if (c.isEmpty)
      None
    else
    //TODO validate c semver
    Some(Block(version = Some(c)))
  }
}


object Parser {

  def parseBlockElement(indexApiBlocks: Seq[Int], detectedElements: Seq[Seq[Element]], filename: String): Seq[Block] = {
    indexApiBlocks.map { blockIndex =>
      val element = detectedElements(blockIndex)
    }
    ???
  }

  /**
   * Return block indexes with active API-elements
   * An @apiIgnore ignores the block.
   * Other, non @api elements, will be ignored.
   * @param blocks
   * @return
   */
  def findBlocksWithApiGetIndex(blocks: Seq[Seq[Element]]): Seq[Int] = {

    blocks.zipWithIndex.foldLeft(Seq[Int]()) {
      case (acc, (elements, index)) =>
        val apiIgnore = elements.exists { elem =>
          val elementName = elem.name
          (elementName.length() >= 9 && elementName.substring(0, 9) == "apiignore")
        }
        val apiElem = elements.exists { elem =>
          val elementName = elem.name
          elementName.length() >= 3 && elementName.substring(0, 3) == "api"
        }
        if (!apiIgnore && apiElem)
          acc :+ index
        else
          acc
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
