package com.culpin.team.parser

import java.io.File

import com.culpin.team.core._
import com.culpin.team.util.Util
import org.json4s.native.JsonMethods._
import org.scalatest.{ Matchers, FlatSpec }

import org.json4s.JsonAST._

class ParserSpec extends FlatSpec with Matchers {

  "Parser" should "find block in file" in {

    val string = "/**\n * Created by valydia on 26/07/15.\n */\npublic class JavaMain " +
      "{\n    /**\n     * Block 1\n     * @param arg\n     */\n    public static void " +
      "main1 (String [] arg) {\n        for (String s: arg) {\n            System.out." +
      "println(s);\n        }\n    }\n}"

    val result = Parser.findBlocks(string)
    val expected = List("Created by valydia on 26/07/15.", "Block 1\n@param arg")

    assert(result === expected)
  }

  "Parser" should "find element in block" in {

    val result = Parser.findElements("Block 1\n@param arg")
    val expected = List(Element("@param arg", "param", "param", "arg"))
    assert(result === expected)

    val result2 = Parser.findElements("More complex block\n@param arg The array of string\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected2 = List(
      Element("@param arg The array of string", "param", "param", "arg The array of string"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string")
    )
    assert(result2 === expected2)

    val result3 = Parser.findElements("Ignored block\n@apiIgnore Not finished Method\n" +
      "@param theString the string\n@param theInt the int\n@return the result string")

    val expected3 = List(
      Element("@apiIgnore Not finished Method", "apiignore", "apiIgnore", "Not finished Method"),
      Element("@param theString the string", "param", "param", "theString the string"),
      Element("@param theInt the int", "param", "param", "theInt the int"),
      Element("@return the result string", "return", "return", "the result string")
    )
    assert(result3 === expected3)

    val result4 = Parser.findElements("Api block\n@apiParam")
    val expected4 = List(Element("@apiParam", "apipara", "apiPara", "m"))
    assert(result4 === expected4)
  }

  "Api Parser" should "parse api element - json" in {

    val apiParser = new ApiParser
    val Some(result) = apiParser.parseBlock("{get} /user/:id")
    val local = result \ "local"
    assert(local \ "type" === JString("get"))
    assert(local \ "url" === JString("/user/:id"))
    assert(local \ "title" === JNothing)

  }

  "Api Parser" should "parse api element with title - json" in {
    val apiParser = new ApiParser
    val Some(result) = apiParser.parseBlock("{get} /user/:id some title")
    val local = result \ "local"
    assert(local \ "type" === JString("get"))
    assert(local \ "url" === JString("/user/:id"))
    assert(local \ "title" === JString("some title"))
  }

  "ApiDefineParser" should "parse api define element" in {

    val apiDefineParser = new ApiDefineParser
    val Some(result) = apiDefineParser.parseBlock("CreateUserError")

    val define = result \ "global" \ "define"

    assert(define \ "name" === JString("CreateUserError"))
    assert(define \ "title" === JString(""))
    assert(define \ "description" === JString(""))

  }

  "ApiDefineParser" should "should parse define element" in {

    val apiDefineParser = new ApiDefineParser

    val Some(result) = apiDefineParser.parseBlock("admin This title is visible in version 0.1.0 and 0.2.0")

    val define = result \ "global" \ "define"

    assert(define \ "name" === JString("admin"))
    assert(define \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(define \ "description" === JString(""))

  }

  "ApiDefineParser" should "should parse define element with multiline" in {

    val apiDefineParser = new ApiDefineParser

    val Some(result) = apiDefineParser.parseBlock("admin Admin access rights needed.\nOptionally you can write here further Informations about the permission.\n\nAn \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version.")

    val define = result \ "global" \ "define"

    assert(define \ "name" === JString("admin"))
    assert(define \ "title" === JString("Admin access rights needed."))
    //fixme descritption not well formatted
    assert(define \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))

  }

  "ApiDescriptionParser" should "parse api description element" in {

    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("Some Description")
    val local = result \ "local"
    assert(local \ "description" === JString("Some Description"))

    val Some(result2) = apiDescriptionParser.parseBlock("Some Description \n on several line \n isnt'it?")
    val local2 = result2 \ "local"
    assert(local2 \ "description" === JString("Some Description \n on several line \n isnt'it?"))
  }

  "ApiDescriptionParser" should "parse empty api description element " in {

    val apiDescriptionParser = new ApiDescriptionParser
    val result = apiDescriptionParser.parseBlock("")
    val expected = None
    assert(result === expected)
  }

  "ApiDescriptionParser" should "parse Word only api description element " in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(resultMap) = apiDescriptionParser.parseBlock("Text")
    val local = resultMap \ "local"
    assert(local \ "description" === JString("Text"))

  }

  "ApiDescriptionParser" should "Trim single line " in {
    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("   Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace). ")
    val local = result \ "local"
    assert(local \ "description" === JString("Text line 1 (Begin: 3xSpaces (3 removed), End: 1xSpace)."))

  }

