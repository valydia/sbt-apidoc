
lazy val root = (project in file(".")).
  settings(
    version := "0.1",
    scalaVersion := "2.12.1",
    name := "example-full",
    version := "0.3.0",
    apidocName := "apidoc-example",
    apidocSampleURL:= Some("https://api.github.com/v1"),
    apidocTitle := Some("Custom apiDoc browser title"),
    apidocVersion := Some("0.3.0"),
    apidocDescription := "apiDoc example project",
    apidocHeader := Some((Compile / resourceDirectory).value / "header.md"),
    apidocFooter := Some((Compile / resourceDirectory).value / "footer.md"),
    TaskKey[Unit]("checkApiData") := {
      import ujson.Js

      val expectedOutput =
      """[
        |   {
        |     "type": "get",
        |     "url": "/user/:id",
        |     "title": "Read data of a User",
        |     "version": "0.3.0",
        |     "name": "GetUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "admin",
        |         "title": "Admin access rights needed.",
        |         "description": "<p>Optionally you can write here further Informations about the permission.</p> <p>An &quot;apiDefinePermission&quot;-block can have an &quot;apiVersion&quot;, so you can attach the block to a specific version.</p>"
        |       }
        |     ],
        |     "description": "<p>Compare Verison 0.3.0 with 0.2.0 and you will see the green markers with new items in version 0.3.0 and red markers with removed items since 0.2.0.</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "Number",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "examples": [
        |       {
        |         "title": "Example usage:",
        |         "content": "curl -i http://localhost/user/4711",
        |         "type": "json"
        |       }
        |     ],
        |     "success": {
        |       "fields": {
        |         "Success 200": [
        |           {
        |             "group": "Success 200",
        |             "type": "Number",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Date",
        |             "optional": false,
        |             "field": "registered",
        |             "description": "<p>Registration Date.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Date",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Fullname of the User.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "String[]",
        |             "optional": false,
        |             "field": "nicknames",
        |             "description": "<p>List of Users nicknames (Array of Strings).</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Object",
        |             "optional": false,
        |             "field": "profile",
        |             "description": "<p>Profile data (example for an Object)</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Number",
        |             "optional": false,
        |             "field": "profile.age",
        |             "description": "<p>Users age.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "profile.image",
        |             "description": "<p>Avatar-Image.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Object[]",
        |             "optional": false,
        |             "field": "options",
        |             "description": "<p>List of Users options (Array of Objects).</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "options.name",
        |             "description": "<p>Option Name.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "options.value",
        |             "description": "<p>Option Value.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "NoAccessRight",
        |             "description": "<p>Only authenticated Admins can access the data.</p>"
        |           },
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNotFound",
        |             "description": "<p>The <code>id</code> of the User was not found.</p>"
        |           }
        |         ]
        |       },
        |       "examples": [
        |         {
        |           "title": "Response (example):",
        |           "content": "HTTP/1.1 401 Not Authenticated\n{\n  \"error\": \"NoAccessRight\"\n}",
        |           "type": "json"
        |         }
        |       ]
        |     },
        |     "filename": "./src/main/scala/example/Main.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user/:id"
        |       }
        |     ]
        |   },
        |   {
        |     "type": "get",
        |     "url": "/user/:id",
        |     "title": "Read data of a User",
        |     "version": "0.2.0",
        |     "name": "GetUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "admin",
        |         "title": "This title is visible in version 0.1.0 and 0.2.0",
        |         "description": ""
        |       }
        |     ],
        |     "description": "<p>Here you can describe the function. Multilines are possible.</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "String",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "success": {
        |       "fields": {
        |         "Success 200": [
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Date",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Fullname of the User.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNotFound",
        |             "description": "<p>The <code>id</code> of the User was not found.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "filename": "./src/main/scala/example/Apidoc.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user/:id"
        |       }
        |     ]
        |   },
        |   {
        |     "type": "get",
        |     "url": "/user/:id",
        |     "title": "Read data of a User",
        |     "version": "0.1.0",
        |     "name": "GetUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "admin",
        |         "title": "This title is visible in version 0.1.0 and 0.2.0",
        |         "description": ""
        |       }
        |     ],
        |     "description": "<p>Here you can describe the function. Multilines are possible.</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "String",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "success": {
        |       "fields": {
        |         "Success 200": [
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           },
        |           {
        |             "group": "Success 200",
        |             "type": "Date",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Fullname of the User.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNotFound",
        |             "description": "<p>The error description text in version 0.1.0.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "filename": "./src/main/scala/example/Apidoc.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user/:id"
        |       }
        |     ]
        |   },
        |   {
        |     "type": "post",
        |     "url": "/user",
        |     "title": "Create a new User",
        |     "version": "0.3.0",
        |     "name": "PostUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "none"
        |       }
        |     ],
        |     "description": "<p>In this case &quot;apiErrorStructure&quot; is defined and used. Define blocks with params that will be used in several functions, so you dont have to rewrite them.</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "String",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Name of the User.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "success": {
        |       "fields": {
        |         "Success 200": [
        |           {
        |             "group": "Success 200",
        |             "type": "Number",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The new Users-ID.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "filename": "./src/main/scala/example/Main.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user"
        |       }
        |     ],
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "NoAccessRight",
        |             "description": "<p>Only authenticated Admins can access the data.</p>"
        |           },
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNameTooShort",
        |             "description": "<p>Minimum of 5 characters required.</p>"
        |           }
        |         ]
        |       },
        |       "examples": [
        |         {
        |           "title": "Response (example):",
        |           "content": "HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}",
        |           "type": "json"
        |         }
        |       ]
        |     }
        |   },
        |   {
        |     "type": "post",
        |     "url": "/user",
        |     "title": "Create a User",
        |     "version": "0.2.0",
        |     "name": "PostUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "none"
        |       }
        |     ],
        |     "description": "<p>In this case &quot;apiErrorStructure&quot; is defined and used. Define blocks with params that will be used in several functions, so you dont have to rewrite them.</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "String",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Name of the User.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "success": {
        |       "fields": {
        |         "Success 200": [
        |           {
        |             "group": "Success 200",
        |             "type": "String",
        |             "optional": false,
        |             "field": "id",
        |             "description": "<p>The Users-ID.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "filename": "./src/main/scala/example/Apidoc.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user"
        |       }
        |     ],
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "NoAccessRight",
        |             "description": "<p>Only authenticated Admins can access the data.</p>"
        |           },
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNameTooShort",
        |             "description": "<p>Minimum of 5 characters required.</p>"
        |           }
        |         ]
        |       },
        |       "examples": [
        |         {
        |           "title": "Response (example):",
        |           "content": "HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}",
        |           "type": "json"
        |         }
        |       ]
        |     }
        |   },
        |   {
        |     "type": "put",
        |     "url": "/user/:id",
        |     "title": "Change a User",
        |     "version": "0.3.0",
        |     "name": "PutUser",
        |     "group": "User",
        |     "permission": [
        |       {
        |         "name": "none"
        |       }
        |     ],
        |     "description": "<p>This function has same errors like POST /user, but errors not defined again, they were included with &quot;apiErrorStructure&quot;</p>",
        |     "parameter": {
        |       "fields": {
        |         "Parameter": [
        |           {
        |             "group": "Parameter",
        |             "type": "String",
        |             "optional": false,
        |             "field": "name",
        |             "description": "<p>Name of the User.</p>"
        |           }
        |         ]
        |       }
        |     },
        |     "filename": "./src/main/scala/example/Main.scala",
        |     "groupTitle": "User",
        |     "sampleRequest": [
        |       {
        |         "url": "https://api.github.com/v1/user/:id"
        |       }
        |     ],
        |     "error": {
        |       "fields": {
        |         "Error 4xx": [
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "NoAccessRight",
        |             "description": "<p>Only authenticated Admins can access the data.</p>"
        |           },
        |           {
        |             "group": "Error 4xx",
        |             "optional": false,
        |             "field": "UserNameTooShort",
        |             "description": "<p>Minimum of 5 characters required.</p>"
        |           }
        |         ]
        |       },
        |       "examples": [
        |         {
        |           "title": "Response (example):",
        |           "content": "HTTP/1.1 400 Bad Request\n{\n  \"error\": \"UserNameTooShort\"\n}",
        |           "type": "json"
        |         }
        |       ]
        |     }
        |   }
        | ]
        | """.stripMargin
      val apiData = target.value / "apidoc" / "api_data.json"
      val output = IO.read(apiData)
      val json = ujson.read(output)
      val expected = ujson.read(expectedOutput)
      assert(json == expected, s"apidoc should produce the right output, received $output")
    },
    TaskKey[Unit]("checkApiProjectHeader") := {
      val apiProject = target.value / "apidoc" / "api_project.json"
      val output = IO.read(apiProject)
      val content = ujson.read(output)("header")("content").str
      assert(content == "<h2 id=\"welcome-to-apidoc\">Welcome to apiDoc</h2>\n<p>Please visit <a href=\"http://apidocjs.com\">apidocjs.com</a> with the full documentation.</p>")
    },
    TaskKey[Unit]("checkApiProjectFooter") := {
      val apiProject = target.value / "apidoc" / "api_project.json"
      val output = IO.read(apiProject)
      val content = ujson.read(output)("footer")("content").str
      assert(content == "<h2 id=\"epilogue\">Epilogue</h2>\n<p>Suggestions, contact, support and error reporting on <a href=\"https://github.com/apidoc/apidoc/issues\">GitHub</a></p>")
    }
  )

