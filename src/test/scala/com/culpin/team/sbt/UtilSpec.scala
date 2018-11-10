package com.culpin.team.sbt

import org.scalatest.FlatSpec
import ujson.Js

class UtilSpec extends FlatSpec {

  "Util" should "merge json" in {
    val scala1 = ujson.read("""
    {
      "lang": "scala",
      "year": 2006,
      "tags": ["fp", "oo"],
      "features": {
        "key1":"val1",
        "key2":"val2"
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
        "key2":"newval2",
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

    val blocks = Js.Arr(block1, block2, block3, block4)

    val res = Util.sortBlocks(blocks)

    assert(res === Js.Arr(block4, block2, block3, block1))
  }

  it should "render markdown with soft break" in {

    assert(Util.renderMarkDown("This is *Sparta*\n") === "<p>This is <em>Sparta</em></p>")
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

}
