package reftree.demo

import reftree.core._
import reftree.geometry.{Point, Polyline}

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

  implicit val treeDerivationConfig = ToRefTree.DerivationConfig[Tree]
    .tweakField("x", _.withoutName)
    .tweakField("c", _.withoutName)

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

  val polyline1 = Polyline(Seq(Point(0, 10), Point(10, 20)))
  val polyline2 = Polyline(Seq(Point(20, 30), Point(40, 50)))

  val edge1 = xml.Utility.trim {
    <svg
      viewBox="50 -200 130 70"
      height="70pt"
      width="130pt"
      shape-rendering="geometricPrecision"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      xmlns="http://www.w3.org/2000/svg">
      <g class="edge">
        <path d="M84.5,-195C84.5,-165.869 62.5907,-160.925 58.9962,-135.762" stroke="#104e8b" fill="none"/>
      </g>
    </svg>
  }

  val edge2 = xml.Utility.trim {
    <svg
      viewBox="50 -200 130 70"
      height="70pt"
      width="130pt"
      shape-rendering="geometricPrecision"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      xmlns="http://www.w3.org/2000/svg">
      <g class="edge">
        <path d="M131.5,-195C131.5,-164.017 162.095,-162.206 166.875,-135.781" stroke="#104e8b" fill="none"/>
      </g>
    </svg>
  }

  val simpleXml = xml.Utility.trim {
    <tree value="1">
      <leaf value="2"/>
      <leaf value="3"/>
      <leaf value="4"/>
      <tree value="5">
        <leaf value="6"/>
        <leaf value="7"/>
      </tree>
    </tree>
  }
}
