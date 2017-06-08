package reftree.demo

object Ammonite extends App {
  val predef = Seq(
    "import scala.collection.immutable._",
    "import de.sciss.fingertree._",
    "import monocle.macros.GenLens",
    "import com.softwaremill.quicklens._",
    "import zipper._",
    "import reftree.core._",
    "import reftree.diagram._",
    "import reftree.svg._",
    "import reftree.svg.ScalaXmlSvgApi.svgUnzip",
    "import reftree.geometry._",
    "import reftree.util.Optics",
    "import reftree.contrib.FingerTreeInstances._",
    "import reftree.contrib.OpticInstances._",
    "import reftree.contrib.ZipperInstances._",
    "import reftree.contrib.XmlInstances._",
    "import reftree.contrib.SimplifiedInstances",
    "import reftree.demo.Data._",
    "import reftree.demo.Shortcuts._"
  ).mkString(";")
  ammonite.Main(predef).run()
}
