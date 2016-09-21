package reftree

import java.io.{StringWriter, File}
import java.nio.file.{Paths, Path}

import uk.co.turingatemyhamster.graphvizs.dsl._
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{BasicIO, Process}

object Diagram {
  case class Options(
    verticalSpacing: Double = 0.8,
    palette: Seq[String] = Array("dodgerblue4", "forestgreen", "coral3"),
    labels: Boolean = true,
    commonNodesBelongToLastTree: Boolean = false
  )

  case class AnimationOptions(
    onionSkin: Int = 1,
    delay: Int = 100,
    loop: Boolean = true,
    verticalSpacing: Double = 0.8,
    color: String = "dodgerblue4"
  ) {
    def toOptions = {
      val palette = if (onionSkin > 0) (50 to 80 by (49 / onionSkin)).take(onionSkin).map(i ⇒ s"gray$i").reverse :+ color else Seq(color)
      Options(verticalSpacing, palette, labels = false, commonNodesBelongToLastTree = true)
    }
  }

  private def label(tree: LabeledRefTree): Seq[Statement] = tree match {
    case LabeledRefTree(label, ref: RefTree.Ref) ⇒
      val labelNodeId = s"${ref.id}-label"
      Seq(
        labelNodeId :| ("shape" := "plaintext", "label" := label, "fontname" := "consolas italic"),
        NodeId(labelNodeId, Some(Port(None, Some(CompassPt.S)))) -->
        NodeId(ref.id, Some(Port(Some("n"), Some(CompassPt.N))))
      )
    case _ ⇒ Seq.empty
  }

