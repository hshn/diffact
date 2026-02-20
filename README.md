# diffact

Structural diff detection library for Scala 3. Zero external dependencies.

## Quick Start

```scala
import diffact.*
```

Three equivalent API styles:

```scala
// 1. Named parameters
Differ.diff(oldValue = 1, newValue = 2)
// Some(Changed(oldValue = 1, newValue = 2))

// 2. Fluent — set new value, specify old with .from()
Differ.diff(2).from(1)
// Some(Changed(oldValue = 1, newValue = 2))

// 3. Fluent — set old value, specify new with .to()
Differ.diff(1).to(2)
// Some(Changed(oldValue = 1, newValue = 2))
```

When values are equal, the result is `None`:

```scala
Differ.diff(1).to(1)
// None
```

## Supported Types

### Value types (`Int`, `String`, case class, etc.)

Result type: `Option[Difference[A]]`

Any type with `equals` works out of the box:

```scala
case class Foo(bar: String)

Differ.diff(Foo("a")).from(Foo("b"))
// Some(Changed(oldValue = Foo("b"), newValue = Foo("a")))

Differ.diff(Foo("a")).from(Foo("a"))
// None
```

### `Option[A]`

Result type: `Option[Difference[A]]`

```scala
Differ.diff(Option("new")).from(Option.empty[String])
// Some(Added("new"))

Differ.diff(Option.empty[String]).from(Option("old"))
// Some(Removed("old"))

Differ.diff(Option("new")).from(Option("old"))
// Some(Changed(oldValue = "old", newValue = "new"))
```

### `Seq[A]`

Result type: `Seq[Difference[A]]`

By default, elements are tracked by index:

```scala
Differ.diff(Seq(2)).from(Nil)
// Seq(Added(2))

Differ.diff(Seq.empty[Int]).from(Seq(1))
// Seq(Removed(1))

Differ.diff(Seq(1)).from(Seq(2))
// Seq(Changed(oldValue = 2, newValue = 1))
```

Use `trackBy` to track elements by a custom key (e.g. ID):

```scala
case class Baz(id: String, qux: String)

given SeqDiffer[Baz, String] = Differ[Baz].trackBy(_.id).toSeq

val oldItems = Seq(Baz("b1", "q1"), Baz("b2", "q2"), Baz("b3", "q3"))
val newItems = Seq(Baz("b2", "q2222"), Baz("b1", "q1"))

Differ.diff(newItems).from(oldItems)
// Seq(
//   Removed(Baz("b3", "q3")),
//   Changed(oldValue = Baz("b2", "q2"), newValue = Baz("b2", "q2222")),
// )
```

### `Set[A]`

Result type: `Seq[Difference[A]]`

Detects added and removed elements only (no `Changed`):

```scala
Differ.diff(Set(2, 3)).from(Set(1, 2))
// contains Added(3) and Removed(1)
```

### `Map[K, V]`

Result type: `Seq[Difference[V]]`

Detects added, removed, and changed values by key:

```scala
Differ.diff(Map("a" -> 10, "c" -> 3)).from(Map("a" -> 1, "b" -> 2))
// contains Added(3), Removed(2), Changed(oldValue = 1, newValue = 10)
```

## Difference Type

```scala
sealed trait Difference[+A]

case class Added[+A](value: A)                   extends Difference[A]
case class Removed[+A](value: A)                 extends Difference[A]
case class Changed[+A](oldValue: A, newValue: A) extends Difference[A]
```

### `Changed#map` — nested diff

`Changed#map` transforms a `Changed` into a diff of a nested field:

```scala
case class Foo(bar: String, baz: Seq[Baz])
case class Baz(id: String, qux: String)

// trackBy(_.id) so elements are matched by ID, not by position
given SeqDiffer[Baz, String] = Differ[Baz].trackBy(_.id).toSeq

val diff = Difference.Changed(
  oldValue = Foo("1", Seq(Baz("b1", "q1"), Baz("b2", "q2"), Baz("b3", "q3"))),
  newValue = Foo("1", Seq(Baz("b2", "q2222"), Baz("b1", "q1"))),
)

diff.map(_.baz)
// Seq(
//   Removed(Baz("b3", "q3")),
//   Changed(oldValue = Baz("b2", "q2"), newValue = Baz("b2", "q2222")),
// )
```

## Identity Tracking

### `trackBy` — identity-aware diff

`ValueDiffer#trackBy` creates a `TrackedValueDiffer` that distinguishes between a value being **modified** (same identity) and **replaced** (different identity):

Result type: `Seq[Difference[A]]`

```scala
case class Plan(id: String, name: String)

val differ: TrackedValueDiffer[Plan, String] = Differ[Plan].trackBy(_.id)

// Same identity, different value → Changed
differ.diff(Plan("p1", "Basic"), Plan("p1", "Pro"))
// Seq(Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro")))

// Different identity → Removed + Added
differ.diff(Plan("p1", "Basic"), Plan("p2", "Enterprise"))
// Seq(Removed(Plan("p1", "Basic")), Added(Plan("p2", "Enterprise")))

// Same identity, same value → empty
differ.diff(Plan("p1", "Basic"), Plan("p1", "Basic"))
// Seq()
```

`TrackedValueDiffer` can be lifted to a `SeqDiffer` via `toSeq`:

```scala
given SeqDiffer[Plan, String] = Differ[Plan].trackBy(_.id).toSeq
```

## Advanced

### `contramap` — custom equality

`ValueDiffer#contramap` creates a differ that uses a projection for equality comparison while keeping results in the original type:

```scala
case class Wrapper(value: Int)

val differ: ValueDiffer[Wrapper] = Differ[Int].contramap(_.value)

differ.diff(Wrapper(1), Wrapper(2))
// Some(Changed(Wrapper(1), Wrapper(2)))

differ.diff(Wrapper(1), Wrapper(1))
// None
```

Combined with `SeqDiffer`, `contramap` enables fine-grained control over what constitutes a change:

```scala
case class Item(id: String, name: String)

given ValueDiffer[Item]          = Differ[String].contramap(_.name)
given SeqDiffer[Item, String]    = Differ[Item].trackBy(_.id).toSeq

val oldItems = Seq(Item("1", "alice"), Item("2", "bob"))
val newItems = Seq(Item("1", "alice"), Item("2", "BOB"))

Differ.diff(newItems).from(oldItems)
// Seq(Changed(oldValue = Item("2", "bob"), newValue = Item("2", "BOB")))
```
