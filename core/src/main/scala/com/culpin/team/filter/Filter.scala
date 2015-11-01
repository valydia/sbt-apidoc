package com.culpin.team.filter

import com.culpin.team.worker.Worker
import org.json4s.JsonAST._
import org.json4s.JsonDSL._

trait Filter {
  def postFilter(parsedFiles: JArray, filenames: List[String], tagName: String = ""): JArray
}

class ApiErrorFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, filenames: List[String], tagName: String = "error"): JArray =
    super.postFilter(parsedFiles,filenames,"error")
}

class ApiHeaderFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, filenames: List[String], tagName: String = "header"): JArray =
    super.postFilter(parsedFiles,filenames,"header")
}

class ApiSuccessFilter extends ApiParamFilter {
  override def postFilter(parsedFiles: JArray, filenames: List[String], tagName: String = "success"): JArray =
    super.postFilter(parsedFiles,filenames,"success")
}

class ApiParamFilter extends Filter {


    def postFilter(parsedFiles: JArray, filenames: List[String], tagName: String = "parameter"): JArray = {
    Worker.mapBlock(parsedFiles,filenames){ case (block, filename) =>
        if (block \ "local" \ tagName \ "fields" == JNothing) block
        else {
          val JObject(bf) = block \ "local" \ tagName \ "fields"

//          block transformField { case ("fields", _) =>
//            ("fields", Filter.filterDuplicateKeys(JObject(bf)))
//          }
          val value: JObject = ("local" ->
                        (tagName ->
                          ("fields" -> Filter.filterDuplicateKeys(JObject(bf)))
                         )
                      )
          block merge value
        }
    }
  }

}

object Filter {

  def filterDuplicateKeys(jobject: JObject): JObject = {
    val obj: List[(String,JValue)] = jobject.obj
    .groupBy{case (key,value) => key}
    .mapValues(_.head)
//     .mapValues{l => if ( l.length > 1) println("Filtering out" + l.tail.mkString(", ")); l.head}
    .valuesIterator.toList

    JObject(obj)
  }

  val filters = List(
    new ApiErrorFilter,
    new ApiHeaderFilter,
    new ApiParamFilter,
    new ApiSuccessFilter
  )

  def apply(parsedFiles: JArray, filenames: List[String] ): JArray = {
    val filteredFiles = filters.foldLeft(parsedFiles){ case (pf, filter) =>
      filter.postFilter(pf, filenames)
    }

    val res = filteredFiles.arr.flatMap{
      case JArray(parsedFile) =>
         parsedFile.collect{ case block if (block \ "global").children.isEmpty && (block \ "local").children.nonEmpty =>
           block \ "local"
         }
      case _ => List()
    }
    JArray(res)
  }

}
