package com.culpin.team.sbt.worker

import com.culpin.team.sbt.Util.merge
import sbt.librarymanagement.VersionNumber
import ujson.Js
import com.gilt.gfc.semver.SemVer
import ujson.Js.Value

case class ErrorMessage(element: String, usage: String, example: String)

/**
 *
 * Attaches defined data to parameter which inherit the data.
 * It uses 2 functions, preProcess and postProcess (with the result of preProcess).
 *
 * preProcess  Generates a list with [defineName][name][version] = value
 * postProcess Attach the preProcess data with the nearest version to the tree.
 *
 */
trait Worker {

  def preProcess(parsedFiles: Js.Arr, target: String = "define")(source: String = target): Js.Value

  def postProcess(parsedFiles: Js.Arr, fileNames: List[String],
    maybeSampleUrl: Option[String], preProcess: Js.Value, source: String = "defineParamTitle",
    target: String = "parameter",
    errorMessage: ErrorMessage = ErrorMessage("apiUse", "@apiUse group", "@apiDefine MyValidGroup Some title\n@apiUse MyValidGroup")): Js.Arr

}

class ApiErrorStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineErrorStructure")(source: String = target): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String], preProcess: Value, source: String = "defineErrorStructure", target: String = "errorStructure", errorMessage: ErrorMessage): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiErrorTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineErrorTitle")(source: String = target): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Value, source: String = "defineErrorTitle",
    target: String = "error", errorMessage: ErrorMessage = ErrorMessage("apiError", "@apiError (group) varname", "@apiDefine MyValidErrorGroup Some title or 40X Error\n@apiError (MyValidErrorGroup) username")): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiGroupWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineGroup")(source: String = target): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Js.Value, source: String = "defineGroup",
    target: String = "group", errorMessage: ErrorMessage = ErrorMessage("apiParam", "@apiParam (group) varname", "")): Js.Arr = {

    parsedFiles.arr.zip(fileNames).map {
      case (parsedFile, filename) =>
        parsedFile.arr.map { block =>
          val namedBlock =
            if (block("global").obj.nonEmpty) block
            else {
              val group =
                block("local")(target) match {
                  case Js.Str(g) => g
                  case _ => filename
                }
              merge(block, Js.Obj("local" -> Js.Obj("target" -> group.replaceAll("""[^\w]""", "_"))))
            }

          val localTarget: Js.Value = namedBlock("local").obj.getOrElse(target, Js.Null)
          if (localTarget == Js.Null) namedBlock
          else {
            val Js.Str(name) = localTarget
            val version =
              namedBlock("version") match {
                case Js.Str(v) => v
                case _ => "0.0.0"
              }

            val matchedData =
              if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null)
                Js.Obj("title" -> localTarget)
              else Worker.matchData(preProcess, source, name, version)

            val newValue = Js.Obj("local" -> Js.Obj("groupTitle" -> matchedData("title"), "groupDescription" -> matchedData.obj.getOrElse("description", Js.Null)))
            merge(namedBlock, newValue)

          }

        }
    }

  }
}

class ApiHeaderStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineHeaderStructure")(source: String = target): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Value, source: String = "defineHeaderStructure", target: String = "headerStructure", errorMessage: ErrorMessage): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiHeaderTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineHeaderTitle")(source: String = target): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Value, source: String = "defineHeaderTitle", target: String = "header", errorMessage: ErrorMessage): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiNameWorker extends Worker {
  override def preProcess(parsedFiles: Js.Arr, target: String = "")(source: String = target): Value = Js.Null

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String], preProcess: Value, source: String = "", target: String = "name", errorMessage: ErrorMessage = ErrorMessage("apiParam", "@apiParam (group) varname", "")): Js.Arr = {
    parsedFiles.arr.map { parsedFile =>
      parsedFile.arr.map { block =>

        if (block("global").obj.nonEmpty) block
        else {

          val name =
            block("local")(target) match {
              case Js.Str(n) => n
              case _ =>
                val Js.Str(_type) = block("local")("type")
                val Js.Str(url) = block("local")("url")
                val initName = _type.toLowerCase.capitalize
                initName + "_" + url.toLowerCase.split("\\s+").map(_.capitalize).mkString("_")
            }
          merge(block, Js.Obj("local" -> Js.Obj("name" -> name.replaceAll("""[^\w]""", "_"))))
        }
      }

    }
  }
}

