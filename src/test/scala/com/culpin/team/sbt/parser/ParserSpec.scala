package com.culpin.team.sbt.parser

import java.io.File

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.FlatSpec
import ujson.Js
import Parser._
import com.culpin.team.sbt.Util.renderMarkDown
import sbt.util.{ Level, Logger }

class ParserSpec extends FlatSpec {

  val stubLogger = new Logger {
    override def log(level: Level.Value, message: => String): Unit = ()

    override def trace(t: => Throwable): Unit = ()

    override def success(message: => String): Unit = ()
  }
  "Parser" should "parse empty files" in {
    assert(Parser(List(new File(getClass.getResource("/NoApidocApplication.scala").getFile)), stubLogger) === (Js.Arr(Js.Arr()), List("NoApidocApplication.scala")))
  }

  it should "parse comment block" in {
    val file = """package simple
                 |
                 |/**
                 |  * A simple class and objects to write tests against.
                 |  */
                 |class Main {
                 |  val default = "the function returned"
                 |  def method = default + " " + Main.function
                 |}
                 |
                 |object Main {
                 |
                 |  val constant = 1
                 |  def function = 2*constant
                 |
                 | /**
                 |  * Block 1
                 |  * @param arg
                 |  */
                 |  def main(args: Array[String]): Unit = {
                 |    println(new Main().default)
                 |  }
                 |
                 |
                 | /**
                 |  * @apiDefine admin Admin access rights needed.
                 |  * Optionally you can write here further Informations about the permission.
                 |  *
                 |  * An "apiDefinePermission"-block can have an "apiVersion", so you can attach the block to a specific version.
                 |  *
                 |  * @apiVersion 0.3.0
                 |  */
                 |  def foo(): Unit = {}
                 |}""".stripMargin

    val result = parseCommentBlocks(file).toList
    assert(result === List("A simple class and objects to write tests against.", "Block 1\n@param arg", "@apiDefine admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.\n\n@apiVersion 0.3.0"))
  }

