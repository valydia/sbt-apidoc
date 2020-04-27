# sbt-apidoc

[![Build Status][build-status-logo]][travis]
[![Download][build-version-logo]][bintray]
[![License: MIT][license-logo]][MIT-license]

An attempt to port the [apidocjs plugin][apidocjs] to sbt.

[apidocjs]: https://apidocjs.com/
[build-status-logo]: https://api.travis-ci.org/valydia/sbt-apidoc.png
[travis]: http://travis-ci.org/valydia/sbt-apidoc
[build-version-logo]: https://api.bintray.com/packages/valydia/sbt-plugins/sbt-apidoc/images/download.svg
[bintray]: https://bintray.com/valydia/sbt-plugins/sbt-apidoc/_latestVersion
[license-logo]: https://img.shields.io/badge/License-MIT-blue.svg 
[MIT-license]: https://opensource.org/licenses/MIT

### Usage

This plugin requires sbt 1.0.0+
Add the following to your `project/plugins.sbt` or `~/.sbt/1.0/plugins/plugins.sbt` file:

```sbt
addSbtPlugin("com.culpin.team" % "sbt-apidoc" % "0.5.5")
```

And in your `build.sbt`:

```sbt
apidocVersion := Some("0.1.0")
```
Or whatever you project version is - here is the supported format (major.minor.patch).  
More info on [Semantic Versioning Specification (SemVer)][semver].
    
And some `apidoc` comments in your source code using the **Javadoc-Style** comments:

```
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

And run the command:
>sbt apidoc

The output is generated under `target/scala-2.12/apidoc`. 
And you can open in your browser the file `target/scala-2.12/apidoc/index.html`.

**NOTE**: If you are using another version of scala - `2.10` or `2.13` - the output will be
generated under `target/scala-2.10/apidoc` or `target/scala-2.13/apidoc`respectively.

[semver]: https://semver.org/

### Templates

#### Http4s

You can start a new project with [http4s][], using a [template][http4s-apidoc]
by running the following commands in your terminal:

1. sbt new valydia/http4s-apidoc.g8
2. cd http4s-apidoc
3. sbt run 
4. open http://localhost:8080/apidoc (or just open this url http://localhost:8080/apidoc in your browser)

You can see more about the template [here][http4s-apidoc].

[http4s-apidoc]: https://github.com/valydia/http4s-apidoc.g8
[http4s]: https://http4s.org/

#### Play Framework

You can start a new project with [play][], using a [template][play-apidoc]
by running the following commands in your terminal:

1. sbt new valydia/play-apidoc.g8
2. cd play-apidoc
3. sbt run
4. open http://localhost:9000/apidoc/index.html (or just open this url http://localhost:9000/apidoc/index.html in your browser)

You can see more about the template [here][play-apidoc].

[play-apidoc]: https://github.com/valydia/play-apidoc.g8
[play]: https://www.playframework.com/

## Examples

### Basic

In this basic example we have a small project file and few setting keys in the `build.sbt`.  
[View example output](http://sbt-apidoc.com/example-basic/)

[`build.sbt`](https://github.com/valydia/sbt-apidoc-example/blob/master/build.sbt)
```
  apidocName := "example",
  apidocVersion := Some("0.3.0")
  apidocDescription := "A basic apiDoc example",
```

`apidocName`, `apidocVersion`, `apidocDescription` are set in the `build.sbt`
Those values are derived from the project `name`, `version`, `description` values by default.

[Basic.scala](https://github.com/valydia/sbt-apidoc-example/blob/master/basic/src/main/scala/example/Basic.scala)
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
Format: *method + path* (e.g. Get + User)
`@apiGroup` should always be used, and is used to group related APIs together.  
All other fields are optional, look at their description under [apiDoc-Params](#apidoc-params).  

### Inherit

Using inherit, you can define reusable snippets of your documentation.  
[View example output](http://sbt-apidoc.com/example-inherit/)

[`build.sbt`](https://github.com/valydia/sbt-apidoc-example/blob/master/build.sbt)
```
  apidocName := "example-inherit",
  apidocDescription := "apiDoc inherit example",
  apidocVersion := Some("0.1.0")
