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

    val parsedFiles = ujson.read(parsedFileString).asInstanceOf[Js.Arr]
    val worker = new ApiParamTitleWorker

    val result = worker.postProcess(parsedFiles, List(), preProcessJson)

    assert(result === parsedFiles)
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

  "ApiUser Worker" should " postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/apiusePreprocess.json").getFile)
    val preProcessString = readFile(preProcessFiles)
    val preProcessJson = ujson.read(preProcessString)

    val blocksFiles = new File(getClass.getResource("/apiuseBlocks.json").getFile)
    val blocksString = readFile(blocksFiles)


    val parsedFiles = ujson.read(blocksString).asInstanceOf[Js.Arr]
    val worker = new ApiUseWorker

    val result = worker.postProcess(parsedFiles,List(), preProcessJson)
    val error = result(0)(0)("local")( "error")

    val error4XX = error("fields")("Error 4xx")
    val error4XX_0 = error4XX(0)
    assert(error4XX_0("group") === Js.Str("Error 4xx"))
    assert(error4XX_0("optional") === Js.Bool(false))
    assert(error4XX_0("field") === Js.Str("NoAccessRight"))
    assert(error4XX_0("description") === Js.Str("Only authenticated Admins can access the data."))

    val error4XX_1 = error4XX(1)
    assert(error4XX_1("group") === Js.Str("Error 4xx"))
    assert(error4XX_1("optional") === Js.Bool(false))
    assert(error4XX_1("field") === Js.Str("UserNameTooShort"))
    assert(error4XX_1("description") === Js.Str("Minimum of 5 characters required."))

    val examples = error("examples")(0)
    assert(examples("title") === Js.Str("Response (example):"))
    assert(examples("content") === Js.Str("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(examples("type") === Js.Str("json"))

  }

  "ApiGroupWorker" should " postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/preprocess.json").getFile)
    val preProcessString = readFile(preProcessFiles)
    val preProcessJson = ujson.read(preProcessString)

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = readFile(parsedFilesFiles)

    val parsedFiles = ujson.read(parsedFileString).asInstanceOf[Js.Arr]
    val worker = new ApiGroupWorker

    val result = worker.postProcess(parsedFiles, List("_apidoc.js", "full-example.js"), preProcessJson)

    val (file1, file2)= (result(0), result(1))

    val (block4, block5, block6) = (file1(3), file1(4), file1(5))
    val (block2_1, block2_2, block2_3) = (file2(0), file2(1), file2(2))
    assert(block4("local")("groupTitle") === Js.Str("User"))
    assert(block5("local")("groupTitle") === Js.Str("User"))
    assert(block6("local")("groupTitle") === Js.Str("User"))
    assert(block2_1("local")("groupTitle") === Js.Str("User"))
    assert(block2_2("local")("groupTitle") === Js.Str("User"))
    assert(block2_3("local")("groupTitle") === Js.Str("User"))
//      assert((result \\ "groupTitle").children.size === 6)

  }

  "ApiNameWorker" should " postprocess" in {

    val preProcessFiles = new File(getClass.getResource("/preprocess.json").getFile)
    val preProcessString = readFile(preProcessFiles)
    val preProcessJson = ujson.read(preProcessString)

    val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
    val parsedFileString = readFile(parsedFilesFiles)
    val parsedFiles = ujson.read(parsedFileString).asInstanceOf[Js.Arr]
    val worker = new ApiNameWorker

    val result = worker.postProcess(parsedFiles, List(), preProcessJson)

    val (file1, file2)= (result(0), result(1))

    val (block4, block5, block6) = (file1(3), file1(4), file1(5))
    val (block2_1, block2_2, block2_3) = (file2(0), file2(1), file2(2))
    assert(block4("local")("name") === Js.Str("GetUser"))
    assert(block5("local")("name") === Js.Str("GetUser"))
    assert(block6("local")("name") === Js.Str("PostUser"))
    assert(block2_1("local")("name") === Js.Str("GetUser"))
    assert(block2_2("local")("name") === Js.Str("PostUser"))
    assert(block2_3("local")("name") === Js.Str("PutUser"))

  }


}
