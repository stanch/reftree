package reftree.diagram

import reftree.core._

case class Fragment(
  tree: RefTree,
  label: Option[String],
  colorIndex: Option[Int],
  namespace: Option[String]
) {
  def withColor(index: Int) = copy(colorIndex = Some(index))
  def toNamespace(name: String) = copy(namespace = namespace.map(name + "/" + _) orElse Some(name))
}

object Fragment {
  def apply[A: ToRefTree](value: A, label: Option[String] = None): Fragment =
    Fragment(value.refTree, label, None, None)

  def autoLabel[A: ToRefTree](value: sourcecode.Text[A], useToString: Boolean = false) =
    Fragment(value.value, Some(if (useToString) value.value.toString else value.source))
}

case class Diagram(fragments: Seq[Fragment]) {
  def +(that: Diagram) = Diagram(this.fragments ++ that.fragments)

  def toNamespace(name: String) = copy(fragments = fragments.map(_.toNamespace(name)))
}

object Diagram {
  def empty = Diagram(Seq.empty)

  def apply[A: ToRefTree](value: sourcecode.Text[A]): Diagram =
    Diagram(Seq(Fragment.autoLabel(value)))

  def apply[A: ToRefTree, B: ToRefTree](
    value1: sourcecode.Text[A],
    value2: sourcecode.Text[B]
  ): Diagram = Diagram(Seq(
    Fragment.autoLabel(value1),
    Fragment.autoLabel(value2)
  ))

  def apply[A: ToRefTree, B: ToRefTree, C: ToRefTree](
    value1: sourcecode.Text[A],
    value2: sourcecode.Text[B],
    value3: sourcecode.Text[C]
  ): Diagram = Diagram(Seq(
    Fragment.autoLabel(value1),
    Fragment.autoLabel(value2),
    Fragment.autoLabel(value3)
  ))

  def noLabel[A: ToRefTree](value: A): Diagram =
    Diagram(Seq(Fragment(value)))

  def noLabel[A: ToRefTree, B: ToRefTree](value1: A, value2: B): Diagram =
    Diagram(Seq(Fragment(value1), Fragment(value2)))

  def noLabel[A: ToRefTree, B: ToRefTree, C: ToRefTree](value1: A, value2: B, value3: C): Diagram =
    Diagram(Seq(Fragment(value1), Fragment(value2), Fragment(value3)))
}