  "ApiDescriptionParser" should "Trim multi line (spaces)" in {

    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("    Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces).  ")
    val local = result \ "local"
    assert(local \ "description" === JString("Text line 1 (Begin: 4xSpaces (3 removed)).\n   Text line 2 (Begin: 3xSpaces (3 removed), End: 2xSpaces)."))
  }

  "ApiDescriptionParser" should "Trim multi line (tabs)" in {

    val apiDescriptionParser = new ApiDescriptionParser
    val Some(result) = apiDescriptionParser.parseBlock("\t\t\tText line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab).\t")
    val local = result \ "local"
    assert(local \ "description" === JString("Text line 1 (Begin: 3xTab (2 removed)).\n\t\tText line 2 (Begin: 2x Tab (2 removed), End: 1xTab)."))
  }

  "ApiErrorExampleParser" should "parse error example element" in {

    val apiErrorExampleParser = new ApiErrorExampleParser
    val Some(result) = apiErrorExampleParser.parseBlock("{json} Error-Response:\n                 This is an example.")

    val examples = (result \ "local" \ "error" \ "examples")(0)
    assert(examples \ "title" === JString("Error-Response:"))
    assert(examples \ "content" === JString("This is an example."))
    assert(examples \ "type" === JString("json"))

  }

  "ApiErrorParser" should "parse error element" in {

    val apiErrorParser = new ApiErrorParser
    val Some(result) = apiErrorParser.parseBlock("UserNotFound The <code>id</code> of the User was not found.")

    val errorFields = (result \ "local" \ "error" \ "fields" \ "Error 4xx")(0)
    assert(errorFields \ "group" === JString("Error 4xx"))
    assert(errorFields \ "optional" === JString("false"))
    assert(errorFields \ "field" === JString("UserNotFound"))
    assert(errorFields \ "description" === JString("The <code>id</code> of the User was not found."))

  }

  "ApiExampleParser" should "parse example element" in {

    val apiExampleParser = new ApiExampleParser
    val Some(result) = apiExampleParser.parseBlock("Example usage:\ncurl -i http://localhost/user/4711")
    val examples = result \ "local" \ "examples"
    assert(examples \ "title" === JString("Example usage:"))
    assert(examples \ "content" === JString("curl -i http://localhost/user/4711"))
    assert(examples \ "type" === JString("json"))

  }

  "ApiGroupParser" should "parse group element" in {

    val apiGroupParser = new ApiGroupParser
    val Some(result) = apiGroupParser.parseBlock("User")

    val local = result \ "local"
    assert(local \ "group" === JString("User"))

  }

  "ApiHeaderExampleParser" should "parse header example element" in {

    val apiHeaderExampleParser = new ApiHeaderExampleParser

    val Some(result) = apiHeaderExampleParser.parseBlock("{json} Header-Example:\n    {\n      \"Accept-Encoding\": \"Accept-Encoding: gzip, deflate\"\n    }")

    val headerExample = (result \ "local" \ "header" \ "examples")(0)
    assert(headerExample \ "title" === JString("Header-Example:"))
    assert(headerExample \ "content" === JString("{\n  \"Accept-Encoding\": \"Accept-Encoding: gzip, deflate\"\n}"))
    assert(headerExample \ "type" === JString("json"))

  }

  "ApiHeaderExampleParser" should "parse header example element 2" in {
    val apiHeaderExampleParser = new ApiHeaderExampleParser

    val Some(result) = apiHeaderExampleParser.parseBlock("{json} Request-Example:\n{ \"content\": \"This is an example content\" }")

    val examples = result \ "local" \ "header" \ "examples"
    assert(examples \ "title" === JString("Request-Example:"))
    assert(examples \ "content" === JString("{ \"content\": \"This is an example content\" }"))
    assert(examples \ "type" === JString("json"))

  }

  "ApiHeaderParser" should "parse param element" in {

    val apiHeaderParser = new ApiHeaderParser

    val Some(result) = apiHeaderParser.parseBlock("{String} authorization Authorization value.")
    val header = result \ "local" \ "header" \ "fields" \ "Header"
    assert(header \ "group" === JString("Header"))
    assert(header \ "type" === JString("String"))
    assert(header \ "optional" === JString("false"))
    assert(header \ "field" === JString("authorization"))
    assert(header \ "defaultValue" === JNothing)
    assert(header \ "description" === JString("Authorization value."))
    assert(header \ "size" === JNothing)
    assert(header \ "allowedValue" === JNothing)
  }

