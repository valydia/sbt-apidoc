package com.culpin.team.filter

import org.json4s.JsonAST.JArray

object Filter {


  def apply(parsedFiles: JArray): JArray = JArray(List(parsedFiles \ "local"))

}
