# sbt-apidoc

An attempt to port the [apidocjs plugin][apidocjs] to sbt.

[![Build Status](https://api.travis-ci.org/valydia/sbt-apidoc.png)](http://travis-ci.org/valydia/sbt-apidoc)

### Installation

This plugin requires sbt 1.0.0+
Add the following to your `project/plugins.sbt` or `~/.sbt/0.13/plugins/plugins.sbt` file:

    addSbtPlugin("com.culpin.team" % "sbt-apidoc" % "0.5.1")
    
It's an auto-plugin so nothing else needs to be added to your `build.sbt`


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