class ApiParamTitleWorker extends Worker {

  /**
   * PreProcess
   *
   * @param parsedFiles
   * @param target       Target path in preProcess-Object (returned result), where the data should be set.
   * @return
   */
  def preProcess(parsedFiles: Js.Arr, target: String = "defineParamTitle")(source: String = "define"): Js.Value = {

    parsedFiles.arr.foldLeft(Js.Obj(target -> Js.Obj()): Js.Value) {
      case (result, parsedFile) =>
        parsedFile.arr.foldLeft(result) {
          case (r, block) =>
            val sourceNode = block("global").obj.getOrElse(source, Js.Null)
            sourceNode match {
              case jsObj @ Js.Obj(_) =>
                val Js.Str(name) = jsObj("name")
                val version =
                  block("version") match {
                    case Js.Str(v) => v
                    case _ => "0.0.0"
                  }

                val result = merge(r, Js.Obj(target -> Js.Obj(name -> Js.Obj(version -> sourceNode))))
                if (result(target) == Js.Null)
                  result.obj.remove(target)
                result
              case _ => r
            }
        }
    }
  }

  def postProcess(parsedFiles: Js.Arr, filenames: List[String], maybeSampleUrl: Option[String],
    preProcess: Js.Value, source: String = "defineParamTitle",
    target: String = "parameter", errorMessage: ErrorMessage = ErrorMessage("apiParam", "@apiParam (group) varname", "")): Js.Arr = {

    parsedFiles.arr.map { parsedFile =>
      parsedFile.arr.map { block =>
        val localTarget: Js.Value = block("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) block
        else {
          val fields = localTarget.obj.getOrElse("field", Js.Obj())
          fields.obj.keySet.foldLeft(Js.Obj(): Js.Value) {
            case (newFields, fieldGroup) =>
              fields(fieldGroup).arr.foldLeft(newFields) {
                case (newField, definition) =>
                  val Js.Str(name) = definition("group")
                  val version =
                    definition("version") match {
                      case Js.Str(v) => v
                      case _ => "0.0.0"
                    }

                  val matchedData =
                    if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null)
                      Js.Obj("name" -> name, "title" -> name)
                    else Worker.matchData(preProcess, source, name, version)

                  val Js.Str(title) = matchedData("title")

                  val newValue = Js.Obj(title -> Js.Arr(definition))
                  merge(newField, newValue)

              }
          }
          block
        }

      }
    }
  }
}

class ApiPermissionWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "definePermission")(source: String = "define"): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr, filenames: List[String], maybeSampleUrl: Option[String], preProcess: Value,
    source: String = "definePermission", target: String = "permission",
    errorMessage: ErrorMessage = ErrorMessage("apiPermission", "@apiPermission group", "@apiDefine MyValidPermissionGroup Some title\n@apiPermission MyValidPermissionGroup")): Js.Arr = {
    parsedFiles.arr.map { parsedFile =>
      parsedFile.arr.map { block =>
        val localTarget: Js.Value = block("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) block
        else {
          val newPermission = localTarget.arr.foldLeft(Js.Arr()) {
            case (permission, definition) =>
              val Js.Str(name) = definition("name")
              val version =
                block("version") match {
                  case Js.Str(v) => v
                  case _ => "0.0.0"
                }

              val metadata =
                if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null) {
                  Js.Obj(
                    "name" -> name,
                    "title" -> definition.obj.getOrElse("title", Js.Null),
                    "description" -> definition.obj.getOrElse("description", ""))
                } else Worker.matchData(preProcess, source, name, version)
              merge(permission, Js.Arr(metadata))
          }
          block("local")(target) = Js.Null
          merge(block, Js.Obj("local" -> Js.Obj(target -> newPermission)))
        }
      }
    }

  }

}

