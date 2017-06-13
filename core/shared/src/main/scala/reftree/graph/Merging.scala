package reftree.graph

import monocle.Optional
import reftree.geometry.Color
import reftree.util.Optics
import reftree.dot._
import reftree.dot.html._
import zipper.Unzip
import com.softwaremill.quicklens._

object Merging {
  /** Get a unique statement identifier, if any */
  private def statementId(statement: GraphStatement) = statement match {
    case n: Node ⇒ Some(n.id)
    case e: Edge ⇒ Some(e.id)
    case _ ⇒ None
  }

  implicit val unzipCellular: Unzip[Cellular] =
    new Unzip[Cellular] {
      def unzip(node: Cellular): List[Cellular] = node match {
        case table: Table ⇒ table.rows.toList
        case row: RowContent ⇒ row.cells.toList
        case _ ⇒ List.empty
      }

      def zip(node: Cellular, children: List[Cellular]): Cellular = node match {
        case table: Table ⇒ table.copy(rows = children collect { case r: Row ⇒ r })
        case row: RowContent ⇒ row.copy(cells = children collect { case c: Cell ⇒ c })
        case _ ⇒ node
      }
    }

  /** A mapping between the node and the background colors specified inside its label */
  private val nodeLabelColors: Optional[Node, List[Color]] = {
    val nodeLabelCellular = Optional[Node, Cellular] {
      case Node(_, table: Table, _) ⇒ Some(table)
      case _ ⇒ None
    } {
      case table: Table ⇒ _.copy(label = table)
      case _ ⇒ identity
    }

    val cellularColors = Optional[Cellular, Color] {
      case table: Table ⇒ table.attrs.bgColor
      case cell: Cell ⇒ cell.attrs.bgColor
      case _ ⇒ None
    } { color ⇒ {
      case table: Table ⇒ table.modify(_.attrs.bgColor).setTo(Some(color))
      case cell: Cell ⇒ cell.modify(_.attrs.bgColor).setTo(Some(color))
      case other ⇒ other
    }}

    nodeLabelCellular composeLens Optics.collectLeftByIndex(cellularColors)
  }

  /** Merge node statements by mixing highlight colors inside them */
  private def mergeNodeStatements(statements: Seq[Node], keepLeft: Boolean, mixColor: Boolean) = {
    val keeper = if (keepLeft) statements.head else statements.last
    val colors = statements.map(nodeLabelColors.getOption(_).getOrElse(List.empty))
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
      nodeLabelColors.set(mixedColors.toList)(keeper)
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
      case Edge(_, NodeId(id, _, _), _, _) if !ids(id) ⇒ false
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
