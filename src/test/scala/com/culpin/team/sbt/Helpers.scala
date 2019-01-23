package com.culpin.team.sbt

import sbt.util.{ Level, Logger }

trait LoggerHelper {
  val stubLogger: Logger = new Logger {
    override def log(level: Level.Value, message: => String): Unit = ()

    override def trace(t: => Throwable): Unit = ()

    override def success(message: => String): Unit = ()
  }
}
