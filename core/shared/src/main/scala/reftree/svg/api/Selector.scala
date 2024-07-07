package reftree.svg.api

import fastparse._, NoWhitespace._

/**
 * A very crude approximation of the CSS selectors, which is enough for our purposes
 *
 * Use `Selector.fromString` to construct a selector.
 */
case class Selector(clauses: Set[Selector.Clause])

object Selector {
  /** A single selector clause */
  case class Clause(element: Option[String], classes: Set[String])

  private def parser[A: P] = {

    def id = CharPred(_.isLetterOrDigit).rep(1).!

    def clause = (id ~ ("." ~ id).rep(0)).map { case (el, cls) => Clause(Some(el), cls.toSet) } |
      ("." ~ id).rep(1).map { cls => Clause(None, cls.toSet) }

    clause.rep(1, sep = " ".rep ~ "," ~ " ".rep).map(_.toSet).map(Selector.apply)
  }

  private var cache = Map.empty[String, Selector]

  def fromString(string: String) = synchronized {
    cache.getOrElse(string, {
      val selector = parse(string, parser(_)).get.value
      cache = cache.updated(string, selector)
      selector
    })
  }
}
