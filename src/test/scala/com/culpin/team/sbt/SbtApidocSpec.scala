package com.github.valydia.sbt

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import ujson.Js

class SbtApidocSpec extends FlatSpec with Matchers with LoggerHelper {

  "SbtApidoc" should "produce `api_project.json`" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val headerFile = new File(getClass.getResource("/header-footer/header.md").getFile)
    val headerTitle = Some("My own header title")
    val footerFile = new File(getClass.getResource("/header-footer/footer.md").getFile)
    val footerTitle = Some("My own footer title")
    val url = "apiUrl"
    val sampleUrl = "apiSampleUrl"
    val config =
      Config(
        apidocName,
        None,
        apidocDescription,
        "0.0.0",
        "buildVersion",
        new File("whocares"),
        Some(url),
        Some(sampleUrl),
        headerTitle,
        Some(headerFile),
        footerTitle,
        Some(footerFile),
        List("a", "b", "c"),
        None,
        None
      )
    val Some((_, apiProjectString)) =  SbtApidoc.run(List(new File(getClass.getResource("/ApidocExample").getFile) -> "./the/path/ApidocExample"), config, stubLogger)
    val apiProject = ujson.read(apiProjectString)
    assert(apiProject("name").str === apidocName)
    assert(apiProject("description").str === apidocDescription)
    assert(apiProject("sampleUrl").str === sampleUrl)
    assert(apiProject("url").str === url)
    assert(apiProject("header")("content").str === "<h2 id=\"welcome-to-apidoc\">Welcome to apiDoc</h2>\n<p>Please visit <a href=\"http://apidocjs.com\">apidocjs.com</a> with the full documentation.</p>")
    assert(apiProject("header")("title").str === headerTitle.get)
    assert(apiProject("footer")("content").str === "<h2 id=\"epilogue\">Epilogue</h2>\n<p>Suggestions, contact, support and error reporting on <a href=\"https://github.com/apidoc/apidoc/issues\">GitHub</a></p>")
    assert(apiProject("footer")("title").str === footerTitle.get)
    assert(apiProject("order") === Js.Arr("a", "b", "c"))
    assert(apiProject("generator")("name").str === "sbt-apidoc")
    assert(apiProject("generator")("version").str === "buildVersion")

  }

  "SbtApidoc" should "produce `api_project.json` with default" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val config =
      Config(
        apidocName,
        None,
        apidocDescription,
        "0.0.0",
        "buildVersion",
        new File("whocares"),
        None,
        None,
        None,
        None,
        None,
        None,
        List(),
        None,
        None
      )
    val Some((_, apiProjectString)) =
      SbtApidoc.run(
        List(
          new File(getClass.getResource("/ApidocExample").getFile) -> "./the/path/ApidocExample"
        ), config, stubLogger)
    val apiProject = ujson.read(apiProjectString)
    assert(apiProject("name").str === apidocName)
    assert(apiProject("description").str === apidocDescription)
    assert(apiProject("sampleUrl").bool === false)
    assert(apiProject("generator")("name").str === "sbt-apidoc")
    assert(apiProject("generator")("version").str === "buildVersion")
    assert(!apiProject.obj.contains("template"))
    assert(!apiProject.obj.contains("footer"))
    assert(!apiProject.obj.contains("header"))
    assert(!apiProject.obj.contains("url"))
    assert(!apiProject.obj.contains("order"))
  }


  "SbtApidoc" should "successfully parse Inherit" in {
    val apidocName = "apidoc-example"
    val apidocDescription = "description"
    val config =
      Config(
        apidocName,
        None,
        apidocDescription,
        "0.0.0",
        "0.0.0",
        new File("whocares"),
        None,
        None,
        None,
        None,
        None,
        None,
        List(),
        None,
        None
      )
    SbtApidoc.run(
      List(
        new File(
          getClass.getResource("/Inherit.scala").getFile) ->
          "./the/path/Inherit.scala"
        ),
        config,
        stubLogger
      )
  }
}
