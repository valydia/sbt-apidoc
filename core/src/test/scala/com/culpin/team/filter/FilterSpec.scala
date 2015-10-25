package com.culpin.team.filter

import org.scalatest.{ Matchers, FlatSpec }
import org.json4s.JsonDSL._

class FilterSpec extends FlatSpec with Matchers {

  "Filter" should "leave unchanged a Jobject with now duplicate" in {
    val v = ("key1" -> "value1") ~ ("key2" -> "value2") ~ ("key3" -> "value3")
    assert(Filter.filterDuplicateKeys(v) === v)
  }

  "Filter" should "remove duplicate key" in {
    val v = ("key1" -> "value1") ~ ("key1" -> "value2") ~ ("key3" -> "value3")
    assert(Filter.filterDuplicateKeys(v) ===  ("key1" -> "value1")~ ("key3" -> "value3"))
  }
}
