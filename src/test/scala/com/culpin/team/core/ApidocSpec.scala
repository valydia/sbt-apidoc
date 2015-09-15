package com.culpin.team.core

import com.culpin.team.SbtApidocConfiguration
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, FlatSpec }
import sbt.Logger

import scala.util.{ Success => USuccess }

class ApidocSpec extends FlatSpec with Matchers with MockitoSugar {

  "Apidoc" should " parse input file and configuration" in {
    val USuccess(Some((apidata, apiconfig))) = Apidoc(Seq(), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger])
    assert(apidata === "")
    assert(apiconfig === "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"sampleUrl\":false,\n  \"version\":\"1.0\"\n}")
  }
}
