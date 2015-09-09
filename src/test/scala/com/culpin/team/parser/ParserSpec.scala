package com.culpin.team.parser

import com.culpin.team.core.Element
import org.scalatest.{ Matchers, FlatSpec }

class ParserSpec extends FlatSpec with Matchers {

  "Parser" should "should find block in file" in {
    val string = "/**\n * Created by valydia on 26/07/15.\n */\npublic class JavaMain {\n    /**\n     * Block 1\n    " +
      " * @param arg\n     */\n    public static void main1 (String [] arg) {\n        for (String s: arg) {\n      " +
      "      System.out.println(s);\n        }\n    }\n}"

    val result = Parser.findBlocks(string)
    val expected = List("Created by valydia on 26/07/15.", "Block 1\n@param arg")
    assert(result === expected)
  }

  "Parser" should "should find element in block" in {
    val result = Parser.findElements("Block 1\n@param arg")
    val expected = List(Element("@param arg", "param", "param", "arg"))
    assert(result === expected)

    val result2 = Parser.findElements("More complex block\n@param arg The array of string\n@param theString the " +
      "string\n@param theInt the int\n@return the result string")
    val expected2 = List(Element("@param arg The array of string", "param", "param", "arg The array of string"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string"))
    assert(result2 === expected2)

    val result3 = Parser.findElements("Ignored block\n@apiIgnore Not finished Method\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected3 = List(Element("@apiIgnore Not finished Method", "apiignore", "apiIgnore", "Not finished Method"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string"))
    assert(result3 === expected3)

    val result4 = Parser.findElements("Api block\n@apiParam")
    val expected4 = List(Element("@apiParam", "apipara", "apiPara", "m"))
    assert(result4 === expected4)
  }

  "Parser" should "find index of api blocks" in {
    val block = List(
      List(),
      List(Element("@param arg", "param", "param", "arg")),
      List(Element("@param arg", "param", "param", "arg")),
      List(Element("@apiParam", "apipara", "apiPara", "m")),
      List(Element("@param arg The array of string", "param", "param", "arg The array of string"),
        Element("@param theString the string", "param", "param", "theString the string"),
        Element("@param theInt the int", "param", "param", "theInt the int"),
        Element("@return the result string", "return", "return", "the result string")),
      List(Element("@apiIgnore Not finished Method", "apiignore", "apiIgnore", "Not finished Method"),
        Element("@param theString the string", "param", "param", "theString the string"),
        Element("@param theInt the int", "param", "param", "theInt the int"),
        Element("@return the result string", "return", "return", "the result string"))
    )
    val result = Parser.findBlocksWithApiGetIndex(block)
    assert(result === List(3))
  }

}
