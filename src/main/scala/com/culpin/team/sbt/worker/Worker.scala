package com.culpin.team.sbt.worker

import com.culpin.team.sbt.SbtApidoc.RelativeFilename
import com.culpin.team.sbt.Util.merge
import com.culpin.team.sbt.worker.Worker.traverseFiles
import ujson.Js
import com.gilt.gfc.semver.SemVer

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ErrorMessage(element: String, usage: String, example: String)

case class WorkerError(
  message: String,
  file: String,
  block: String,
  element: String,
  definition: String,
  example: String,
  extra: Js.Value
)

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

  def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineParamTitle",
    target: String = "parameter",
    errorMessage: ErrorMessage = ErrorMessage(
      "apiUse",
      "@apiUse group",
      "@apiDefine MyValidGroup Some title\n@apiUse MyValidGroup"
    )
  ): Either[WorkerError, Js.Arr]

}

class ApiErrorStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineErrorStructure")(
    source: String = target
  ): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineErrorStructure",
    target: String = "errorStructure",
    errorMessage: ErrorMessage
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )

}

class ApiErrorTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineErrorTitle")(
    source: String = target
  ): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineErrorTitle",
    target: String = "error",
    errorMessage: ErrorMessage = ErrorMessage(
      "apiError",
      "@apiError (group) varname",
      "@apiDefine MyValidErrorGroup Some title or 40X Error\n@apiError (MyValidErrorGroup) username"
    )
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )

}

class ApiGroupWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineGroup")(
    source: String = target
  ): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineGroup",
    target: String = "group",
    errorMessage: ErrorMessage = ErrorMessage("apiParam", "@apiParam (group) varname", "")
  ): Either[WorkerError, Js.Arr] =
    traverseFiles(parsedFiles, fileNames) {
      case (block, filename) =>
        val namedBlock =
          if (block("global").obj.nonEmpty) block
          else {
            val group =
              block("local")(target) match {
                case Js.Str(g) => g
                case _         => filename
              }
            merge(block, Js.Obj("local" -> Js.Obj(target -> group.replaceAll("""[^\w]""", "_"))))
          }

        val localTarget: Js.Value =
          namedBlock("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) Right(namedBlock)
        else {
          val Js.Str(name) = localTarget
          val version =
            namedBlock("version") match {
              case Js.Str(v) => v
              case _         => "0.0.0"
            }

          val matchedData =
            if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null)
              Js.Obj("title" -> localTarget)
            else Worker.matchData(preProcess, source, name, version)
          val map = new mutable.LinkedHashMap[String, Js.Value]()
          map.put("groupTitle", matchedData("title"))
          matchedData.obj
            .get("description")
            .foreach(s => map.put("groupDescription", s))
          val newValue =
            Js.Obj("local" -> Js.Obj.from(map))

          Right(merge(namedBlock, newValue))

        }

    }
}

class ApiHeaderStructureWorker extends ApiUseWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineHeaderStructure")(
    source: String = target
  ): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineHeaderStructure",
    target: String = "headerStructure",
    errorMessage: ErrorMessage
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )

}

class ApiHeaderTitleWorker extends ApiParamTitleWorker {

  override def preProcess(parsedFiles: Js.Arr, target: String = "defineHeaderTitle")(
    source: String = target
  ): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineHeaderTitle",
    target: String = "header",
    errorMessage: ErrorMessage
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )

}

class ApiNameWorker extends Worker {
  override def preProcess(parsedFiles: Js.Arr, target: String = "")(
    source: String = target
  ): Js.Value = Js.Null

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "",
    target: String = "name",
    errorMessage: ErrorMessage
  ): Either[WorkerError, Js.Arr] =
    traverseFiles(parsedFiles, fileNames) {
      case (block, _) =>
        if (block("global").obj.nonEmpty) Right(block)
        else {

          val name =
            block("local")(target) match {
              case Js.Str(n) => n
              case _ =>
                val Js.Str(_type) = block("local")("type")
                val Js.Str(url)   = block("local")("url")
                val initName      = _type.toLowerCase.capitalize
                initName + "_" + url.toLowerCase
                  .split("\\s+")
                  .map(_.capitalize)
                  .mkString("_")
            }
          Right(
            merge(
              block,
              Js.Obj(
                "local" -> Js.Obj("name" -> name.replaceAll("""[^\w]""", "_"))
              )
            )
          )
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
  def preProcess(parsedFiles: Js.Arr, target: String = "defineParamTitle")(
    source: String = "define"
  ): Js.Value =
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
                    case _         => "0.0.0"
                  }

                val result = merge(
                  r,
                  Js.Obj(
                    target ->
                      Js.Obj(
                        name ->
                          Js.Obj(version -> sourceNode)
                      )
                  )
                )
                if (result(target) == Js.Null)
                  result.obj.remove(target)
                result
              case _ => r
            }
        }
    }

  private val message =
    ErrorMessage(
      "apiParam",
      "@apiParam (group) varname",
      "@apiDefine MyValidParamGroup Some title\n@apiParam (MyValidParamGroup) username"
    )
  def postProcess(
    parsedFiles: Js.Arr,
    filenames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineParamTitle",
    target: String = "parameter",
    errorMessage: ErrorMessage = message
  ): Either[WorkerError, Js.Arr] =
    traverseFiles(parsedFiles, filenames) {
      case (block, _) =>
        val localTarget: Js.Value =
          block("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) Right(block)
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
                      case _         => "0.0.0"
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
          Right(block)
        }
    }

}

