---
slug: /
sidebar_position: 1
---

# Overview

Behold, automatically generated diagrams and animations for your data!
`reftree` is a *Scala* and *Scala.js* library that allows you to
create data structure visualizations with very little effort.

![teaser](images/teaser.gif)

There are a few ways you can use `reftree`:

* [improving the documentation of your projects](https://stanch.github.io/zipper/);
* [live-coding demos and talks](./talks/index.md);
* exploring how things work;
* anywhere you need diagrams of your Scala data structures.

## Features

* Pre-made visualizations of many standard collections:
  [lists, queues, vectors, etc](./talks/Immutability.md#immutable-data-structures).

  ![lists](images/immutability/lists.png)

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

  ![startup](images/immutability/startup.png)

* Static diagrams as well as animations can be generated.
* Hassle-free captions (using [sourcecode](https://github.com/lihaoyi/sourcecode)).
* Scala.js support (*experimental*).
