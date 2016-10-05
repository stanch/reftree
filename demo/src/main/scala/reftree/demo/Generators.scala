package reftree.demo

import org.scalacheck.Gen
import scalaz.scalacheck.ScalaCheckBinding._
import com.thoughtworks.each.Monadic._

object Generators {
  import Data._

  def employeeGen(minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Employee] = monadic[Gen] {
    def name = faker.Name.first_name
    val salary = Gen.choose(minSalary, maxSalary).each
    Employee(name, salary)
  }

  val employees = employeeGen()

  def startupGen(revenue: Long = 10000L): Gen[Startup] = monadic[Gen] {
    def name = faker.Company.name
    val ceo = employeeGen(revenue / 3, revenue / 2).each
    val remainder = revenue - ceo.salary
    val num = 4
    val team = Gen.listOfN(num, employeeGen(remainder / num - 300, remainder / num + 300)).each
    Startup(name, ceo, team)
  }.retryUntil(_.name.length < 20)

  val startups = startupGen()

  def hierarchyGen(depth: Int, minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Hierarchy] = monadic[Gen] {
    val boss = employeeGen(minSalary, maxSalary).each
    val team = if (depth < 2) List.empty else {
      Gen.listOfN(2, hierarchyGen(depth - 1, minSalary * 2 / 3, boss.salary)).each
    }
    Hierarchy(boss, team)
  }

  def companyGen(depth: Int = 3, minSalary: Long = 1000L, maxSalary: Long = 2000): Gen[Company] = monadic[Gen] {
    def name = faker.Company.name
    val hierarchy = hierarchyGen(depth, minSalary, maxSalary).each
    Company(name, hierarchy)
  }.retryUntil(_.name.length < 20)

  val companies = companyGen()

  val simpleTree = Tree(1, List(Tree(2), Tree(3), Tree(4), Tree(5, List(Tree(6), Tree(7)))))
}
