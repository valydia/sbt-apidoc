package com.culpin.team.sbt

import java.util.Collections

import com.gilt.gfc.semver.SemVer
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import ujson.Js
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet

object Util {

  private[sbt] def merge(val1: Js.Arr, val2: Js.Arr): Js.Arr =
    mergeVals(val1.arr.toList, val2.arr.toList)

  private[sbt] def merge(val1: Js.Value, val2: Js.Value): Js.Value =
    (val1, val2) match {
      case (Js.Obj(xs), Js.Obj(ys)) =>
        Js.Obj.from(mergeFields(xs.toList, ys.toList))
      case (Js.Arr(xs), Js.Arr(ys)) => mergeVals(xs.toList, ys.toList)
      case (Js.Null, x)             => x
      case (x, Js.Null)             => x
      case (x, _)                   => x
    }

  private def mergeFields(
      vs1: List[(String, Js.Value)],
      vs2: List[(String, Js.Value)]): List[(String, Js.Value)] = {
    @scala.annotation.tailrec
    def mergeRec(acc: List[(String, Js.Value)],
                 xleft: List[(String, Js.Value)],
                 yleft: List[(String, Js.Value)]): List[(String, Js.Value)] =
      xleft match {
        case Nil => acc ++ yleft
        case (xn, xv) :: xs =>
          yleft find (_._1 == xn) match {
            case Some(y @ (_, yv)) =>
              mergeRec(acc ++ List((xn, merge(xv, yv))),
                       xs,
                       yleft filterNot (_ == y))
            case None => mergeRec(acc ++ List((xn, xv)), xs, yleft)
          }
      }

    mergeRec(List(), vs1, vs2)
  }

  private def mergeVals(vs1: List[Js.Value],
                        vs2: List[Js.Value]): List[Js.Value] = {
    def mergeRec(xleft: List[Js.Value], yleft: List[Js.Value]): List[Js.Value] =
      xleft match {
        case Nil => yleft
        case x :: xs =>
          yleft find (_ == x) match {
            case Some(y) =>
              merge(x, y) :: mergeRec(xs, yleft filterNot (_ == y))
            case None => x :: mergeRec(xs, yleft)
          }
      }

    mergeRec(vs1, vs2)
  }

  // sort by group ASC, name ASC, version DESC
  private[sbt] def sortBlocks(blocks: Js.Arr): Js.Arr = {
    val sortedChildren = blocks.arr.sortWith {
      case (a, b) =>
        val Js.Str(groupA) = a("group")
        val Js.Str(nameA) = a("name")

        val Js.Str(groupB) = b("group")
        val Js.Str(nameB) = b("name")

        val labelA = groupA + nameA
        val labelB = groupB + nameB

        if (labelA.equals(labelB)) {
          val Js.Str(versionA) = a("version")
          val Js.Str(versionB) = b("version")
          SemVer(versionA) > SemVer(versionB)
        } else
          labelA <= labelB

    }
    Js.Arr(sortedChildren)
  }

  private[sbt] def buildObj(elems: (String, Option[Js.Value])*) : Js.Obj = {
    val tupleList =
      elems.foldLeft(List.empty[(String, Js.Value)]) { case (acc, elem) =>
        val newVal =
          elem match {
            case (_, None) => Nil
            case (key, Some(value)) => List(key -> value)
          }
        acc ++ newVal
      }
    Js.Obj.from(tupleList)
  }


  private[sbt] def renderMarkDown(value: String): String = {

    val options = new MutableDataSet().set(
      Parser.EXTENSIONS,
      Collections.singleton(TypographicExtension.create())
    )

    options.set[Integer](HtmlRenderer.MAX_TRAILING_BLANK_LINES, -1)
    options.set(HtmlRenderer.SOFT_BREAK, " ")
    options.set(TypographicExtension.DOUBLE_QUOTE_OPEN, "&quot;")
    options.set(TypographicExtension.DOUBLE_QUOTE_CLOSE, "&quot;")
    options.set(HtmlRenderer.RENDER_HEADER_ID, new java.lang.Boolean(true))

    val parser = Parser.builder(options).build
    val renderer = HtmlRenderer.builder(options).build

    val document = parser.parse(value)
    // TODO a a bit hacky, the might be some api in the library that could do the job
    renderer.render(document).replaceAll("</p>\n<p>", "</p> <p>")
  }

  private[sbt] def renderMarkDownNoPTags(value: String): String =
    renderMarkDown(value).replaceAll("<p>|</p>", "")


}
