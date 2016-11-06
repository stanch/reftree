package reftree.graph

import monocle.{Iso, Lens}
import reftree.geometry.Color
import reftree.util.Lenses
import com.softwaremill.quicklens._
import uk.co.turingatemyhamster.graphvizs.dsl.AttributeAssignment.AnyAttributeAssignment
import uk.co.turingatemyhamster.graphvizs.dsl._

object Merging {
  // Get a unique statement identifier, if any
  private def statementId(statement: Statement) = statement match {
    case n: NodeStatement ⇒
      Some(n.node.id.asInstanceOf[ID.Quoted].value)
    case e: EdgeStatement ⇒
      Some(e.attributes.get.attrs.find(_.name == ID("id")).get.value.get.asInstanceOf[ID.Quoted].value)
    case _ ⇒ None
  }

  // A mapping between a sequence of rgba color strings and sequence of colors
  private val colorSequenceLens: Lens[Seq[String], Seq[Color]] =
    Lenses.partitionLens[String](_.startsWith("#")) composeIso
    Iso[Seq[String], Seq[Color]](_.map(Color.fromRgbaString(_)))(_.map(_.toRgbaString))

  // A mapping between the node label and the background colors specified inside it
  private val nodeColorLens: Lens[NodeStatement, Seq[Color]] =
    Lens[NodeStatement, Seq[String]] { statement ⇒
      val label = statement.attributes.get.attrs
        .find(_.name == ID("label")).get
        .value.get.asInstanceOf[ID.Identifier].value
      label.replaceAll("""bgcolor="(.+?)"""", """bgcolor="<>$1<>"""").split("<>")
    } { label ⇒ statement ⇒
      statement.modify {
        _.attributes.each.attrs.eachWhere(_.name == ID("label"))
          .when[AnyAttributeAssignment].value.each
      }.setTo(ID.Identifier(label.mkString))
    } composeLens colorSequenceLens

  // Merge node statements by mixing highlight colors inside them
  private def mergeNodeStatements(statements: Seq[NodeStatement], keepLeft: Boolean, mixColor: Boolean) = {
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
      nodeColorLens.set(mixedColors)(keeper)
    }
  }

  // Merge statements with the same ids to eliminate duplicates
  private def merge(statements: Seq[Statement], keepLeft: Boolean, mixColor: Boolean): Seq[Statement] = {
    val groupedById = statements.zipWithIndex.groupBy { case (s, i) ⇒ statementId(s) }.values.toSeq
    val merged = groupedById.flatMap {
      case single @ Seq(_) ⇒ single
      case nodes @ Seq((n: NodeStatement, _), _*) ⇒
        val index = if (keepLeft) nodes.head._2 else nodes.last._2
        val merged = mergeNodeStatements(nodes.map(_._1.asInstanceOf[NodeStatement]), keepLeft, mixColor)
        Seq((merged, index))
      case edges @ Seq((n: EdgeStatement, _), _*) ⇒
        if (!keepLeft) edges.takeRight(1) else edges.take(1)
      case other ⇒ other
    }
    merged.sortBy(_._2).map(_._1)
  }

  def mergeLeft(statements: Seq[Statement]): Seq[Statement] =
    merge(statements, keepLeft = true, mixColor = true)

  def mergeRight(statements: Seq[Statement]): Seq[Statement] =
    merge(statements, keepLeft = false, mixColor = false)
}
