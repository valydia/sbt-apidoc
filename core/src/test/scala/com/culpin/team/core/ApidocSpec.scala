package com.culpin.team.core

import java.io.File


import com.culpin.team.util.Util
import org.scalatest.{ Matchers, FlatSpec }


import scala.util.{ Success => USuccess }

class ApidocSpec extends FlatSpec with Matchers {

  "Apidoc" should " parse empty input file and configuration" in {
    assert(Apidoc(Seq(), SbtApidocConfiguration("name", "description", false, "1.0")) === USuccess(None))
  }

  "Apidoc" should " parse very basic input file and configuration" in {
    val sources = Seq(new File(getClass.getResource("/Application.scala").getFile))
    val conf = SbtApidocConfiguration("name", "description", false, "1.0")
    val USuccess(Some((apidata, apiconfig))) = Apidoc(sources, conf)
    val expectedApiData = "[ {\n  \"type\" : \"get\",\n  \"url\" : \"/\",\n  \"title\" :" +
      " \"Home page.\",\n  \"name\" : \"Welcome_Page.\",\n  \"group\" : \"Application\"," +
      "\n  \"version\" : \"1.0.0\",\n  \"description\" : \"Renders the welcome page\",\n" +
      "  \"success\" : {\n    \"examples\" : [ {\n      \"title\" : \"Success-Response:"  +
      "\",\n      \"content\" : \"HTTP/1.1 200 OK\\nHTML for welcome page\\n{\\n  \\\"em" +
      "ailAvailable\\\": \\\"true\\\"\\n}\",\n      \"type\" : \"json\"\n    } ]\n  }\n} ]"
    assert(apidata === expectedApiData)

    val expectedConf = "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"" +
      "sampleUrl\":false,\n  \"version\":\"1.0\"\n}"
    assert(apiconfig === expectedConf)
  }

  "Apidoc" should " parse basic input file and configuration" in {
    val sources = Seq(new File(getClass.getResource("/simple-example.js").getFile))
    val conf = SbtApidocConfiguration("name", "description", false, "1.0")
    val USuccess(Some((apidata, apiconfig))) = Apidoc(sources, conf)

    val expectedApiData = Util.readFile(new File(getClass.getResource("/expected/apidata.json").getFile))
    assert(apidata === expectedApiData)
    val expectedConf = "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"" +
      "sampleUrl\":false,\n  \"version\":\"1.0\"\n}"
    assert(apiconfig === expectedConf)
  }

}
