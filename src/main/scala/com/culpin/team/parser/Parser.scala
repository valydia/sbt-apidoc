package com.culpin.team.parser

import java.io.File

import com.culpin.team.core.Element

import scala.util.matching.Regex

object Parser {

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
