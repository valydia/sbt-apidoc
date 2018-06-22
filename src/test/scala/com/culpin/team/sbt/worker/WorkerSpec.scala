package com.culpin.team.sbt.worker

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import ujson.Js

class WorkerSpec extends FlatSpec with Matchers  {

  def readFile(file: File): String = {
    val source = scala.io.Source.fromFile(file)
    val src = try source.mkString finally source.close()
    src
  }

  "ApiParamTitleWorker" should "preprocess parsed files" in {
    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)

    val parsedFiles: Js.Arr = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiParamTitleWorker
    val result = worker.preProcess(parsedFiles, "0.0.0", "defineErrorTitle")("define")
    println("----- " + result)

    val defineErrorTitle = result("defineErrorTitle")
    val createUser = defineErrorTitle("CreateUserError")("0.2.0")
    assert(createUser("name") === Js.Str("CreateUserError"))
    assert(createUser("title") === Js.Str(""))
    assert(createUser("description") === Js.Str(""))

    val admin_0_3 = defineErrorTitle("admin")("0.3.0")
    assert(admin_0_3("name") === Js.Str("admin"))
    assert(admin_0_3("title") === Js.Str("Admin access rights needed."))
    assert(admin_0_3("description") === Js.Str("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val admin_0_1 = defineErrorTitle("admin")("0.1.0")
    assert(admin_0_1("name") === Js.Str("admin"))
    assert(admin_0_1("title") === Js.Str("This title is visible in version 0.1.0 and 0.2.0"))
    assert(admin_0_1("description") === Js.Str(""))
  }


}
