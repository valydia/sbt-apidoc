package com.culpin.team.core

import java.io.File

import com.culpin.team.filter.Filter
import com.culpin.team.parser.Parser
import com.culpin.team.worker.Worker
import com.gilt.gfc.semver.SemVer
import org.json4s.{ FieldSerializer, NoTypeHints }

import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty

import org.json4s.jackson.JsonMethods._

import scala.util.{ Success, Try }

import org.json4s.JsonAST.{ JString, JArray, JNothing }

case class Element(source: String, name: String, sourceName: String, content: String)

case class SbtApidocConfiguration(name: String, description: String, sampleUrl: Option[String], version: String)

object Apidoc {

  /**
   * @param sources the sources file to process
   * @param config the apidoc configuration
   * @return A Some(Pair) of JSon string if there are some apidoc comment, None if not
   */
  def apply(sources: List[File], config: SbtApidocConfiguration): Try[Option[(String, String)]] = {
    println("run  parser")
    val (blocks, filenames) = Parser(sources)

    println("run  worker")
    val processedFiles = Worker(blocks, filenames, config)

    println("run  filter")
    val filteredBlocks = Filter(processedFiles)

    val sortedBlocks = sortBlock(filteredBlocks)

    if (sortedBlocks.children.isEmpty || sortedBlocks.children == List(JNothing))
      Success(None)
    else {

      implicit val formats = Serialization.formats(NoTypeHints) + buildSbtApidocSerializer
      Success(Some((pretty(sortedBlocks), writePretty(config))))
    }
  }

  // sort by group ASC, name ASC, version DESC
  def sortBlock(blocks: JArray): JArray = {
    val sortedChildren = blocks.arr.sortWith {
      case (a, b) =>
        val JString(groupA) = a \ "group"
        val JString(nameA) = a \ "name"

        val JString(groupB) = b \ "group"
        val JString(nameB) = b \ "name"

        val labelA = groupA + nameA
        val labelB = groupB + nameB

        if (labelA.equals(labelB)) {
          val JString(versionA) = a \ "version"
          val JString(versionB) = b \ "version"
          (SemVer(versionA) compareTo SemVer(versionB)) >= 0
        } else {
          (labelA compareTo labelB) <= 0
        }
    }
    JArray(sortedChildren)
  }

  def buildSbtApidocSerializer: FieldSerializer[SbtApidocConfiguration] = {

    val emptySampleUrl: PartialFunction[(String, Any), Option[(String, Any)]] = { case ("sampleUrl", None) => Some("sampleUrl", "false") }
    val definedSampleUrlSerializer: PartialFunction[(String, Any), Option[(String, Any)]] = { case ("sampleUrl", Some(url)) => Some("sampleUrl", url) }
    val nameSerializer: PartialFunction[(String, Any), Option[(String, Any)]] = { case ("name", name) => Some("name", name) }
    val descriptionSerializer: PartialFunction[(String, Any), Option[(String, Any)]] = { case ("description", description) => Some("description", description) }
    val versionSerializer: PartialFunction[(String, Any), Option[(String, Any)]] = { case ("version", version) => Some("version", version) }

    val serializer: PartialFunction[(String, Any), Option[(String, Any)]] = nameSerializer orElse descriptionSerializer orElse definedSampleUrlSerializer orElse emptySampleUrl orElse versionSerializer

    FieldSerializer[SbtApidocConfiguration](
      serializer,
      PartialFunction.empty
    )
  }

}
