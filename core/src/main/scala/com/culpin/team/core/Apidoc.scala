package com.culpin.team.core

import java.io.File

//import com.culpin.team.SbtApidocConfiguration
import com.culpin.team.filter.Filter
import com.culpin.team.parser.Parser
import org.json4s.JsonAST.{ JNothing, JArray }

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty

import org.json4s.jackson.JsonMethods._

import scala.util.{ Success, Try }

case class Element(source: String, name: String, sourceName: String, content: String)

case class SbtApidocConfiguration(name: String, description: String, sampleUrl: Option[String], version: String)

object Apidoc {

  /**
   * @param sources the sources file to process
   * @param config the apidoc configuration
   * @return A Some(Pair) of JSon string if there are some apidoc comment, None if not
   */
  def apply(sources: Seq[File], config: SbtApidocConfiguration): Try[Option[(String, String)]] = {

    val (blocks,filenames) = Parser(sources)
    val filteredBlocks = Filter(blocks)
    if (filteredBlocks.children == List(JNothing))
      Success(None)
    else {
      implicit val formats = Serialization.formats(NoTypeHints)
      Success(Some((pretty(filteredBlocks), writePretty(config))))
    }
  }

}