  private def node(ref: RefTree.Ref, color: String): NodeStatement = {
    val title = s"""<td port="n">${ref.name}</td>"""
    val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i) }
    val highlight = if (ref.highlight) """bgcolor="bisque"""" else ""
    val style = s"""style="rounded" cellspacing="0" cellpadding="6" cellborder="0" columns="*" $highlight"""
    val label = s"""<<table $style><tr>${(title +: cells).mkString}</tr></table>>"""
    val labelAttribute = AttributeAssignment("label", ID.Identifier(label))
    ref.id :| ("id" := ref.id, labelAttribute, "color" := color, "fontcolor" := color)
  }

  private def cell(tree: RefTree, i: Int): String = {
    val label = tree match {
      case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ value.toBinaryString
      case RefTree.Val(value, _, _) ⇒ value.toString.replace(" ", "_")
      case _: RefTree.Null ⇒ "&empty;"
      case _: RefTree.Elided ⇒ "&hellip;"
      case RefTree.Ref(_, id, _, _) ⇒ "&middot;"
    }
    val port = tree match {
      case RefTree.Ref(_, id, _, _) ⇒ s"""PORT="$id-$i""""
      case _ ⇒ ""
    }
    val highlight = (tree, tree.highlight) match {
      case (_, false) | (_: RefTree.Ref, _) ⇒ ""
      case _ ⇒ """bgcolor="bisque""""
    }
    s"""<td $port $highlight>$label</td>"""
  }

  private def link(id: String, tree: RefTree, i: Int, color: String): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, linkId, _, _) ⇒ Some(
      NodeId(id, Some(Port(Some(s"$linkId-$i"), Some(CompassPt.S)))) -->
        NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N)))) :|
          ("id" := s"$id-$i-$linkId", "color" := color)
    )
    case _ ⇒ None
  }

  private case class ColorlessStatement(s: Statement) {
    override def equals(other: Any) = other match {
      case ColorlessStatement(o) ⇒ (s, o) match {
        case (n1: NodeStatement, n2: NodeStatement) ⇒ n1.copy(attributes = None) == n2.copy(attributes = None)
        case (e1: EdgeStatement, e2: EdgeStatement) ⇒ e1.copy(attributes = None) == e2.copy(attributes = None)
        case (x, y) ⇒ x == y
      }
      case _ ⇒ false
    }

    override def hashCode() = s match {
      case n: NodeStatement ⇒ n.copy(attributes = None).hashCode()
      case e: EdgeStatement ⇒ e.copy(attributes = None).hashCode()
      case x ⇒ x.hashCode()
    }
  }

  private def graph(options: Options)(trees: Seq[LabeledRefTree]): Graph = {
    val graphAttrs = "graph" :| ("ranksep" := options.verticalSpacing)
    val nodeAttrs = "node" :| ("shape" := "plaintext", "fontname" := "consolas")
    val edgeAttrs = "edge" :| ("arrowsize" := "0.7")

    val labels = if (options.labels) trees.flatMap(label) else Seq.empty

    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs, edgeAttrs) ++ labels ++ {
      def inner(tree: RefTree, color: String): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children, _) ⇒
          Seq(node(r, color)) ++
            children.flatMap(inner(_, color)) ++
            children.zipWithIndex.flatMap { case (c, i) ⇒ link(id, c, i, color) }
        case _ ⇒
          Seq.empty
      }
      trees.map(_.tree).zipWithIndex.flatMap {
        case (tree, i) ⇒ inner(tree, options.palette(i % options.palette.length))
      }
    }

    def sharing[A](sequence: Seq[A]) =
      if (options.commonNodesBelongToLastTree) sequence.reverse.distinct.reverse else sequence.distinct
    val distinct = sharing(statements.map(ColorlessStatement)).map(_.s)
    NonStrictDigraph("g", distinct: _*)
  }

  private def graphFrames(options: AnimationOptions)(trees: Seq[LabeledRefTree]): Seq[Graph] = {
    val prefix = Seq.fill(options.onionSkin)(trees.head)
    (prefix ++ trees).sliding(options.onionSkin + 1).map(graph(options.toOptions)).toSeq
  }

  private def writePng(graph: Graph, output: Path): Unit = {
    val opts = DotOpts(Some(DotLayout.dot), Some(DotFormat.png), Some(output.toFile), Seq("-Gdpi=300"))
    val process = Process("dot", opts.generate)
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _)
    (process run io).exitValue()
    ()
  }

  private def produceSvg(graph: Graph): xml.Node = {
    val opts = DotOpts(Some(DotLayout.dot), Some(DotFormat.svg))
    val process = Process("dot", opts.generate)
    val output = new StringWriter
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _).withOutput(BasicIO.processFully(output))
    (process run io).exitValue()
    xml.XML.loadString(output.toString)
  }

  private def svgToPng(svg: xml.Node): File = {
    val svgFile = File.createTempFile("frame", ".svg")
    val pngFile = File.createTempFile("frame", ".png")
    xml.XML.save(svgFile.getAbsolutePath, svg, "UTF-8", xmlDecl = true)
    val args = Seq("-z", "-b", "white", "-d", "100", "-e", pngFile.getAbsolutePath, svgFile.getAbsolutePath)
    val process = Process("inkscape", args)
    process.run().exitValue()
    svgFile.delete()
    pngFile
  }

  private def stitchFiles(files: Seq[Path], output: Path, options: AnimationOptions): Unit = {
    val args = Seq(
      "-delay", options.delay.toString,
      "-loop", if (options.loop) "0" else "1",
      "-verbose"
    ) ++ files.map(_.toString) :+ output.toString
    val process = Process("convert", args)
    process.run().exitValue()
  }

  def show(output: Path, options: Options = Options())(trees: LabeledRefTree*): Unit =
    writePng(graph(options)(trees), output)

  def showDefault(trees: LabeledRefTree*): Unit =
    writePng(graph(Options())(trees), Paths.get("diagram.png"))

  def animate[A: ToRefTree](output: Path, options: AnimationOptions = AnimationOptions())(data: Seq[A]) = {
    val trees = data.map(d ⇒ LabeledRefTree("", d.refTree))
    val frames = graphFrames(options)(trees)
    val svgs = frames.map(produceSvg)
    val ids = trees.map(_.tree) collect { case RefTree.Ref(_, id, _, _) ⇒ id }
    val anchors = if (options.onionSkin > 0) (ids zip ids.dropRight(1)).toSeq else (ids zip ids.drop(1)).toSeq
    val adjustedSvgs = SvgMagic.adjust(svgs, anchors)
    val files = adjustedSvgs.map(svgToPng)
    stitchFiles(files.map(_.toPath), output, options)
    files.foreach(_.delete())
  }
}
