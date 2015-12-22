package com.culpin.team.worker

import com.culpin.team.core.SbtApidocConfiguration
import com.culpin.team.exception.WorkerException
import com.culpin.team.parser.Parser
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

/**
 * @author
 */
trait Worker {

  val name: String

  def preProcess(parsedFiles: JArray, target: String = "define"): JValue = {

    def buildResult(source: String): JValue = {
      val initResult: JValue = (target -> JObject())
      parsedFiles.arr.foldLeft(initResult) {
        case (result, parsedFile) =>
          val JArray(blocks) = parsedFile
          blocks.foldLeft(result) {
            case (r, block) =>
              val preprocessedValue = preprocessBlock(block, source, r)
              r merge preprocessedValue
          }
      }
    }

    val result = buildResult(source(target))

    if (result \ target == JNothing)
      (result removeField { case JField(key, value) => key == target })
    else result
  }

  def preprocessBlock(block: JValue, source: String, r: JValue): JValue

  def source(target: String): String

  def postProcess(parsedFiles: JArray, filenames: List[String],
    preProcess: JValue, packageInfos: SbtApidocConfiguration): JArray = parsedFiles

  val errorMap: Map[String, String] = Map(
    "element" -> "apiUse",
    "usage" -> "@apiUse group",
    "example" -> "@apiDefine MyValidGroup Some title\n@apiUse MyValidGroup"
  )

}

class ApiErrorStructureWorker extends ApiUseWorker {

  override val name = "api-error-structure"

  override def preProcess(parsedFiles: JArray, target: String = "defineErrorStructure"): JValue =
    super.preProcess(parsedFiles, "defineErrorStructure")

  override val source: String = "defineErrorStructure"
  override val target: String = "errorStructure"

}

class ApiErrorTitleWorker extends ApiParamTitleWorker {

  override val name = "api-error-title"

  override def preProcess(parsedFiles: JArray, target: String = "defineErrorTitle"): JValue =
    super.preProcess(parsedFiles, "defineErrorTitle")

  override val source: String = "defineErrorTitle"
  override val target: String = "error"

}

class ApiGroupWorker extends ApiParamTitleWorker {

  override val name = "api-group"

  override def preProcess(parsedFiles: JArray, target: String = "defineGroup"): JValue =
    super.preProcess(parsedFiles, target)

  override def matchData(preProcess: JValue, source: String = "defineGroup", name: String, version: String = "0.0.0") = {
    if (preProcess \ source \ name \ version != JNothing)
      preProcess \ source \ name \ version
    else {
      import com.gilt.gfc.semver.SemVer
      val versionKeys = (preProcess \ source \ name) match {
        case JObject(l) => JObject(l).values.keys.toList
        case _ => List()
      }

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

      val versionName = versionKeys(foundIndex)
      preProcess \ source \ name \ versionName

    }
  }

  override def postProcessBlock(block: JValue, preProcess: JValue): JValue = {

    val localTarget: JValue = block \ "local" \ target
    if (localTarget == JNothing) block
    else {
      val JString(name) = localTarget
      val version = block \ "version" match {
        case JString(v) => v
        case _ => "0.0.0"
      }
      val matchedData: JValue =
        if (preProcess \ source \ name == JNothing) {
          ("title" -> localTarget)
        } else {
          matchData(preProcess, source, name, version)
        }

      val value: JObject = ("local" ->
        ("groupTitle" -> matchedData \ "title") ~
        ("groupDescription" -> matchedData \ "description")
      )

      block merge value
    }
  }

  override val source: String = "defineGroup"
  override val target: String = "group"

  override def postProcess(parsedFiles: JArray, filenames: List[String], preProcess: JValue,
    packageInfos: SbtApidocConfiguration): JArray = {
    def setGroupNameIfEmpty: JArray = {
      Worker.mapBlock(parsedFiles, filenames) {
        case (block, filename) =>
          if ((block \ "global").children.nonEmpty) block
          else {
            val group = block \ "local" \ target match {
              case JString(g) => g
              case _ => filename
            }

            val valToAppend: JObject = ("local" ->
              (target -> group.replaceAll("""[^\w]""", "_"))
            )
            block merge valToAppend
          }
      }
    }

    setGroupNameIfEmpty.arr.zipWithIndex.map {
      case (parsedFile, parsedFileIndex) =>
        val JArray(blocks) = parsedFile
        blocks.map { postProcessBlock(_, preProcess) }
    }

  }

}