  //TODO rewrite using TableProperties
  it should "find element in block" in {

    val result = parseElement("Block 1\n@param arg")
    val expected = List(Element("@param arg", "param", "param", "arg"))
    assert(result.toList === expected)

    val result2 = parseElement("More complex block\n@param arg The array of string\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected2 = List(
      Element("@param arg The array of string", "param", "param", "arg The array of string"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string"))
    assert(result2.toList === expected2)

    val result3 = parseElement("Ignored block\n@apiIgnore Not finished Method\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected3 = List(
      Element("@apiIgnore Not finished Method", "apiignore", "apiIgnore", "Not finished Method"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string"))
    assert(result3.toList === expected3)

    val result4 = parseElement("@apiDefine admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.\n\n@apiVersion 0.3.0")
    val expected4 = List(
      Element("@apiDefine admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.", "apidefine", "apiDefine", "admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."),
      Element("@apiVersion 0.3.0", "apiversion", "apiVersion", "0.3.0"))

    assert(result4.toList === expected4)

    //TODO Check what the expected behaviour with the original library
    //    val result4 = SbtApidocjsPlugin.parseElement("Api block\n@apiParam")
    //    val expected4 = List(Element("@apiParam", "apipara", "apiPara", "m"))
    //    assert(result4.toList === expected4)
  }

  it should "parse api element with title" in {
    val result = api("{get} /user/:id some title")
    val localJson = result.get.apply("local")
    assert(localJson("type") === Js.Str("get"))
    assert(localJson("url") === Js.Str("/user/:id"))
    assert(localJson("title") === Js.Str("some title"))
  }

  it should "parse api element" in {
    val result = api("{get} /user/:id")
    val localJson = result.get.apply("local")
    assert(localJson("type") === Js.Str("get"))
    assert(localJson("url") === Js.Str("/user/:id"))
  }

  val apidefineTestCases =
    Table(
      ("content", "name", "title", "description"),
      ("admin This title is visible in version 0.1.0 and 0.2.0", Js.Str("admin"): Js.Value, Js.Str("This title is visible in version 0.1.0 and 0.2.0"): Js.Value, None),
      ("admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.",
        Js.Str("admin"): Js.Value, Js.Str("Admin access rights needed."): Js.Value,
        Some(Js.Str(renderMarkDown("Optionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.")))))

  it should "parse apidefine element" in {
    forAll(apidefineTestCases) { (content, name, title, description) =>
      val result = apiDefine(content)
      val defineJson = result.get.apply("global")("define")
      assert(defineJson("name") === name)
      assert(defineJson("title") === title)
      description.foreach(s => assert(defineJson("description") === s))
    }
  }

  val descriptions =
    Table(
      ("content", "result"),
      ("Some Description", "Some Description"),
      ("Text", "Text"),
      ("   Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace). ",
        "Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace)."),
      ("    Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).  ",
        "Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces)."),
      ("\t\t\tText line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab).\t",
        "Text line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab)."))

  it should "parse apidescription element" in {
    forAll(descriptions) { (content, expected) =>
      val Some(result) = apiDescription(content)
      val description = result("local")("description")
      assert(description.str === renderMarkDown(expected))
    }
  }

  it should "parse apidescription an empty element" in {
    val result = apiDescription("")
    assert(result === None)
  }

  it should "parse apiexample element" in {
    val result = apiExample("Example usage:\ncurl -i http://localhost/user/4711")
    val exampleJson = result.get.apply("local")("examples")(0)
    assert(exampleJson("title") === Js.Str("Example usage:"))
    assert(exampleJson("content") === Js.Str("curl -i http://localhost/user/4711"))
    assert(exampleJson("type") === Js.Str("json"))
  }

  it should "parse apierrorexample element" in {
    val result = apiErrorExample("{json} Error-Response:\n                 This is an example.")
    val exampleJson = result.get.apply("local")("error")("examples")(0)
    assert(exampleJson("title") === Js.Str("Error-Response:"))
    assert(exampleJson("content") === Js.Str("This is an example."))
    assert(exampleJson("type") === Js.Str("json"))
  }

  val headers =
    Table(
      ("data", "title", "content", "type"),
      ("{json} Header-Example:\n    {\n      \"Accept-Encoding\": \"Accept-Encoding: gzip, deflate\"\n    }", "Header-Example:", "{\n  \"Accept-Encoding\": \"Accept-Encoding: gzip, deflate\"\n}", "json"),
      ("{json} Request-Example:\n{ \"content\": \"This is an example content\" }", "Request-Example:", "{ \"content\": \"This is an example content\" }", "json"))

  it should "parse apiheaderexample element" in {
    forAll(headers) {
      case (data, title, content, _type) =>
        val result = apiHeaderExample(data)
        val exampleJson = result.get.apply("local")("header")("examples")(0)
        assert(exampleJson("title").str === title)
        assert(exampleJson("content").str === content)
        assert(exampleJson("type").str === _type)
    }
  }

  it should "parse apiparamexample element" in {
    val result = apiParamExample("{json} Request-Example:\n                 { \"content\": \"This is an example content\" }")
    val exampleJson = result.get.apply("local")("parameter")("examples")(0)
    assert(exampleJson("title") === Js.Str("Request-Example:"))
    assert(exampleJson("content") === Js.Str("{ \"content\": \"This is an example content\" }"))
    assert(exampleJson("type") === Js.Str("json"))
  }

  it should "parse apisuccessexample element" in {
    val result = apiSuccessExample("Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n")
    val exampleJson = result.get.apply("local")("success")("examples")(0)
    assert(exampleJson("title") === Js.Str("Success-Response:"))
    assert(exampleJson("content") === Js.Str("HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}"))
    assert(exampleJson("type") === Js.Str("json"))
  }

  it should "parse apigroup element" in {
    val result = apiGroup("User")
    assert(result.get.apply("local")("group") === Js.Str("User"))
  }

  val unindents =
    Table(
      ("content", "expected"),
      ("  a\n    b\n   c", "a\n  b\n c"),
      ("\t\ta\n\t\t\t\tb\n\t\t\tc", "a\n\t\tb\n\tc"),
      ("   \t   a", "a"),
      ("    a\n   b\nc   d\n   e", "    a\n   b\nc   d\n   e"),
      ("\ta\n\t  b\n\t c", "a\n  b\n c"))

  it should "unindent" in {
    forAll(unindents) { (content, expected) =>
      val result = unindent(content)
      assert(result === expected)
    }
  }

  val apiParamTestCase =
    Table(
      ("content", "group", "type", "optional", "field", "defaultValue", "size", "allowedValue", "description"),
      ("{String} country=\"DE\" Mandatory with default value \"DE\".", "Parameter", Some(Js.Str("String")), Js.Bool(false), Js.Str("country"), Some(Js.Str("DE")), None, None, Some(Js.Str(renderMarkDown("Mandatory with default value \"DE\".")))),
      ("{String} lastname     Mandatory Lastname.", "Parameter", Some(Js.Str("String")), Js.Bool(false), Js.Str("lastname"), None, None, None, Some(Js.Str(renderMarkDown("Mandatory Lastname.")))),
      ("simple", "Parameter", None, Js.Bool(false), Js.Str("simple"), None, None, None, None),
      ("{String} name The users name.", "Parameter", Some(Js.Str("String")), Js.Bool(false), Js.Str("name"), None, None, None, Some(Js.Str(renderMarkDown("The users name.")))),
      ("( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }  [ \\MyClass\\field.user_first-name = \'John Doe\' ] Some description.", "MyGroup", Some(Js.Str("\\Object\\String.uni-code_char[]")), Js.Bool(true), Js.Str("\\MyClass\\field.user_first-name"), Some(Js.Str("John Doe")), Some(Js.Str("1..10")), Some(Js.Arr("\'abc\'", "\'def\'")), Some(Js.Str(renderMarkDown("Some description.")))),
      ("( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }  \\MyClass\\field.user_first-name = John_Doe Some description.", "MyGroup", Some(Js.Str("\\Object\\String.uni-code_char[]")), Js.Bool(false), Js.Str("\\MyClass\\field.user_first-name"), Some(Js.Str("John_Doe")), Some(Js.Str("1..10")), Some(Js.Arr("\'abc\'", "\'def\'")), Some(Js.Str(renderMarkDown("Some description.")))))

  it should "parse apiparam element" in {
    forAll(apiParamTestCase) { (content, group, `type`, optional, field, defaultValue, size, allowedValue, description) =>

      val result = apiParam(content)
      val parameterJson = result.get.apply("local")("parameter")("fields")(group)(0)
      assert(parameterJson("group") === Js.Str(group))
      `type`.foreach(t => assert(parameterJson("type") === t))
      assert(parameterJson("optional") === optional)
      assert(parameterJson("field") === field)
      defaultValue.foreach(dv => assert(parameterJson("defaultValue") === dv))
      size.foreach(s => assert(parameterJson("size") === s))
      allowedValue.foreach(aw => assert(parameterJson("allowedValue") === aw))
      description.foreach(d => assert(parameterJson("description") === d))
    }
  }

  it should "parse apisuccess element" in {

    val result = apiSuccess("{String} firstname Firstname of the User.")
    val parameterJson = result.get.apply("local")("success")("fields")("Success 200")(0)
    assert(parameterJson("group") === Js.Str("Success 200"))
    assert(parameterJson("type") === Js.Str("String"))
    assert(parameterJson("optional") === Js.Bool(false))
    assert(parameterJson("field") === Js.Str("firstname"))
    assert(parameterJson("description") === Js.Str(renderMarkDown("Firstname of the User.")))
  }

  it should "parse apiuse element" in {
    val result = apiUse("MySuccess")
    assert(result.get.apply("local")("use")(0)("name") === Js.Str("MySuccess"))
  }

  it should "parse apipermission element" in {
    val result = apiPermission("admin")
    assert(result.get.apply("local")("permission")(0)("name") === Js.Str("admin"))
  }

  it should "parse apisamplerequest element" in {
    val result = apiSampleRequest("http://test.github.com")
    assert(result.get.apply("local")("sampleRequest")(0)("url") === Js.Str("http://test.github.com"))
  }

  it should "parse apiversion element" in {
    val result = apiVersion("1.6.2")
    assert(result.get.apply("local")("version") === Js.Str("1.6.2"))
  }

}
