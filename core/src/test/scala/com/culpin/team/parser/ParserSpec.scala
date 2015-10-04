package com.culpin.team.parser

import java.io.File

import com.culpin.team.core._
import org.scalatest.{ Matchers, FlatSpec }

import org.json4s.JsonAST.{ JArray, JObject, JNothing, JString }


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

    val parameter = result \ "local" \ "sampleRequest"

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

    val sources = Seq(new File(getClass.getResource("/Application.scala").getFile))
    val blocks = Parser(sources)
    val block = blocks(0)(0)
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

    val sources = Seq(new File(getClass.getResource("/_apidoc.js").getFile),
                      new File(getClass.getResource("/full-example.js").getFile))
    val blocks = Parser(sources)

    import org.json4s.native.JsonMethods._
    //TODO test the result
    //println(compact(render(blocks)))
  }

}
