package org.example.sbt

import org.example.sbt.SbtApidocjsPlugin.Element
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}
import ujson.Js

class SbtapidocjsSpec extends FlatSpec with Matchers  {

  "Parser" should "parse comment block" in {
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
                 |}""".stripMargin


    val result = SbtApidocjsPlugin.parseCommentBlocks(file)
    result shouldEqual Seq("A simple class and objects to write tests against.","Block 1\n@param arg")
  }

  it should "find element in block" in {

    val result = SbtApidocjsPlugin.parseElement("Block 1\n@param arg")
    val expected = List(Element("@param arg", "param", "param", "arg"))
    assert(result.toList === expected)

    val result2 = SbtApidocjsPlugin.parseElement("More complex block\n@param arg The array of string\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected2 = List(
      Element("@param arg The array of string", "param", "param", "arg The array of string"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string")
    )
    assert(result2.toList === expected2)

    val result3 = SbtApidocjsPlugin.parseElement("Ignored block\n@apiIgnore Not finished Method\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected3 = List(
      Element("@apiIgnore Not finished Method", "apiignore", "apiIgnore", "Not finished Method"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string")
    )
    assert(result3.toList === expected3)

//TODO Check what the expected behaviour with the original library
//    val result4 = SbtApidocjsPlugin.parseElement("Api block\n@apiParam")
//    val expected4 = List(Element("@apiParam", "apipara", "apiPara", "m"))
//    assert(result4.toList === expected4)
  }


  it should "parse api element with title" in {
    val result = SbtApidocjsPlugin.apiParse("{get} /user/:id some title")
    val localJson = result.get.apply("local")
    assert(localJson("type") === Js.Str("get"))
    assert(localJson("url") === Js.Str("/user/:id"))
    assert(localJson("title") === Js.Str("some title"))
  }

  it should "parse api element" in {
    val result = SbtApidocjsPlugin.apiParse("{get} /user/:id")
    val localJson = result.get.apply("local")
    assert(localJson("type") === Js.Str("get"))
    assert(localJson("url") === Js.Str("/user/:id"))
    assert(localJson("title") === Js.Null)
  }

  val apidefineTestCases =
    Table(
      ("content", "name", "title", "description"),
      ("admin This title is visible in version 0.1.0 and 0.2.0", Js.Str("admin"): Js.Value, Js.Str("This title is visible in version 0.1.0 and 0.2.0"): Js.Value, Js.Null: Js.Value),
      ("admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.",
        Js.Str("admin"): Js.Value, Js.Str("Admin access rights needed."): Js.Value,
        Js.Str("Optionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."): Js.Value)
    )

  it should "parse apidefine element" in {
    forAll(apidefineTestCases){ (content, name, title, description) =>
      val result = SbtApidocjsPlugin.apiDefineParse(content)
      val defineJson = result.get.apply("global")("define")
      assert(defineJson("name") === name)
      assert(defineJson("title") === title)
      assert(defineJson("description") === description)
    }
  }

  val descriptions =
    Table(
      ("content", "result"),
      ("Some Description", Js.Str("Some Description")),
      ("Text", Js.Str("Text")),
      ("   Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace). ",
        Js.Str("Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace).")),
      ("    Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).  ",
        Js.Str("Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).")),
      ("\t\t\tText line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab).\t",
        Js.Str("Text line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab)."))
    )


  it should "parse apidescription element" in {
    forAll(descriptions){ (content, expected) =>
      val result = SbtApidocjsPlugin.apiDescription(content)
      val localJson = result.get.apply("local")
      assert(localJson("description") === expected)
    }
  }

  it should "parse apidescription element - empty" in {
    val result = SbtApidocjsPlugin.apiDescription("")
    assert(result === None)
  }


}