  "ApiHeaderParser" should "parse param element with group" in {

    val apiHeaderParser = new ApiHeaderParser

    val Some(result) = apiHeaderParser.parseBlock("(MyHeaderGroup) {String} authorization Authorization value.")

    val header = result \ "local" \ "header" \ "fields" \ "MyHeaderGroup"
    assert(header \ "group" === JString("MyHeaderGroup"))
    assert(header \ "type" === JString("String"))
    assert(header \ "optional" === JString("false"))
    assert(header \ "field" === JString("authorization"))
    assert(header \ "defaultValue" === JNothing)
    assert(header \ "description" === JString("Authorization value."))
    assert(header \ "size" === JNothing)
    assert(header \ "allowedValue" === JNothing)

  }

  "ApiNameParser" should "parse name element" in {
    val apiNameParser = new ApiNameParser

    val Some(result) = apiNameParser.parseBlock("Welcome Page.")
    val local = result \ "local"
    assert(local \ "name" === JString("Welcome_Page."))
  }

  "ApiParamExampleParser" should "parse param example element" in {

    val apiParamExampleParser = new ApiParamExampleParser
    val Some(result) = apiParamExampleParser.parseBlock("{json} Request-Example:\n                 { \"content\": \"This is an example content\" }")

    val example = (result \ "local" \ "parameter" \ "examples")(0)
    assert(example \ "title" === JString("Request-Example:"))
    assert(example \ "content" === JString("{ \"content\": \"This is an example content\" }"))
    assert(example \ "type" === JString("json"))
  }

  "ApiParamParser" should "parse param element - json " in {

    val apiParamParser = new ApiParamParser

    val Some(result) = apiParamParser.parseBlock("{String} country=\"DE\" Mandatory with default value \"DE\".")
    val parameter = result \ "local" \ "parameter" \ "fields" \ "Parameter"
    assert(parameter \ "group" === JString("Parameter"))
    assert(parameter \ "type" === JString("String"))
    assert(parameter \ "optional" === JString("false"))
    assert(parameter \ "field" === JString("country"))
    assert(parameter \ "defaultValue" === JString("DE"))
    assert(parameter \ "description" === JString("Mandatory with default value \"DE\"."))
    assert(parameter \ "size" === JNothing)
    assert(parameter \ "allowedValue" === JNothing)
    assert(parameter \ "size111" === JNothing)

    val Some(result2) = apiParamParser.parseBlock("{String} lastname     Mandatory Lastname.")
    val parameter2 = result2 \ "local" \ "parameter" \ "fields" \ "Parameter"
    assert(parameter2 \ "group" === JString("Parameter"))
    assert(parameter2 \ "type" === JString("String"))
    assert(parameter2 \ "optional" === JString("false"))
    assert(parameter2 \ "field" === JString("lastname"))
    assert(parameter2 \ "defaultValue" === JNothing)
    assert(parameter2 \ "description" === JString("Mandatory Lastname."))
    assert(parameter2 \ "size" === JNothing)

  }

  "ApiParamParser" should "parse Simple fieldname only - json" in {

    val apiParamParser = new ApiParamParser
    val Some(result) = apiParamParser.parseBlock("simple")

    val parameter = result \ "local" \ "parameter" \ "fields" \ "Parameter"
    assert(parameter \ "field" === JString("simple"))
    assert(parameter \ "description" === JString(""))
    assert(parameter \ "optional" === JString("false"))
    assert(parameter \ "group" === JString("Parameter"))
  }

  "ApiParamParser" should "parse Type, Fieldname, Description" in {

    val apiParamParser = new ApiParamParser

    val Some(result) = apiParamParser.parseBlock("{String} name The users name.")
    val parameter = result \ "local" \ "parameter" \ "fields" \ "Parameter"
    assert(parameter \ "field" === JString("name"))
    assert(parameter \ "description" === JString("The users name."))
    assert(parameter \ "optional" === JString("false"))
    assert(parameter \ "type" === JString("String"))
    assert(parameter \ "group" === JString("Parameter"))

  }

  "ApiParamParser" should "parse all options, with optional defaultValue" in {

    val apiParamParser = new ApiParamParser
    val content = "( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }" +
      "[ \\MyClass\\field.user_first-name = \'John Doe\' ] Some description."
    val Some(result) = apiParamParser.parseBlock(content)

    val parameter = result \ "local" \ "parameter" \ "fields" \ "MyGroup"

    assert(parameter \ "field" === JString("\\MyClass\\field.user_first-name"))
    assert(parameter \ "size" === JString("1..10"))
    assert(parameter \ "description" === JString("Some description."))
    assert(parameter \ "optional" === JString("true"))
    assert(parameter \ "type" === JString("\\Object\\String.uni-code_char[]"))
    assert(parameter \ "group" === JString("MyGroup"))
    assert(parameter \ "defaultValue" === JString("John Doe"))
    assert(parameter \ "allowedValue" === JArray(List(JString("\'abc\'"), JString("\'def\'"))))

  }

