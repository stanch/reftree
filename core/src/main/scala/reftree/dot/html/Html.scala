package reftree.dot.html

import reftree.geometry.Color

/**
 * A simple representation of an HTML label in the dot language
 * @see http://www.graphviz.org/doc/info/shapes.html#html
 */
sealed trait Html

trait Tag extends Html {
  def tagName: String
  def children: Seq[Html]
  def attrs: TagAttrs
}

sealed trait TagAttrs

object TagAttrs {
  case class Empty() extends TagAttrs
}

case class Raw(text: String) extends Html
case class Plain(text: String) extends Html

case class Italic(text: String) extends Tag {
  val tagName = "i"
  val children = Seq(Plain(text))
  val attrs = TagAttrs.Empty()
}

sealed trait Cellular extends Tag

case class Table(
  rows: Seq[Row],
  attrs: Table.Attrs = Table.Attrs()
) extends Cellular {
  val tagName = "table"
  val children = rows
}

object Table {
  case class Attrs(
    cellSpacing: Option[Int] = None,
    cellPadding: Option[Int] = None,
    cellBorder: Option[Int] = None,
    columns: Option[String] = None,
    bgColor: Option[Color] = None,
    style: Option[String] = None
  ) extends TagAttrs
}

sealed trait Row extends Cellular

case class RowContent(cells: Seq[Cell]) extends Row {
  val tagName = "tr"
  val children = cells
  val attrs = TagAttrs.Empty()
}

case object RowDivider extends Row {
  val tagName = "hr"
  val children = Seq.empty[Html]
  val attrs = TagAttrs.Empty()
}

case class Cell(
  content: Html,
  attrs: Cell.Attrs = Cell.Attrs()
) extends Cellular {
  val tagName = "td"
  val children = Seq(content)
}

object Cell {
  case class Attrs(
    port: Option[String] = None,
    rowSpan: Option[Int] = None,
    bgColor: Option[Color] = None
  ) extends TagAttrs
}
