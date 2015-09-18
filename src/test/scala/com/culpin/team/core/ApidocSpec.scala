package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, FlatSpec }
import sbt.Logger

import scala.util.{ Success => USuccess }

class ApidocSpec extends FlatSpec with Matchers with MockitoSugar {

  //  "Apidoc" should " parse empty input file and configuration" in {
  //    assert(Apidoc(Seq(), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger]) === USuccess(None))
  //  }
  //
  //  "Apidoc" should " parse very basic input file and configuration" in {
  //    val USuccess(Some((apidata, apiconfig))) = Apidoc(Seq(new File(getClass.getResource("/Application.scala").getFile)), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger])
  //    val expectedApiData = "[\n  {\n    \"type\":\"get\",\n    \"title\":\"Home page.\",\n    \"name\":\"Welcome_Page.\",\n    \"url\":\"/\",\n    \"group\":\"Application\",\n    \"version\":\"1.0.0\",\n    \"description\":\"Renders the welcome page\",\n    \"success\":{\n      \"examples\":[\n        {\n          \"title\":\"Success-Response:\",\n          \"content\":\"HTTP/1.1 200 OK\\nHTML for welcome page\\n{\\n  \\\"emailAvailable\\\": \\\"true\\\"\\n}\",\n          \"type\":\"json\"\n        }\n      ]\n    }\n  }\n]"
  //
  //    assert(apidata === expectedApiData)
  //    assert(apiconfig === "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"sampleUrl\":false,\n  \"version\":\"1.0\"\n}")
  //  }

  "Apidoc" should " parse basic input file and configuration" in {
    val USuccess(Some((apidata, apiconfig))) = Apidoc(Seq(new File(getClass.getResource("/simple-example.js").getFile)), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger])
    val expectedApiData = "[\n  {\n    \"type\":\"get\",\n    \"title\":\"Home page.\",\n    \"name\":\"Welcome_Page.\",\n    \"url\":\"/\",\n    \"group\":\"Application\",\n    \"version\":\"1.0.0\",\n    \"description\":\"Renders the welcome page\",\n    \"success\":{\n      \"examples\":[\n        {\n          \"title\":\"Success-Response:\",\n          \"content\":\"HTTP/1.1 200 OK\\nHTML for welcome page\\n{\\n  \\\"emailAvailable\\\": \\\"true\\\"\\n}\",\n          \"type\":\"json\"\n        }\n      ]\n    }\n  }\n]"
    println("-------")
    println(apidata)
    //assert(apidata === expectedApiData)
    //assert(apiconfig === "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"sampleUrl\":false,\n  \"version\":\"1.0\"\n}")
  }

}
