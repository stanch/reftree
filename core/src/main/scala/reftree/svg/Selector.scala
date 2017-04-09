package reftree.svg

/**
 * A very crude approximation of the CSS selectors, which is enough for our purposes
 *
 * Use `Selector.fromString` to construct a selector.
 * Alternatively, this package provides a shorthand string interpolator: sel"g.node, path".
 */
case class Selector(clauses: Set[Selector.Clause]) extends (xml.Node ⇒ Boolean) {
  def apply(svg: xml.Node) = clauses.exists(_(svg))
}

object Selector {
  /** A single selector clause */
  case class Clause(element: Option[String], classes: Set[String]) extends (xml.Node ⇒ Boolean) {
    def apply(svg: xml.Node) =
    element.forall(_ == svg.label) &&
      (classes.isEmpty || classes((svg \ "@class").text))
  }

  def fromString(string: String) = parser.parse(string).get.value

  private def parser = {
    import fastparse.all._

    val id = CharPred(_.isLetterOrDigit).rep(min = 1).!

    val clause = (id ~ ("." ~ id).rep(min = 0)).map { case (el, cls) ⇒ Clause(Some(el), cls.toSet) } |
      ("." ~ id).rep(min = 1).map { cls ⇒ Clause(None, cls.toSet) }

    clause.rep(min = 1, sep = " ".rep ~ "," ~ " ".rep).map(_.toSet).map(Selector.apply)
  }
}
