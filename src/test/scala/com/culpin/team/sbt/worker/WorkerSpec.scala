package com.culpin.team.sbt.worker

import java.io.File

import com.culpin.team.sbt.parser.Parser
import org.scalatest.FlatSpec
import sbt.util.{Level, Logger}
import ujson.Js

class WorkerSpec extends FlatSpec  {

  val stubLogger = new Logger {
    override def log(level: Level.Value, message: => String): Unit = ()

    override def trace(t: => Throwable): Unit = ()

    override def success(message: => String): Unit = ()
  }

  private def loadFixture(parsedFilesUrl: String = "/parsedFiles-filename.json", preprocessUrl: String = "/preprocess.json"): (Js.Arr, Js.Value) = {
    val preProcessFiles = new File(getClass.getResource(preprocessUrl).getFile)
    val preProcessString = readFile(preProcessFiles)


    val parsedFilesFiles = new File(getClass.getResource(parsedFilesUrl).getFile)
    val parsedFileString = readFile(parsedFilesFiles)
    (ujson.read(parsedFileString).asInstanceOf[Js.Arr],  ujson.read(preProcessString))

  }

  private def readFile(file: File): String = {
    val source = scala.io.Source.fromFile(file)
    val src = try source.mkString finally source.close()
    src
  }

  "ApiParamTitleWorker" should "preprocess parsed files" in {
    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)

    val parsedFiles: Js.Arr = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiParamTitleWorker
    val result = worker.preProcess(parsedFiles, "defineErrorTitle")("define")

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

    val (parsedFiles, preProcessJson) = loadFixture()
    val worker = new ApiParamTitleWorker

    val result = worker.postProcess(parsedFiles, List(), None, preProcessJson)

    assert(result === parsedFiles)
  }

  "ApiUseWorker" should " preProcess parsed Files" in {

    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)
    val jarray = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiUseWorker
    val result = worker.preProcess(jarray, "defineErrorStructure")()
    val structure = result("defineErrorStructure")
    assert(structure == Js.Obj())
  }

  "ApiUseWorker" should " preProcess parsed Files 2" in {

    val file = new File(getClass.getResource("/parsedFiles.json").getFile)
    val jsonString = readFile(file)
    val jarray = ujson.read(jsonString).asInstanceOf[Js.Arr]

    val worker = new ApiUseWorker
    val result = worker.preProcess(jarray)()

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

    val (parsedFiles, preProcessJson) = loadFixture("/apiuseBlocks.json","/apiusePreprocess.json")
    val worker = new ApiUseWorker

    val result = worker.postProcess(parsedFiles, List(), None, preProcessJson)
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

    val (parsedFiles, preProcessJson) = loadFixture()
    val worker = new ApiGroupWorker

    val result = worker.postProcess(parsedFiles, List("_apidoc.js", "full-example.js"), None, preProcessJson)

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

    val (parsedFiles, preProcessJson) = loadFixture()
    val worker = new ApiNameWorker

    val result = worker.postProcess(parsedFiles, List(), None, preProcessJson)


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


  "ApiPermissionWorker" should " postprocess" in {

    val (parsedFiles, preProcessJson) = loadFixture()
    val worker = new ApiPermissionWorker

    val result = worker.postProcess(parsedFiles, List("_apidoc.js", "full-example.js"), None, preProcessJson)

    val (file1, file2)= (result(0), result(1))

    val (block4, block5, block6) = (file1(3), file1(4), file1(5))


    val permission4 = block4("local")("permission")(0)
    assert(permission4("name") === Js.Str("admin"))
    assert(permission4("title") === Js.Str("This title is visible in version 0.1.0 and 0.2.0"))
    assert(permission4("description") === Js.Str(""))

    val permission5 = block5("local")("permission")(0)

    assert(permission5("name") === Js.Str("admin"))
    assert(permission5("title") === Js.Str("This title is visible in version 0.1.0 and 0.2.0"))
    assert(permission5("description") === Js.Str(""))

    val permission6 = block6("local")("permission")(0)

    assert(permission6("name") === Js.Str("none"))
    assert(permission6("title") === Js.Str(""))
    assert(permission6("description") === Js.Str(""))


    val (block1, block2, block3) = (file2(0), file2(1), file2(2))

    val permission1 = block1("local")("permission")(0)

    assert(permission1("name") === Js.Str("admin"))
    assert(permission1("title") === Js.Str("Admin access rights needed."))
    assert(permission1("description") === Js.Str("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

    val permission2 = block2("local")("permission")(0)

    assert(permission2("name") === Js.Str("none"))
    assert(permission2("title") === Js.Str(""))
    assert(permission2("description") === Js.Str(""))

    val permission3 = block3("local")("permission")(0)

    assert(permission3("name") === Js.Str("none"))
    assert(permission3("title") === Js.Str(""))
    assert(permission3("description") === Js.Str(""))

//      assert(JArray(l).diff(result).deleted === Js.Null)
//      assert(JArray(l).diff(result).changed === Js.Null)

  }

  "ApiSampleRequestWorker" should " postprocess - local url with sampleURL" in {

    val (parsedFiles, preProcessJson) = loadFixture()
    val worker = new ApiSampleRequestWorker

    val result = worker.postProcess(parsedFiles, List("_apidoc.js", "full-example.js"), Option("https://api.github.com/v1"), preProcessJson)
    assert(result === parsedFiles)

  }

  it should " postprocess with sampleURL" in {

    val (parsedFiles, filenames) = Parser(List(new File(getClass.getResource("/sampleRequest.js").getFile)), stubLogger)


    val worker = new ApiSampleRequestWorker

    val result = worker.postProcess(parsedFiles, filenames, Option("https://api.github.com/v1"), Js.Null)

    val file1 = result(0)
    val (block1, block2, block3) = (file1(0), file1(1), file1(2))
    assert(block1("local")("sampleRequest")(0)("url") === Js.Str("http://www.example.com/user/4711"))
    assert(block2("local")("sampleRequest")(0)("url")  === Js.Str("https://api.github.com/v1/car/4711"))
    assert(block3("local")("sampleRequest") === Js.Null)
  }

  "ApiSampleRequestWorker" should " postprocess without sampleURL" in {

    val (parsedFiles, filenames) = Parser(List(new File(getClass.getResource("/sampleRequest.js").getFile)), stubLogger)


    val worker = new ApiSampleRequestWorker

    val result = worker.postProcess(parsedFiles, filenames, None, Js.Null)

    val file1 = result(0)
    val (block1, block2, block3) = (file1(0), file1(1), file1(2))
    assert(block1("local")("sampleRequest")(0)("url") === Js.Str("http://www.example.com/user/4711"))
    assert(block2("local")("sampleRequest")(0)("url")  === Js.Str("/car/4711"))
    assert(block3("local")("sampleRequest") === Js.Null)

  }

}
