package reftree.svg

case class Selector(clauses: Set[Selector.Clause]) {
  def matches(svg: xml.Node) = clauses.exists(_.matches(svg))
}

object Selector {
  case class Clause(element: Option[String], classes: Set[String]) {
    def matches(svg: xml.Node) =
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
