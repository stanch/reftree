package reftree

import scala.util.Try
import scala.xml.{UnprefixedAttribute, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

object SvgMagic {
  private case class Point(x: Double, y: Double) {
    def +(delta: Point) = Point(x + delta.x, y + delta.y)
    def -(delta: Point) = Point(x - delta.x, y - delta.y)

    def topLeftMost(other: Point) = Point(x min other.x, y min other.y)
    def bottomRightMost(other: Point) = Point(x max other.x, y max other.y)

    override def toString = s"$x $y"
  }

  private object Point {
    def sum(points: Seq[Point]) = points.foldLeft(Point(0, 0))(_ + _)

    def fromString(string: String, sep: String) = {
      val Array(x, y) = string.split(sep).map(_.toDouble)
      Point(x, y)
    }

    def sequenceFromString(string: String, sep1: String, sep2: String) =
      string.split(sep1).flatMap(_.split(sep2)).map(_.toDouble).grouped(2).toSeq map {
        case Array(x, y) ⇒ Point(x, y)
      }
  }

  private case class Rectangle(topLeft: Point, bottomRight: Point) {
    def width = bottomRight.x - topLeft.x
    def height = bottomRight.y - topLeft.y

    def +(delta: Point) = Rectangle(topLeft + delta, bottomRight + delta)

    def union(other: Rectangle) = Rectangle(
      topLeft topLeftMost other.topLeft,
      bottomRight bottomRightMost other.bottomRight
    )

    override def toString = s"$topLeft $width $height"
  }

  private object Rectangle {
    def union(rectangles: Seq[Rectangle]) = rectangles.reduce(_ union _)

    def fromString(string: String, sep1: String, sep2: String) = {
      val points = Point.sequenceFromString(string, sep1, sep2)
      Rectangle(points.head, points.head + points.last)
    }
  }

  private case class SvgData(svg: xml.Node, viewBox: Rectangle, translation: Point) {
    def translate(delta: Point) = copy(svg, viewBox + delta, translation + delta)

    def anchorPosition(anchorId: String) = {
      val anchorNode = (svg \\ "g").find(g ⇒ (g \ "@id").text == anchorId).get
      val anchor = Point.sequenceFromString(((anchorNode \\ "polygon").head \ "@points").text, " ", ",").head
      anchor + translation
    }

    def render = new RuleTransformer(new RewriteRule {
      override def transform(n: xml.Node): Seq[xml.Node] = n match {
        case e @ Elem(_, "svg", attrs, _, _*) ⇒
          val width = new UnprefixedAttribute("width", s"${viewBox.width}pt", xml.Null)
          val height = new UnprefixedAttribute("height", s"${viewBox.height}pt", xml.Null)
          val box = new UnprefixedAttribute("viewBox", s"$viewBox", xml.Null)
          e.asInstanceOf[Elem] % width % height % box

        case e @ Elem(_, "g", attrs, _, _*) if attrs.get("class").map(_.text).contains("graph") ⇒
          val transformSpec = s"scale(1 1) rotate(0) translate($translation)"
          val transform = new UnprefixedAttribute("transform", transformSpec, xml.Null)
          e.asInstanceOf[Elem] % transform

        case other ⇒ other
      }
    }).apply(svg)
  }

  private object SvgData {
    def apply(svg: xml.Node): SvgData = {
      val translation = {
        val graphNode = (svg \\ "g").find(g ⇒ (g \ "@class").text == "graph").get
        val transform = graphNode.attributes("transform").text
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1), " ")
      }
      val viewBox = Rectangle.fromString(svg.attributes("viewBox").text, " ", " ")
      SvgData(svg, viewBox, translation)
    }
  }

  def adjust(svgs: Seq[xml.Node], anchorIds: Seq[String], anchoring: Boolean) = {
    val data = svgs.map(SvgData.apply)
    val deltas = (data.sliding(2).toSeq zip anchorIds.sliding(2).toSeq) map {
      case (Seq(prev, next), Seq(prevAnchorId, nextAnchorId)) ⇒
        val nextAnchor = if (anchoring) {
          // TODO: allow auto-anchoring through secondary nodes
          Try(next.anchorPosition(prevAnchorId)) getOrElse next.anchorPosition(nextAnchorId)
        } else {
          next.anchorPosition(nextAnchorId)
        }
        prev.anchorPosition(prevAnchorId) - nextAnchor
    }
    val accumulatedDeltas = deltas.inits.toSeq.reverse.map(Point.sum)
    val translated = (data zip accumulatedDeltas) map {
      case (d, delta) ⇒ d.translate(delta)
    }
    val maxViewBox = Rectangle.union(translated.map(_.viewBox))
    val resized = translated.map(_.copy(viewBox = maxViewBox))
    resized.map(_.render)
  }
}
