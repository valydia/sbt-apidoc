package com.culpin.team.sbt.worker

import com.culpin.team.sbt.Util
import sbt.librarymanagement.VersionNumber
import ujson.{Js, Visitor}
import com.gilt.gfc.semver.SemVer

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

  def preProcess(parsedFiles: Js.Arr, defaultVersion: String, target: String)(source: String): Js.Value

  def postProcess(parsedFiles: Js.Arr,
                  preProcess: Js.Value, source: String,
                 target: String, errorMessage: ErrorMessage): Js.Arr

}

class ApiParamTitleWorker extends Worker {

  /**
    * PreProcess
    *
    * @param parsedFiles
    * @param defaultVersion
    * @param target       Target path in preProcess-Object (returned result), where the data should be set.
    * @return
    */
  def preProcess(parsedFiles: Js.Arr, defaultVersion: String, target: String = "defineParamTitle")(source: String): Js.Value = {

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
                    case _ => defaultVersion //TODO or the '0.0.0' if so remove defautlVersion
                  }
                val x = Util.merge(r, Js.Obj(target -> Js.Obj(name -> Js.Obj(version -> sourceNode))))
                if (x(target) == Js.Null)
                  x.obj.remove(target)
                x
              case _ => r
            }
          }
    }
  }

  def postProcess(parsedFiles: Js.Arr,
                  preProcess: Js.Value, source: String = "defineParamTitle" ,
                  target: String = "parameter", errorMessage: ErrorMessage = ErrorMessage("apiParam","@apiParam (group) varname","")): Js.Arr = {

    parsedFiles.arr.map { parsedFile =>
        parsedFile.arr.map { block =>
          val localTarget: Js.Value = block("local").obj.getOrElse(target, Js.Null) //block \ "local" \ target
          if (localTarget == Js.Null) block
          else {
            val fields = localTarget.obj.getOrElse("field", Js.Obj())
             fields.obj.keySet.foldLeft(Js.Obj(): Js.Value){
               case (newFields, fieldGroup) =>
                 fields(fieldGroup).arr.foldLeft(newFields){
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
                       else preProcess(source)(name).obj.getOrElse(version, {

                         val versionKeys = preProcess(source)(name).obj.keySet.toList

                         // find nearest matching version
                         var foundIndex = -1
                         var lastVersion = "0.0.0"
                         versionKeys.zipWithIndex.foreach {
                           case (currentVersion, versionIndex) =>
                             VersionNumber(version)
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

                     val Js.Str(title) = matchedData("title")

                     val newValue = Js.Obj(title -> Js.Arr(definition))
                     Util.merge(newField, newValue)

                 }

             }
            block
          }

        }
    }

  }
}

class ApiUseWorker extends Worker {

  /**
    * PreProcess
    *
    * @param parsedFiles
    * @param defaultVersion
    * @param target       Target path in preProcess-Object (returned result), where the data should be set.
    * @return
    */
  def preProcess(parsedFiles: Js.Arr, defaultVersion: String, target: String = "define")(source: String = target): Js.Value = {

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
                    case _ => defaultVersion //TODO or the '0.0.0' if so remove defautlVersion
                  }
               Util.merge(r, Js.Obj(target -> Js.Obj(name -> Js.Obj(version -> block("local")))))

              case _ => r
            }
        }
    }
  }

  def postProcess(parsedFiles: Js.Arr,
                  preProcess: Js.Value, source: String = "define" ,
                  target: String = "use", errorMessage: ErrorMessage = ErrorMessage("apiParam","@apiParam (group) varname","")): Js.Arr = {

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
            val matchedData =
              preProcess(source)(name).obj.getOrElse(version, {

                val versionKeys = preProcess(source)(name).obj.keySet.toList

                // find nearest matching version
                var foundIndex = -1
                var lastVersion = "0.0.0"
                versionKeys.zipWithIndex.foreach {
                  case (currentVersion, versionIndex) =>
                    VersionNumber(version)
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
            block("local")(target) = Js.Null
            Util.merge(block, Js.Obj("local" -> matchedData))
          }
        }
      }

    }
  }
}

object Worker {

}
