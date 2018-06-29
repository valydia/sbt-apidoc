package com.culpin.team.sbt

import org.scalatest.FlatSpec


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

}
