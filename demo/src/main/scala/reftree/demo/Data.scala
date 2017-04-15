package reftree.demo

import reftree.core._

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

  val employee = Employee("Michael", 4000)

  case class Startup(
    name: String,
    founder: Employee,
    team: List[Employee]
  )

  val startup = Startup("Acme", employee, List(
    Employee("Adam", 2100),
    Employee("Bella", 2100),
    Employee("Chad", 1980),
    Employee("Delia", 1850)
  ))

  case class Hierarchy(
    employee: Employee,
    team: List[Hierarchy] = List.empty
  )

  case class Company(
    name: String,
    hierarchy: Hierarchy
  )

  val company = Company("Acme Corp", Hierarchy(
    Employee("Michael", 6000), List(
      Hierarchy(
        Employee("Adam", 3200), List(
          Hierarchy(Employee("Anna", 1600)),
          Hierarchy(Employee("Alex", 1500))
        )
      ),
      Hierarchy(
        Employee("Bella", 3100), List(
          Hierarchy(Employee("Brad", 1500)),
          Hierarchy(Employee("Betty", 1400))
        )
      )
    )
  ))

  val newHire = Hierarchy(Employee("Bert", 1300))

  case class Tree(x: Int, c: List[Tree] = Nil)

  implicit val treeFieldConfig = ToRefTree.FieldConfig[Tree].noName("x").noName("c")

  val simpleTree = Tree(1, List(Tree(2), Tree(3), Tree(4), Tree(5, List(Tree(6), Tree(7)))))

  def letterTraversal(predicate: Char ⇒ Boolean) = new PTraversal[String, String, Char, Char] {
    override def modifyF[F[_]: Applicative](f: Char ⇒ F[Char])(s: String): F[String] = {
      Applicative[F].sequence(s.toList map {
        case x if predicate(x) ⇒ f(x)
        case x ⇒ Applicative[F].point(x)
      }).map(_.mkString)
    }
  }

  val vowels = Set('A', 'E', 'I', 'O', 'U', 'a', 'e', 'i', 'o', 'u')
  val vowelTraversal = letterTraversal(vowels)
  val consonantTraversal = letterTraversal(x ⇒ !vowels(x))
}