class ApiPermissionWorker extends ApiParamTitleWorker {

  override def preProcess(
    parsedFiles: Js.Arr,
    target: String = "definePermission"
  )(source: String = "define"): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  private val errorMessage =
    ErrorMessage(
      "apiPermission",
      "@apiPermission group",
      "@apiDefine MyValidPermissionGroup Some title\n@apiPermission MyValidPermissionGroup"
    )

  override def postProcess(
    parsedFiles: Js.Arr,
    filenames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "definePermission",
    target: String = "permission",
    errorMessage: ErrorMessage = errorMessage
  ): Either[WorkerError, Js.Arr] =
    traverseFiles(parsedFiles, filenames) {
      case (block, _) =>
        val localTarget: Js.Value =
          block("local").obj.getOrElse(target, Js.Null)
        if (localTarget == Js.Null) Right(block)
        else {
          val newPermission = localTarget.arr.foldLeft(Js.Arr()) {
            case (permission, definition) =>
              val Js.Str(name) = definition("name")
              val version =
                block("version") match {
                  case Js.Str(v) => v
                  case _         => "0.0.0"
                }

              val metadata =
                if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null) {
                  val map = new mutable.LinkedHashMap[String, Js.Value]()
                  map.put("name", name)
                  definition.obj.get("title").foreach(t => map.put("title", t))
                  definition.obj.get("description").foreach(d => map.put("description", d))
                  Js.Obj.from(map)
                } else Worker.matchData(preProcess, source, name, version)
              merge(permission, Js.Arr(metadata))
          }
          block("local")(target) = Js.Null
          Right(merge(block, Js.Obj("local" -> Js.Obj(target -> newPermission))))
        }
    }

}

class ApiSampleRequestWorker extends Worker {

  import Worker._

  override def preProcess(
    parsedFiles: Js.Arr,
    target: String
  )(source: String): Js.Value = Js.Null

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "",
    target: String = "sampleRequest",
    errorMessage: ErrorMessage
  ): Either[WorkerError, Js.Arr] = {

    def appendSampleUrl(url: String): Js.Obj =
      maybeSampleUrl match {
        case Some(sampleUrl) if url.length >= 4 && !url.toLowerCase.startsWith("http") =>
          Js.Obj("url" -> (sampleUrl + url))
        case _ => Js.Obj("url" -> url)
      }

    traverseFiles(parsedFiles, fileNames) {
      case (block, _) =>
        val sampleBlock: Js.Value =
          block("local").obj.getOrElse(target, Js.Null)
        if (sampleBlock != Js.Null) {
          sampleBlock match {
            case Js.Arr(entries) =>
              val newTarget =
                Js.Arr.from(
                  entries
                    .collect {
                      case entry if !entry("url").str.equals("off") =>
                        val Js.Str(url) = entry("url")
                        appendSampleUrl(url)
                    }
                )
              block("local")(target) = newTarget
            case _ => //silently ignore
          }
          Right(block)
        } else {
          if (maybeSampleUrl.isDefined && block("local").obj.getOrElse("url", Js.Null) != Js.Null) {
            val Some(sampleUrl) = maybeSampleUrl
            val Js.Str(url)     = block("local")("url")
            val value           = Js.Obj("url" -> (sampleUrl + url))
            block("local")(target) = Js.Arr(value)
          }
          Right(block)
        }
    }

  }

}

class ApiStructureWorker extends ApiUseWorker {

