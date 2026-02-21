# diffact [![Test](https://github.com/hshn/diffact/actions/workflows/test.yml/badge.svg)](https://github.com/hshn/diffact/actions/workflows/test.yml)

Structural diff detection library for Scala 3.

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| [diffact-core](#quick-start) | `"dev.hshn" %% "diffact"` | Core diffing — zero external dependencies |
| [diffact-zio](#diffact-zio) | `"dev.hshn" %% "diffact-zio"` | ZPure state-diff integration |
| [diffact-slick](#diffact-slick) | `"dev.hshn" %% "diffact-slick"` | Slick DBIO sync integration |
| [diffact-zio-slick](#diffact-zio-slick) | `"dev.hshn" %% "diffact-zio-slick"` | ZPure + Slick combined |

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

## diffact-zio

ZIO Prelude `ZPure` integration. Automatically diffs the initial state against the final state after running a `ZPure` computation.

```scala
import diffact.*
```

### `ZPure#runAllStateDiff`

```scala
extension [W, S1, S2, R, E, A](zpure: ZPure[W, S1, S2, R, E, A]) {
  def runAllStateDiff(
    s1: S1
  )(using Any <:< R, S2 <:< S1, differ: Differ[S1]): Either[E, (Chunk[W], differ.DiffResult, A)]
}
```

Runs the computation with initial state `s1`, then diffs the initial state against the final state. Returns the logs, state diff, and result value.

```scala
case class MyState(name: String, count: Int)

val computation: ZPure[String, MyState, MyState, Any, Nothing, Unit] =
  for {
    _ <- ZPure.log("updating")
    _ <- ZPure.update[MyState, MyState](s => s.copy(count = s.count + 1))
  } yield ()

val initial = MyState("test", 0)

computation.runAllStateDiff(initial)
// Right((Chunk("updating"), Some(Changed(MyState("test", 0), MyState("test", 1))), ()))
```

When the state is unchanged, the diff result is `None`. On error, returns `Left(E)`.

## diffact-slick

Slick integration for synchronizing differences to a database via `DBIOAction`.

### Setup

Mix `DifferSlickComponent` into your Slick profile:

```scala
import diffact.slick.*

object MyProfile extends slick.jdbc.PostgresProfile with DifferSlickComponent {
  object api extends JdbcAPI with DifferSlickApi
}

import MyProfile.api.*
```

### `Difference[A]#sync`

Dispatches a single difference to the appropriate handler:

```scala
val diff: Difference[User] = Difference.Added(User("alice"))

diff.sync(
  add    = d => userTable += d.value,
  remove = d => userTable.filter(_.name === d.value.name).delete,
  change = d => userTable.filter(_.name === d.oldValue.name).update(d.newValue),
)
// DBIOAction[Int, NoStream, Effect.Write]
```

### `Option[Difference[A]]#sync`

Handles `None` by returning `Monoid[R].empty`:

```scala
val diff: Option[Difference[User]] = Differ.diff(newUser).from(oldUser)

diff.sync(
  add    = d => userTable += d.value,
  remove = d => userTable.filter(_.name === d.value.name).delete,
  change = d => userTable.filter(_.name === d.oldValue.name).update(d.newValue),
)
```

### `Seq[Difference[A]]#sync`

Groups differences by type and dispatches to batch handlers. Each handler receives a `NonEmptyList`:

```scala
val diffs: Seq[Difference[User]] = Differ.diff(newUsers).from(oldUsers)

diffs.sync(
  add    = ds => userTable ++= ds.toList.map(_.value),
  remove = ds => userTable.filter(_.name inSet ds.toList.map(_.value.name)).delete,
  change = ds => DBIO.sequence(ds.toList.map(d => userTable.filter(_.name === d.oldValue.name).update(d.newValue))),
)
```

All variants have a `syncDiscard` counterpart that discards the result and returns `DBIOAction[Unit, ...]`.

### `Seq[Difference[A]]#syncEach`

Like `sync`, but dispatches to per-element handlers instead of batch handlers:

```scala
val diffs: Seq[Difference[User]] = Differ.diff(newUsers).from(oldUsers)

diffs.syncEach(
  add    = d => userTable += d.value,
  remove = d => userTable.filter(_.name === d.value.name).delete,
  change = d => userTable.filter(_.name === d.oldValue.name).update(d.newValue),
)
```

Also has a `syncEachDiscard` counterpart.

### `EitherDBIOComponent`

Adds monadic operations on `DBIOAction[Either[L, R], ...]`. Useful for railway-oriented programming with Slick — no ZIO dependency required.

#### Setup

```scala
import diffact.slick.*

object MyProfile extends slick.jdbc.PostgresProfile
  with DifferSlickComponent
  with EitherDBIOComponent {
  object api extends JdbcAPI with DifferSlickApi with EitherDBIOApi
}

import MyProfile.api.*
```

#### `semiflatMap`

Applies `f` to `Right` values, short-circuits on `Left`:

```scala
val action: DBIOAction[Either[MyError, User], NoStream, Effect] = ???

action.semiflatMap { user =>
  userTable.filter(_.id === user.id).update(user)
}
// DBIOAction[Either[MyError, Int], NoStream, Effect]
```

#### `subflatMap`

Maps `Right` values with a pure `Either`-returning function:

```scala
action.subflatMap { user =>
  if (user.isActive) Right(user) else Left(InactiveUserError)
}
```

#### `flatMapF`

Chains with another `DBIOAction[Either[...], ...]`:

```scala
action.flatMapF { user =>
  findProfile(user.id) // DBIOAction[Either[MyError, Profile], ...]
}
```

#### `right`

Extracts `Right` when `Left` is `Nothing`:

```scala
val infallible: DBIOAction[Either[Nothing, User], NoStream, Effect] = ???
infallible.right // DBIOAction[User, NoStream, Effect]
```

#### `rollbackOnLeft`

Wraps the action in a transaction that rolls back on `Left` while preserving the `Left` value:

```scala
action.rollbackOnLeft.transactionally
// DBIOAction[Either[MyError, User], NoStream, Effect & Effect.Transactional]
```

## diffact-zio-slick

Combines ZPure state-diff with Slick, allowing you to run pure state machines within database transactions.

### Setup

```scala
import diffact.slick.*

object MyProfile extends slick.jdbc.PostgresProfile
  with DifferSlickComponent
  with EitherDBIOComponent
  with ZPureDifferSlickComponent {
  object api extends JdbcAPI with DifferSlickApi with EitherDBIOApi with ZPureDifferSlickApi
}

import MyProfile.api.*
```

### `ZPure#runAllStateAsDBIO`

```scala
extension [W, S1, S2, R, E, A](zpure: ZPure[W, S1, S2, R, E, A]) {
  def runAllStateAsDBIO(
    s1: S1
  )(using Any <:< R, S2 <:< S1, differ: Differ[S1]): DBIOAction[Either[E, (Chunk[W], differ.DiffResult, A)], NoStream, Effect]
}
```

Wraps `runAllStateDiff` in a `DBIOAction`. The result is `Either[E, (Chunk[W], differ.DiffResult, A)]` — pattern match to handle errors and skip DB writes when unnecessary:

```scala
val action = for {
  currentState <- readAggregate(id)
  result       <- stateMachine.runAllStateAsDBIO(currentState).semiflatMap {
    // No state change — nothing to write
    case (events, None, a)       => DBIO.successful(a)
    // State changed — sync diff to DB
    case (events, Some(diff), a) => write(diff) as a
  }
} yield result

db.run(action.transactionally)
```

On `Left(E)` (domain error), `semiflatMap` short-circuits and no DB write is performed. The transaction is rolled back, keeping the database consistent.
