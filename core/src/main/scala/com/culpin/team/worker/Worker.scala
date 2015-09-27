package com.culpin.team.worker

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
/**
 * @author
 */
trait Worker {

  def preProcess(parsedFiles: JArray, target: String = "define"): JObject


  def postProcess(parsedFiles: JArray): JObject

}

class ApiErrorTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineErrorTitle"): JObject =
      super.preProcess(parsedFiles, "defineErrorTitle")

  override def postProcess(parsedFiles: JArray): JObject = ???

}

class ApiHeaderTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineHeaderTitle"): JObject =
    super.preProcess(parsedFiles, "defineHeaderTitle")

  override def postProcess(parsedFiles: JArray): JObject = ???

}


class ApiParamTitleWorker extends Worker {

  override def preProcess(parsedFiles: JArray, target: String = "defineParamTitle"): JObject = {

    val source = "define"

    val initResult: JObject = (target -> JObject())

    val result = parsedFiles.arr.foldLeft(initResult) { case (result,parsedFiles) =>
      parsedFiles.asInstanceOf[JArray].arr.foldLeft(result) { case (r,block) =>
        val sourceNode = block \ "global" \ source
        if (sourceNode != JNothing){
          val JString(name) = (sourceNode \ "name")
          val JString(version) = (block \ "version")

          val newVal = r.transformField {   case (target, _) =>
            (target ->
              (name ->
                (version -> block \ "global" \ source)
                )
              )
          }

          (r merge newVal).asInstanceOf[JObject]
        }
        else
          r
      }
    }
    if (result \ target == JNothing) (result removeField{ case JField(key, value) => key == target}).asInstanceOf[JObject]
    else result
  }


  override def postProcess(parsedFiles: JArray): JObject = ???

}

class ApiSuccessTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineSuccessTitle"): JObject =
    super.preProcess(parsedFiles, "defineSuccessTitle")

  override def postProcess(parsedFiles: JArray): JObject = ???

}

class ApiUserWorker extends Worker {

  override def preProcess(parsedFiles: JArray, target: String = "define"): JObject = {

    val result = parsedFiles.arr.foldLeft(JObject()) { case (result,parsedFiles) =>
      parsedFiles.asInstanceOf[JArray].arr.foldLeft(result) { case (r,block) =>
         val source = block \ "global" \ target
        if (source != JNothing){
            val JString(name) = (source \ "name")
            val JString(version) = (block \ "version")

            r.transformField {   case (name, _) =>
              (name ->
                (version -> block \ "local")
               )
            }.asInstanceOf[JObject]
        }
        else
        r
      }
    }
    if (result \ target == JNothing) (result removeField{ case JField(key, value) => key == target}).asInstanceOf[JObject]
    else result
  }


  override def postProcess(parsedFiles: JArray): JObject = ???

}




