package com.culpin.team.util

import org.scalatest.{ FlatSpec, Matchers }

class UtilSpec extends FlatSpec with Matchers {

  "Util unindent" should "strip common leading spaces " in {
    assert(Util.unindent("  a\n    b\n   c") === "a\n  b\n c")
  }

  "Util unindent" should "strip common leading tabs" in {
    assert(Util.unindent("\t\ta\n\t\t\t\tb\n\t\t\tc") === "a\n\t\tb\n\tc")
  }

  "Util unindent" should "should strip all leading whitespace from a single line" in {
    assert(Util.unindent("   \t   a") === "a")
  }

  "Util unindent" should "should not modify the empty string" in {
    assert(Util.unindent("") === "")
  }

  "Util unindent" should "should not modify if any line starts with non-whitespace" in {
    val s = "    a\n   b\nc   d\n   e"
    assert(Util.unindent(s) === s)
  }

  "Util unindent" should "should strip common leading tabs and keep spaces" in {

    assert(Util.unindent("\ta\n\t  b\n\t c") === "a\n  b\n c")
  }

  "Util unindent" should "should strip common leading tabs and 1 space on each line" in {
    val s = "    a\n   b\nc   d\n   e"
    assert(Util.unindent("\ta\n\t  b\n\t c") === "a\n  b\n c")
  }

}
