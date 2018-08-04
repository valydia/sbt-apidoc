# sbt-apidoc

An attempt to port the [apidocjs plugin][apidocjs] to sbt.

[![Build Status](https://api.travis-ci.org/valydia/sbt-apidoc.png)](http://travis-ci.org/valydia/sbt-apidoc)

### Installation

This plugin requires sbt 1.0.0+
Add the following to your `project/plugins.sbt` or `~/.sbt/0.13/plugins/plugins.sbt` file:

    addSbtPlugin("com.culpin.team" % "sbt-apidoc" % "0.5.1")
    
You can custom the different apidoc keys into the `build.sbt`:

```
  apidocName := """apidoc-example""",
  apidocTitle := """"Custom apiDoc browser title"""",
  apidocDescription := "apidoc example project",
  apidocURL := Some(url("https://api.github.com/v1")),
  apidocSampleURL := Some(url("https://api.github.com/v1")),
  apidocVersion := Some("0.3.0")
```


### Using SBT Apidoc

Annotate your comments as described in this [page][apidocjs].

### Running

    >sbt
    
    >apidoc
    
The output is generated under target/apidoc. 

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).


[apidocjs]: http://apidocjs.com/