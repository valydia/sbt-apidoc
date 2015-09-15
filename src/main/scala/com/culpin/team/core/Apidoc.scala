package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty
import sbt.Logger

import scala.util.{ Success => USuccess, Try }

case class Element(source: String, name: String, sourceName: String, content: String)
case class Block(`type`: Option[String] = None, title: Option[String] = None, name: Option[String] = None, url: Option[String] = None,
  group: Option[String] = None, version: Option[String] = None, description: Option[String] = None, success: Option[Success] = None,
  size: Option[String] = None, optional: Option[String] = None, field: Option[String] = None, defaultValue: Option[String] = None)
case class Example(title: Option[String], content: Option[String], `type`: Option[String])
case class Success(examples: List[Example])
//"type":"get","url":"/","title":"Home page.","name":"Welcome_Page.","group":"Application","version":"1.0.0",
// "description":"<p>Renders the welcome page</p> ",
// "success":{"examples":[{"title":"Success-Response:","content":"HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}","type":"json"
object Apidoc {

  /**
   * @param sources the sources file to process
   * @param config the apidoc configuration
   * @param log the sbt logger
   * @return A Some(Pair) of JSon string if there are some apidoc comment, None if not
   */
  def apply(sources: Seq[File], config: SbtApidocConfiguration, log: Logger): Try[Option[(String, String)]] = {

    implicit val formats = Serialization.formats(NoTypeHints)
    USuccess(Some(("", writePretty(config))))
  }

}
