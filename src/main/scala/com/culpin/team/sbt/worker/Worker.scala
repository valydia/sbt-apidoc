package com.culpin.team.sbt.worker

import com.culpin.team.sbt.{Config, Util}
import ujson.Js

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

  def postProcess(parsedFiles: Js.Arr, filenames: List[String],
                  preProcess: Js.Value, config: Config, source: String,
                 target: String, errorMessage: ErrorMessage): Js.Arr

}

class ApiParamTitleWorker extends Worker {

  def preProcess(parsedFiles: Js.Arr, defaultVersion: String, target: String = "defineParamTitle")(source: String): Js.Value = {
    val initResult: Js.Value = Js.Obj(target -> Js.Obj())

    parsedFiles.value.foldLeft(initResult) {
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
                    case _ => defaultVersion
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

  def postProcess(parsedFiles: Js.Arr, filenames: List[String],
                  preProcess: Js.Value, config: Config, source: String,
                  target: String, errorMessage: ErrorMessage): Js.Arr = ???
}

object Worker {

}
