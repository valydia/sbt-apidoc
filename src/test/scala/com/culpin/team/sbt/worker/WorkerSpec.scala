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

  it should "postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/preprocess.json").getFile)
    val preProcessString = readFile(preProcessFiles)
    val preProcessJson = ujson.read(preProcessString)

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = readFile(parsedFilesFiles)

    val filenames = ujson.read(parsedFileString).asInstanceOf[Js.Arr]
    val worker = new ApiParamTitleWorker

    val result = worker.postProcess(filenames, preProcessJson)

    assert(result === filenames)
  }

  "ApiUseWorker" should " preProcess parsed Files" in {

    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)
    val jarray = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiUseWorker
    val result = worker.preProcess(jarray,"0.0.0", "defineErrorStructure")()
    val structure = result("defineErrorStructure")
    assert(structure == Js.Obj())
  }

  "ApiUseWorker" should " preProcess parsed Files 2" in {

    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)
    val jarray = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiUseWorker
    val result = worker.preProcess(jarray, "0.0.0")()

    val createUser = result("define")("CreateUserError" )("0.2.0")
    assert(createUser("version") === Js.Str("0.2.0"))
    val errorField = createUser("error" )("fields" )( "Error 4xx")

    val error1 = errorField(0)
    assert(error1("group") === Js.Str("Error 4xx"))
    assert(error1("optional") === Js.Bool(false))
    assert(error1( "field") === Js.Str("NoAccessRight"))
    assert(error1("description") === Js.Str("Only authenticated Admins can access the data."))

    val error2 = errorField(1)
    assert(error2("group") === Js.Str("Error 4xx"))
    assert(error2("optional") === Js.Bool(false))
    assert(error2("field") === Js.Str("UserNameTooShort"))
    assert(error2("description") === Js.Str("Minimum of 5 characters required."))

    val examples = createUser("error")("examples")(0)
    assert(examples("title") === Js.Str("Response (example):"))
    assert(examples("content") === Js.Str("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(examples("type") === Js.Str("json"))

  }


}
