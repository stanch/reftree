package reftree.graph

import com.softwaremill.quicklens._
import monocle.Lens
import reftree.geometry.Color
import reftree.util.Optics
import zipper.Unzip

object Merging {
  /** Get a unique statement identifier, if any */
  private def statementId(statement: GraphStatement) = statement match {
    case n: Node ⇒
      Some(n.id)
    case e: Edge ⇒
      Some(e.attrs.find(_.name == "id").get.value)
    case _ ⇒ None
  }

  /** Enable navigating through XML with a zipper */
  implicit val unzipXml: Unzip[xml.Node] =
    Optics.unzip(Optics.xmlImmediateChildren)

  /** A mapping between the node label and the background colors specified inside it */
  val nodeColorLens: Lens[Node, List[Color]] = {
    val color: Lens[xml.Node, Color] = Optics.xmlAttr("bgcolor") composeIso
      Color.rgbaStringIso

    val elementsWithColor: Lens[xml.Node, List[xml.Node]] = Optics.collectLeftByIndex(
      Optics.only(Optics.xmlOptAttr("bgcolor").exist(_.nonEmpty))
    )

    val colors: Lens[xml.Node, List[Color]] =
      elementsWithColor composeLens Optics.sequenceLens(color)

    val nodeLabel: Lens[Node, xml.Node] =
      Lens[Node, xml.Node](
        _.attrs.find(_.name == "label").get.value.asInstanceOf[Identifier.Html].value
      ) { html ⇒ node ⇒
        node.modify(_.attrs.eachWhere(_.name == "label").value.when[Identifier.Html].value).setTo(html)
      }

    nodeLabel composeLens colors
  }

  /** Merge node statements by mixing highlight colors inside them */
  private def mergeNodeStatements(statements: Seq[Node], keepLeft: Boolean, mixColor: Boolean) = {
    val keeper = if (keepLeft) statements.head else statements.last
    val colors = statements.map(nodeColorLens.get)
    if (colors.exists(_.isEmpty)) keeper else {
      val mixedColors = colors.transpose.map { cs ⇒
        // we don’t want to mix the default transparent background in...
        val ignoreDefault = cs.map(_.toRgba).filter(_ != Primitives.defaultBackground)
        // ...but if that’s the only color there is, leave it be
        if (ignoreDefault.isEmpty) Primitives.defaultBackground
        // a single color does not need mixing
        else if (ignoreDefault.length == 1) ignoreDefault.head
        // if mixing is enabled, mix things up
        else if (mixColor) Color.mix(ignoreDefault)
        // otherwise pick the winning color
        else if (keepLeft) ignoreDefault.head else ignoreDefault.last
      }
      nodeColorLens.set(mixedColors.toList)(keeper)
    }
  }

  /** Merge statements with the same ids to eliminate duplicates */
  private def merge(statements: Seq[GraphStatement], keepLeft: Boolean, mixColor: Boolean): Seq[GraphStatement] = {
    val groupedById = statements.zipWithIndex.groupBy { case (s, i) ⇒ statementId(s) }.values.toSeq
    val merged = groupedById.flatMap {
      case single @ Seq(_) ⇒ single
      case nodes @ Seq((n: Node, _), _*) ⇒
        val index = if (keepLeft) nodes.head._2 else nodes.last._2
        val merged = mergeNodeStatements(nodes.map(_._1.asInstanceOf[Node]), keepLeft, mixColor)
        Seq((merged, index))
      case edges @ Seq((n: Edge, _), _*) ⇒
        if (!keepLeft) edges.takeRight(1) else edges.take(1)
      case other ⇒ other
    }
    merged.sortBy(_._2).map(_._1)
  }

  /** Remove edges pointing to elided objects which are not explicitly included */
  private def removeDanglingEdges(statements: Seq[GraphStatement]): Seq[GraphStatement] = {
    val ids = statements.flatMap(statementId).toSet
    statements filter {
      case Edge(_, NodeId(id, _, _), _*) if !ids(id) ⇒ false
      case _ ⇒ true
    }
  }

  /** Merge statements belonging to the same layer */
  def mergeLayer(statements: Seq[GraphStatement]): Seq[GraphStatement] =
    merge(removeDanglingEdges(statements), keepLeft = true, mixColor = true)

  /** Merge statements belonging to different onion skin layers */
  def mergeLayers(statements: Seq[Seq[GraphStatement]]): Seq[GraphStatement] =
    merge(removeDanglingEdges(statements.flatMap(mergeLayer)), keepLeft = false, mixColor = false)
}