  override def preProcess(
    parsedFiles: Js.Arr,
    target: String = "defineStructure"
  )(source: String): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  private val errorMessage =
    ErrorMessage(
      "apiStructure",
      "@apiStructure group",
      "@apiDefine MyValidStructureGroup Some title\n@apiStructure MyValidStructureGroup"
    )

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineStructure",
    target: String = "structure",
    errorMessage: ErrorMessage = errorMessage
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )

}

class ApiSuccessStructureWorker extends ApiUseWorker {

  override def preProcess(
    parsedFiles: Js.Arr,
    target: String = "defineSuccessStructure"
  )(source: String): Js.Value =
    super.preProcess(parsedFiles, target)(source)

  private val errorMessage =
    ErrorMessage(
      "apiSuccessStructure",
      "@apiSuccessStructure group",
      "@apiDefine MyValidSuccessStructureGroup Some title\n@apiSuccessStructure MyValidSuccessStructureGroup"
    )

  override def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "defineSuccessStructure",
    target: String = "successStructure",
    errorMessage: ErrorMessage = errorMessage
  ): Either[WorkerError, Js.Arr] =
    super.postProcess(
      parsedFiles,
      fileNames,
      maybeSampleUrl,
      preProcess,
      source,
      target,
      errorMessage
    )
}

class ApiSuccessTitleWorker extends ApiParamTitleWorker {

  override def preProcess(
    parsedFiles: Js.Arr,
    target: String = "defineSuccessTitle"
  )(source: String): Js.Value =
    super.preProcess(parsedFiles, target)(source)

}

class ApiUseWorker extends Worker {

  import Worker._

