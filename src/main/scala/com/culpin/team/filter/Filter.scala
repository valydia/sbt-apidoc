package com.culpin.team.filter

import com.culpin.team.core.Block

object Filter {

  def apply(parsedFiles: Seq[Seq[Block]]): Seq[Block] = if (parsedFiles.isEmpty) Seq() else parsedFiles.head

}
