package reftree.svg

/**
 * A very crude approximation of the CSS selectors, which is enough for our purposes
 *
 * Use `Selector.fromString` to construct a selector.
 */
case class Selector(clauses: Set[Selector.Clause])

object Selector {
  /** A single selector clause */
  case class Clause(element: Option[String], classes: Set[String])

  private val parser = {
    import fastparse.all._

    val id = CharPred(_.isLetterOrDigit).rep(min = 1).!

    val clause = (id ~ ("." ~ id).rep(min = 0)).map { case (el, cls) ⇒ Clause(Some(el), cls.toSet) } |
      ("." ~ id).rep(min = 1).map { cls ⇒ Clause(None, cls.toSet) }

    clause.rep(min = 1, sep = " ".rep ~ "," ~ " ".rep).map(_.toSet).map(Selector.apply)
  }

  def fromString(string: String) = parser.parse(string).get.value
}
