## Visualize your data structures!

This page contains the materials for my talk “Visualize your data structures!”.
Here are some past and future presentations:

* [ScalaDays Chicago, April 2017](http://event.scaladays.org/scaladays-chicago-2017#!#schedulePopupExtras-8067)
* [Scala Swarm, June 2017](http://scala-swarm.org/)

You can use this page in two ways:

* as a reference/refresher on the material covered in the talk;
* as an interactive playground where you can try the same commands I presented.

Here is an overview:

* [Introducing `reftree`](#introducing-reftree)
* [Inside `reftree`](#inside-reftree)
* [Functional animation](#functional-animation)
* [Zipping it up](#zipping-it-up)

Throughout this page we will assume the following
declarations (each section might add its own):

```scala
import reftree.core._
import reftree.diagram._
import reftree.render._
import reftree.geometry._
import reftree.svg._
import reftree.svg.ScalaXmlSvgApi.svgUnzip
import reftree.contrib.XmlInstances._
import reftree.contrib.OpticInstances._
import reftree.contrib.ZipperInstances._
import reftree.util.Optics
import reftree.demo.Data
import reftree.demo.Shortcuts
import scala.collection.immutable._
import java.nio.file.Paths
import Diagram.{sourceCodeCaption ⇒ diagram}
```

To start an interactive session, just run

```
$ sbt demo
@ render(List(1, 2, 3))
```

and open `diagram.png` in your favorite image viewer (hopefully one that
reloads images automatically on file change). You will also need to have
[GraphViz](http://www.graphviz.org/) installed. *The interactive session
already has all the necessary imports in scope.*

### Introducing `reftree`

```scala
// extra declarations for this section
val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 100),
  directory = Paths.get("images", "visualize", "intro")
)
import renderer._
```

[reftree](https://stanch.github.io/reftree) is a library for visualizing Scala data structures.

Let’s look at a quick usage example:

```scala
scala> case class Person(firstName: String, age: Int)
defined class Person

scala> val bob = Person("Bob", 42)
bob: Person = Person(Bob,42)

scala> diagram(bob).render("bob")
```

<p align="center"><img src="images/visualize/intro/bob.png" width="20%" /></p>

That’s it! You can configure the visualization as you like:

```scala
scala> // render strings as a single box
     | import reftree.contrib.SimplifiedInstances.string
import reftree.contrib.SimplifiedInstances.string

scala> // rename the firstName field (pun not intended)
     | implicit val personConfig = (ToRefTree.DerivationConfig[Person]
     |   .tweakField("firstName", _.withName("name")))
personConfig: reftree.core.ToRefTree.DerivationConfig[Person] = DerivationConfig(None,Set(),Map(firstName -> <function1>))

scala> diagram(bob).render("bob-simplified")
```

<p align="center"><img src="images/visualize/intro/bob-simplified.png" width="20%" /></p>

There are various ways you can use `reftree`:

* improving the documentation of your projects;
* live-coding demos;
* exploring how things work;
* anywhere you need diagrams of your Scala data structures.

(*Incidentally, this talk is an example of all of the above.*)

My previous `reftree`-powered [talk](immutability.md) focused on
immutable data and various ways it can be manipulated (I do recommend it).

Today I would like to take you on a journey deep inside `reftree` itself,
so that we can see how some of these techniques and concepts can be applied...
to produce visualizations of themselves — using one of my favorite `reftree`
features: animations.

```scala
Animation
  .startWith(Queue(1, 2, 3))
  .repeat(3)(_.iterate(2)(q ⇒ q :+ (q.max + 1)).iterate(2)(_.tail))
  .build(Diagram.toStringCaption(_).withAnchor("queue"))
  .render("queue")
```

<p align="center"><img src="images/immutability/data/queue.gif" width="40%" /></p>

### Inside `reftree`

```scala
// extra declarations for this section
import reftree.contrib.SimplifiedInstances.{option, seq, list}

val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 100),
  directory = Paths.get("images", "visualize", "inside")
)
import renderer._
```

First, we need to grasp the basics of `reftree`.

To visualize a value of some type `A`, `reftree` converts it into a data structure
called `RefTree` (surprise!), using a typeclass `ToRefTree[A]`.
For many Scala collections and case classes this is done automagically, using
[shapeless](https://github.com/milessabin/shapeless/wiki/Feature-overview:-shapeless-2.0.0#generic-representation-of-sealed-families-of-case-classes).
(*If you are curious about the magic, take a look at [this file](core/src/main/scala/reftree/core/GenericInstances.scala).*)

What does a `RefTree` look like? The best way to find out is to visualize a `RefTree`
of a `RefTree`!

```scala
scala> diagram(bob.refTree).render("reftree")
```

<p align="center"><img src="images/visualize/inside/reftree.png" width="100%" /></p>

As you can see, it contains values (`Val`) and references (`Ref`).

How do we get from `RefTree` to an image though?
This is where [GraphViz](http://www.graphviz.org/) comes in.
From a `RefTree` we can obtain a graph definition that can be rendered by GraphViz:

```scala
scala> Shortcuts.graph(bob)
res8: reftree.graph.Graph =
digraph {
  graph [ranksep = 0.8];
  node [shape = "plaintext"; fontname = "Source Code Pro"; fontcolor = "#000000"];
  edge [arrowsize = "0.7"; color = "#000000"];
  "-Person396310856" [id = "-Person396310856"; label = <<table cellspacing="0" cellpadding="6" cellborder="0" columns="*" bgcolor="#ffffff00" style="rounded"><tr><td rowspan="2" port="n">Person</td><td rowspan="1" bgcolor="#ffffff00"><i>name</i></td><td rowspan="1" bgcolor="#ffffff00"><i>age</i></td></tr><hr/><tr><td rowspan="1" port="java.lang.String1180229690-0" bgcolor="#ffffff00">&middot;</td><td rowspan="1" bgcolor="#ffffff00">42</td></tr></table>>; color = "#104e8bff"; fontcolor = "#104e8bff"];
  "-java.lang.String1180229690" [id = "-java.lang.String1180229690"; label = <<table cellspacing="...
```

Going even further, we can ask GraphViz for an [SVG](https://en.wikipedia.org/wiki/Scalable_Vector_Graphics) output:

```scala
scala> Shortcuts.svg(bob)
res9: scala.xml.Node = <svg viewBox="0.00 0.00 171.00 168.00" height="168pt" width="171pt" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"><g class="graph" id="graph0"><title>%3</title><polygon points="-4,4 -4,-164 167,-164 167,4 -4,4" stroke="none" fill="white"/><g class="node" id="-Person396310856"><title>-Person396310856</title><path d="M20,-100C20,-100 143,-100 143,-100 149,-100 155,-106 155,-112 155,-112 155,-144 155,-144 155,-150 149,-156 143,-156 143,-156 20,-156 20,-156 14,-156 8,-150 8,-144 8,-144 8,-112 8,-112 8,-106 14,-100 20,-100" stroke="none" fill="none"/><text fill="#104e8b" font-size="14.00" font-family="Source Code Pro" y="-124.3" x="15.5" text-anchor="start">Person</text><polygon points="71.5,-128 71.5,-155 117.5,-155 117.5,-128 71.5,-128...
```

At this point you might be guessing how we can use this as a basis for our animation approach.
Every state of a data structure will be a separate frame in the SVG format.
However, an animation consisting of these frames alone would be too jumpy.
We need to add intermediate frames to smoothly “morph” one frame into another.
With SVG being a vector format, this sounds simple.
We just have to individually morph different aspects of the image:

* graph node positions;
* graph edges and their shapes;
* colors;
* stroke thickness;
* transparency.

Ouch! A sane functional approach would definitely help here :)

### Functional animation

```scala
// extra declarations for this section
val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 100),
  directory = Paths.get("images", "visualize", "animation")
)
import renderer._
```

Let’s start by introducing an abstraction for morphing, or, in other words,
interpolating things of type `A`:

```scala
trait Interpolation[A] {
  def apply(left: A, right: A, time: Double): A
  def sample(left: A, right: A, n: Int, inclusive: Boolean = true): Seq[A]
}
```

(*If you are curious, [here is the actual implementation](core/src/main/scala/reftree/geometry/Interpolation.scala).*)

Once we have an instance of `Interpolation[xml.Node]`, we can generate
as many intermediate frames as we want! But how do we construct this instance?

Consider a lowly floating point number (it can represent an *x* coordinate of some element in our SVG, for example).
There is an obvious way to implement `Interpolation[Double]`, which `reftree` already defines as `Interpolation.double`:

```scala
scala> val numbers = Interpolation.double.sample(0, 10, 5)
numbers: Seq[Double] = List(0.0, 2.5, 5.0, 7.5, 10.0)

scala> diagram(numbers).render("numbers")
```

<p align="center"><img src="images/visualize/animation/numbers.png" width="20%" /></p>

Now if you think about a point in 2D space, it’s just two numbers joined together:

```scala
scala> val point = Point(0, 10)
point: reftree.geometry.Point = 0.0 10.0

scala> diagram(point).render("point")
```

<p align="center"><img src="images/visualize/animation/point.png" width="15%" /></p>

Can we use the number interpolation to interpolate these two numbers?
To answer this question, let’s introduce more abstraction
(in a great tradition of functional programming).

A lens `Lens[A, B]` is something that can “focus” on a piece of data of type `B`
inside a data structure of type `A` and provide read-write access to it.
We will use the excellent [Monocle library](https://github.com/julien-truffaut/Monocle)
to create lenses and other optics along the way:

```scala
scala> import monocle.macros.GenLens
import monocle.macros.GenLens

scala> val x = GenLens[Point](_.x)
x: monocle.Lens[reftree.geometry.Point,Double] = $anon$1@34f3cb1e

scala> val y = GenLens[Point](_.y)
y: monocle.Lens[reftree.geometry.Point,Double] = $anon$1@607d521f

scala> (diagram(OpticFocus(x, point)).toNamespace("x") +
     |   diagram(OpticFocus(y, point)).toNamespace("y")).render("x+y")
```

<p align="center"><img src="images/visualize/animation/x+y.png" width="40%" /></p>

Lenses provide several methods to manipulate data:

```scala
scala> x.get(point)
res14: Double = 0.0

scala> y.set(20)(point)
res15: reftree.geometry.Point = 0.0 20.0

scala> y.modify(_ + 20)(point)
res16: reftree.geometry.Point = 0.0 30.0
```

If we can read and write each coordinate field, we can interpolate them separately
and update the point field by field.
We do this by piping `Interpolation.double` through `x` and `y` lenses
and combining the resulting interpolations:

```scala
scala> val pointInterpolation = (
     |   x.interpolateWith(Interpolation.double) +
     |   y.interpolateWith(Interpolation.double))
pointInterpolation: reftree.geometry.Interpolation[reftree.geometry.Point] = reftree.geometry.Interpolation$$anon$2@418f3119

scala> val points = pointInterpolation.sample(Point(0, 0), Point(10, 20), 5)
points: Seq[reftree.geometry.Point] = List(0.0 0.0, 2.5 5.0, 5.0 10.0, 7.5 15.0, 10.0 20.0)

scala> diagram(points).render("points")
```

<p align="center"><img src="images/visualize/animation/points.png" width="80%" /></p>

Of course, `reftree` already defines this as `Point.interpolation`.

Using the same approach, we can build a polyline interpolator
(assuming the polylines being interpolated consist of equal number of points):

```scala
scala> Data.polyline1
res18: reftree.geometry.Polyline = 0.0 10.0,10.0 20.0

scala> Data.polyline2
res19: reftree.geometry.Polyline = 20.0 30.0,40.0 50.0

scala> val polylineInterpolation = (GenLens[Polyline](_.points)
     |   .interpolateEachWith(Point.interpolation))
polylineInterpolation: reftree.geometry.Interpolation[reftree.geometry.Polyline] = reftree.geometry.Interpolation$$anon$2@1313e714

scala> val polylines = polylineInterpolation.sample(Data.polyline1, Data.polyline2, 3)
polylines: Seq[reftree.geometry.Polyline] = List(0.0 10.0,10.0 20.0, 10.0 20.0,25.0 35.0, 20.0 30.0,40.0 50.0)

scala> diagram(polylines).render("polylines")
```

<p align="center"><img src="images/visualize/animation/polylines.png" width="85%" /></p>

We are finally ready to implement our first substantial interpolator: one that morphs graph edges.
*The following approach is inspired by Mike Bostock’s [path tween](https://bl.ocks.org/mbostock/3916621),
however `reftree` puts more emphasis on types and even includes its own
[SVG path parser and simplification algorithm](core/src/main/scala/reftree/geometry/Path.scala).*

The resulting animation should look like this:

<p align="center"><img src="images/visualize/animation/edges-100.gif" width="20%" /></p>

An edge is drawn with an [SVG path](https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths),
which consists of several commands, e.g. “move to”, “line to”, “bezier curve to”.
Here is a minimized SVG snippet for an actual edge:

```scala
scala> Data.edge1
res21: scala.xml.Node = <svg viewBox="50 -200 130 70" height="70pt" width="130pt" shape-rendering="geometricPrecision" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><g class="edge"><path d="M84.5,-195C84.5,-165.869 62.5907,-160.925 58.9962,-135.762" stroke="#104e8b" fill="none"/></g></svg>

scala> diagram(Data.edge1).render("edge")
```

<p align="center"><img src="images/visualize/animation/edge.png" width="80%" /></p>

As you can see, the commands themselves are given in the `d` attribute inside the `path` element
in a rather obscure format. Luckily, we have lenses and other optics at our disposal
to plumb through this mess.

First, let’s get to the `path` element. `reftree` implements a few things that will help us:
* `ScalaXmlSvgApi`, an implementation of several useful SVG operations based on Scala’s `xml.Node`.
  In particular, if offers a CSS selector-like method for matching elements of certain type and/or class.
* An optic that focuses on an element deep inside XML or any other recursive data structure: `Optics.collectFirst`.
  It is actually an `Optional`, not a `Lens`, since the element might be missing.

```scala
scala> val edgePathElement = Optics.collectFirst(ScalaXmlSvgApi.select("path"))
edgePathElement: monocle.Optional[scala.xml.Node,scala.xml.Node] = monocle.Optional$$anon$6@f3b3d30

scala> diagram(OpticFocus(edgePathElement, Data.edge1)).render("edgePathElement")
```

<p align="center"><img src="images/visualize/animation/edgePathElement.png" width="80%" /></p>

Next, we need to “descend” to the `d` attribute. Here is where optics really shine:
we can compose `Optional[A, B]` with `Lens[B, C]` to get an `Optional[A, C]`:

```scala
scala> val d = Optics.xmlAttr("d")
d: monocle.Lens[scala.xml.Node,String] = monocle.PLens$$anon$7@33273b31

scala> val edgePathString = edgePathElement composeLens d
edgePathString: monocle.POptional[scala.xml.Node,scala.xml.Node,String,String] = monocle.POptional$$anon$1@69c5297

scala> diagram(OpticFocus(edgePathString, Data.edge1)).render("edgePathString")
```

<p align="center"><img src="images/visualize/animation/edgePathString.png" width="80%" /></p>

Next, we will use an isomorphism, another kind of optic, to view
the string as a nice case class:

```scala
scala> Path.stringIso
res25: monocle.Iso[String,reftree.geometry.Path] = monocle.PIso$$anon$10@c2df53c

scala> val edgePath = edgePathString composeIso Path.stringIso
edgePath: monocle.POptional[scala.xml.Node,scala.xml.Node,reftree.geometry.Path,reftree.geometry.Path] = monocle.POptional$$anon$1@11941988

scala> diagram(edgePath.getOption(Data.edge1)).render("edgePath")
```

<p align="center"><img src="images/visualize/animation/edgePath.png" width="85%" /></p>

And finally, another isomorphism takes us from a `Path` to its sampled representation
as a `Polyline`. (*Purists will say that this is not really an isomorphism because
it’s not reversible, but with a lot of points you can get pretty close ;)*)

```scala
scala> Path.polylineIso(points = 4)
res27: monocle.Iso[reftree.geometry.Path,reftree.geometry.Polyline] = monocle.PIso$$anon$10@dd2f255

scala> def edgePolyline(points: Int) = edgePath composeIso Path.polylineIso(points)
edgePolyline: (points: Int)monocle.POptional[scala.xml.Node,scala.xml.Node,reftree.geometry.Polyline,reftree.geometry.Polyline]

scala> diagram(edgePolyline(4).getOption(Data.edge1)).render("edgePolyline")
```

<p align="center"><img src="images/visualize/animation/edgePolyline.png" width="100%" /></p>

Let’s interpolate!

```scala
scala> def edgeInterpolation(points: Int) = edgePolyline(points).interpolateWith(Polyline.interpolation)
edgeInterpolation: (points: Int)reftree.geometry.Interpolation[scala.xml.Node]

scala> def edges(points: Int, frames: Int) = (Data.edge1 +:
     |   edgeInterpolation(points).sample(Data.edge1, Data.edge2, frames, inclusive = false) :+
     |   Data.edge2)
edges: (points: Int, frames: Int)Seq[scala.xml.Node]

scala> AnimatedGifRenderer.renderAnimatedGif(
     |   edges(4, 4),
     |   Paths.get("images", "visualize", "animation", "edges-4.gif"),
     |   RenderingOptions(density = 200),
     |   AnimationOptions(framesPerSecond = 1)
     | )

scala> AnimatedGifRenderer.renderAnimatedGif(
     |   edges(100, 32),
     |   Paths.get("images", "visualize", "animation", "edges-100.gif"),
     |   RenderingOptions(density = 200),
     |   AnimationOptions(framesPerSecond = 8)
     | )
```

With 4 points and 4 frames:

<p align="center"><img src="images/visualize/animation/edges-4.gif" width="20%" /></p>

With 100 points and 32 frames:

<p align="center"><img src="images/visualize/animation/edges-100.gif" width="20%" /></p>

*Interpolating the entire image is left as an exercise for the reader,
although the impatient will find the complete implementation
[here](core/src/main/scala/reftree/svg/SvgGraphAnimation.scala).*

Notice that we never touched XML directly.
In fact, equipped with the same set of optics for another format or representation
([scala-js?](https://www.scala-js.org/)),
we would be able to operate on it without changing the code too much.
This goes to show the flexibility and usefulness of optics.

### Zipping it up

```scala
// extra declarations for this section
val renderer = Renderer(
  renderingOptions = RenderingOptions(density = 100),
  directory = Paths.get("images", "visualize", "zippers")
)
import renderer._
```

In the previous section we saw `Optics.collectFirst` — an optic that is able to perform
modifications deep inside SVG. How do we go about implementing something like this,
or, more generally, how do we edit recursive data structures such as XML?

This solution is called a “Zipper”, and was introduced by Gérard Huet in 1997.
It consists of a “cursor” pointing to a location anywhere in a tree — “current focus”.
The cursor can be moved freely with operations like `moveDownLeft`, `moveRight`, `moveUp`, etc.
Current focus can be updated, deleted, or new nodes can be inserted to its left or right.
Zippers are immutable, and every operation returns a new Zipper.
All the changes made to the tree can be committed, yielding a new modified version of the original tree.

My [zipper library](https://github.com/stanch/zipper#zipper--an-implementation-of-huets-zipper)
provides a few useful movements and operations. Just like optics, it’s rather generic and flexible.
The zipper can operate on any type, as long as an instance of the `Unzip` typeclass is available,
which can be automatically derived in many cases.
(*Note that the derivation of `Unzip` for SVG can be found
[here](core/src/main/scala/reftree/svg/package.scala).*)

Consider a simple XML tree:

```scala
scala> Data.simpleXml
res32: scala.xml.Node = <tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>

scala> diagram(Data.simpleXml).render("simpleXml")
```

<p align="center"><img src="images/visualize/zippers/simpleXml.png" width="80%" /></p>

When we wrap a Zipper around this tree, it does not look very interesting yet:

```scala
scala> import zipper.Zipper
import zipper.Zipper

scala> val zipper1 = Zipper(Data.simpleXml)
zipper1: zipper.Zipper[scala.xml.Node] = Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>,List(),None)

scala> (diagram(Data.simpleXml) + diagram(zipper1)).render("zipper1")
```

<p align="center"><img src="images/visualize/zippers/zipper1.png" width="80%" /></p>

We can see that it just points to the original tree.
In this case the focus is the root of the tree, which has no siblings,
and the parent zipper does not exist, since we are at the top level.

To move down the tree, we “unzip” it, separating the child nodes into
the focused node and its left and right siblings:

```scala
scala> val zipper2 = zipper1.moveDownLeft
zipper2: zipper.Zipper[scala.xml.Node] = Zipper(List(),<leaf value="2"/>,List(<leaf value="3"/>, <leaf value="4"/>, <tree value="5"><leaf value="6"/><leaf value="7"/></tree>),Some(Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>,List(),None)))

scala> (diagram(zipper1) + diagram(zipper2)).render("zipper1+2")
```

<p align="center"><img src="images/visualize/zippers/zipper1+2.png" width="80%" /></p>

The new Zipper links to the old one,
which will allow us to return to the root of the tree when we are done applying changes.
This link however prevents us from seeing the picture clearly.
Let’s look at the second zipper alone:

```scala
scala> diagram(zipper2).render("zipper2b")
```

<p align="center"><img src="images/visualize/zippers/zipper2b.png" width="70%" /></p>

Great! We have `2` in focus and `3, 4, 5` as right siblings. What happens if we move right a bit?

```scala
scala> val zipper3 = zipper2.moveRightBy(2)
zipper3: zipper.Zipper[scala.xml.Node] = Zipper(List(<leaf value="3"/>, <leaf value="2"/>),<leaf value="4"/>,List(<tree value="5"><leaf value="6"/><leaf value="7"/></tree>),Some(Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>,List(),None)))

scala> diagram(zipper3).render("zipper3")
```

<p align="center"><img src="images/visualize/zippers/zipper3.png" width="70%" /></p>

This is interesting! Notice that the left siblings are “inverted”.
This allows to move left and right in constant time, because the sibling
adjacent to the focus is always at the head of the list.

This also allows us to insert new siblings easily:

```scala
scala> val zipper4 = zipper3.insertLeft(<fruit/>)
zipper4: zipper.Zipper[scala.xml.Node] = Zipper(List(<fruit/>, <leaf value="3"/>, <leaf value="2"/>),<leaf value="4"/>,List(<tree value="5"><leaf value="6"/><leaf value="7"/></tree>),Some(Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>,List(),None)))

scala> diagram(zipper4).render("zipper4")
```

<p align="center"><img src="images/visualize/zippers/zipper4.png" width="70%" /></p>

And, as you might know, we can delete nodes and update the focus:

```scala
scala> val zipper5 = zipper4.deleteAndMoveRight.set(<worm/>)
zipper5: zipper.Zipper[scala.xml.Node] = Zipper(List(<fruit/>, <leaf value="3"/>, <leaf value="2"/>),<worm/>,List(),Some(Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><leaf value="4"/><tree value="5"><leaf value="6"/><leaf value="7"/></tree></tree>,List(),None)))

scala> diagram(zipper5).render("zipper5")
```

<p align="center"><img src="images/visualize/zippers/zipper5.png" width="35%" /></p>

Finally, when we move up, the siblings at the current level are “zipped”
together and their parent node is updated:

```scala
scala> val zipper6 = zipper5.moveUp
zipper6: zipper.Zipper[scala.xml.Node] = Zipper(List(),<tree value="1"><leaf value="2"/><leaf value="3"/><fruit/><worm/></tree>,List(),None)

scala> diagram(zipper6).render("zipper6")
```

<p align="center"><img src="images/visualize/zippers/zipper6.png" width="50%" /></p>

When we are done editing, the `.commit` shorthand can be used for going
all the way up (applying all the changes) and returning the focus.
Notice how all the unchanged nodes are shared between the old and the new XML.

```scala
scala> val notSoSimpleXml = zipper6.commit
notSoSimpleXml: scala.xml.Node = <tree value="1"><leaf value="2"/><leaf value="3"/><fruit/><worm/></tree>

scala> (diagram(Data.simpleXml) + diagram(notSoSimpleXml)).render("notSoSimpleXml")
```

<p align="center"><img src="images/visualize/zippers/notSoSimpleXml.png" width="100%" /></p>

*Using an XML zipper, a determined reader can easily implement advanced lenses,
such as `Optics.collectFirst`, `Optics.collectLeftByKey`, etc, all found
[here](core/src/main/scala/reftree/util/Optics.scala).*

To conclude, here is an animation of a zipper and the tree it operates on
(from my previous talk), produced (as we know now) not without zippers’ help:

<p align="center"><img src="images/immutability/zippers/tree+zipper.gif" /></p>

That’s all! Thank you for reading this far.
I hope you are leaving this page with some great `reftree` use-cases in mind :)