  "ApiParamParser" should "parse all options, without optional-marker, without default value quotes" in {

    val apiParamParser = new ApiParamParser
    val content = "( MyGroup ) { \\Object\\String.uni-code_char[] { 1..10 } = \'abc\', \'def\' }  " +
      "\\MyClass\\field.user_first-name = John_Doe Some description."
    val Some(result) = apiParamParser.parseBlock(content)

    val parameter = result \ "local" \ "parameter" \ "fields" \ "MyGroup"

    assert(parameter \ "field" === JString("\\MyClass\\field.user_first-name"))
    assert(parameter \ "size" === JString("1..10"))
    assert(parameter \ "description" === JString("Some description."))
    assert(parameter \ "optional" === JString("false"))
    assert(parameter \ "type" === JString("\\Object\\String.uni-code_char[]"))
    assert(parameter \ "group" === JString("MyGroup"))
    assert(parameter \ "defaultValue" === JString("John_Doe"))
    assert(parameter \ "allowedValue" === JArray(List(JString("\'abc\'"), JString("\'def\'"))))
  }

  "ApiPermissionParser" should "parse permission element" in {

    val apiPermissionParser = new ApiPermissionParser

    val Some(result) = apiPermissionParser.parseBlock("admin")

    val permission = (result \ "local" \ "permission")(0)

    assert(permission \ "name" === JString("admin"))

  }

  "ApiSampleRequestParser" should "parse success element" in {

    val apiSampleRequestParser = new ApiSampleRequestParser

    val Some(result) = apiSampleRequestParser.parseBlock("http://test.github.com")

    val parameter = (result \ "local" \ "sampleRequest")(0)

    assert(parameter \ "url" === JString("http://test.github.com"))

  }

  "ApiSuccessExampleParser" should "parse success exemple element" in {

    val apiSuccessExampleParser = new ApiSuccessExampleParser

    val Some(result) = apiSuccessExampleParser.parseBlock("Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n")
    val examples = result \ "local" \ "success" \ "examples"

    assert(examples \ "title" === JString("Success-Response:"))
    assert(examples \ "content" === JString("HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}"))
    assert(examples \ "type" === JString("json"))

  }

  "ApiSuccessParser" should "parse success element" in {

    val apiSuccessParser = new ApiSuccessParser

    val Some(result) = apiSuccessParser.parseBlock("{String} firstname Firstname of the User.")

    val parameter = (result \ "local" \ "success" \ "fields" \ "Success 200")(0)

    assert(parameter \ "field" === JString("firstname"))
    assert(parameter \ "size" === JNothing)
    assert(parameter \ "description" === JString("Firstname of the User."))
    assert(parameter \ "optional" === JString("false"))
    assert(parameter \ "type" === JString("String"))
    assert(parameter \ "group" === JString("Success 200"))
    assert(parameter \ "defaultValue" === JNothing)
    assert(parameter \ "allowedValue" === JNothing)
  }

  "ApiUseParser" should "parse user element" in {

    val apiUseParser = new ApiUseParser
    val Some(result) = apiUseParser.parseBlock("MySuccess")

    val use = (result \ "local" \ "use")(0)
    assert(use \ "name" === JString("MySuccess"))

  }

  "ApiVersionParser" should "parse version element" in {

    val apiVersionParser = new ApiVersionParser

    val Some(result) = apiVersionParser.parseBlock("1.6.2")

    val local = result \ "local"

    assert(local \ "version" === JString("1.6.2"))

  }

  "Parser" should "parse block element" in {

    val detectedElement = Seq(
      Seq(
        Element("@api {get} / Home page.", "api", "api", "{get} / Home page."),
        Element("@apiName Welcome Page.", "apiname", "apiName", "Welcome Page."),
        Element("@apiGroup Application", "apigroup", "apiGroup", "Application"),
        Element("@apiVersion 1.0.0", "apiversion", "apiVersion", "1.0.0"),
        Element("@apiDescription Renders the welcome page\n", "apidescription", "apiDescription", "Renders the welcome page\n"),
        Element("@apiSuccessExample Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n", "apisuccessexample", "apiSuccessExample", "Success-Response:\n    HTTP/1.1 200 OK\n    HTML for welcome page\n    {\n      \"emailAvailable\": \"true\"\n    }\n")
      )
    )

    val result = Parser.parseBlockElement(detectedElement, "app/controllers/gathr/culpinteam/v1/Application.scala")

    val local = result(0) \ "local"
    assert(local \ "type" === JString("get"))
    assert(local \ "url" === JString("/"))
    assert(local \ "title" === JString("Home page."))
    assert(local \ "name" === JString("Welcome_Page."))
    assert(local \ "group" === JString("Application"))
    assert(local \ "version" === JString("1.0.0"))
    assert(local \ "description" === JString("Renders the welcome page"))

    val examples = local \ "success" \ "examples"
    assert(examples \ "title" === JString("Success-Response:"))
    assert(examples \ "content" === JString("HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}"))
    assert(examples \ "type" === JString("json"))

    val global = result(0) \ "global"
    assert(global === JObject())

  }

