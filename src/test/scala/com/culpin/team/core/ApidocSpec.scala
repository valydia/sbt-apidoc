package com.culpin.team.core

import java.io.File

import com.culpin.team.util.Util
import org.json4s.JsonAST.{ JArray, JObject }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, FlatSpec }

import org.json4s.JsonDSL._
import sbt.Logger

import scala.util.Success

class ApidocSpec extends FlatSpec with Matchers with MockitoSugar {

  val mockLogger = mock[Logger]

  "Apidoc" should " parse empty input file and configuration" in {

    assert(Apidoc(List(), SbtApidocConfiguration("name", "description", None, "1.0"), mockLogger) === Success(None))
  }

  "Apidoc" should " parse very basic input file and configuration" in {
    val sources = List(new File(getClass.getResource("/Application.scala").getFile))
    val conf = SbtApidocConfiguration("name", "description", None, "1.0")
    val Success(Some((apidata, apiconfig))) = Apidoc(sources, conf, mockLogger)
    val expectedApiData = "[ {\n  \"type\" : \"get\",\n  \"url\" : \"/\",\n  \"title\" :" +
      " \"Home page.\",\n  \"name\" : \"Welcome_Page_\",\n  \"group\" : \"Application\",\n " +
      " \"version\" : \"1.0.0\",\n  \"description\" : \"Renders the welcome page\",\n  \"success\"" +
      " : {\n    \"examples\" : [ {\n      \"title\" : \"Success-Response:\",\n      \"content\" :" +
      " \"HTTP/1.1 200 OK\\nHTML for welcome page\\n{\\n  \\\"emailAvailable\\\": \\\"true\\\"\\n}\"" +
      ",\n      \"type\" : \"json\"\n    } ]\n  },\n  \"filename\" : \"Application.scala\",\n " +
      " \"groupTitle\" : \"Application\"\n} ]"
    //TODO - Check Welcome page ie api name worker / parser
    assert(apidata === expectedApiData)

    val expectedConf = "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"" +
      "sampleUrl\":\"false\",\n  \"version\":\"1.0\"\n}"
    assert(apiconfig === expectedConf)
  }

  "Apidoc" should " parse basic input file and configuration" in {
    val sources = List(new File(getClass.getResource("/simple-example.js").getFile))
    val conf = SbtApidocConfiguration("name", "description", Some("http://api.github.com"), "1.0")
    val Success(Some((apidata, apiconfig))) = Apidoc(sources, conf, mockLogger)

    val expectedApiData = Util.readFile(new File(getClass.getResource("/expected/apidata.json").getFile))
    assert(apidata === expectedApiData)
    val expectedConf = "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"" +
      "sampleUrl\":\"http://api.github.com\",\n  \"version\":\"1.0\"\n}"
    assert(apiconfig === expectedConf)
  }

  "Apidoc" should "sort by group ASC, name ASC, version DESC" in {
    val block1: JObject =
      ("group" -> "group1") ~ ("name" -> "name1") ~ ("version" -> "0.1.0")

    val block2: JObject =
      ("group" -> "abc") ~ ("name" -> "efg") ~ ("version" -> "0.1.1")

    val block3: JObject =
      ("group" -> "abc") ~ ("name" -> "hij") ~ ("version" -> "3.1.1")

    val block4: JObject =
      ("group" -> "abc") ~ ("name" -> "efg") ~ ("version" -> "3.1.1")

    val blocks = JArray(List(block1, block2, block3, block4))

    val JArray(res) = Apidoc.sortBlock(blocks)

    assert(res === List(block4, block2, block3, block1))
  }

  "Apidoc" should " parse complete input file and configuration" in {
    val sources = List(new File(getClass.getResource("/_apidoc.js").getFile),
      new File(getClass.getResource("/full-example.js").getFile))
    val conf = SbtApidocConfiguration("name", "description", Some("http://api.github.com"), "1.0")
    val Success(Some((apidata, apiconfig))) = Apidoc(sources, conf, mockLogger)
    //println(apidata)
    //TODO test

  }
}