```

[Inherit.scala](https://github.com/valydia/sbt-apidoc-example/blob/master/inherit/src/main/scala/example/Inherit.scala)
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
[View example output](http://sbt-apidoc.com/example-versioning/)

In the example, click top right on select box (the main version) and select 
`Compare all with predecessor`.

* The main navigation mark all changed methods with a green bar.
* Each method show the actual difference compare to its predecessor.
* Green marks contents that were added (in this case title text changed and field `registered` was added).
* Red marks contents that were removed.
You can change the main version (top right) to a previous version and compare older methods with their predecessor.

[`build.sbt`](https://github.com/valydia/sbt-apidoc-example/blob/master/build.sbt)
```
  apidocName := "example-versioning",
  apidocDescription := "apiDoc versioning example",
  apidocVersion := Some("0.2.0"),
  apidocVersionFile := (resource in Compile).value / "apidoc"
```

In order to avoid code bloat when API documentation changes over time, it is recommended to use a separate history file or folder named `resources/apidoc` (can be overriden with the setting key `apidocVersionFile`).  
Before you change your documentation block, copy the old documentation to to this file, apiDoc will include the historical information automatically.


[`resources/apidoc`](https://github.com/valydia/sbt-apidoc-example/blob/master/versioning/src/main/resources/apidoc)
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

[`Versioning.scala`](https://github.com/valydia/sbt-apidoc-example/blob/master/versioning/src/main/scala/example/Versioning.scala) (your current project file)
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
    
This is a complex example with `inherit`, `versioning` file and history file `resources/apidoc`, explanation is within code and generated documentation.
[View example output](http://sbt-apidoc.com/example-full/)

Files:
 * [resources/apidoc](https://github.com/valydia/sbt-apidoc-example/blob/master/versioning/src/main/resources/apidoc)
 * [Hello.scala](https://github.com/valydia/sbt-apidoc-example/blob/master/full/src/main/scala/example/Full.scala)
 * [build.sbt](https://github.com/valydia/sbt-apidoc-example/blob/master/build.sbt)

## Configuration

### Setting keys

|                        Key |       Type        | Default                      |                                                    Description                                                                                                               |
|---------------------------:|:-----------------:|:-----------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|          `apidocOutputDir` |       `File`      |        `target/apidoc/`      | Location where to put the generated documentation.                                                                                                                           |
|               `apidocName` |      `String`     |      same as `name` value    | Name of your project, by default uses the name `name` setting key.                                                                                                           |
|              `apidocTitle` |  `Option[String]` |             `None`           | Browser title text.                                                                                                                                                          |
|        `apidocDescription` |      `String`     | same as `description` value  | Introduction of your project. By default, it is set to the `description` setting key.                                                                                        |
|                `apidocURL` |  `Option[String]` |             `None`           | Prefix for api path (endpoints), e.g. `https://api.github.com/v1`                                                                                                            |
|          `apidocSampleURL` |  `Option[String]` |             `None`           | If set, a form to test an api method (send a request) will be visible. See [@apiSampleRequest](#apiSampleRequest) for more details.                                          |
|            `apidocVersion` |  `Option[String]` |   same as `version` value    | Version of your project - supported (major.minor.patch). If not set, uses the same as the `version` setting key if it uses [Semantic Versioning][semver] or 0.0.0.           |
|        `apidocVersionFile` |  `Option[File]`   |      `resources/apidoc`      | File/Folder to keep track of the old api. It is set by default to `resources/apidoc`.                                                                                        |
|        `apidocHeaderTitle` |  `Option[String]` |             `None`           | Navigation text for the included `Header` file.                                                                                                                              |
|         `apidocHeaderFile` |   `Option[File]`  |             `None`           | Filename (markdown-file) for the included `Header` file.                                                                                                                     |
|        `apidocFooterTitle` |  `Option[String]` |             `None`           | Navigation text for the included `Footer` file.                                                                                                                              |                                   
|         `apidocFooterFile` |   `Option[File]`  |             `None`           | Filename (markdown-file) for the included `Footer` file.                                                                                                                     |
|              `apidocOrder` |  `Option[String]` |             `None`           | A list of api-names / group-names for ordering the output. Not defined names are automatically displayed last.                                                               |
|    `apidocTemplateCompare` | `Option[Boolean]` |             `None`           | Enable comparison with older api versions. Default: Enabled.                                                                                                                 |
|  `apidocTemplateGenerator` | `Option[Boolean]` |             `None`           | Output the generator information at the footer. Default: Enabled.                                                                                                            |


## apiDoc-Params

Structure parameter like:

* `@apiDefine`
is used to define a reusable documentation block. This block can be included in normal api documentation blocks. 
Using `@apiDefine` allows you to better organize complex documentation and avoid duplicating recurrent blocks.
A defined block can have all params (like `@apiParam`), **except other defined blocks**.

### @api

```
@api {method} path [title]
```
**Required!**  
Without that indicator, apiDoc parser ignore the documentation block.
The only exception are documentation blocks defined by `@apiDefine`, they don't require `@api`.

Usage: `@api {get} /user/:id Users unique ID.`

|Name            |Description                                              |
|:---------------|:--------------------------------------------------------|
|method          |Request method name: `DELETE`, `GET`, `POST`, `PUT`, ... |
|path            |Request Path.                                            |
|title optional  |A short title. (used for navigation and article header)  |

Example:
```
/**
 * @api {get} /user/:id
 */
```

### @apiDefine

```
@apiDefine name [title]
                [description]
```
Defines a documentation block to be embedded within `@api` blocks or in an api function like `@apiPermission`.
`@apiDefine` can only be used once per block.  
By using `@apiUse` a defined block will be imported, or with the name the title and description will be used.

Usage: `@apiDefine MyError`

|Name                 |Description                                                                                                                  |
|:--------------------|:----------------------------------------------------------------------------------------------------------------------------|
|name                 |Unique name for the block / value. Same name with different `@apiVersion` can be defined.                                    |
|title       optional |A short title. Only used for named functions like `@apiPermission` or `@apiParam (name)`                                     |
|description optional |Detailed description start at the next line, multiple lines can be used. Only used for named functions like `@apiPermission`.|

Examples:
```scala
/**
 * @apiDefine MyError
 * @apiError UserNotFound The <code>id</code> of the User was not found.
 */

/**
 * @api {get} /user/:id
 * @apiUse MyError
 */
```

```scala
/**
 * @apiDefine admin User access only
 * This optional description belong to to the group admin.
 */

/**
 * @api {get} /user/:id
 * @apiPermission admin
 */
```
For more details, see [inherit example](#inherit).

### @apiDeprecated

```
@apiDeprecated [text]
```
Mark an API Method as deprecated.  
Usage: `@apiDeprecated use now (#Group:Name)`

|Name                 |Description        |
|:--------------------|:------------------|
|text                 |Multiline text.    |

Example:
```scala
/**
 * @apiDeprecated
 */

/**
 * @apiDeprecated use now (#Group:Name).
 *
 * Example: to set a link to the GetDetails method of your group User
 * write (#User:GetDetails)
 */
```

### @apiDescription

```
@apiDescription text
```
Detailed description of the API Method.  
Usage: `@apiDescription This is the Description.`

|Name                 |Description                    |
|:--------------------|:------------------------------|
|text                 |Multiline description text..   |

Example:
```scala
/**
 * @apiDescription This is the Description.
 * It is multiline capable.
 *
 * Last line of Description.
 */
```

### @apiError

```
@apiError [(group)] [{type}] field [description]
```

Usage: `@apiError UserNotFound`

|Name                   |Description                                                                                                                                                         |
|:----------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|(group)       optional |All parameters will be grouped by this name.  Without a group, the default `Error 4xx` is set. You can set a title and description with [`@apiDefine`](#apiDefine). |
|{type}        optional | Return type, e.g. `{Boolean}`, `{Number}`, `{String}`, `{Object}`, `{String[]}` (array of strings), ...                                                            |
|field                  | Return Identifier (returned error code).                                                                                                                           |
|description   optional | Description of the field.                                                                                                                                          |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiError UserNotFound The <code>id</code> of the User was not found.
 */
```

### @apiErrorExample

```
@apiErrorExample [{type}] [title]
                 example
```
Example of an error return message, output as a pre-formatted code.

Usage: `@apiErrorExample {json} Error-Response:
                         This is an example.`

|Name                 |Description                            |
|:--------------------|:--------------------------------------|
|type        optional | Response format.                      |
|title       optional | Short title for the example.          |
|example              | Detailed example, multilines capable. |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiErrorExample {json} Error-Response:
 *     HTTP/1.1 404 Not Found
 *     {
 *       "error": "UserNotFound"
 *     }
 */
```
### @apiExample

```
@apiExample [{type}] title
            example
```
Example for usage of an API method. Output as a pre-formatted code.
Use it for a complete example at the beginning of the description of an endpoint.

Usage: `@apiExample {js} Example usage:
                    This is an example.`

|Name              |Description                            |
|:-----------------|:--------------------------------------|
|type      optional|Code language.                         |
|title             | Short title for the example.          |
|example           |Detailed example, multilines capable.  |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiExample {curl} Example usage:
 *     curl -i http://localhost/user/4711
 */
```

### @apiGroup

```
@apiGroup name
```
**Should always be used.**    
Defines to which group the method documentation block belongs. 
Groups will be used for the Main-Navigation in the generated output. 
Structure definition doesn't need `@apiGroup`.

Usage: `@apiGroup User`

|Name        |Description                                         |
|:-----------|:---------------------------------------------------|
|name        | Name of the group. Also used as navigation title.  |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiGroup User
 */
```

### @apiHeader

```
@apiHeader [(group)] [{type}] [field=defaultValue] [description]
```
Describe a parameter passed to you API-Header e.g. for Authorization.  
*Similar operation as [`@apiParam`](#apiparam), only the output is above the parameters.*

Usage: `@apiHeader (MyHeaderGroup) {String} authorization Authorization value`

|Name                   |Description                                                                                                                                                        |
|:----------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|(group)       optional | All parameters will be grouped by this name. Without a group, the default `Parameter` is set.. You can set a title and description with [@apiDefine](#apidefine). |
|{type}        optional | Return type, e.g. {Boolean}, {Number}, {String}, {Object}, {String[]} (array of strings), ...                                                                     |
|field                  | Variable name.                                                                                                                                                    |
|\[field\]              | Fieldname with brackets define the Variable as optional.                                                                                                          |
|=defaultValue optional | The parameters default value.                                                                                                                                     |
|description   optional | Description of the field.                                                                                                                                         |

Example:
```
/**
 * @api {get} /user/:id
 * @apiHeader {String} access-key Users unique access-key.
 */
```

### @apiHeaderExample

```
@apiHeaderExample [{type}] [title]
                   example
```
Parameter request example.

Usage: `@apiHeaderExample {json} Request-Example:
                         { "content": "This is an example content" }`

|Name                 | Description                           |
|:--------------------|:--------------------------------------|
|name        optional | Request format.                       |
|title       optional | Short title for the example.          |
|example              | Detailed example, multilines capable. |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Accept-Encoding": "Accept-Encoding: gzip, deflate"
 *     }
 */
```

### @apiIgnore

```
@apiIgnore [hint]
```
**Place it on top of a block.**
A block with `@apiIgnore` will not be parsed. 
It is useful, if you leave outdated or not finished Methods in your source code and you don't want to publish it into the documentation.

Usage: `@apiIgnore Not finished Method`

|Name                 |Description                                            |
|:--------------------|:------------------------------------------------------|
|hint optional        |Short information why this block should be ignored.    |

Example:
```scala
/**
 * @apiIgnore Not finished Method
 * @api {get} /user/:id
 */
```

### @apiName

```
@apiName name
```
**Should always be used.**  
Defines the name of the method documentation block. 
Names will be used for the Sub-Navigation in the generated output. 
Structure definition doesn't need `@apiName`.

Usage: `@apiName GetUser`

<table>
  <tbody>
    <tr>
      <th>Name</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>text</td>
      <td>
        <p>Unique name of the method. Same name with different <strong>@apiVersion</strong> can be defined.</p>
        <p>Format: <em>method + path</em> (e.g. Get + User), only a proposal, you can name as you want.</p>
        <p>Also used as navigation title.</p>
      </td>
    </tr>
  </tbody>
</table>

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiName GetUser
 */
```

### @apiParam

```
@apiParam [(group)] [{type}] [field=defaultValue] [description]
```

Describe a parameter passed to you API-Method.  
Usage: `@apiParam (MyGroup) {Number} id Users unique ID.`  
For nested parameters, use square bracket notation (`[]`).

<table>
  <tbody>
    <tr>
      <th>Name</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>(group) optional</td>
      <td>
          <p>All parameters will be grouped by this name. Without a group, the default <strong>Parameter</strong> is set.</p>
          <p>You can set a title and description with <strong><a href="#apidefine">@apiDefine</a></strong>.</p>
      </td>
    </tr>
    <tr>
      <td>{type} optional</td>
      <td>Return type, e.g. <strong>{Boolean}, {Number}, {String}, {Object}, {String[]}</strong> (array of strings), ...</td>
    </tr>
    <tr>
      <td>{type{size}} optional</td>
      <td>Information about the size of the variable. <strong>{string{..5}}</strong> a string that has max 5 chars. <strong>{number{100-999}}</strong> a number between 100 and 999.</td>
    </tr>
    <tr>
      <td>{type=allowedValues} optional</td>
      <td>
        <p>Information about allowed values of the variable.</p>
        <p><strong>{string="small"}</strong> a string that can only contain the word "small" (a constant).</p>
        <p><strong>{string="small","huge"}</strong> a string that can contain the words "small" or "huge".</p>
        <p><strong>{number=1,2,3,99}</strong> a number with allowed values of 1, 2, 3 and 99.</p> 
        <p>Can be combined with size: <strong>{string {..5}="small","huge"}</strong> a string that has max 5 chars and only contain the words "small" or "huge".</p>
      </td>
    </tr>
    <tr>
      <td>field</td>
      <td>Fieldname.</td>
    </tr>
    <tr>
      <td>[field]</td>
      <td>Fieldname with brackets define the Variable as optional.</td>
    </tr>
    <tr>
      <td>field[nestedField]</td>
      <td>Mandatory nested field.</td>
    </tr>
    <tr>
      <td>=defaultValue optional</td>
      <td>The parameters default value.</td>
    </tr>
    <tr>
      <td>description optional</td>
      <td>Description of the field.</td>
    </tr>
  </tbody>
</table>

Example:
```
/**
 * @api {get} /user/:id
 * @apiParam {Number} id Users unique ID.
 */

/**
 * @api {post} /user/
 * @apiParam {String} [firstname]       Optional Firstname of the User.
 * @apiParam {String} lastname          Mandatory Lastname.
 * @apiParam {String} country="DE"      Mandatory with default value "DE".
 * @apiParam {Number} [age=18]          Optional Age with default 18.
 *
 * @apiParam (Login) {String} pass      Only logged in users can post this.
 *                                      In generated documentation a separate
 *                                      "Login" Block will be generated.
 *
 * @apiParam {Object} [address]         Optional nested address object.
 * @apiParam {String} [address[street]] Optional street and number.
 * @apiParam {String} [address[zip]]    Optional zip code.
 * @apiParam {String} [address[city]]   Optional city.
 */
```

### @apiParamExample

```
@apiParamExample [{type}] [title]
                   example
```
Parameter request example.

Usage: `@apiParamExample {json} Request-Example:
                         { "content": "This is an example content" }`

|Name                 |Description                            |
|:--------------------|:--------------------------------------|
|type optional        | Request format.                       |
|title optional       | Short title for the example.          |
|example              | Detailed example, multilines capable. |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiParamExample {json} Request-Example:
 *     {
 *       "id": 4711
 *     }
 */
```
### @apiPermission

```
@apiPermission name
```

Outputs the permission name. 
If the name is defined with `@apiDefine` the generated documentation include the additional title and description.

Usage: `@apiPermission admin`

|Name         |Description                     |
|:------------|:-------------------------------|
|name         |Unique name of the permission.  |

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiPermission none
 */
```

### @apiSampleRequest

```
@apiSampleRequest url
```

Use this parameter in conjunction with the `settingsKey` in the `build.sbt` [apidocSampleURL](#setting-keys).
If `apidocSampleURL` is set, all methods will have the api test form (the endpoint from [@api](#api) will be appended).
Without `apidocSampleURL` only methods with `@apiSampleRequest` will have a form.
if `@apiSampleRequest url` is set in a method block, this url will be used for the request (it overrides apidocSampleURL when it starts with http).
If `apidocSampleURL` is set and you don't want a method with a test form, then add `@apiSampleRequest off` to the documentation block.

Usage: `@apiSampleRequest http://test.github.com`

<table>
  <tbody>
    <tr>
      <th>Name</th>
      <th>Description</th>
    </tr>
    <tr>
      <td>url</td>
      <td>
        <p>Url to your test api server.</p>
        <p>Overwrite the configuration parameter apidocSampleURL and append <a href="#api">@api</a> url:
          <strong>@apiSampleRequest http://www.example.com</strong></p>
        <p>Prefix the <a href="#api">@api</a> url:
          <strong>@apiSampleRequest /my_test_path</strong></p>
        <p>Disable api test if configuration parameter apidocSampleURL is set:
          <strong>@apiSampleRequest off</strong></p>
      </td>
    </tr>
  </tbody>
</table>

Examples:  
This will send the api request to **http://api.github.com/user/:id**  
```
Configuration parameter apidocSampleURL := Some("http://api.github.com")
/**
 * @api {get} /user/:id
 */
```
This will send the api request to **http://test.github.com/some_path/user/:id**  
It overwrites apidocSampleURL.
```
Configuration parameter apidocSampleURL := Some("http://api.github.com")
/**
 * @api {get} /user/:id
 * @apiSampleRequest http://test.github.com/some_path/
 */
```
This will send the api request to **http://api.github.com/test/user/:id**  
It extends apidocSampleURL.
```
Configuration parameter apidocSampleURL := Some("http://api.github.com")
/**
 * @api {get} /user/:id
 * @apiSampleRequest /test
 */
```
This will disable the api request for this api-method.
```
Configuration parameter apidocSampleURL := Some("http://api.github.com")
/**
 * @api {get} /user/:id
 * @apiSampleRequest off
 */
```
This will send the api request to **http://api.github.com/some_path/user/:id**  
It activates the request for this method only, because sampleUrl is not set.
```
Configuration parameter apidocSampleURL is not set, apidocSampleURL := None
/**
 * @api {get} /user/:id
 * @apiSampleRequest http://api.github.com/some_path/
 */
```

### @apiSuccess

```
@apiSuccess [(group)] [{type}] field [description]
```
Success return Parameter.  
Usage: `@apiSuccess {String} firstname Firstname of the User`.

|Name                   |Description                                                                                                                                                           |
|:----------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|(group)       optional | All parameters will be grouped by this name. Without a group, the default `Success 200` is set. You can set a title and description with [`@apiDefine`](#apidefine). |
|{type}        optional | Return type, e.g. `{Boolean}`, `{Number}`, `{String}`, `{Object}`, `{String[]}` (array of strings), ...                                                              |
|field                  | Return Identifier (returned success code).                                                                                                                           |
|description   optional | Description of the field.                                                                                                                                            |

Example:
```
/**
 * @api {get} /user/:id
 * @apiSuccess {String} firstname Firstname of the User.
 * @apiSuccess {String} lastname  Lastname of the User.
 */
```

Example with `(group)`, more group-examples at [@apiSuccessTitle](#apisuccesstitle):
```
/**
 * @api {get} /user/:id
 * @apiSuccess (200) {String} firstname Firstname of the User.
 * @apiSuccess (200) {String} lastname  Lastname of the User.
 */
```
Example with Object:
```
/**
 * @api {get} /user/:id
 * @apiSuccess {Boolean} active        Specify if the account is active.
 * @apiSuccess {Object}  profile       User profile information.
 * @apiSuccess {Number}  profile.age   Users age.
 * @apiSuccess {String}  profile.image Avatar-Image.
 */
```

Example with Array:
```
/**
 * @api {get} /users
 * @apiSuccess {Object[]} profiles       List of user profiles.
 * @apiSuccess {Number}   profiles.age   Users age.
 * @apiSuccess {String}   profiles.image Avatar-Image.
 */
```

### @apiSuccessExample

```
@apiSuccessExample [{type}] [title]
                   example
```
Example of a success return message, output as a pre-formatted code.

Usage: `@apiSuccessExample {json} Success-Response:
                           { "content": "This is an example content" }`

| Name             | Description                          |
|:-----------------|:-------------------------------------|
|type     optional | Response format.                     |
|title    optional | Short title for the example.         |
|example           | Detailed example, multilines capable.|

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiSuccessExample {json} Success-Response:
 *     HTTP/1.1 200 OK
 *     {
 *       "firstname": "John",
 *       "lastname": "Doe"
 *     }
 */
```
### @apiUse

```
@apiUse name
```
Include a with `@apiDefine` defined block. 
If used with `@apiVersion` the same or nearest predecessor will be included.

Usage: `@apiUse MySuccess`

|Name    |Description                   |
|:-------|:-----------------------------|
| name   | Name of the defined block.   |

Example:
```scala
/**
 * @apiDefine MySuccess
 * @apiSuccess {string} firstname The users firstname.
 * @apiSuccess {number} age The users age.
 */

/**
 * @api {get} /user/:id
 * @apiUse MySuccess
 */
```

### @apiVersion

```
@apiVersion version
```
Set the version of an documentation block. Version can also be used in `@apiDefine`.
Blocks with same group and name, but different versions can be compared in the generated output, 
so you or a frontend developer can retrace what changes in the API since the last version.

Usage: `@apiVersion 1.6.2`

|Name          |Description                                                                                                         |
|:-------------|:-------------------------------------------------------------------------------------------------------------------|
|version       | Simple versioning supported (major.minor.patch). More info on [Semantic Versioning Specification (SemVer)][semver].|

Example:
```scala
/**
 * @api {get} /user/:id
 * @apiVersion 1.6.2
 */
```
For more watch [versioning example](#versioning).

## License 

This code is open source software licensed under the [MIT License][MIT-license].