  "Parser" should "parse file" in {

    val sources = List(new File(getClass.getResource("/Application.scala").getFile))
    val (blocks, filenames) = Parser(sources)
    assert(filenames === List("Application.scala"))
    // val block = blocks(0)(0)
    val local = blocks(0)(0) \ "local"
    assert(local \ "type" === JString("get"))
    assert(local \ "url" === JString("/"))
    assert(local \ "title" === JString("Home page."))
    assert(local \ "name" === JString("Welcome_Page."))
    assert(local \ "group" === JString("Application"))
    assert(local \ "version" === JString("1.0.0"))
    assert(local \ "description" === JString("Renders the welcome page"))

    val examples = local \ "success" \ "examples"
    assert(examples \ "title" === JString("Success-Response:"))
    assert(examples \ "content" === JString("HTTP/1.1 200 OK\nHTML for welcome page\n{\n  \"emailAvailable\": \"true\"\n}"))
    assert(examples \ "type" === JString("json"))

    val global = blocks(0)(0) \ "global"
    assert(global === JObject())
  }

  "Parser" should "parse files 2" in {

    val sources = List(new File(getClass.getResource("/_apidoc.js").getFile),
      new File(getClass.getResource("/full-example.js").getFile))

    val (JArray(List(file1, file2)), filenames) = Parser(sources)

    assert(filenames === List("_apidoc.js", "full-example.js"))

    val JArray(List(block1, block2, block3, block4, block5, block6)) = file1
    val JArray(List(block2_1, block2_2, block2_3)) = file2

    /////File 1

    ///// Block1

    assert(block1 \ "version" === JString("0.2.0"))
    assert(block1 \ "index" === JInt(1))

    val d1 = block1 \ "global" \ "define"

    assert(d1 \ "name" === JString("CreateUserError"))
    assert(d1 \ "title" === JString(""))
    assert(d1 \ "description" === JString(""))
    val l1 = block1 \ "local"

    assert(l1 \ "version" === JString("0.2.0"))

    val JArray(List(ef1, ef2)) = l1 \ "error" \ "fields" \ "Error 4xx"
    assert(ef1 \ "group" === JString("Error 4xx"))
    assert(ef1 \ "optional" === JString("false"))
    assert(ef1 \ "field" === JString("NoAccessRight"))
    assert(ef1 \ "description" === JString("Only authenticated Admins can access the data."))

    assert(ef2 \ "group" === JString("Error 4xx"))
    assert(ef2 \ "optional" === JString("false"))
    assert(ef2 \ "field" === JString("UserNameTooShort"))
    assert(ef2 \ "description" === JString("Minimum of 5 characters required."))

    val JArray(List(ex1)) = l1 \ "error" \ "examples"

    assert(ex1 \ "title" === JString("Response (example):"))
    assert(ex1 \ "content" === JString("HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}"))
    assert(ex1 \ "type" === JString("json"))

    ///// Block2

    assert(block2 \ "version" === JString("0.3.0"))
    assert(block2 \ "index" === JInt(2))

    val d2 = block2 \ "global" \ "define"
    //
    assert(d2 \ "name" === JString("admin"))
    assert(d2 \ "title" === JString("Admin access rights needed."))
    assert(d2 \ "description" === JString("Optionallyyou can write here further Informations about the permission.An \"apiDefinePermission\"-block can have an \"apiVersion\", so you can attach the block to a specific version."))
    //
    val l2 = block2 \ "local"

    assert(l2 \ "version" === JString("0.3.0"))

    ///// Block3

    assert(block3 \ "version" === JString("0.1.0"))
    assert(block3 \ "index" === JInt(3))
    //
    val d3 = block3 \ "global" \ "define"

    assert(d3 \ "name" === JString("admin"))
    assert(d3 \ "title" === JString("This title is visible in version 0.1.0 and 0.2.0"))
    assert(d3 \ "description" === JString(""))
    val l3 = block3 \ "local"

    assert(l3 \ "version" === JString("0.1.0"))

    ///// Block4

    assert(block4 \ "version" === JString("0.2.0"))
    assert(block4 \ "index" === JInt(4))
    assert(block4 \ "global" === JObject())

    val l4 = block4 \ "local"

    assert(l4 \ "version" === JString("0.2.0"))
    assert(l4 \ "type" === JString("get"))
    assert(l4 \ "url" === JString("/user/:id"))
    assert(l4 \ "title" === JString("Read data of a User"))
    assert(l4 \ "name" === JString("GetUser"))
    assert(l4 \ "group" === JString("User"))
    assert(l4 \ "description" === JString("Here you can describe the function.\nMultilines are possible."))
    //
    val JArray(List(per4)) = l4 \ "permission"
    assert(per4 \ "name" === JString("admin"))

    val JArray(List(par4_1)) = l4 \ "parameter" \ "fields" \ "Parameter"
    assert(par4_1 \ "group" === JString("Parameter"))
    assert(par4_1 \ "type" === JString("String"))
    assert(par4_1 \ "optional" === JString("false"))
    assert(par4_1 \ "field" === JString("id"))
    assert(par4_1 \ "description" === JString("The Users-ID."))

    val JArray(List(suc4_1, suc4_2)) = l4 \ "success" \ "fields" \ "Success 200"
    assert(suc4_1 \ "group" === JString("Success 200"))
    assert(suc4_1 \ "type" === JString("String"))
    assert(suc4_1 \ "optional" === JString("false"))
    assert(suc4_1 \ "field" === JString("id"))
    assert(suc4_1 \ "description" === JString("The Users-ID."))

    assert(suc4_2 \ "group" === JString("Success 200"))
    assert(suc4_2 \ "type" === JString("Date"))
    assert(suc4_2 \ "optional" === JString("false"))
    assert(suc4_2 \ "field" === JString("name"))
    assert(suc4_2 \ "description" === JString("Fullname of the User."))

    val JArray(List(ef4_1)) = l4 \ "error" \ "fields" \ "Error 4xx"
    assert(ef4_1 \ "group" === JString("Error 4xx"))
    assert(ef4_1 \ "optional" === JString("false"))
    assert(ef4_1 \ "field" === JString("UserNotFound"))
    assert(ef4_1 \ "description" === JString("The <code>id</code> of the User was not found."))

    ///// Block5

    assert(block5 \ "version" === JString("0.1.0"))
    assert(block5 \ "index" === JInt(5))
    assert(block5 \ "global" === JObject())

    val l5 = block5 \ "local"

    assert(l5 \ "version" === JString("0.1.0"))
    assert(l5 \ "type" === JString("get"))
    assert(l5 \ "url" === JString("/user/:id"))
    assert(l5 \ "title" === JString("Read data of a User"))
    assert(l5 \ "name" === JString("GetUser"))
    assert(l5 \ "group" === JString("User"))
    assert(l5 \ "description" === JString("Here you can describe the function.\nMultilines are possible."))
    //
    val JArray(List(per5)) = l5 \ "permission"
    assert(per5 \ "name" === JString("admin"))

    val JArray(List(par5_1)) = l5 \ "parameter" \ "fields" \ "Parameter"
    assert(par5_1 \ "group" === JString("Parameter"))
    assert(par5_1 \ "type" === JString("String"))
    assert(par5_1 \ "optional" === JString("false"))
    assert(par5_1 \ "field" === JString("id"))
    assert(par5_1 \ "description" === JString("The Users-ID."))

    val JArray(List(suc5_1, suc5_2)) = l5 \ "success" \ "fields" \ "Success 200"
    assert(suc5_1 \ "group" === JString("Success 200"))
    assert(suc5_1 \ "type" === JString("String"))
    assert(suc5_1 \ "optional" === JString("false"))
    assert(suc5_1 \ "field" === JString("id"))
    assert(suc5_1 \ "description" === JString("The Users-ID."))

    assert(suc5_2 \ "group" === JString("Success 200"))
    assert(suc5_2 \ "type" === JString("Date"))
    assert(suc5_2 \ "optional" === JString("false"))
    assert(suc5_2 \ "field" === JString("name"))
    assert(suc5_2 \ "description" === JString("Fullname of the User."))

    val JArray(List(ef5_1)) = l5 \ "error" \ "fields" \ "Error 4xx"
    assert(ef5_1 \ "group" === JString("Error 4xx"))
    assert(ef5_1 \ "optional" === JString("false"))
    assert(ef5_1 \ "field" === JString("UserNotFound"))
    assert(ef5_1 \ "description" === JString("The error description text in version 0.1.0."))

    ///// Block6

    assert(block6 \ "version" === JString("0.2.0"))
    assert(block6 \ "index" === JInt(6))
    assert(block6 \ "global" === JObject())

    val l6 = block6 \ "local"

    assert(l6 \ "version" === JString("0.2.0"))
    assert(l6 \ "type" === JString("post"))
    assert(l6 \ "url" === JString("/user"))
    assert(l6 \ "title" === JString("Create a User"))
    assert(l6 \ "name" === JString("PostUser"))
    assert(l6 \ "group" === JString("User"))
    assert(l6 \ "description" === JString("In this case \"apiErrorStructure\" is defined and used.\nDefine blocks with params that will be used in several functions, so you dont have to rewrite them."))

    val JArray(List(per6)) = l6 \ "permission"
    assert(per6 \ "name" === JString("none"))

    val JArray(List(par6_1)) = l6 \ "parameter" \ "fields" \ "Parameter"
    assert(par6_1 \ "group" === JString("Parameter"))
    assert(par6_1 \ "type" === JString("String"))
    assert(par6_1 \ "optional" === JString("false"))
    assert(par6_1 \ "field" === JString("name"))
    assert(par6_1 \ "description" === JString("Name of the User."))

    val JArray(List(suc6)) = l6 \ "success" \ "fields" \ "Success 200"
    assert(suc6 \ "group" === JString("Success 200"))
    assert(suc6 \ "type" === JString("String"))
    assert(suc6 \ "optional" === JString("false"))
    assert(suc6 \ "field" === JString("id"))
    assert(suc6 \ "description" === JString("The Users-ID."))

    val JArray(List(u6)) = l6 \ "use"
    assert(u6 \ "name" === JString("CreateUserError"))

    /////File 2

    ///// Block1

    assert(block2_1 \ "version" === JString("0.3.0"))
    assert(block2_1 \ "index" === JInt(1))
    assert(block2_1 \ "global" === JObject())

    val l2_1 = block2_1 \ "local"

    assert(l2_1 \ "version" === JString("0.3.0"))
    assert(l2_1 \ "type" === JString("get"))
    assert(l2_1 \ "url" === JString("/user/:id"))
    assert(l2_1 \ "title" === JString("Read data of a User"))
    assert(l2_1 \ "name" === JString("GetUser"))
    assert(l2_1 \ "group" === JString("User"))
    assert(l2_1 \ "description" === JString("Compare Verison 0.3.0 with 0.2.0 and you will see the green markers with new items in version 0.3.0 and red markers with removed items since 0.2.0."))

    val JArray(List(per2_1)) = l2_1 \ "permission"
    assert(per2_1 \ "name" === JString("admin"))

    val JArray(List(par2_1)) = l2_1 \ "parameter" \ "fields" \ "Parameter"
    assert(par2_1 \ "group" === JString("Parameter"))
    assert(par2_1 \ "type" === JString("Number"))
    assert(par2_1 \ "optional" === JString("false"))
    assert(par2_1 \ "field" === JString("id"))
    assert(par2_1 \ "description" === JString("The Users-ID."))

    val JArray(List(ex2_1)) = l2_1 \ "examples"
    assert(ex2_1 \ "title" === JString("Example usage:"))
    assert(ex2_1 \ "content" === JString("curl -i http://localhost/user/4711"))
    assert(ex2_1 \ "type" === JString("json"))

    val JArray(List(suc2_1, suc2_2, suc2_3, suc2_4, suc2_5, suc2_6, suc2_7, suc2_8, suc2_9, suc2_10)) = l2_1 \ "success" \ "fields" \ "Success 200"
    assert(suc2_1 \ "group" === JString("Success 200"))
    assert(suc2_1 \ "type" === JString("Number"))
    assert(suc2_1 \ "optional" === JString("false"))
    assert(suc2_1 \ "field" === JString("id"))
    assert(suc2_1 \ "description" === JString("The Users-ID."))

    assert(suc2_2 \ "group" === JString("Success 200"))
    assert(suc2_2 \ "type" === JString("Date"))
    assert(suc2_2 \ "optional" === JString("false"))
    assert(suc2_2 \ "field" === JString("registered"))
    assert(suc2_2 \ "description" === JString("Registration Date."))

    assert(suc2_3 \ "group" === JString("Success 200"))
    assert(suc2_3 \ "type" === JString("Date"))
    assert(suc2_3 \ "optional" === JString("false"))
    assert(suc2_3 \ "field" === JString("name"))
    assert(suc2_3 \ "description" === JString("Fullname of the User."))

    assert(suc2_4 \ "group" === JString("Success 200"))
    assert(suc2_4 \ "type" === JString("String[]"))
    assert(suc2_4 \ "optional" === JString("false"))
    assert(suc2_4 \ "field" === JString("nicknames"))
    assert(suc2_4 \ "description" === JString("List of Users nicknames (Array of Strings)."))

    assert(suc2_5 \ "group" === JString("Success 200"))
    assert(suc2_5 \ "type" === JString("Object"))
    assert(suc2_5 \ "optional" === JString("false"))
    assert(suc2_5 \ "field" === JString("profile"))
    assert(suc2_5 \ "description" === JString("Profile data (example for an Object)"))

    assert(suc2_6 \ "group" === JString("Success 200"))
    assert(suc2_6 \ "type" === JString("Number"))
    assert(suc2_6 \ "optional" === JString("false"))
    assert(suc2_6 \ "field" === JString("profile.age"))
    assert(suc2_6 \ "description" === JString("Users age."))

    assert(suc2_7 \ "group" === JString("Success 200"))
    assert(suc2_7 \ "type" === JString("String"))
    assert(suc2_7 \ "optional" === JString("false"))
    assert(suc2_7 \ "field" === JString("profile.image"))
    assert(suc2_7 \ "description" === JString("Avatar-Image."))

    assert(suc2_8 \ "group" === JString("Success 200"))
    assert(suc2_8 \ "type" === JString("Object[]"))
    assert(suc2_8 \ "optional" === JString("false"))
    assert(suc2_8 \ "field" === JString("options"))
    assert(suc2_8 \ "description" === JString("List of Users options (Array of Objects)."))

    assert(suc2_9 \ "group" === JString("Success 200"))
    assert(suc2_9 \ "type" === JString("String"))
    assert(suc2_9 \ "optional" === JString("false"))
    assert(suc2_9 \ "field" === JString("options.name"))
    assert(suc2_9 \ "description" === JString("Option Name."))

    assert(suc2_10 \ "group" === JString("Success 200"))
    assert(suc2_10 \ "type" === JString("String"))
    assert(suc2_10 \ "optional" === JString("false"))
    assert(suc2_10 \ "field" === JString("options.value"))
    assert(suc2_10 \ "description" === JString("Option Value."))

    val JArray(List(er2_1, er2_2)) = l2_1 \ "error" \ "fields" \ "Error 4xx"
    assert(er2_1 \ "group" === JString("Error 4xx"))
    assert(er2_1 \ "optional" === JString("false"))
    assert(er2_1 \ "field" === JString("NoAccessRight"))
    assert(er2_1 \ "description" === JString("Only authenticated Admins can access the data."))

    assert(er2_2 \ "group" === JString("Error 4xx"))
    assert(er2_2 \ "optional" === JString("false"))
    assert(er2_2 \ "field" === JString("UserNotFound"))
    assert(er2_2 \ "description" === JString("The <code>id</code> of the User was not found."))

    val JArray(List(error_example2)) = l2_1 \ "error" \ "examples"
    assert(error_example2 \ "title" === JString("Response (example):"))
    assert(error_example2 \ "content" === JString("HTTP/1.1 401 Not Authenticated\n{\n  \"error\": \"NoAccessRight\"\n}"))
    assert(error_example2 \ "type" === JString("json"))

    ///// Block2

    assert(block2_2 \ "version" === JString("0.3.0"))
    assert(block2_2 \ "index" === JInt(2))
    assert(block2_2 \ "global" === JObject())

    val l2_2 = block2_2 \ "local"

    assert(l2_2 \ "version" === JString("0.3.0"))
    assert(l2_2 \ "type" === JString("post"))
    assert(l2_2 \ "url" === JString("/user"))
    assert(l2_2 \ "title" === JString("Create a new User"))
    assert(l2_2 \ "name" === JString("PostUser"))
    assert(l2_2 \ "group" === JString("User"))
    assert(l2_2 \ "description" === JString("In this case \"apiErrorStructure\" is defined and used.\nDefine blocks with params that will be used in several functions, so you dont have to rewrite them."))

    val JArray(List(per2_2)) = l2_2 \ "permission"
    assert(per6 \ "name" === JString("none"))

    val JArray(List(par2_2)) = l2_2 \ "parameter" \ "fields" \ "Parameter"
    assert(par2_2 \ "group" === JString("Parameter"))
    assert(par2_2 \ "type" === JString("String"))
    assert(par2_2 \ "optional" === JString("false"))
    assert(par2_2 \ "field" === JString("name"))
    assert(par2_2 \ "description" === JString("Name of the User."))

    val JArray(List(success2)) = l2_2 \ "success" \ "fields" \ "Success 200"
    assert(success2 \ "group" === JString("Success 200"))
    assert(success2 \ "type" === JString("Number"))
    assert(success2 \ "optional" === JString("false"))
    assert(success2 \ "field" === JString("id"))
    assert(success2 \ "description" === JString("The new Users-ID."))

    val JArray(List(use2)) = l2_2 \ "use"
    assert(use2 \ "name" === JString("CreateUserError"))

    ///// Block3

    assert(block2_3 \ "version" === JString("0.3.0"))
    assert(block2_3 \ "index" === JInt(3))
    assert(block2_3 \ "global" === JObject())
    //
    val l2_3 = block2_3 \ "local"

    assert(l2_3 \ "version" === JString("0.3.0"))
    assert(l2_3 \ "type" === JString("put"))
    assert(l2_3 \ "url" === JString("/user/:id"))
    assert(l2_3 \ "title" === JString("Change a User"))
    assert(l2_3 \ "name" === JString("PutUser"))
    assert(l2_3 \ "group" === JString("User"))
    assert(l2_3 \ "description" === JString("This function has same errors like POST /user, but errors not defined again, they were included with \"apiErrorStructure\""))

    val JArray(List(per2_3)) = l2_3 \ "permission"
    assert(per2_3 \ "name" === JString("none"))

    val JArray(List(par2_3)) = l2_3 \ "parameter" \ "fields" \ "Parameter"
    assert(par2_3 \ "group" === JString("Parameter"))
    assert(par2_3 \ "type" === JString("String"))
    assert(par2_3 \ "optional" === JString("false"))
    assert(par2_3 \ "field" === JString("name"))
    assert(par2_3 \ "description" === JString("Name of the User."))

    val JArray(List(use3)) = l2_3 \ "use"
    assert(use3 \ "name" === JString("CreateUserError"))

  }

}
