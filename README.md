# diffact [![Maven Central](https://img.shields.io/maven-central/v/dev.hshn/diffact_3)](https://central.sonatype.com/artifact/dev.hshn/diffact_3) [![Test](https://github.com/hshn/diffact/actions/workflows/test.yml/badge.svg)](https://github.com/hshn/diffact/actions/workflows/test.yml)

Structural diff detection library for Scala 3.

## Install

| Module | Artifact | Description |
|--------|----------|-------------|
| [diffact-core](#diff--detecting-changes) | `"dev.hshn" %% "diffact"` | Core diffing — zero external dependencies |
| [diffact-zio](#diffact-zio) | `"dev.hshn" %% "diffact-zio"` | ZPure state-diff integration |
| [diffact-slick](#act--applying-changes) | `"dev.hshn" %% "diffact-slick"` | Slick DBIO sync integration |
| [diffact-zio-slick](#diffact-zio-slick) | `"dev.hshn" %% "diffact-zio-slick"` | ZPure + Slick combined |

## Diff — Detecting Changes

```scala
import diffact.*
```

Three equivalent API styles:

```scala
Differ.diff(oldValue = 1, newValue = 2)
Differ.diff(2).from(1)
Differ.diff(1).to(2)
// all return Some(Changed(oldValue = 1, newValue = 2))
```

### Values

Any type with `equals` works out of the box. Result type: `Option[Difference[A]]`

```scala
case class User(name: String)

Differ.diff(User("bob")).from(User("alice"))
// Some(Changed(oldValue = User("alice"), newValue = User("bob")))

Differ.diff(User("alice")).from(User("alice"))
// None
```

### Option

Wraps the underlying differ. Result type matches the inner differ (typically `Option[Difference[A]]`):

```scala
Differ.diff(Option("new")).from(Option.empty[String])
// Some(Added("new"))

Differ.diff(Option.empty[String]).from(Option("old"))
// Some(Removed("old"))

Differ.diff(Option("new")).from(Option("old"))
// Some(Changed(oldValue = "old", newValue = "new"))
```

### Seq

Result type: `Seq[Difference[A]]`. By default, elements are tracked by index:

```scala
Differ.diff(Seq(2)).from(Seq(1))
// Seq(Changed(oldValue = 1, newValue = 2))

Differ.diff(Seq(1, 2)).from(Seq(1))
// Seq(Added(2))
```

Use `trackBy` to match elements by a custom key (see [Identity Tracking](#identity-tracking)):

```scala
case class Item(id: String, name: String)

given SeqDiffer[Item, String] = ValueDiffer[Item].trackBy(_.id).toSeq

val oldItems = Seq(Item("1", "alice"), Item("2", "bob"), Item("3", "charlie"))
val newItems = Seq(Item("2", "BOB"), Item("1", "alice"))

Differ.diff(newItems).from(oldItems)
// Seq(
//   Removed(Item("3", "charlie")),
//   Changed(oldValue = Item("2", "bob"), newValue = Item("2", "BOB")),
// )
```

### Set

Result type: `Seq[Difference[A]]`. Detects added and removed elements only (no `Changed`):

```scala
Differ.diff(Set(2, 3)).from(Set(1, 2))
// contains Added(3) and Removed(1)
```

### Map

Result type: `Seq[(K, Difference[V])]`. Detects added, removed, and changed values by key:

```scala
Differ.diff(Map("a" -> 10, "c" -> 3)).from(Map("a" -> 1, "b" -> 2))
// contains ("c", Added(3)), ("b", Removed(2)), ("a", Changed(oldValue = 1, newValue = 10))
```

### Identity Tracking

`ValueDiffer#trackBy` creates a `TrackedValueDiffer` that distinguishes between a value being **modified** (same identity) and **replaced** (different identity).

Result type: `Difference.Tracked[A]`

```scala
case class Plan(id: String, name: String)

val differ = ValueDiffer[Plan].trackBy(_.id)

// Same identity, different value → Changed
differ.diff(Plan("p1", "Basic"), Plan("p1", "Pro"))
// Difference.Tracked.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))

// Different identity → Replaced
differ.diff(Plan("p1", "Basic"), Plan("p2", "Enterprise"))
// Difference.Tracked.Replaced(removedValue = Plan("p1", "Basic"), addedValue = Plan("p2", "Enterprise"))

// Same identity, same value → Unchanged
differ.diff(Plan("p1", "Basic"), Plan("p1", "Basic"))
// Difference.Tracked.Unchanged
```

A `TrackedValueDiffer` can be lifted to a `SeqDiffer` via `toSeq`:

```scala
given SeqDiffer[Plan, String] = ValueDiffer[Plan].trackBy(_.id).toSeq
```

### Nested Diff

`Difference#map` transforms a diff into a diff of a nested field, delegating to the appropriate `Differ`:

```scala
case class Foo(bar: String, baz: Seq[Baz])
case class Baz(id: String, qux: String)

given SeqDiffer[Baz, String] = ValueDiffer[Baz].trackBy(_.id).toSeq

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

## Result Types

### Difference

The base result type for all diffs:

```scala
sealed trait Difference[+A]

case class Added[+A](value: A)                   extends Difference[A]
case class Removed[+A](value: A)                 extends Difference[A]
case class Changed[+A](oldValue: A, newValue: A) extends Difference[A]
```

| Method | Description |
|--------|-------------|
| `fold(added, removed, changed)` | Exhaustive dispatch without pattern match |
| `map(f)(using Differ[B])` | Diff a nested field |
| `show(using Show[A])` | Human-readable string (`+alice`, `1 → 2`) |

### Difference.Tracked

Result type for `TrackedValueDiffer`. Represents the diff of a single identity-tracked value:

```scala
sealed trait Tracked[+A]

case object Unchanged                                         extends Tracked[Nothing]
case class Added[+A](value: A)                                extends Tracked[A]
case class Removed[+A](value: A)                              extends Tracked[A]
case class Changed[+A](oldValue: A, newValue: A)              extends Tracked[A]
case class Replaced[+A](removedValue: A, addedValue: A)       extends Tracked[A]
```

| Method | Description |
|--------|-------------|
| `toDifferences` | Convert to `Seq[Difference[A]]` (Replaced becomes Removed + Added) |
| `show(using Show[A])` | Human-readable string |

## Act — Applying Changes

> Module: `diffact-slick`

### Setup

Mix `DiffactComponent` into your Slick profile:

```scala
import diffact.slick.*

object MyProfile extends slick.jdbc.PostgresProfile with DiffactComponent {
  object api extends JdbcAPI with DiffactApi
}

import MyProfile.api.*
```

### Sync

`Sync` is a builder for declaratively registering per-handler dispatch logic:

```scala
val handler = Sync[User]
  .added(d => userTable += d.value)
  .removed(d => userTable.filter(_.id === d.value.id).delete)
  .changed(d => userTable.filter(_.id === d.oldValue.id).update(d.newValue))
```

Dispatch to the appropriate handler based on the diff type:

```scala
// Option[Difference[A]] — from ValueDiffer
val diff: Option[Difference[User]] = Differ.diff(newUser).from(oldUser)
handler(diff)

// Difference.Tracked[A] — from TrackedValueDiffer
// Replaced is automatically handled as remove → add
val tracked: Difference.Tracked[Plan] = planDiffer.diff(oldPlan, newPlan)
handler(tracked)

// Seq[Difference[A]] — from SeqDiffer (per-element dispatch)
val diffs: Seq[Difference[Item]] = Differ.diff(newItems).from(oldItems)
handler(diffs)
```

Use `void` to unify return types when handlers return different types, or when you only need a subset of handlers:

```scala
// Only handle changes — Added/Removed will fail at runtime
val changeOnly = Sync[User]
  .changed(d => userTable.filter(_.id === d.oldValue.id).update(d.newValue))
  .void

changeOnly(diff) // DBIOAction[Unit, ...]
```

### Batch Dispatch

`Sync.batchNel` and `Sync.batchSeq` group differences by type and pass each group to the corresponding handler in a single call. Use these for bulk operations like batch inserts or deletes.

`batchNel` handlers receive `NonEmptyList`, guaranteeing at least one element:

```scala
val batchHandler = Sync.batchNel[Item]
  .added(ds => itemTable ++= ds.toList.map(_.value))
  .removed(ds => itemTable.filter(_.id inSet ds.toList.map(_.value.id)).delete)
  .changed(ds => DBIO.sequence(ds.toList.map(d =>
    itemTable.filter(_.id === d.oldValue.id).update(d.newValue)
  )))

val diffs: Seq[Difference[Item]] = Differ.diff(newItems).from(oldItems)
batchHandler(diffs)
```

`batchSeq` handlers receive `Seq` — useful when the downstream API already expects `Seq`:

```scala
val batchHandler = Sync.batchSeq[Item]
  .added(ds => itemTable ++= ds.map(_.value))
  .removed(ds => itemTable.filter(_.id inSet ds.map(_.value.id)).delete)
  .void
```

## ZIO Integration

### diffact-zio

`ZPure#runAllStateDiff` diffs the initial state against the final state after running a ZPure computation:

```scala
import diffact.*

case class MyState(name: String, count: Int)

val computation = for {
  _ <- ZPure.log("updating")
  _ <- ZPure.update[MyState, MyState](s => s.copy(count = s.count + 1))
} yield ()

computation.runAllStateDiff(MyState("test", 0))
// Right((Chunk("updating"), Some(Changed(MyState("test", 0), MyState("test", 1))), ()))
```

When the state is unchanged, the diff result is `None`. On error, returns `Left(E)`.

### diffact-zio-slick

Combines ZPure state-diff with Slick. Mix in both components:

```scala
import diffact.slick.*

object MyProfile extends slick.jdbc.PostgresProfile
  with DiffactComponent
  with DiffactZPureComponent {
  object api extends JdbcAPI with DiffactApi with DiffactZPureApi
}

import MyProfile.api.*
```

`ZPure#runAllStateAsDBIO` wraps `runAllStateDiff` in a `DBIOAction`:

```scala
val action = for {
  currentState <- readAggregate(id)
  result       <- stateMachine.runAllStateAsDBIO(currentState).semiflatMap {
    case (events, None, a)       => DBIO.successful(a)       // no change
    case (events, Some(diff), a) => write(diff) as a          // sync diff to DB
  }
} yield result

db.run(action.transactionally)
```

## Advanced

### contramap

`ValueDiffer#contramap` creates a differ that uses a projection for equality comparison while keeping results in the original type:

```scala
case class Item(id: String, name: String)

// Only compare by name — ignore id changes
given ValueDiffer[Item]       = ValueDiffer[String].contramap(_.name)
given SeqDiffer[Item, String] = ValueDiffer[Item].trackBy(_.id).toSeq

val oldItems = Seq(Item("1", "alice"), Item("2", "bob"))
val newItems = Seq(Item("1", "alice"), Item("2", "BOB"))

Differ.diff(newItems).from(oldItems)
// Seq(Changed(oldValue = Item("2", "bob"), newValue = Item("2", "BOB")))
```
