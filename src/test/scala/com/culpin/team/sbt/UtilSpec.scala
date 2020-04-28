package com.github.valydia.sbt

import org.scalatest.FlatSpec
import ujson.Js
import org.scalatest.prop.TableDrivenPropertyChecks._

class UtilSpec extends FlatSpec {

  "Util" should "merge json giving precedence to the left side" in {
    val scala1 = ujson.read("""
    {
      "lang": "scala",
      "year": 2006,
      "tags": ["fp", "oo"],
      "features": {
        "key1":"val1",
        "key2":"oldval2"
      }
    }""")

    val scala2 = ujson.read("""
    {
      "tags": ["static-typing","fp"],
      "compiled": true,
      "lang": "scala",
      "features": {
        "key2":"newval2",
        "key3":"val3"
      }
    }""")

    val expectedMergeResult = ujson.read("""
    {
      "lang": "scala",
      "year": 2006,
      "tags": ["fp", "oo", "static-typing"],
      "features": {
        "key1":"val1",
        "key2":"oldval2",
        "key3":"val3"
      },
      "compiled": true
    }""")

    assert(Util.merge(scala1, scala2) === expectedMergeResult)
  }

  it should "sort by group ASC, name ASC, version DESC" in {
    val block1 =
      Js.Obj("group" -> "group1", "name" -> "name1", "version" -> "0.1.0")

    val block2 =
      Js.Obj("group" -> "abc", "name" -> "efg", "version" -> "0.1.1")

    val block3 =
      Js.Obj("group" -> "abc", "name" -> "hij", "version" -> "3.1.1")

    val block4 =
      Js.Obj("group" -> "abc", "name" -> "efg", "version" -> "3.1.1")

    val block5 =
      Js.Obj("group" -> "group1", "name" -> "name1", "version" -> "0.1.1")

    val blocks = Js.Arr(block1, block2, block3, block4, block5)

    val res = Util.sortBlocks(blocks)

    assert(res === Js.Arr(block4, block2, block3, block5, block1))
  }

  it should "render markdown with soft break" in {

    assert(Util.renderMarkDown("Here you can describe the function.\nMultilines are possible.") === "<p>Here you can describe the function. Multilines are possible.</p>")
  }

  it should "render markdown and handle breaklines between p blocks" in {
    val description =
      """|Optionally you can write here further Informations about the permission.
         |
         |An "apiDefinePermission"-block can have an "apiVersion", so you can attach the block to a specific version.""".stripMargin

    val expected =
      "<p>Optionally you can write here further Informations about the permission.</p> <p>An &quot;apiDefinePermission&quot;-block can have an &quot;apiVersion&quot;, so you can attach the block to a specific version.</p>"

    assert(Util.renderMarkDown(description) === expected)
  }

  val defaultVersionTestCases =
    Table(
      ("apidocVersion", "projectVersion", "expected"),
      (Some("invalid"), "invalid", "0.0.0"),
      (None, "invalid", "0.0.0"),
      (Some("1.0"), "", "1.0.0"),
      (Some("1.2.3"), "", "1.2.3"),
      (Some("1.2.3-SNAPSHOT"), "", "1.2.3"),
      (Some("invalid"), "1.2.3", "1.2.3"),
      (None, "1.2.3", "1.2.3" ),
      (None, "1.2.3-SNAPSHOT", "1.2.3"),
      // FIXME Strange behaviour
      //see https://github.com/valydia/sbt-apidoc/issues/11
      (None, "1.2.3+0-1234abcd+20140707-1030", "1.2.0" )
    )

  it should "calculate the default ApiVersion" in new LoggerHelper {
    forAll(defaultVersionTestCases){ (apidocVersion, projectVersion, expected) =>
      assert(Util.defaultVersion(apidocVersion, projectVersion, stubLogger) === expected)
    }
  }

}
