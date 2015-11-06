package com.culpin.team.filter

import java.io.File

import com.culpin.team.util.Util
import org.json4s.JsonAST.JArray
import org.scalatest.{ Matchers, FlatSpec }
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

class FilterSpec extends FlatSpec with Matchers {

  "Filter" should "leave unchanged a Jobject with now duplicate" in {
    val v = ("key1" -> "value1") ~ ("key2" -> "value2") ~ ("key3" -> "value3")
    assert(Filter.filterDuplicateKeys(v) === v)
  }

  "Filter" should "remove duplicate key" in {
    val v = ("key1" -> "value1") ~ ("key1" -> "value2") ~ ("key3" -> "value3")
    assert(Filter.filterDuplicateKeys(v) === ("key1" -> "value1") ~ ("key3" -> "value3"))
  }

  "Filter" should "remove non empty block with global" in {

    val rawblocks = new File(getClass.getResource("/rawblocks.json").getFile)
    val JArray(json) = parse(Util.readFile(rawblocks))
    val filteredBlocks = Filter(JArray(json))

    assert(filteredBlocks.children.size === 6)
  }
}
