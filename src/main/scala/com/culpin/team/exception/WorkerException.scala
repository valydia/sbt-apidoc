package com.culpin.team.exception

case class WorkerException(message: String) extends IllegalArgumentException(message) {

  def this(filename: String, block: BigInt, errorMap: Map[String, String]) = this(
    "Filename: " + filename + "\n" +
      "Block: " + block + "\n" +
      errorMap.map { case (k, v) => k + ": " + v }.mkString("\n")
  )

}