class ApiHeaderStructureWorker extends ApiUseWorker {

  override val name = "api-header-structure"

  override def preProcess(parsedFiles: JArray, target: String = "defineHeaderStructure"): JValue =
    super.preProcess(parsedFiles, "defineHeaderStructure")

  override val source: String = "defineHeaderStructure"
  override val target: String = "headerStructure"

}

class ApiHeaderTitleWorker extends ApiParamTitleWorker {

  override val name = "api-header-title"

  override def preProcess(parsedFiles: JArray, target: String = "defineHeaderTitle"): JValue =
    super.preProcess(parsedFiles, "defineHeaderTitle")

  override val source: String = "defineHeaderTitle"
  override val target: String = "header"

}

class ApiNameWorker extends Worker {

  override val name = "api-name"

  //Not used
  override def preProcess(parsedFiles: JArray, target: String = "name"): JValue = JNothing

  //Not used
  override def preprocessBlock(block: JValue, source: String, r: JValue): JValue = JNothing

  //Not used
  override def source(target: String): String = ""

  val source: String = "define"
  val target: String = "name"

  override def postProcess(parsedFiles: JArray, filenames: List[String], preProcess: JValue,
    packageInfos: SbtApidocConfiguration): JArray = {
    parsedFiles.arr.map { parsedFile =>
      val JArray(blocks) = parsedFile
      blocks.map { postProcessBlock(_, preProcess) }
    }
  }

  def postProcessBlock(block: JValue, preProcess: JValue): JValue = {
    def capitalize(string: String): String = {
      string.length() match {
        case 0 => ""
        case 1 => string.charAt(0).toUpper.toString()
        case _ => string.charAt(0).toUpper + string.take(1).toLowerCase
      }
    }

    if ((block \ "global").children.nonEmpty) block
    else {
      val name = block \ "local" \ target match {
        case JString(n) => n
        case _ =>
          val JString(theType) = block \ "local" \ "type"
          val JString(url) = block \ "local" \ "url"
          val initName = capitalize(theType)

          val matches = Parser.parseAndFilterNullGroup("""[\w]+""".r)(url)
          matches.foldLeft(initName) {
            case (acc, theMatch) =>
              acc + capitalize(theMatch)
          }
      }
      val newVal: JObject = ("local" ->
        ("name" -> name.replaceAll("""[^\w]""", "_"))
      )

      block merge newVal
    }
  }

}

class ApiPermissionWorker extends ApiParamTitleWorker {

  override val name = "api-permission"

  override def preProcess(parsedFiles: JArray, target: String = "definePermission"): JValue =
    super.preProcess(parsedFiles, target)

  override val source: String = "definePermission"

  override val target: String = "permission"

  override def postProcessBlock(block: JValue, preProcess: JValue): JValue = {
    if (block \ "local" \ target == JNothing) block
    else {

      val definitions = block \ "local" \ target

      val newPermission = definitions.children.foldLeft(JArray(List())) {
        case (permission, definition) =>

          val JString(name) = definition \ "name"
          val version = block \ "version" match {
            case JString(v) => v
            case _ => "0.0.0"
          }

          val metadata =
            if (preProcess \ source \ name == JNothing)
              ("name" -> name) ~ ("title" -> definition \ "title") ~ ("description" -> definition \ "description")
            else
              matchData(preProcess, source, name, version)

          val JArray(r) = permission merge JArray(List(metadata))
          JArray(r)
      }

      val valToAppend: JObject = ("local" ->
        (target -> newPermission)
      )
      block.replace("local" :: target :: Nil, newPermission)
    }
  }

}

class ApiParamTitleWorker extends Worker {

  val name = "api-param-title"

  override def preProcess(parsedFiles: JArray, target: String = "defineParamTitle"): JValue =
    super.preProcess(parsedFiles, target)

  def preprocessBlock(block: JValue, source: String, r: JValue): JValue = {
    val sourceNode = block \ "global" \ source
    sourceNode match {
      case JNothing => JNothing
      case _ => {
        val JString(name) = (sourceNode \ "name")
        val version = (block \ "version") match {
          case JString(v) => v
          case _ => "0.0.0"
        }

        r.transformField {
          case (target, _) =>
            (target ->
              (name ->
                (version -> sourceNode)
              )
            )
        }
      }
    }
  }

