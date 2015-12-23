package com.culpin.team.filter

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import sbt.Logger

trait Filter {

  val name: String

  def postFilter(parsedFiles: JArray, tagName: String = ""): JArray
}

case class ApiErrorFilter(override val name: String = "apierror") extends ApiParamFilter(name) {

  override def postFilter(parsedFiles: JArray, tagName: String = "error"): JArray =
    super.postFilter(parsedFiles, "error")
}

case class ApiHeaderFilter(override val name: String = "apiheader") extends ApiParamFilter(name) {

  override def postFilter(parsedFiles: JArray, tagName: String = "header"): JArray =
    super.postFilter(parsedFiles, "header")
}

case class ApiSuccessFilter(override val name: String = "apisuccess") extends ApiParamFilter(name) {

  override def postFilter(parsedFiles: JArray, tagName: String = "success"): JArray =
    super.postFilter(parsedFiles, "success")
}

class ApiParamFilter(val name: String = "apiparam") extends Filter {

  def postFilter(parsedFiles: JArray, tagName: String = "parameter"): JArray = {
    parsedFiles.arr.map { parsedFileArray =>
      val JArray(parsedFile) = parsedFileArray
      parsedFile.map { block =>
        if (block \ "local" \ tagName \ "fields" == JNothing) block
        else {
          val JObject(bf) = block \ "local" \ tagName \ "fields"
          block.replace(List("local", tagName, "fields"), Filter.filterDuplicateKeys(JObject(bf)))
        }
      }
    }
  }

}

object ApiParamFilter {
  def apply(name: String = "apiparam"): ApiParamFilter = new ApiParamFilter(name)
}

object Filter {

  def filterDuplicateKeys(jobject: JObject): JObject = {
    val obj: List[(String, JValue)] = jobject.obj
      .groupBy { case (key, value) => key }
      .mapValues(_.head)
      .valuesIterator.toList

    JObject(obj)
  }

  val filters = List(
    ApiErrorFilter(),
    ApiHeaderFilter(),
    ApiParamFilter(),
    ApiSuccessFilter()
  )

  def apply(parsedFiles: JArray, logger: Logger): JArray = {
    val filteredFiles = filters.foldLeft(parsedFiles) {
      case (pf, filter) =>
        logger.verbose("filter postFilter: " + filter.name)
        filter.postFilter(pf)
    }
    val res = filteredFiles.arr.flatMap {
      case JArray(parsedFile) =>
        parsedFile
          .collect {
            case block if (block \ "global").children.isEmpty && (block \ "local").children.nonEmpty =>
              block \ "local"
          }
      case _ => List()
    }
    JArray(res)
  }

}
