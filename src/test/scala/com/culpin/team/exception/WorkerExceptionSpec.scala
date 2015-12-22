package com.culpin.team.exception

import java.io.File

import com.culpin.team.core.{ Apidoc, SbtApidocConfiguration }
import org.scalatest.{ Matchers, FlatSpec }

import scala.util.Failure

class WorkerExceptionSpec extends FlatSpec with Matchers {

  "Worker exception" should " display error message when no @apiDefine is set" in {

    val sources = List(new File(getClass.getResource("/exception/WorkerException.scala").getFile))
    val conf = SbtApidocConfiguration("name", "description", Some("http://api.github.com"), "1.0")
    val Failure(ex) = Apidoc(sources, conf)
    val expected = "Filename: WorkerException.scala\nBlock: 1\nelement: apiUse\nusage: @apiUse group\nexample: @apiDefine MyValidGroup Some title\n@apiUse MyValidGroup"
    assert(ex.getMessage === expected)
  }
}
