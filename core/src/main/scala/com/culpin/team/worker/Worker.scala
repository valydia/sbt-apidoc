package com.culpin.team.worker

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

/**
 * @author
 */
trait Worker {


  def preProcess(parsedFiles: JArray, target: String = "define"): JValue = {

    def buildResult(source: String): JValue = {
      val initResult: JValue = (target -> JObject())
      parsedFiles.arr.foldLeft(initResult) { case (result, parsedFile) =>
        parsedFile match {
          case JArray(blocks) => blocks.foldLeft(result) { case (r, block) =>
            val valToAppend = produceValueToAppend(block, source, r)
            (r merge valToAppend)
          }
          case _ => throw new IllegalArgumentException
        }
      }
    }

    val result = buildResult(source(target))

    if (result \ target == JNothing)
      (result removeField { case JField(key, value) => key == target})
    else result
  }

  def produceValueToAppend(block: JValue, source: String, r: JValue): JValue

  def source(target: String): String


  def postProcess(parsedFiles: JArray,preProcess: JValue, source: String, target: String): JValue = ???

}

class ApiErrorStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineErrorStructure"): JValue =
    super.preProcess(parsedFiles, "defineErrorStructure")



}

class ApiErrorTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineErrorTitle"): JValue =
      super.preProcess(parsedFiles, "defineErrorTitle")



}

class ApiGroupWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineGroup"): JValue =
    super.preProcess(parsedFiles,target)


}


class ApiHeaderStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineHeaderStructure"): JValue =
    super.preProcess(parsedFiles, "defineHeaderStructure")

}


class ApiHeaderTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineHeaderTitle"): JValue =
    super.preProcess(parsedFiles, "defineHeaderTitle")

}

class ApiPermissionWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "definePermission"): JValue =
   super.preProcess(parsedFiles,target)


}


class ApiParamTitleWorker extends Worker {

  override def preProcess(parsedFiles: JArray, target: String = "defineParamTitle"): JValue =
    super.preProcess(parsedFiles, target)

  def produceValueToAppend(block: JValue, source: String, r: JValue): JValue = {
    val sourceNode = block \ "global" \ source
    sourceNode match {
      case JNothing => JNothing
      case _ =>   {
        val JString(name) = (sourceNode \ "name")
        val JString(version) = (block \ "version")

        r.transformField { case (target, _) =>
          (target ->
            (name ->
              (version -> block \ "global" \ source)
              )
            )
        }
      }
    }
  }

  def source(target: String): String = "define"

  def matchData(preProcess: JValue, source: String = "define", name: String,  version: String = "0.0.0"): JValue = {

    if ( preProcess \ source \ name == JNothing)
      ("name" -> name) ~ ("title" -> name)
    else if (preProcess \ source \ name \ version  != JNothing)
      preProcess \ source \ name \ version
    else {
      import com.gilt.gfc.semver.SemVer
      val versionKeys = (preProcess \ source \ name) match {
        case JObject(l) => JObject(l).values.keys.toList
        case _ => List()
      }

      println(preProcess \ source \ name)
      // find nearest matching version
      var foundIndex = -1;
      var lastVersion = "0.0.0";
      versionKeys.zipWithIndex.foreach { case (currentVersion, versionIndex) =>
        if (((SemVer(version) compareTo SemVer(currentVersion)) > 0) &&
          ((SemVer(currentVersion) compareTo SemVer(lastVersion)) > 0)) {
          foundIndex = versionIndex
          lastVersion = currentVersion
        }
      }

      val versionName = versionKeys(foundIndex)
      preProcess \ source \ name \ versionName

    }
  }

  def postProcessBlock(block: JValue, preProcess: JValue, source: String = "defineParamTitle", target: String = "parameter"): JValue = {

    if (block \ "local" \ target == JNothing || block \ "local" \ target \ "fields" == JNothing) block
    else {


      val fields = block \ "local" \ target \ "fields" match {
        case JObject(fieldGroup) => JObject(fieldGroup)
        case _ => JObject()
      }


      val initNewFields : JValue = JObject()
      val nf = fields.obj.toMap.keySet.foldLeft(initNewFields) { case (newFields, fieldGroup) =>
        val param = (fields \ fieldGroup).asInstanceOf[JArray]
        param.arr.foldLeft(newFields) { case (newFields, definition) =>

          val JString(name) = (definition \ "group")
          val version = (definition \ "version") match {
            case JString(v) => v
            case _ => "0.0.0"
          }

          val matchedData = matchData(preProcess, source, name, version)
          val JString(title) = (matchedData \ "title")

          val newValue: JValue = (title -> JArray(List(definition)))
          newFields merge newValue

        }
      }
      val valToAppend: JObject = ("local" ->
                                        (target ->
                                          ("fields" -> nf)
                                          )
                                  )
      block merge valToAppend
    }
  }

  override def postProcess(parsedFiles: JArray,preProcess: JValue, source: String = "define", target: String = "parameter"): JArray = {
    parsedFiles.arr.zipWithIndex.map { case(parsedFile, parsedFileIndex) =>
      parsedFile match {
        case JArray(blocks) => blocks.map { postProcessBlock(_, preProcess, source, target)}
        case _ => throw new IllegalArgumentException
      }
    }

  }


}

class ApiStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineStructure"): JValue =
    super.preProcess(parsedFiles, "defineStructure")


}


class ApiSuccessStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineSuccessStructure"): JValue =
    super.preProcess(parsedFiles, "defineSuccessStructure")


}


class ApiSuccessTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: JArray, target: String = "defineSuccessTitle"): JValue =
    super.preProcess(parsedFiles, "defineSuccessTitle")


}

class ApiUseWorker extends Worker {

  def produceValueToAppend(block: JValue, source: String, r: JValue): JValue = {
    val sourceNode = block \ "global" \ source
    sourceNode match {
      case JNothing => JNothing
      case _ => {
        val JString(name) = (sourceNode \ "name")
        val JString(version) = (block \ "version")

        r.transformField { case (target, _) =>
          (target ->
            (name ->
              (version -> block \ "local")
              )
            )
        }
      }
    }
  }

  def source(target: String) = target

  def matchData(preProcess: JValue, source: String = "define", name: String,  version: String = "0.0.0") = {
    if (preProcess \ source \ name \ version != JNothing)
      preProcess \ source \ name \ version
    else {
      import com.gilt.gfc.semver.SemVer
      val versionKeys = (preProcess \ source \ name) match {
        case JObject(l) => JObject(l).values.keys.toList
        case _ => List()
      }

      // find nearest matching version
      var foundIndex = -1;
      var lastVersion = "0.0.0";
      versionKeys.zipWithIndex.foreach { case (currentVersion, versionIndex) =>
        if (((SemVer(version) compareTo SemVer(currentVersion)) > 0) &&
          ((SemVer(currentVersion) compareTo SemVer(lastVersion)) > 0)) {
          foundIndex = versionIndex
          lastVersion = currentVersion
        }
      }

      val versionName = versionKeys(foundIndex)
      preProcess \ source \ name \ versionName

    }
  }

  def postProcessBlock(block: JValue, preProcess: JValue, source: String = "define", target: String = "use"): JValue = {

      val localTarget: JValue = block \ "local" \ target
        if (localTarget == JNothing) block
      else {
        val JString(name) = localTarget \ "name"
        val version = block \ "version" match {
          case JString(v) => v
          case _ => "0.0.0"
        }
        if (preProcess \ source == JNothing || preProcess \ source \ name == JNothing)
          throw new IllegalArgumentException
        else {
          val matchedData = matchData(preProcess, source, name, version)

          val cleanedUpBlock = block removeField { case (key, value) => key == target}

          val valueToAppend = block \ "local" merge matchedData
          //TODO recursive merge
          block merge valueToAppend

        }
      }
  }

  override def postProcess(parsedFiles: JArray,preProcess: JValue, source: String = "define", target: String = "use"): JArray = {
    parsedFiles.arr.zipWithIndex.map { case(parsedFile, parsedFileIndex) =>
      parsedFile match {
        case JArray(blocks) => blocks.map { postProcessBlock(_, preProcess, source, target)}
        case _ => throw new IllegalArgumentException
      }
      }

    }

}

object Worker {

  val workers = List(
                  new ApiErrorStructureWorker,
                  new ApiErrorTitleWorker,
                  new ApiGroupWorker,
                  new ApiHeaderStructureWorker,
                  new ApiHeaderTitleWorker,
                  new ApiParamTitleWorker,
                  new ApiPermissionWorker,
                  new ApiStructureWorker,
                  new ApiSuccessStructureWorker,
                  new ApiSuccessTitleWorker,
                  new ApiUseWorker
                  )

  def processFilename(parsedFiles: JArray, filenames: List[String]): JArray = {

    parsedFiles.arr.zipWithIndex.map{ case (JArray(parsedFile), index) =>
      parsedFile.map { block =>
       if ( (block \ "global").children.isEmpty && (block \ "local").children.nonEmpty) {
//         import org.json4s.native.JsonMethods._
//         println("--- " + index)
//         println(compact(render(block)))

          val newType = block \ "local" \ "type" match {
            case JString(theType) => theType
            case _ => ""
          }

         val newUrl = block \ "local" \ "url" match {
           case JString(url) => url
           case _ => ""
         }

         val newVersion = block \ "local" \ "version" match {
           case JString(version) => version
           case _ => "0.0.0"
         }

         val newFilename = block \ "local" \ "filename" match {
           case JString(fileName) => fileName
           case _ => filenames(index)
         }

         val newLocal: JObject = ("local" ->
                                    ("type" -> newType) ~
                                    ("url" -> newUrl)   ~
                                    ("version" -> newVersion) ~
                                    ("filename" -> newFilename)
                                  )
            block merge newLocal
       }
        else
         block
      }
        case _ => throw new IllegalArgumentException()
    }
  }


  def preProcess(parsedFiles: JArray) : JValue = {
    val initResult: JValue = JObject()
    workers.foldLeft(initResult){ case (preProcessResult, worker) =>
       preProcessResult merge worker.preProcess(parsedFiles)
    }
  }

}






