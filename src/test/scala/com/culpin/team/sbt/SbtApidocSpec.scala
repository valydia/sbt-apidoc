package com.culpin.team.sbt

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

class SbtApidocSpec extends FlatSpec with Matchers with LoggerHelper {

  "SbtApidoc" should "produce `api_project.json`" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val headerFile = new File(getClass.getResource("/header-footer/header.md").getFile)
    val headerTitle = Some("My own header title")
    val footerFile = new File(getClass.getResource("/header-footer/footer.md").getFile)
    val footerTitle = Some("My own footer title")
    val config = Config(apidocName, None, apidocDescription, "0.0.0", None, None, headerTitle, Some(headerFile), footerTitle, Some(footerFile))
    val Some((_, apiProjectString)) =  SbtApidoc.run(List(new File(getClass.getResource("/ApidocExample").getFile) -> "./the/path/ApidocExample"), config, stubLogger)
    val apiProject = ujson.read(apiProjectString)
    assert(apiProject("name").str === apidocName)
    assert(apiProject("description").str === apidocDescription)
    assert(apiProject("apidoc").str === "0.3.0")
    assert(apiProject("sampleUrl").bool === false)
    assert(apiProject("header")("content").str === "<h2 id=\"welcome-to-apidoc\">Welcome to apiDoc</h2>\n<p>Please visit <a href=\"http://apidocjs.com\">apidocjs.com</a> with the full documentation.</p>")
    assert(apiProject("header")("title").str === headerTitle.get)
    assert(apiProject("footer")("content").str === "<h2 id=\"epilogue\">Epilogue</h2>\n<p>Suggestions, contact, support and error reporting on <a href=\"https://github.com/apidoc/apidoc/issues\">GitHub</a></p>")
    assert(apiProject("footer")("title").str === footerTitle.get)
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