  def source(target: String): String = "define"

  def matchData(preProcess: JValue, source: String = "define", name: String, version: String = "0.0.0"): JValue = {

    if (preProcess \ source \ name == JNothing)
      ("name" -> name) ~ ("title" -> name)
    else if (preProcess \ source \ name \ version != JNothing)
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
      versionKeys.zipWithIndex.foreach {
        case (currentVersion, versionIndex) =>
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

  def postProcessBlock(block: JValue, preProcess: JValue): JValue = {

    if (block \ "local" \ target \ "fields" == JNothing) block
    else {

      val fields = block \ "local" \ target \ "fields" match {
        case JObject(fieldGroup) => JObject(fieldGroup)
        case _ => JObject()
      }

      val initNewFields: JValue = JObject()
      val nf = fields.obj.toMap.keySet.foldLeft(initNewFields) {
        case (newFields, fieldGroup) =>
          val param = (fields \ fieldGroup).asInstanceOf[JArray]
          param.arr.foldLeft(newFields) {
            case (newFields, definition) =>

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
  val source: String = "define"
  val target: String = "parameter"

  override def postProcess(parsedFiles: JArray, filenames: List[String],
    preProcess: JValue, packageInfos: SbtApidocConfiguration): JArray = {
    parsedFiles.arr.map { parsedFile =>
      val JArray(blocks) = parsedFile
      blocks.map { postProcessBlock(_, preProcess) }
    }
  }

}

class ApiSampleRequestWorker extends Worker {

  override val name = "api-sample-request"

  override def preprocessBlock(block: JValue, source: String, r: JValue): JValue = JNothing

  override def source(target: String): String = ""

  val target: String = "sampleRequest"

  override def postProcess(parsedFiles: JArray, filenames: List[String],
    preProcess: JValue, packageInfos: SbtApidocConfiguration): JArray = {
    def appendSampleUrl(url: String): JObject = {
      if (packageInfos.sampleUrl.isDefined && url.length >= 4 && !url.toLowerCase.startsWith("http")) {
        val Some(sampleUrl) = packageInfos.sampleUrl
        ("url" -> (sampleUrl + url))
      } else ("url" -> url)
    }

    Worker.mapBlock(parsedFiles, filenames) {
      case (block, filename) =>

        val sampleBlock = block \ "local" \ target
        val newBlock = sampleBlock match {
          case JArray(entries) => JArray(entries
            .collect {
              case entry if { val JString(url) = entry \ "url"; !url.equals("off") } =>
                val JString(url) = entry \ "url"
                appendSampleUrl(url)
            })

          case _ =>
            if (packageInfos.sampleUrl.isDefined && block \ "local" \ "url" != JNothing) {
              val Some(sampleUrl) = packageInfos.sampleUrl
              val JString(url) = block \ "local" \ "url"
              val value: JObject = ("url" -> (sampleUrl + url))
              JArray(List(value))
            } else JArray(List())
        }

        if (newBlock.children.isEmpty) {
          block removeField {
            case (key, _) => key == "sampleRequest"
          }
        } else {

          block transformField {
            case ("sampleRequest", _) =>
              ("sampleRequest" -> newBlock)
          }
        }

    }
  }

}

class ApiStructureWorker extends ApiUseWorker {

  override val name = "api-structure"

  override def preProcess(parsedFiles: JArray, target: String = "defineStructure"): JValue =
    super.preProcess(parsedFiles, "defineStructure")

  override val source: String = "defineStructure"
  override val target: String = "structure"

}

class ApiSuccessStructureWorker extends ApiUseWorker {

  override val name = "api-success-structure"

  override def preProcess(parsedFiles: JArray, target: String = "defineSuccessStructure"): JValue =
    super.preProcess(parsedFiles, "defineSuccessStructure")

  override val source: String = "defineSuccessStructure"
  override val target: String = "successStructure"
}

class ApiSuccessTitleWorker extends ApiParamTitleWorker {

  override val name = "api-success-title"

  override def preProcess(parsedFiles: JArray, target: String = "defineSuccessTitle"): JValue =
    super.preProcess(parsedFiles, "defineSuccessTitle")

}

class ApiUseWorker extends Worker {

  override val name = "api-use"

  def preprocessBlock(block: JValue, source: String, r: JValue): JValue = {
    val sourceNode = block \ "global" \ source
    sourceNode match {
      case JNothing => JNothing
      case _ => {
        val JString(name) = (sourceNode \ "name")
        val JString(version) = (block \ "version")

        r.transformField {
          case (target, _) =>
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

  def matchData(preProcess: JValue, source: String = "define", name: String, version: String = "0.0.0") = {
    if (preProcess \ source \ name \ version != JNothing)
      preProcess \ source \ name \ version
    else {
      import com.gilt.gfc.semver.SemVer
      val versionKeys = {
        val JObject(l) = preProcess \ source \ name
        JObject(l).values.keys.toList
      }

      // find nearest matching version
      var foundIndex = -1;
      var lastVersion = "0.0.0";
      versionKeys.zipWithIndex.foreach {
        case (currentVersion, versionIndex) =>
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

  def postProcessBlock(block: JValue, preProcess: JValue): JValue = {
    val localTarget: JValue = block \ "local" \ target
    if (localTarget == JNothing) block
    else {

      val JString(name) = localTarget \ "name"
      val version = block \ "version" match {
        case JString(v) => v
        case _ => "0.0.0"
      }

      if (preProcess \ source \ name == JNothing) {
        val JInt(index) = block \ "index"
        val JString(filename) = block \ "local" \ "filename"
        throw new WorkerException(filename, index, errorMap)
      } else {
        val matchedData = matchData(preProcess, source, name, version)

        val fieldToRemove = block \ "local" \ target
        val cleanedUpBlock = block remove { _ == fieldToRemove }

        val localMatchedData: JValue = ("local" -> matchedData)

        val valueToAppend = cleanedUpBlock merge localMatchedData
        cleanedUpBlock merge valueToAppend

      }
    }
  }

  val source: String = "define"
  val target: String = "use"

  override def postProcess(parsedFiles: JArray, filenames: List[String], preProcess: JValue,
    packageInfos: SbtApidocConfiguration): JArray = {
    parsedFiles.arr.map { parsedFile =>
      val JArray(blocks) = parsedFile
      blocks.map { postProcessBlock(_, preProcess) }
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
    new ApiNameWorker,
    new ApiParamTitleWorker,
    new ApiPermissionWorker,
    new ApiSampleRequestWorker,
    new ApiStructureWorker,
    new ApiSuccessStructureWorker,
    new ApiSuccessTitleWorker,
    new ApiUseWorker
  )

  def mapBlock(parsedFiles: JArray, filenames: List[String])(f: (JValue, String) => JValue): JArray = {
    parsedFiles.arr.zipWithIndex.map {
      case (parsedFileArray, index) =>
        val JArray(parsedFile) = parsedFileArray
        parsedFile.map { block =>
          f(block, filenames(index))
        }
    }
  }

  def processFilenameBlock(block: JValue, filename: String): JValue = {
    if ((block \ "global").children.isEmpty && (block \ "local").children.nonEmpty) {
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
        case _ => filename
      }

      val newLocal: JObject = ("local" ->
        ("type" -> newType) ~
        ("url" -> newUrl) ~
        ("version" -> newVersion) ~
        ("filename" -> newFilename)
      )
      block merge newLocal
    } else
      block
  }

  def processFilename(parsedFiles: JArray, filenames: List[String]): JArray =
    mapBlock(parsedFiles, filenames)(processFilenameBlock)

  def preProcess(parsedFiles: JArray): JValue = {
    val initResult: JValue = JObject()
    workers.foldLeft(initResult) {
      case (preProcessResult, worker) =>
        preProcessResult merge worker.preProcess(parsedFiles)
    }
  }

  def postProcess(parsedFiles: JArray, filenames: List[String],
    preProcess: JValue, packageInfos: SbtApidocConfiguration): JArray = {
    workers.foldLeft(parsedFiles) {
      case (pf, worker) =>
        worker.postProcess(pf, filenames, preProcess, packageInfos)
    }
  }

  def apply(parsedFiles: JArray, filenames: List[String], packageInfos: SbtApidocConfiguration): JArray = {
    val processedNames = processFilename(parsedFiles, filenames)
    val preprocessResult = preProcess(processedNames)
    postProcess(processedNames, filenames, preprocessResult, packageInfos)
  }

}

