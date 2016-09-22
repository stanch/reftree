## reftree — automatic object tree diagrams for immutable data

[![Join the chat at https://gitter.im/stanch/reftree](https://badges.gitter.im/stanch/reftree.png)](https://gitter.im/stanch/reftree?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project aims to provide visualizations for common functional data structures used in Scala.
The visualizations are generated automatically from code, which allows to use them in an interactive fashion.
To use this library you will need to have [GraphViz](http://www.graphviz.org/) installed (and have `dot` on your `PATH`).

For more examples see the [materials for my talk “Unzipping Immutability”](https://github.com/stanch/unzimm).

### Examples

The following examples will assume these declarations:
```tut:silent
import scala.collection.immutable._
import java.nio.file.Paths
import reftree.Diagram
import reftree.Diagram.Options

def name(n: String) = Paths.get("examples", s"$n.png")
```

Since all the example code is actually run by [tut](https://github.com/tpolecat/tut),
you can find the resulting images in the `examples` directory.

#### Lists

```tut:silent
val list1 = List(1, 2, 3, 4, 5)
val list2 = List(-1, -2) ++ list1.drop(2)

Diagram.renderPng(name("lists"))(list1, list2)
```

<p align="center"><img src="examples/lists.png" width="40%" /></p>

By default the trees will be labeled with the arguments passed to `plot`
(using [sourcecode](https://github.com/lihaoyi/sourcecode)),
but you can provide the labels explicitly:

```tut:silent
val list1 = List(1, 2, 3, 4, 5)
val list2 = List(-1, -2) ++ list1.drop(2)

Diagram.renderPng(name("lists2"))(
  "positive" → list1,
  "negative" → list2
)
```

<p align="center"><img src="examples/lists2.png" width="40%" /></p>

#### Queues

```tut:silent
val queue1 = Queue(1, 2) :+ 3 :+ 4
val queue2 = (queue1 :+ 5).tail

Diagram.renderPng(name("queues"), Options(verticalSpacing = 1.2))(queue1, queue2)
```

<p align="center"><img src="examples/queues.png" width="40%" /></p>

To reduce visual noise from `Cons` and `Nil`, the visualization of lists can be simplified.
Note however that this option also hides structural sharing:

```tut:silent
import reftree.ToRefTree.Simple.list

val queue1 = Queue(1, 2) :+ 3 :+ 4
val queue2 = (queue1 :+ 5).tail

Diagram.renderPng(name("queues2"))(queue1, queue2)
```

<p align="center"><img src="examples/queues2.png" width="50%" /></p>


#### Vectors

```tut:silent
 val vector = 1 +: Vector(10 to 42: _*) :+ 50

 Diagram.renderPng(name("vector"), Options(verticalSpacing = 2))(vector)
```

<p align="center"><img src="examples/vector.png" width="100%" /></p>

#### HashSets

```tut:silent
val set = HashSet(1L, 2L + 2L * Int.MaxValue, 3L, 4L)

Diagram.renderPng(name("hashset"))(set)
```

<p align="center"><img src="examples/hashset.png" width="100%" /></p>

#### Case classes

Arbitrary case classes are supported automatically via
[shapeless’ Generic](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#generic-representation-of-sealed-families-of-case-classes),
as long as the types or their fields are supported.

```tut:silent
import com.softwaremill.quicklens._

case class Street(name: String, house: Int)
case class Address(street: Street, city: String)
case class Person(address: Address, age: Int)

val person1 = Person(Address(Street("Functional Rd.", 1), "London"), 35)
val person2 = person1.modify(_.address.street.house).using(_ + 2)

Diagram.renderPng(name("case-classes"))(
  person1,
  "person next door" → person2
)
```

<p align="center"><img src="examples/case-classes.png" width="70%" /></p>

#### Animations

You can generate animations using `Diagram.renderAnimatedGif`.
For this you will need [Inkscape](https://inkscape.org/en/) and [ImageMagick](http://www.imagemagick.org/) installed
(and have `inkscape` and `convert` on your `PATH`).

Here is an example:

```tut:silent
import reftree.Utils
import reftree.ToRefTree.Actual.list
import reftree.Diagram.AnimationOptions

Diagram.renderAnimatedGif(
  Paths.get("examples", "list-prepend.gif"),
  AnimationOptions(diffAccent = true))(
  Utils.iterate(List(1))(2 :: _, 3 :: _, 4 :: _)
)

Diagram.renderAnimatedGif(
  Paths.get("examples", "list-append.gif"),
  AnimationOptions(onionSkin = 3))(
  Utils.iterate(List(1))(_ :+ 2, _ :+ 3, _ :+ 4)
)
```

<p align="center">
  <img src="examples/list-prepend.gif" width="30%" />
  <img src="examples/list-append.gif" width="52%" />
</p>

### Usage

This project is intended for educational purposes and therefore is licensed under GPL 3.0.

To try it interactively:

```
$ sbt amm
@ show(List(1, 2, 3))
// display diagram.svg with your favorite image viewer
```

You can depend on the library by adding these lines to your `build.sbt`
(the latest version can be found here:
[ ![Download](https://api.bintray.com/packages/stanch/maven/reftree/images/download.svg) ](https://bintray.com/stanch/maven/reftree/_latestVersion)):

```scala
resolvers ++= Seq(
  Resolver.bintrayRepo("stanch", "maven"),
  Resolver.bintrayRepo("drdozer", "maven")
)

libraryDependencies += "org.stanch" %% "reftree" % "latest-version"
```
