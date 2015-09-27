package com.culpin.team.worker

import java.io.File

import com.culpin.team.util.Util
import org.scalatest.{Matchers, FlatSpec}

import org.json4s._
import org.json4s.native.JsonMethods._
/**
 * Created by valydia on 26/09/15.
 */


class WorkerSpec  extends FlatSpec with Matchers {

  "Worker" should "map over jarray" in {
    val file = new File(getClass.getResource("/expected/parsedFiles.json").getFile)
    val jsonString = Util.readFile(file)
    val json = parse(jsonString).asInstanceOf[JArray]

    val worker = new ApiParamTitleWorker
    val result = worker.preProcess(json, "defineErrorTitle")

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


}
