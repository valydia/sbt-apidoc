val root = (project in file(".")).enablePlugins(SbtApidoc)

import SbtApidoc._

name := """Test plugin"""

apidocName := """Super API"""

description := """Project to test up the sbt plugin"""

apidocDescription := """Super API is an api that is Awesome"""

version := """1.0"""

apidocVersion := Some("2.0")
