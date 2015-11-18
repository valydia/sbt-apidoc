#sbt-apidoc

An attempt to port the [apidocjs plugin][apidocjs] to sbt

[apidocjs]: http://apidocjs.com/

[![Build Status](https://api.travis-ci.org/valydia/sbt-apidoc.png)](http://travis-ci.org/valydia/sbt-apidoc)

### Installation

Add the following to your `project/plugins.sbt` or `~/.sbt/0.13/plugins/plugins.sbt` file:

    import sbt._
 
    lazy val root = Project("plugins", file(".")).dependsOn(plugin)
 
    lazy val plugin = uri("https://github.com/valydia/sbt-apidoc.git")
    
It's an auto-plugin so nothing else needs to be added to your `build.sbt`  
    
Only support sbt versions from 0.13.5 until 0.13.7 because [this](https://github.com/json4s/json4s/issues/236) issue

### Using SBT Apidoc

Annotate your comments as described in this [page][apidocjs]

### Running

    >sbt
    
    >apidoc