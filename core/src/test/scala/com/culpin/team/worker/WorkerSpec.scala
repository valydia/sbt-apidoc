package com.culpin.team.worker

import java.io.File

import com.culpin.team.parser.Parser
import com.culpin.team.util.Util
import org.json4s.JsonAST.JArray
import org.scalatest.{Matchers, FlatSpec}

import org.json4s._
import org.json4s.native.JsonMethods._

class WorkerSpec  extends FlatSpec with Matchers {

  "ApiParamTitleWorker" should " preProcess parsed Files" in {
    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiParamTitleWorker
    val result = worker.preProcess(JArray(json), "defineErrorTitle")

    val defineErrorTitle = result \ "defineErrorTitle"
    val createUser = defineErrorTitle \ "CreateUserError" \ "0.2.0"
    assert( createUser \ "name" === JString("CreateUserError"))
    assert( createUser \ "title" === JString(""))
    assert( createUser \ "description" === JString(""))

    val admin_0_3 = defineErrorTitle \ "admin" \ "0.3.0"
    assert( admin_0_3 \ "name" === JString("admin"))
    assert( admin_0_3 \ "title" === JString("Admin access rights needed."))
    assert( admin_0_3 \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val admin_0_1 = defineErrorTitle \ "admin" \ "0.1.0"
    assert( admin_0_1 \ "name" === JString("admin"))
    assert( admin_0_1 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert( admin_0_1 \ "description" === JString(""))

  }

  "ApiUseWorker" should " preProcess parsed Files" in {


    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiUseWorker
    val result = worker.preProcess(JArray(json), "defineErrorStructure")
    val structure = result \ "defineErrorStructure"
    assert( structure == JObject())
  }

  "ApiUseWorker" should " preProcess parsed Files 2" in {


    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiUseWorker
    val result = worker.preProcess(JArray(json))

    val createUser =  result \ "define" \ "CreateUserError" \ "0.2.0"
    assert( createUser \ "version" === JString("0.2.0"))
    val errorField = createUser \ "error" \"fields" \ "Error 4xx"



    val error1 = errorField.children(0)
    assert( error1 \ "group" === JString("Error 4xx"))
    assert( error1 \ "optional" === JString("false"))
    assert( error1 \ "field" === JString("NoAccessRight"))
    assert( error1 \ "description" === JString("Only authenticated Admins can access the data."))

    val error2 = errorField.children(1)
    assert( error2 \ "group" === JString("Error 4xx"))
    assert( error2 \ "optional" === JString("false"))
    assert( error2 \ "field" === JString("UserNameTooShort"))
    assert( error2 \ "description" === JString("Minimum of 5 characters required."))

    val examples = createUser \ "error" \ "examples"
    assert(examples \ "title" === JString("Response (example):"))
    assert(examples \ "content" === JString("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(examples \ "type" === JString("json"))


  }



  "ApiPermissionWorker" should " preProcess parsed Files" in {
    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val JArray(json) = parse(jsonString)

    val worker = new ApiPermissionWorker
    val result = worker.preProcess(JArray(json))

    val defineErrorTitle = result \ "definePermission"
    val createUser = defineErrorTitle \ "CreateUserError" \ "0.2.0"
    assert( createUser \ "name" === JString("CreateUserError"))
    assert( createUser \ "title" === JString(""))
    assert( createUser \ "description" === JString(""))

    val admin_0_3 = defineErrorTitle \ "admin" \ "0.3.0"
    assert( admin_0_3 \ "name" === JString("admin"))
    assert( admin_0_3 \ "title" === JString("Admin access rights needed."))
    assert( admin_0_3 \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val admin_0_1 = defineErrorTitle \ "admin" \ "0.1.0"
    assert( admin_0_1 \ "name" === JString("admin"))
    assert( admin_0_1 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert( admin_0_1 \ "description" === JString(""))

  }

  "Worker" should " process filename" in {

    val sources = Seq(new File(getClass.getResource("/_apidoc.js").getFile),
      new File(getClass.getResource("/full-example.js").getFile))

    val (json,filenames) = Parser(sources)


    val JArray(List(file1,file2)) = Worker.processFilename(json, filenames.toList)

    val JArray(blocks1) = file1

    //blocks1(3)
    assert(blocks1(3) \ "local" \ "filename" === JString("_apidoc.js"))
    assert(blocks1(4) \ "local" \ "filename" === JString("_apidoc.js"))
    assert(blocks1(5) \ "local" \ "filename" === JString("_apidoc.js"))

    val JArray(blocks2) = file2

    assert(blocks2(0) \ "local" \ "filename" === JString("full-example.js"))
    assert(blocks2(1) \ "local" \ "filename" === JString("full-example.js"))
    assert(blocks2(2) \ "local" \ "filename" === JString("full-example.js"))

  }

    "Worker" should " preProcess " in {


      val sources = Seq(new File(getClass.getResource("/_apidoc.js").getFile),
        new File(getClass.getResource("/full-example.js").getFile))

      val (json,filenames) = Parser(sources)

      val parsedFiles = Worker.processFilename(json, filenames.toList)

      val preProcessResults = Worker.preProcess(parsedFiles)


      assert( preProcessResults.children.size === 11)
      assert(preProcessResults \ "defineErrorStructure" === JObject())
      assert(preProcessResults \ "defineHeaderStructure" === JObject())
      assert(preProcessResults \ "defineSuccessStructure" === JObject())
      assert(preProcessResults \ "defineErrorTitle" \ "CreateUserError"  !== JNothing)
      assert(preProcessResults \ "defineErrorTitle" \ "admin"  !== JNothing)

    //TODO more test
//      println(compact(render(preProcessResults)))

  }




    "ApiParmaTitle Worker" should " postprocess" in {

      val preProcessFiles = new File(getClass.getResource("/expected/preprocess.json").getFile)
      val preProcessString = Util.readFile(preProcessFiles)
      val preProcessJson = parse(preProcessString)

      val parsedFilesFiles = new File(getClass.getResource("/parsedFiles-filename.json").getFile)
      val parsedFileString = Util.readFile(parsedFilesFiles)

      val JArray(l) = parse(parsedFileString)
      val worker = new ApiParamTitleWorker

      val result = worker.postProcess(JArray(l), preProcessJson)

      assert(result === JArray(l))
  }


}
