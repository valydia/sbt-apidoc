package com.culpin.team.util

import java.io.File

import scala.util.matching.Regex

import scala.language.postfixOps

object Util {

  def unindent(s: String): String = {
    val lines = s.split('\n')
    val nonWhiteSpaceRegex = """\S""".r

    val xs = lines.filter(matches(nonWhiteSpaceRegex, _)).sorted
    if (xs.isEmpty)
      s
    else {
      val a = xs.head
      val b = xs.last

      val whiteSpaceRegex = """\s""".r

      val i = (a zip b) takeWhile { case (aa, bb) => matches(whiteSpaceRegex, aa.toString) && aa == bb } length

      lines.map(_.substring(i)).mkString("\n")
    }

  }

  def matches(nonWhiteSpaceRegex: Regex, input: String): Boolean = {
    nonWhiteSpaceRegex.findFirstIn(input).isDefined
  }

  def trim(s: String): String = {
    val trimRegex = """^\s*|\s*$""".r
    trimRegex.replaceAllIn(s, "")
  }

  def readFile(file: File): String = {
    val source = scala.io.Source.fromFile(file)
    val src = try source.mkString finally source.close()
    src
  }

}
