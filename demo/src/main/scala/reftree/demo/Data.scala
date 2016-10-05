package reftree.demo

import scala.language.higherKinds

import monocle.PTraversal

import scalaz.Applicative
import scalaz.std.list._
import scalaz.syntax.applicative._

object Data {
  case class Employee(
    name: String,
    salary: Long
  )

  case class Startup(
    name: String,
    ceo: Employee,
    team: List[Employee]
  )

  case class Hierarchy(
    employee: Employee,
    team: List[Hierarchy]
  )

  case class Company(
    name: String,
    hierarchy: Hierarchy
  )

  case class Tree(x: Int, c: List[Tree] = Nil)

  val vowelLens = new PTraversal[String, String, Char, Char] {
    override def modifyF[F[_]: Applicative](f: Char ⇒ F[Char])(s: String): F[String] = {
      Applicative[F].sequence(s.toList map {
        case v @ ('A' | 'E' | 'I' | 'O' | 'U' | 'a' | 'e' | 'i' | 'o' | 'u') ⇒ f(v)
        case x ⇒ Applicative[F].point(x)
      }).map(_.mkString)
    }
  }
}
