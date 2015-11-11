#sbt-apidoc

An attempt to port the [apidocjs plugin][apidocjs] to sbt

[apidocjs]: http://apidocjs.com/

[![Build Status](https://api.travis-ci.org/valydia/sbt-apidoc.png)](http://travis-ci.org/valydia/sbt-apidoc)

### Installation

Add the following to your `project/plugins.sbt` or `~/.sbt/0.13/plugins/plugins.sbt` file:

    import sbt._
 
    lazy val root = Project("plugins", file(".")).dependsOn(plugin)
 
    lazy val plugin = uri("https://github.com/valydia/sbt-apidoc.git")
    
Add at the top of the `build.sbt` file:

    val root = (project in file(".")).enablePlugins(SbtApidoc)

### Using SBT Apidoc

Annotate your comments as described in this [page][apidocjs]

### Running

    >sbt
    
    >apidoc