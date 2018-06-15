package org.example.sbt

import org.example.sbt.SbtApidocjsPlugin.Element
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

  it should "parse apidefine element" in {
    val result = SbtApidocjsPlugin.apiDefineParse("admin This title is visible in version 0.1.0 and 0.2.0")
    val defineJson = result.get.apply("global")("define")
    assert(defineJson("name") === Js.Str("admin"))
    assert(defineJson("title") === Js.Str("This title is visible in version 0.1.0 and 0.2.0"))
    assert(defineJson("description") === Js.Null)
  }


  it should "parse apidefine element with description" in {
    val result = SbtApidocjsPlugin.apiDefineParse("admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.")
    val defineJson = result.get.apply("global")("define")
    assert(defineJson("name") === Js.Str("admin"))
    assert(defineJson("title") === Js.Str("Admin access rights needed."))
    assert(defineJson("description") === Js.Str("Optionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))
  }

  it should "parse apidescription element" in {
    val result = SbtApidocjsPlugin.apiDescription("Some Description")
    val localJson = result.get.apply("local")
    assert(localJson("description") === Js.Str("Some Description"))
  }

  it should "parse apidescription element - empty" in {
    val result = SbtApidocjsPlugin.apiDescription("")
    assert(result === None)
  }


  it should "parse apidescription element - word only " in {
    val result = SbtApidocjsPlugin.apiDescription("Text")
    val localJson = result.get.apply("local")
    assert(localJson("description") === Js.Str("Text"))
  }

  it should "parse apidescription element - trim single line  " in {
    val result = SbtApidocjsPlugin.apiDescription("   Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace). ")
    val localJson = result.get.apply("local")
    assert(localJson("description") === Js.Str("Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace)."))
  }

  it should "parse apidescription element - trim multi line (spaces)  " in {
    val result = SbtApidocjsPlugin.apiDescription("    Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).  ")
    val localJson = result.get.apply("local")
    assert(localJson("description") === Js.Str("Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces)."))
  }

  it should "parse apidescription element - trim multi line (tabs)  " in {
    val result = SbtApidocjsPlugin.apiDescription("\t\t\tText line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab).\t")
    val localJson = result.get.apply("local")
    assert(localJson("description") === Js.Str("Text line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab)."))
  }
}
