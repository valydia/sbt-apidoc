package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration
import com.culpin.team.filter.Filter
import com.culpin.team.parser.Parser

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty
import sbt.Logger

import scala.util.{ Success => USuccess, Try }

case class Element(source: String, name: String, sourceName: String, content: String)
case class Block(`type`: Option[String] = None, title: Option[String] = None, name: Option[String] = None,
  url: Option[String] = None, group: Option[String] = None, version: Option[String] = None,
  description: Option[String] = None, success: Option[Success] = None,
  size: Option[String] = None, optional: Option[String] = None, field: Option[String] = None,
  defaultValue: Option[String] = None, examples: Option[List[Example]] = None, error: Option[Error] = None)
case class Example(title: Option[String], content: Option[String], `type`: Option[String])
case class Success(examples: List[Example])
case class Error(examples: List[Example])
object Apidoc {

  /**
   * @param sources the sources file to process
   * @param config the apidoc configuration
   * @param log the sbt logger
   * @return A Some(Pair) of JSon string if there are some apidoc comment, None if not
   */
  def apply(sources: Seq[File], config: SbtApidocConfiguration, log: Logger): Try[Option[(String, String)]] = {

    val blocks = Parser(sources, log)
    val filteredBlocks = Filter(blocks)
    filteredBlocks match {
      case Seq() => USuccess(None)
      // case Seq(Seq()) => USuccess(None)
      case _ => {
        implicit val formats = Serialization.formats(NoTypeHints)
        USuccess(Some((writePretty(filteredBlocks), writePretty(config))))
      }
    }
  }

}
