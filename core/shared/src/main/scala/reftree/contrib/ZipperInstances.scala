package reftree.contrib

import reftree.core._
import zipper.Zipper
import com.softwaremill.quicklens._

/**
 * [[ToRefTree]] instances for a [[Zipper]]
 */
object ZipperInstances {
  implicit def zipperDerivationConfig[A] =
    ToRefTree.DerivationConfig[Zipper[A]]
      .tweakField("focus", _.withTreeHighlight(true))
      .tweakFieldWith("top", z ⇒ _.withElideRefs(z.top.isDefined))

  /** A class to represent a [[Zipper]] together with its target */
  case class ZipperFocus[A](zipper: Zipper[A], target: A)

  object ZipperFocus {
    /** Construct a [[ZipperFocus]] from a [[Zipper]], using its focus as the target */
    def apply[A](zipper: Zipper[A]): ZipperFocus[A] = ZipperFocus(zipper, zipper.commit)
  }

  implicit def `ZipperFocus RefTree`[A](implicit refTreeA: ToRefTree[A]): ToRefTree[ZipperFocus[A]] =
    ToRefTree[ZipperFocus[A]] { value ⇒
      val focus = value.zipper.focus.refTree

      def inner(tree: RefTree): RefTree = tree match {
        case `focus` ⇒ tree.withHighlight(true)
        case ref: RefTree.Ref ⇒ ref.modify(_.children.each.value).using(inner)
        case _ ⇒ tree
      }

      inner(value.target.refTree)
    }
}
