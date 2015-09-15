package com.culpin.team.parser

import java.io.File

import com.culpin.team.core.{ Example, Success, Block, Element }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, FlatSpec }
import sbt.Logger

class ParserSpec extends FlatSpec with Matchers with MockitoSugar {

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

  "Api Parser" should "parse api element " in {
    val apiParser = new ApiParser
    val Some(result) = apiParser.parseBlock("{get} /user/:id")
    val expected = Block(`type` = Some("get"), url = Some("/user/:id"))
    assert(result === expected)
  }

  "Api Parser" should "parse api element with title" in {
    val apiParser = new ApiParser
    val Some(result) = apiParser.parseBlock("{get} /user/:id some title")
    val expected = Block(`type` = Some("get"), url = Some("/user/:id"), title = Some("some title"))
    assert(result === expected)
  }

  "ApiDescriptionParser" should "parse api description element" in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("Some Description")
    val expected = Block(description = Some("Some Description"))
    assert(result === expected)

    val Some(result2) = apiDescriptionParser.parseBlock("Some Description \n on several line \n isn'it?")
    val expected2 = Block(description = Some("Some Description \n on several line \n isn'it?"))
    assert(result2 === expected2)
  }

  "ApiDescriptionParser" should "parse empty api description element " in {
    val apiDescriptionParser = new ApiDescriptionParser
    val result = apiDescriptionParser.parseBlock("")
    val expected = None
    assert(result === expected)

  }

  "ApiDescriptionParser" should "parse Word only api description element " in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(resultMap) = apiDescriptionParser.parseBlock("Text")
    val expected = Block(description = Some("Text"))
    assert(resultMap === expected)

  }
  "ApiDescriptionParser" should "Trim single line " in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("   Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace). ")
    val expected = Block(description = Some("Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace)."))
    assert(result === expected)

  }

  "ApiDescriptionParser" should "Trim multi line (spaces)" in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("    Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).  ")
    val expected = Block(description = Some("Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces)."))
    assert(result === expected)

  }

  "ApiDescriptionParser" should "Trim multi line (tabs)" in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("\t\t\tText line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab).\t")
    val expected = Block(description = Some("Text line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab)."))
    assert(result === expected)

  }

  "ApiParamParser" should "parse param element" in {
    val apiParamParser = new ApiParamParser

    val Some(result) = apiParamParser.parseBlock("{String} country=\"DE\" Mandatory with default value \"DE\".")
    val expected = Block(field = Some("country"), description = Some("Mandatory with default value \"DE\"."),
      optional = Some("false"), `type` = Some("String"), defaultValue = Some("DE"), group = Some("Parameter"))
    assert(result === expected)

    val Some(result2) = apiParamParser.parseBlock("{String} lastname     Mandatory Lastname.")

    val expected2 = Block(field = Some("lastname"), description = Some("Mandatory Lastname."),
      optional = Some("false"), `type` = Some("String"), group = Some("Parameter"))
    assert(result2 === expected2)
  }

  "ApiParamParser" should "parse Simple fieldname only" in {
    val apiParamParser = new ApiParamParser

    val Some(result) = apiParamParser.parseBlock("simple")

    val expected = Block(field = Some("simple"), description = Some(""),
      optional = Some("false"), group = Some("Parameter"))
    assert(result === expected)

  }

  "ApiParamParser" should "parse Type, Fieldname, Description" in {
    val apiParamParser = new ApiParamParser

    val Some(result) = apiParamParser.parseBlock("{String} name The users name.")
    val expected = Block(field = Some("name"), description = Some("The users name."),
      optional = Some("false"), `type` = Some("String"), group = Some("Parameter"))
    assert(result === expected)

  }

  "ApiParamParser" should "parse all options, with optional defaultValue" in {
    val apiParamParser = new ApiParamParser
    val content = "( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }" +
      "[ \\MyClass\\field.user_first-name = \'John Doe\' ] Some description."
    val Some(result) = apiParamParser.parseBlock(content)
    val expected = Block(size = Some("1..10"), field = Some("\\MyClass\\field.user_first-name"), description = Some("Some description."),
      optional = Some("true"), `type` = Some("\\Object\\String.uni-code_char[]"), defaultValue = Some("John Doe"), group = Some("MyGroup"))
    assert(result === expected)
  }

  "ApiParamParser" should "parse all options, without optional-marker, without default value quotes" in {
    val apiParamParser = new ApiParamParser
    val content = "( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }  " +
      "\\MyClass\\field.user_first-name = John_Doe Some description."
    val Some(result) = apiParamParser.parseBlock(content)
    val expected = Block(size = Some("1..10"), field = Some("\\MyClass\\field.user_first-name"), description = Some("Some description."),
      optional = Some("false"), `type` = Some("\\Object\\String.uni-code_char[]"), defaultValue = Some("John_Doe"), group = Some("MyGroup"))
    assert(result === expected)
  }

  "ApiSuccessParser" should "parse success element" in {
    val apiSuccessParser = new ApiSuccessParser

    val Some(result) = apiSuccessParser.parseBlock("{String} firstname Firstname of the User.")
    val expected = Block(field = Some("firstname"), description = Some("Firstname of the User."),
      optional = Some("false"), `type` = Some("String"), group = Some("Success 200"))
    assert(result === expected)
  }

  "ApiNameParser" should "parse name element" in {
    val apiNameParser = new ApiNameParser

    val Some(result) = apiNameParser.parseBlock("Welcome Page.")
    val expected = Block(name = Some("Welcome_Page."))
    assert(result === expected)
  }

  "Parser" should "parse block element" in {
    val detectedElement = Seq(
      Seq(
        Element("@api {get} / Home page.", "api", "api", "{get} / Home page."),
        Element("@apiName Welcome Page.", "apiname", "apiName", "Welcome Page."),
        Element("@apiGroup Application", "apigroup", "apiGroup", "Application"),
        Element("@apiVersion 1.0.0", "apiversion", "apiVersion", "1.0.0"),
        Element("@apiDescription Renders the welcome page\n", "apidescription", "apiDescription", "Renders the welcome page\n"),
        Element("@apiSuccessExample Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n", "apisuccessexample", "apiSuccessExample", "Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n")
      )
    )

    val result = Parser.parseBlockElement(detectedElement, "app/controllers/gathr/culpinteam/v1/Application.scala")

    //      val expected = Seq(
    //          Block(
    //            Some("get"), Some("Home page."), Some("Welcome_Page."), Some("/"), Some("Application"), Some("1.0.0"),
    //            Some("<p>Renders the welcome page</p> "),
    //            Some(Success(List(Example(Some("Success-Response:"), Some("HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}"),Some("json")))))
    //          )
    //      )
    val expected = Seq(
      Block(
        Some("get"), Some("Home page."), Some("Welcome_Page."), Some("/"), Some("Application"), Some("1.0.0"),
        Some("Renders the welcome page")
      )
    )
    assert(result === expected)
  }

  "Parser" should "parse file" in {

    val sources = Seq(new File(getClass.getResource("/Application.scala").getFile))
    val blocks = Parser(sources, mock[Logger])
    val block = blocks(0)(0)
    println(block)
    assert(block.`type` === Some("get"))
    assert(block.title === Some("Home page."))
    assert(block.name === Some("Welcome_Page."))
    assert(block.url === Some("/"))
    assert(block.group === Some("Application"))
    assert(block.version === Some("1.0.0"))
    assert(block.description === Some("Renders the welcome page"))

  }

}
