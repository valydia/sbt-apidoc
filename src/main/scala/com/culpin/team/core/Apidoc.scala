package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty
import sbt.Logger

object Apidoc {

  def apply(sources: Seq[File], config: SbtApidocConfiguration, log: Logger): (String, String) = {
    sources.foreach(f => log.info(f.getAbsolutePath))
    log.info(config.name)

    implicit val formats = Serialization.formats(NoTypeHints)
    ("", writePretty(config))
  }

}