  /**
    * PreProcess
    *
    * @param parsedFiles
    * @param target       Target path in preProcess-Object (returned result), where the data should be set.
    * @return
    */
  def preProcess(parsedFiles: Js.Arr, target: String = "define")(
    source: String = target
  ): Js.Value =
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
                    case _         => "0.0.0"
                  }
                val newLocal = Js.Obj(
                  target ->
                    Js.Obj(
                      name ->
                        Js.Obj(version -> block("local"))
                    )
                )
                merge(r, newLocal)
              case _ => r
            }
        }
    }

  private val errorMessage =
    ErrorMessage(
      "apiUse",
      "@apiUse group",
      "@apiDefine MyValidGroup Some title\n@apiUse MyValidGroup"
    )

  def postProcess(
    parsedFiles: Js.Arr,
    fileNames: List[String],
    maybeSampleUrl: Option[String],
    preProcess: Js.Value,
    source: String = "define",
    target: String = "use",
    errorMessage: ErrorMessage = errorMessage
  ): Either[WorkerError, Js.Arr] =
    traverseFiles(parsedFiles, fileNames) {
      case (block, filename) =>
        val localTarget: Js.Value =
          block("local").obj.getOrElse(target, Js.Null)

        val eitherBlock = Right(block): Either[WorkerError, Js.Value]
        if (localTarget == Js.Null) eitherBlock
        else {
          localTarget.arr.foldLeft(eitherBlock) {
            case (accumalatorDefinition, definition) =>
              accumalatorDefinition.flatMap { acc =>
                val Js.Str(name) = definition("name")
                val version =
                  acc("version") match {
                    case Js.Str(v) => v
                    case _         => "0.0.0"
                  }
                if (preProcess(source).obj.getOrElse(name, Js.Null) == Js.Null) {
                  val Js.Num(index) = acc("index")
                  Left(
                    WorkerError(
                      "Referenced groupname does not exist / it is not defined with @apiDefine.",
                      filename,
                      index.toString,
                      errorMessage.element,
                      errorMessage.usage,
                      errorMessage.example,
                      Js.Arr(Js.Obj("Groupname" -> name))
                    )
                  )
                } else {
                  val metadata =
                    if (preProcess(source).obj
                          .getOrElse(name, Js.Obj())
                          .obj
                          .getOrElse(version, Js.Null) != Js.Null)
                      preProcess(source)(name)(version)
                    else Worker.matchData(preProcess, source, name, version)

                  acc("local").obj.remove(target)
                  Right(merge(acc, Js.Obj("local" -> metadata)))
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
    //TODO api_success_title.js
    new ApiUseWorker
  )

  //FIXME
  private[worker] def matchData(
    preProcess: Js.Value,
    source: String,
    name: String,
    version: String
  ): Js.Value =
    preProcess(source)(name).obj.getOrElse(
      version, {

        val versionKeys = preProcess(source)(name).obj.keySet.toList

        // find nearest matching version
        var foundIndex  = -1
        var lastVersion = "0.0.0"
        versionKeys.zipWithIndex.foreach {
          case (currentVersion, versionIndex) =>
            if ((SemVer(version) >= SemVer(currentVersion)) &&
                (SemVer(currentVersion) >= SemVer(lastVersion))) {
              foundIndex = versionIndex
              lastVersion = currentVersion
            }
        }
        //TODO handle not found case

        val versionName = versionKeys(foundIndex)
        preProcess(source)(name)(versionName)
      }
    )

  def preProcess(parsedFiles: Js.Arr): Js.Value =
    workers.foldLeft(Js.Obj(): Js.Value) {
      case (preProcessResult, worker) =>
        merge(preProcessResult, worker.preProcess(parsedFiles)())
    }

  def postProcess(
    parsedFiles: Js.Arr,
    filenames: List[String],
    sampleUrl: Option[String],
    preProcess: Js.Value
  ): Either[WorkerError, Js.Arr] =
    workers.foldLeft(Right(parsedFiles): Either[WorkerError, Js.Arr]) {
      case (eitherParsedFile, worker) =>
        eitherParsedFile.flatMap(worker.postProcess(_, filenames, sampleUrl, preProcess))
    }

  def apply(
    parsedFiles: Js.Arr,
    filenames: List[String],
    sampleUrl: Option[String]
  ): Either[WorkerError, Js.Arr] = {

    val pf = parsedFiles.arr.zip(filenames) map {
      case (parsedFile, filename) =>
        parsedFile.arr map { block =>
          if (block("global").obj.isEmpty && block("local").obj.nonEmpty) {
            val newType = block("local")("type") match {
              case Js.Str(theType) => theType
              case _               => ""
            }

            val newUrl = block("local")("url") match {
              case Js.Str(url) => url
              case _           => ""
            }

            val newVersion = block("local")("version") match {
              case Js.Str(version) => version
              case _               => "0.0.0"
            }

            val newFilename =
              block("local").obj.getOrElse("filename", Js.Null) match {
                case Js.Str(name) => name
                case _            => filename
              }
            val newLocal = Js.Obj(
              "local" ->
                Js.Obj(
                  "type"     -> newType,
                  "url"      -> newUrl,
                  "version"  -> newVersion,
                  "filename" -> newFilename
                )
            )
            merge(block, newLocal)
          } else block
        }
    }

    postProcess(pf, filenames, sampleUrl, preProcess(pf))
  }

  private[worker] def map2[L, A, B, C](fa: Either[L, A], fb: Either[L, B])(
    f: (A, B) => C
  ): Either[L, C] =
    for { a <- fa; b <- fb } yield f(a, b)
  private def unit[L, A](a: A): Either[L, A] = Right(a)

  def traverse[L, A, B](
    as: mutable.ArrayBuffer[A]
  )(f: A => Either[L, B]): Either[L, mutable.ArrayBuffer[B]] =
    as.foldRight(unit[L, mutable.ArrayBuffer[B]](mutable.ArrayBuffer[B]()))(
      (a, fbs) => map2(f(a), fbs)(_ +: _)
    )

  def sequence[L, A](as: mutable.ArrayBuffer[Either[L, A]]): Either[L, mutable.ArrayBuffer[A]] =
    traverse(as)(identity)

  private[worker] def traverseJsArr[L](
    list: mutable.ArrayBuffer[Js.Value]
  )(f: Js.Value => Either[L, Js.Value]): Either[L, Js.Arr] =
    traverse(list)(f).map(Js.Arr.apply)

  private[worker] def traversePair[L, B](
    list: mutable.ArrayBuffer[(Js.Value, B)]
  )(f: ((Js.Value, B)) => Either[L, Js.Value]): Either[L, Js.Arr] =
    list
      .foldRight(unit[L, mutable.ArrayBuffer[Js.Value]](mutable.ArrayBuffer[Js.Value]()))(
        (a, fbs) => map2(f(a), fbs)(_ +: _)
      )
      .map(Js.Arr.apply)

  private[worker] def traverseFiles(parsedFiles: Js.Arr, fileNames: List[RelativeFilename])(
    process: ((Js.Value, RelativeFilename)) => Either[WorkerError, Js.Value]
  ): Either[WorkerError, Js.Arr] =
    traversePair(parsedFiles.arr.zip(fileNames): ArrayBuffer[(Js.Value, RelativeFilename)]) {
      case (parsedFile, filename) =>
        traverseJsArr(parsedFile.arr) { block =>
          process(block, filename)
        }
    }
}