class ApiSampleRequestWorker extends Worker {

  override def preProcess(parsedFiles: Js.Arr, target: String)(source: String): Value = Js.Null

  override def postProcess(
    parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Value, source: String = "",
    target: String = "sampleRequest", errorMessage: ErrorMessage = ErrorMessage("", "", "")): Js.Arr = {
    //FIXME
    def appendSampleUrl(url: String): Js.Obj = {
      maybeSampleUrl match {
        case Some(sampleUrl) if url.length >= 4 && !url.toLowerCase.startsWith("http") =>
          Js.Obj("url" -> (sampleUrl + url))
        case _ => Js.Obj("url" -> url)
      }
    }

    parsedFiles.arr.map { parsedFile =>
      parsedFile.arr.map { block =>
        val sampleBlock: Js.Value = block("local").obj.getOrElse(target, Js.Null)
        if (sampleBlock == Js.Null) block
        else {
          val newBlock = sampleBlock match {
            case Js.Arr(entries) =>
              Js.Arr.from(entries
                .collect {
                  case entry if !entry("url").str.equals("off") =>
                    val Js.Str(url) = entry("url")
                    appendSampleUrl(url)
                })
            case _ =>
              if (maybeSampleUrl.isDefined && block("local")("url") != Js.Null) {
                val Some(sampleUrl) = maybeSampleUrl
                val Js.Str(url) = block("local")("url")
                val value = Js.Obj("url" -> (sampleUrl + url))
                Js.Arr(value)
              } else Js.Arr()
          }

          block("local")(target) = if (newBlock.arr.isEmpty) Js.Null else newBlock
          block

        }

      }
    }
  }
}

class ApiStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineStructure")(source: String): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String], preProcess: Value, source: String = "defineStructure", target: String = "structure", errorMessage: ErrorMessage): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiSuccessStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineSuccessStructure")(source: String): Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String], preProcess: Value, source: String = "defineSuccessStructure", target: String = "successStructure", errorMessage: ErrorMessage): Js.Arr =
    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)
}

class ApiSuccessTitleWorker extends ApiParamTitleWorker {
  override def preProcess(parsedFiles: Js.Arr, target: String = "defineSuccessTitle")(source: String): Value =
    super.preProcess(parsedFiles, target)(source)

  //  override def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String], preProcess: Value, source: String = "defineSuccessStructure", target: String = "successStructure", errorMessage: ErrorMessage): Js.Arr =
  //    super.postProcess(parsedFiles, fileNames, maybeSampleUrl, preProcess, source, target, errorMessage)

}

class ApiUseWorker extends Worker {

  /**
   * PreProcess
   *
   * @param parsedFiles
   * @param target       Target path in preProcess-Object (returned result), where the data should be set.
   * @return
   */
  def preProcess(parsedFiles: Js.Arr, target: String = "define")(source: String = target): Js.Value = {

    parsedFiles.arr.foldLeft(Js.Obj(target -> Js.Obj()): Js.Value) {
      case (result, parsedFile) =>
        parsedFile.arr.foldLeft(result) {
          case (r, block) =>
            val sourceNode = block("global").obj.getOrElse(source, Js.Null)
            sourceNode match {
              case jsObj @ Js.Obj(_) =>
                val Js.Str(name) = jsObj("name")
                val version =
                  block("version") match {
                    case Js.Str(v) => v
                    case _ => "0.0.0"
                  }
                merge(r, Js.Obj(target -> Js.Obj(name -> Js.Obj(version -> block("local")))))

              case _ => r
            }
        }
    }
  }

