package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization.writePretty
import sbt.Logger

import scala.util.{ Success, Try }

object Apidoc {

  /**
   * @param sources the sources file to process
   * @param config the apidoc configuration
   * @param log the sbt logger
   * @return A Some(Pair) of JSon string if there are some apidoc comment, None if not
   */
  def apply(sources: Seq[File], config: SbtApidocConfiguration, log: Logger): Try[Option[(String, String)]] = {
    sources.foreach(f => log.info(f.getAbsolutePath))
    log.info(config.name)

    implicit val formats = Serialization.formats(NoTypeHints)
    Success(Some(("", writePretty(config))))
  }

}
