package com.culpin.team.sbt

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

class SbtApidocSpec extends FlatSpec with Matchers with LoggerHelper {

  "SbtApidoc" should "produce `api_project.json`" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val config = Config(apidocName, None, apidocDescription, "0.0.0", None, None, None, None, None, None)
    val Some((_, apiProjectString)) =  SbtApidoc.run(List(new File(getClass.getResource("/ApidocExample").getFile) -> "./the/path/ApidocExample"), config, stubLogger)
    val apiProject = ujson.read(apiProjectString)
    assert(apiProject("name").str === apidocName)
    assert(apiProject("description").str === apidocDescription)
    assert(apiProject("apidoc").str === "0.3.0")
    assert(apiProject("sampleUrl").bool === false)
    assert(apiProject("generator")("name").str === "sbt-apidoc")
  }

  "SbtApidoc" should "produce `api_project.json` in Inherit" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val config = Config(apidocName, None, apidocDescription, "0.0.0", None, None, None, None, None, None)
    val Some((_, apiProjectString)) =  SbtApidoc.run(List(new File(getClass.getResource("/Inherit.scala").getFile) -> "./the/path/Inherit.scala"), config, stubLogger)
    val apiProject = ujson.read(apiProjectString)
    assert(apiProject("name").str === apidocName)
    assert(apiProject("description").str === apidocDescription)
    assert(apiProject("apidoc").str === "0.3.0")
    assert(apiProject("sampleUrl").bool === false)
    assert(apiProject("generator")("name").str === "sbt-apidoc")
  }

}