  def postProcess(parsedFiles: Js.Arr, fileNames: List[String], maybeSampleUrl: Option[String],
    preProcess: Js.Value, source: String = "define",
    target: String = "use", errorMessage: ErrorMessage = ErrorMessage("apiParam", "@apiParam (group) varname", "")): Js.Arr = {

    parsedFiles.arr.map { parsedFile =>
      parsedFile.arr.map { block =>

        val localTarget: Js.Value = block("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) block
        else {
          val Js.Str(name) = localTarget(0)("name")
          val version =
            block("version") match {
              case Js.Str(v) => v
              case _ => "0.0.0"
            }
          if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null) {
            val Js.Num(index) = block("index")
            val Js.Str(filename) = block("local")("filename")
            ???
          } else {
            val metadata = Worker.matchData(preProcess, source, name, version)

            block("local")(target) = Js.Null
            merge(block, Js.Obj("local" -> metadata))
          }
        }
      }

    }
  }
}

object Worker {

  private val workers = List(
    new ApiErrorStructureWorker,
    new ApiErrorTitleWorker,
    new ApiGroupWorker,
    new ApiHeaderStructureWorker,
    new ApiHeaderTitleWorker,
    new ApiNameWorker,
    new ApiParamTitleWorker,
    new ApiPermissionWorker,
    new ApiSampleRequestWorker,
    new ApiStructureWorker,
    new ApiSuccessStructureWorker,
    new ApiSuccessTitleWorker,
    new ApiUseWorker)

  def matchData(preProcess: Js.Value, source: String, name: String, version: String): Js.Value = {
    preProcess(source)(name).obj.getOrElse(version, {

      val versionKeys = preProcess(source)(name).obj.keySet.toList

      // find nearest matching version
      var foundIndex = -1
      var lastVersion = "0.0.0"
      versionKeys.zipWithIndex.foreach {
        case (currentVersion, versionIndex) =>
          if (((SemVer(version) compareTo SemVer(currentVersion)) > 0) &&
            ((SemVer(currentVersion) compareTo SemVer(lastVersion)) > 0)) {
            foundIndex = versionIndex
            lastVersion = currentVersion
          }
      }
      //TODO handle not found case

      val versionName = versionKeys(foundIndex)
      preProcess(source)(name)(versionName)
    })
  }

  def preProcess(parsedFiles: Js.Arr): Js.Value = {
    workers.foldLeft(Js.Obj(): Js.Value) {
      case (preProcessResult, worker) =>
        merge(preProcessResult, worker.preProcess(parsedFiles)())
    }
  }

  def postProcess(parsedFiles: Js.Arr, filenames: List[String], sampleUrl: Option[String],
    preProcess: Js.Value): Js.Arr = {
    workers.foldLeft(parsedFiles) {
      case (pf, worker) =>
        worker.postProcess(pf, filenames, sampleUrl, preProcess)
    }
  }

  def apply(parsedFiles: Js.Arr, filenames: List[String], sampleUrl: Option[String]): Js.Arr = {

    val pf = parsedFiles.arr.zip(filenames) map {
      case (parsedFile, filename) =>
        parsedFile.arr map { block =>
          if (block("global").obj.isEmpty && block("local").obj.nonEmpty) {
            val newType = block("local")("type") match {
              case Js.Str(theType) => theType
              case _ => ""
            }

            val newUrl = block("local")("url") match {
              case Js.Str(url) => url
              case _ => ""
            }

            val newVersion = block("local")("version") match {
              case Js.Str(version) => version
              case _ => "0.0.0"
            }

            val newFilename = block("local").obj.getOrElse("filename", Js.Null) match {
              case Js.Str(name) => name
              case _ => filename
            }
            val newLocal = Js.Obj("local" ->
              Js.Obj(
                "type" -> newType,
                "url" -> newUrl,
                "version" -> newVersion,
                "filename" -> newFilename))
            merge(block, newLocal)
          } else block
        }
    }

    postProcess(pf, filenames, sampleUrl, preProcess(pf))

  }

}
