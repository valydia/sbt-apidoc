# sbt-apidoc

An attempt to port the [apidocjs plugin][apidocjs] to sbt.

[![Build Status](https://api.travis-ci.org/valydia/sbt-apidoc.png)](http://travis-ci.org/valydia/sbt-apidoc)

### Installation

This plugin requires sbt 1.0.0+
Add the following to your `project/plugins.sbt` or `~/.sbt/1.0/plugins/plugins.sbt` file:

    addSbtPlugin("com.culpin.team" % "sbt-apidoc" % "0.5.1")
    
You can custom the different apidoc keys into the `build.sbt`:

```
  apidocName := """apidoc-example""",
  apidocTitle := """Custom apiDoc browser title""",
  apidocDescription := "apidoc example project",
  apidocURL := Some(url("https://api.github.com/v1")),
  apidocSampleURL := Some("https://api.github.com/v1"),
  apidocVersion := Some("0.3.0")
```

And the task:
>apidoc

The output is generated under `target/apidoc`. 
And you can open in your browser the `target/apidoc/index.html`


## Examples

### Basic

In this basic example we have a small project file and an apidoc.json.

`build.sbt`
```
  apidocName := "example",
  apidocDescription := "A basic apiDoc example",
  apidocVersion := Some("0.3.0")
```

From apidoc.json apiDoc get the name, version and description of your project.
The file is optional (it depend on your template if the data is required).

```scala
/**
 * @api {get} /user/:id Request User information
 * @apiName GetUser
 * @apiGroup User
 *
 * @apiParam {Number} id Users unique ID.
 *
 * @apiSuccess {String} firstname Firstname of the User.
 * @apiSuccess {String} lastname  Lastname of the User.
 *
 * @apiSuccessExample Success-Response:
 *     HTTP/1.1 200 OK
 *     {
 *       "firstname": "John",
 *       "lastname": "Doe"
 *     }
 *
 * @apiError UserNotFound The id of the User was not found.
 *
 * @apiErrorExample Error-Response:
 *     HTTP/1.1 404 Not Found
 *     {
 *       "error": "UserNotFound"
 *     }
 */
```

A documentation block starts with `/**` and end with `*/`.
This example describes a `GET` Method to request the User Information by the user's `id`.
`@api {get} /user/:id Request User information` is mandatory, without `@api` apiDoc ignores a documentation block.
`@apiName` must be a unique name and should always be used.
Format: method + path (e.g. Get + User)
`@apiGroup` should always be used, and is used to group related APIs together.
All other fields are optional, look at their description under apiDoc-Params.

### Inherit

Using inherit, you can define reusable snippets of your documentation.

`build.sbt`
```
  apidocName := "example-inherit",
  apidocDescription := "apiDoc inherit example",
  apidocVersion := Some("0.1.0")
```

```scala
/**
 * @apiDefine UserNotFoundError
 *
 * @apiError UserNotFound The id of the User was not found.
 *
 * @apiErrorExample Error-Response:
 *     HTTP/1.1 404 Not Found
 *     {
 *       "error": "UserNotFound"
 *     }
 */

/**
 * @api {get} /user/:id Request User information
 * @apiName GetUser
 * @apiGroup User
 *
 * @apiParam {Number} id Users unique ID.
 *
 * @apiSuccess {String} firstname Firstname of the User.
 * @apiSuccess {String} lastname  Lastname of the User.
 *
 * @apiSuccessExample Success-Response:
 *     HTTP/1.1 200 OK
 *     {
 *       "firstname": "John",
 *       "lastname": "Doe"
 *     }
 *
 * @apiUse UserNotFoundError
 */

/**
 * @api {put} /user/ Modify User information
 * @apiName PutUser
 * @apiGroup User
 *
 * @apiParam {Number} id          Users unique ID.
 * @apiParam {String} [firstname] Firstname of the User.
 * @apiParam {String} [lastname]  Lastname of the User.
 *
 * @apiSuccessExample Success-Response:
 *     HTTP/1.1 200 OK
 *
 * @apiUse UserNotFoundError
 */
```

In this example, a block named `UserNotFoundError` is defined with `@apiDefine`.
That block could be used many times with `@apiUse UserNotFoundError`.
In the generated output, both methods `GET` and `PUT` will have the complete `UserNotFoundError` documentation.

To define an inherit block, use `apiDefine`.
to reference a block, use `apiUse`. `apiGroup` and `apiPermission` are use commands to, but in their context the not inherit parameters, 
only title and description (in combination with apiVersion).

Inheritance only works with 1 parent, more levels would make the inline code unreadable and changes really complex.

### Versioning

A useful feature provided by apiDoc is the ability to maintain the documentation for all previous versions and the latest version of the API.  
This makes it possible to compare a methods version with its predecessor. 
Frontend Developer can thus simply see what have changed and update their code accordingly.

In the example, click top right on select box (the main version) and select 
`Compare all with predecessor`.

* The main navigation mark all changed methods with a green bar.
* Each method show the actual difference compare to its predecessor.
* Green marks contents that were added (in this case title text changed and field `registered` was added).
* Red marks contents that were removed.
You can change the main version (top right) to a previous version and compare older methods with their predecessor.

`build.sbt`
```
  apidocName := "example-versioning",
  apidocDescription := "apiDoc versioning example",
  apidocVersion := Some("0.2.0")
```

`_apidoc.js`
```scala
/**
 * @api {get} /user/:id Get User information
 * @apiVersion 0.1.0
 * @apiName GetUser
 * @apiGroup User
 *
 * @apiParam {Number} id Users unique ID.
 *
 * @apiSuccess {String} firstname Firstname of the User.
 * @apiSuccess {String} lastname  Lastname of the User.
 *
 * @apiSuccessExample Success-Response:
 *     HTTP/1.1 200 OK
 *     {
 *       "firstname": "John",
 *       "lastname": "Doe"
 *     }
 *
 * @apiError UserNotFound The id of the User was not found.
 *
 * @apiErrorExample Error-Response:
 *     HTTP/1.1 404 Not Found
 *     {
 *       "error": "UserNotFound"
 *     }
 */
```

`Hello.scala`
```scala
/**
 * @api {get} /user/:id Get User information and Date of Registration.
 * @apiVersion 0.2.0
 * @apiName GetUser
 * @apiGroup User
 *
 * @apiParam {Number} id Users unique ID.
 *
 * @apiSuccess {String} firstname  Firstname of the User.
 * @apiSuccess {String} lastname   Lastname of the User.
 * @apiSuccess {Date}   registered Date of Registration.
 *
 * @apiSuccessExample Success-Response:
 *     HTTP/1.1 200 OK
 *     {
 *       "firstname": "John",
 *       "lastname": "Doe"
 *     }
 *
 * @apiError UserNotFound The id of the User was not found.
 *
 * @apiErrorExample Error-Response:
 *     HTTP/1.1 404 Not Found
 *     {
 *       "error": "UserNotFound"
 *     }
 */
```
Important is to set the version with `@apiVersion` on every documentation block.
The version can be used on every block, also on inherit blocks. 
You don't have to change the version on an inherit block, the parser check automatically for the nearest predecessor.

### Full example
    
This is a complex example with inherit, versioning file and history file _apidoc.js, explanation is within code and generated documentation.
Files:
 * _apidoc.js
 * example.js
 * apidoc.json

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).
