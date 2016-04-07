## reftree — automatic object tree diagrams for immutable data

This project aims to provide visualizations for common functional data structures used in Scala.
The visualizations are generated automatically from code, which allows to use them in an interactive fashion.
To use this library you will need to have [GraphViz](http://www.graphviz.org/) installed.


### Examples

The following examples will assume these declarations:
```scala
import scala.collection.immutable._
import java.nio.file.Paths
import reftree.Diagram

def name(n: String) = Paths.get("examples", s"$n.png")
```

Since all the example code is actually run by [tut](https://github.com/tpolecat/tut),
you can find the resulting images in the `examples` directory.

#### Lists

```scala
val list1 = List(1, 2, 3, 4, 5)
val list2 = List(-1, -2) ++ list1.drop(2)

Diagram(name("lists")).show(list1, list2)
```

<img src="examples/lists.png" height="500px" alt="Lists example" />

By default the trees will be labeled with the arguments passed to `plot`
(using [sourcecode](https://github.com/lihaoyi/sourcecode)),
but you can provide the labels explicitly:

```scala
val list1 = List(1, 2, 3, 4, 5)
val list2 = List(-1, -2) ++ list1.drop(2)

Diagram(name("lists2")).show(
  "positive" → list1,
  "negative" → list2
)
```

<img src="examples/lists2.png" height="500px" alt="Lists example" />

#### Queues

```scala
val queue1 = Queue(1, 2) :+ 3 :+ 4
val queue2 = (queue1 :+ 5).tail

Diagram(name("queues"), verticalSpacing = 1.2).show(queue1, queue2)
```

<img src="examples/queues.png" height="500px" alt="Queue example" />

To reduce visual noise from `Cons` and `Nil`, the visualization of lists can be simplified.
Note however that this option also hides structural sharing:

```scala
import reftree.ToRefTree.Simple.list

val queue1 = Queue(1, 2) :+ 3 :+ 4
val queue2 = (queue1 :+ 5).tail

Diagram(name("queues2")).show(queue1, queue2)
```

<img src="examples/queues2.png" alt="Queue example" />


#### Vectors

```scala
 val vector = 1 +: Vector(10 to 42: _*) :+ 50

 Diagram(name("vector"), verticalSpacing = 2).show(vector)
```

<img src="examples/vector.png" alt="Vector example" />

#### HashSets

```scala
val set = HashSet(1L, 2L + 2L * Int.MaxValue, 3L, 4L)

Diagram(name("hashset")).show(set)
```

<img src="examples/hashset.png" height="500px" alt="HashSet example" />

#### Case classes

Arbitrary case classes are supported automatically via
[shapeless’ Generic](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#generic-representation-of-sealed-families-of-case-classes),
as long as the types or their fields are supported.

```scala
import com.softwaremill.quicklens._

case class Street(name: String, house: Int)
case class Address(street: Street, city: String)
case class Person(address: Address, age: Int)

val person1 = Person(Address(Street("Functional Rd.", 1), "London"), 35)
val person2 = person1.modify(_.address.street.house).using(_ + 2)

Diagram(name("case-classes")).show(
  person1,
  "person next door" → person2
)
```

<img src="examples/case-classes.png" alt="case classes example" />


### Usage

This project is intended for educational purposes and therefore is licensed under GPL 3.0.

To try it interactively:

```
$ sbt amm
@ show(List(1, 2, 3))
// display diagram.png with your favorite image viewer
```

You can depend on the library by adding these lines to your `build.sbt`:

```scala
resolvers += Resolver.bintrayRepo("stanch", "maven")

libraryDependencies += "org.stanch" %% "reftree" % "0.3.0"
```
