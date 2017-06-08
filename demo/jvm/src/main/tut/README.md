## reftree — diagrams for immutable data

[![Join the chat at https://gitter.im/stanch/reftree](https://badges.gitter.im/stanch/reftree.png)](https://gitter.im/stanch/reftree?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project aims to provide visualizations for common functional data structures used in Scala.
The visualizations are generated automatically from code, which allows to use them in an interactive fashion.
To use this library you will need to have [GraphViz](http://www.graphviz.org/) installed (and have `dot` on your `PATH`).
I also recommend to install the [Source Code Pro](https://github.com/adobe-fonts/source-code-pro) fonts (regular and italic),
as I find they look the best among the free options and therefore are used by default.

For more examples see the [demo page](demo.md).

<p align="center"><img src="images/teaser.gif" /></p>

* [Features](#features)
* [Getting Started](#getting-started)
* [Usage](#usage)
* [Contributing](#contributing)

### Features

* Pre-made visualizations of many standard collections.

  <p align="center"><img src="images/immutability/data/lists.png" width="20%" /></p>

* Automatic visualization of case classes (using
  [shapeless](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#generic-representation-of-sealed-families-of-case-classes)).

  ```scala
  case class Employee(
    name: String,
    salary: Long
  )

  case class Startup(
    name: String,
    founder: Employee,
    team: List[Employee]
  )
  ```

  <p align="center"><img src="images/immutability/lenses/startup.png" /></p>

* Static images as well as animations can be generated.
* Hassle-free captions (using [sourcecode](https://github.com/lihaoyi/sourcecode)).

### Getting Started

#### `RefTree`

This library renders diagrams based on a simple data representation called
[`RefTree`](https://github.com/stanch/reftree/blob/master/core/src/main/scala/reftree/core/RefTree.scala).
Essentially, a `RefTree` denotes either an object (`AnyRef`) with a number of fields,
or a primitive (`AnyVal`).

To render a value of type `A`, you will need an implicit instance of `ToRefTree[A]`.
For many Scala collections, as well as case classes, no extra work is needed,
as these instances are readily available or generated on the fly.

You can configure the automatically generated instances like so:

```tut:silent
import reftree.core.ToRefTree

case class Tree(size: Int, value: Int, children: List[Tree])

implicit val treeDerivationConfig = (ToRefTree.DerivationConfig[Tree]
  .rename("MyTree")                                // display as “MyTree”
  .tweakField("size", _.withName("s"))             // label the field “s”, instead of “size”
  .tweakField("value", _.withTreeHighlight(true))  // highlight the value
  .tweakField("children", _.withoutName))          // do not label the “children” field

implicitly[ToRefTree[Tree]] // auto-derivation will use the configuration above
```

For something custom, manual derivation is the way to go, for example:

```tut:silent
import reftree.core._

implicit def treeInstance: ToRefTree[Tree] = ToRefTree[Tree] { tree =>
  RefTree.Ref(tree, Seq(
    // display the size as a hexadecimal number (why not?)
    RefTree.Val(tree.size).withHint(RefTree.Val.Hex).toField.withName("s"),
    // highlight the value
    tree.value.refTree.withHighlight(true).toField.withName("value"),
    // do not label the children
    tree.children.refTree.toField
  )).rename("MyTree") // change the name displayed for the class
}
```

#### `Renderer`

To render diagrams and animations, you will need a `Renderer`:

```tut:silent
import reftree.render._
import reftree.diagram._
import java.nio.file.Paths
import scala.collection.immutable.Queue

val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 75),
  directory = Paths.get("images", "usage")
)
```

You can also pass a `format` parameter as a String to the `Renderer` constructor
to specify the format you require. The default is `png`. You can specify any
file type supported by `dot -T`.

There are two ways to use it:

```tut:silent
// using the `render` method
renderer.render("queue", Diagram(Queue(1)))

// using syntactic sugar
import renderer._
Diagram(Queue(1)).render("queue")
```

There are various rendering options you can set, for example:

```tut:silent
// using the `render` method
renderer.tweakRendering(_.withVerticalSpacing(2)).render("queue", Diagram(Queue(1)))

// using syntactic sugar
Diagram(Queue(1)).render("queue", _.withVerticalSpacing(2))
```

#### `Diagram`

Diagrams can be created and combined into bigger diagrams using the following API:

```tut:silent
// no caption
Diagram(Queue(1)).render("caption-none")
```

<p align="center"><img src="images/usage/caption-none.png" /></p>

```tut:silent
// automatically set caption to "Queue(1) :+ 2"
Diagram.sourceCodeCaption(Queue(1) :+ 2).render("caption-source")
```

<p align="center"><img src="images/usage/caption-source.png" /></p>

```tut:silent
// use toString to get the caption, i.e. "Queue(1, 2)"
Diagram.toStringCaption(Queue(1) :+ 2).render("caption-tostring")
```

<p align="center"><img src="images/usage/caption-tostring.png" /></p>

```tut:silent
// merge two diagrams, set captions manually
(Diagram(Queue(1)).withCaption("one") + Diagram(Queue(2)).withCaption("two")).render("one-two")
```

<p align="center"><img src="images/usage/one-two.png" /></p>

```tut:silent
// isolate each diagram in its own namespace (graph nodes will not be shared across them)
(Diagram(Queue(1)).toNamespace("one") + Diagram(Queue(2)).toNamespace("two")).render("namespaced")
```

<p align="center"><img src="images/usage/namespaced.png" /></p>

#### `Animation`

Animation is essentially a sequence of diagrams, which can be rendered to an animated GIF.
The simplest way to create an animation is to use the builder API:

```tut:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
  .build()
  .render("animation-simple"))
```

<p align="center"><img src="images/usage/animation-simple.gif" /></p>

You can also configure how the diagram for each frame is produced:

```tut:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
  .build(Diagram(_).withCaption("My Queue").withColor(2))
  .render("animation-captioned-red"))
```

<p align="center"><img src="images/usage/animation-captioned-red.gif" /></p>

Note that by default the library will try to reduce the average movement of
all tree nodes across animation frames. Sometimes you want to “anchor”
the root of the data structure instead, to force it to stay still
while everything else is moving. You can achieve this via `withAnchor` method:

```tut:silent
(Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
  .build(Diagram(_).withAnchor("queue").withCaption("This node is anchored!"))
  .render("animation-anchored"))
```

<p align="center"><img src="images/usage/animation-anchored.gif" /></p>

Finally, animations can be combined in sequence or in parallel, for example:

```tut:silent
val queue1 = (Animation
  .startWith(Queue(1))
  .iterateWithIndex(2)((queue, i) ⇒ queue :+ (i + 1))
  .build()
  .toNamespace("one"))

val queue2 = (Animation
  .startWith(Queue(10))
  .iterateWithIndex(2)((queue, i) ⇒ queue :+ (10 * (i + 1)))
  .build()
  .toNamespace("two"))

(queue1 + queue2).render("animation-parallel")
```

<p align="center"><img src="images/usage/animation-parallel.gif" /></p>

See the [materials for my talk “Unzipping Immutability”](demo.md) for more inspiration!

### Usage

This project is intended for educational purposes and therefore is licensed under GPL 3.0.

To try it interactively:

```
$ sbt demo
@ render(List(1, 2, 3))
// display diagram.png with your favorite image viewer
```

You can depend on the library by adding these lines to your `build.sbt`
(the latest version can be found here:
[ ![Download](https://api.bintray.com/packages/stanch/maven/reftree/images/download.svg) ](https://bintray.com/stanch/maven/reftree/_latestVersion)):

```scala
resolvers += Resolver.bintrayRepo("stanch", "maven")

libraryDependencies += "org.stanch" %% "reftree" % "latest-version"
```

### Contributing

Contributions are welcome! A few helpful tips:

* To update the documentation or the demo, change the files in `demo/src/main/tut/` and run `sbt tutQuick`.
  Do not update the markdown files at the root of the repository by hand,
  since they are autogenerated by [`tut`](https://github.com/tpolecat/tut).
* For now the diagrams generated by `sbt tut` and `sbt "demo/runMain reftree.demo.All"` *are* the system tests.
  Look through them if you are introducing any rendering changes.
  Note that the animation examples in `sbt demo/run` are quite demanding in terms of RAM and running time.
* On Linux I recommend `eog` and `gifview` for viewing PNG and animated GIF respectively.
