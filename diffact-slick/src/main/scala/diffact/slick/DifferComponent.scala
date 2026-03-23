package diffact.slick

import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Monoid
import slick.jdbc.JdbcProfile

import diffact.*
import scala.concurrent.ExecutionContext

trait DifferComponent { self: JdbcProfile =>
  trait DifferApi { api: JdbcAPI =>

    class Sync[A, RA, RR, RC] private[diffact] (
      add: Difference.Added[A] => DBIOAction[RA, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[RR, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[RC, NoStream, Effect.Write],
    ) {

      def apply(diff: Difference[A])(using RA =:= RR, RR =:= RC): DBIOAction[RA | RR | RC, NoStream, Effect.Write] =
        diff match {
          case d: Difference.Added[A]   => add(d)
          case d: Difference.Removed[A] => remove(d)
          case d: Difference.Changed[A] => change(d)
        }

      def apply(diff: Option[Difference[A]])(using RA =:= RR, RR =:= RC, Monoid[RA]): DBIOAction[RA | RR | RC, NoStream, Effect.Write] =
        diff match {
          case Some(d) => this(d)
          case None    => DBIO.successful(Monoid[RA].empty)
        }

      def apply(
        diff: Difference.Tracked[A]
      )(using ev1: RA =:= RR, ev2: RR =:= RC, m: Monoid[RA], ec: ExecutionContext): DBIOAction[RA | RR | RC, NoStream, Effect.Write] =
        diff match {
          case Difference.Tracked.Unchanged                          => DBIO.successful(m.empty)
          case Difference.Tracked.Added(value)                       => add(Difference.Added(value))
          case Difference.Tracked.Removed(value)                     => remove(Difference.Removed(value))
          case Difference.Tracked.Changed(oldValue, newValue)        => change(Difference.Changed(oldValue, newValue))
          case Difference.Tracked.Replaced(removedValue, addedValue) =>
            for {
              r1 <- remove(Difference.Removed(removedValue))
              r2 <- add(Difference.Added(addedValue))
            } yield m.combine(ev1.flip(r1), r2)
        }

      def apply(
        diffs: Seq[Difference[A]]
      )(using ev1: RA =:= RR, ev2: RR =:= RC, m: Monoid[RA], ec: ExecutionContext): DBIOAction[RA | RR | RC, NoStream, Effect.Write] = {
        val empty                                    = DBIO.successful(m.empty)
        val (maybeAdded, maybeRemoved, maybeChanged) = diffs.groupNelByType
        val convertRR: RR => RA                      = ev1.flip(_)
        val convertRC: RC => RA                      = r => ev1.flip(ev2.flip(r))

        for {
          r1 <- maybeRemoved.fold(empty)(nel => DBIO.sequence(nel.toList.map(remove)).map(_.map(convertRR).combineAll))
          r2 <- maybeAdded.fold(empty)(nel => DBIO.sequence(nel.toList.map(add)).map(_.combineAll))
          r3 <- maybeChanged.fold(empty)(nel => DBIO.sequence(nel.toList.map(change)).map(_.map(convertRC).combineAll))
        } yield m.combine(m.combine(r1, r2), r3)
      }

      def void: Sync[A, Unit, Unit, Unit] = new Sync[A, Unit, Unit, Unit](
        add = v => this.add(v).void,
        remove = v => this.remove(v).void,
        change = v => this.change(v).void,
      )

      def added[RA1](f: Difference.Added[A] => DBIOAction[RA1, NoStream, Effect.Write]): Sync[A, RA1, RR, RC] =
        new Sync[A, RA1, RR, RC](add = f, remove = this.remove, change = this.change)

      def removed[RR1](f: Difference.Removed[A] => DBIOAction[RR1, NoStream, Effect.Write]): Sync[A, RA, RR1, RC] =
        new Sync[A, RA, RR1, RC](add = this.add, remove = f, change = this.change)

      def changed[RC1](f: Difference.Changed[A] => DBIOAction[RC1, NoStream, Effect.Write]): Sync[A, RA, RR, RC1] =
        new Sync[A, RA, RR, RC1](add = this.add, remove = this.remove, change = f)
    }

    object Sync {
      def apply[A]: Sync[A, Nothing, Nothing, Nothing] = new Sync[A, Nothing, Nothing, Nothing](
        add = _ => DBIO.failed(new IllegalStateException("No Sync handler for Added")),
        remove = _ => DBIO.failed(new IllegalStateException("No Sync handler for Removed")),
        change = _ => DBIO.failed(new IllegalStateException("No Sync handler for Changed")),
      )
    }

    extension [A](diff: Difference[A]) {
      def sync[R](
        add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
      ): DBIOAction[R, NoStream, Effect.Write] = diff match {
        case added: Difference.Added[A]     => add(added)
        case removed: Difference.Removed[A] => remove(removed)
        case changed: Difference.Changed[A] => change(changed)
      }

      def syncDiscard(
        add: Difference.Added[A] => DBIOAction[Any, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[Any, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[Any, NoStream, Effect.Write],
      ): DBIOAction[Unit, NoStream, Effect.Write] = diff.sync(
        add = diff => add(diff).void,
        remove = diff => remove(diff).void,
        change = diff => change(diff).void,
      )
    }

    extension [A](diff: Option[Difference[A]]) {
      def sync[R: Monoid](
        add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
      ): DBIOAction[R, NoStream, Effect.Write] = diff match {
        case Some(diff) => diff.sync(add = add, remove = remove, change = change)
        case None       => DBIO.successful(Monoid[R].empty)
      }

      def syncDiscard(
        add: Difference.Added[A] => DBIOAction[Any, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[Any, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[Any, NoStream, Effect.Write],
      ): DBIOAction[Unit, NoStream, Effect.Write] = diff.sync(
        add = diff => add(diff).void,
        remove = diff => remove(diff).void,
        change = diff => change(diff).void,
      )
    }

    extension [A](diff: Difference.Tracked[A]) {
      def sync[R: Monoid](
        add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = diff match {
        case Difference.Tracked.Unchanged                          => DBIO.successful(Monoid[R].empty)
        case Difference.Tracked.Added(value)                       => add(Difference.Added(value))
        case Difference.Tracked.Removed(value)                     => remove(Difference.Removed(value))
        case Difference.Tracked.Changed(oldValue, newValue)        => change(Difference.Changed(oldValue, newValue))
        case Difference.Tracked.Replaced(removedValue, addedValue) =>
          for {
            r1 <- remove(Difference.Removed(removedValue))
            r2 <- add(Difference.Added(addedValue))
          } yield r1 |+| r2
      }

      def syncDiscard(
        add: Difference.Added[A] => DBIOAction[Any, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[Any, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[Any, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[Unit, NoStream, Effect.Write] = diff.sync(
        add = d => add(d).void,
        remove = d => remove(d).void,
        change = d => change(d).void,
      )
    }

    extension [A](diffs: Seq[Difference[A]]) {
      def sync[R: Monoid](
        add: NonEmptyList[Difference.Added[A]] => DBIOAction[R, NoStream, Effect.Write],
        remove: NonEmptyList[Difference.Removed[A]] => DBIOAction[R, NoStream, Effect.Write],
        change: NonEmptyList[Difference.Changed[A]] => DBIOAction[R, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = {
        val empty                                    = DBIO.successful(Monoid[R].empty)
        val (maybeAdded, maybeRemoved, maybeChanged) = diffs.groupNelByType

        for {
          r1 <- maybeRemoved.map(remove).getOrElse(empty)
          r2 <- maybeAdded.map(add).getOrElse(empty)
          r3 <- maybeChanged.map(change).getOrElse(empty)
        } yield {
          r1 |+| r2 |+| r3
        }
      }

      def syncDiscard(
        add: NonEmptyList[Difference.Added[A]] => DBIOAction[Any, NoStream, Effect.Write],
        remove: NonEmptyList[Difference.Removed[A]] => DBIOAction[Any, NoStream, Effect.Write],
        change: NonEmptyList[Difference.Changed[A]] => DBIOAction[Any, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[Unit, NoStream, Effect.Write] = {
        sync(
          add = diffs => add(diffs).void,
          remove = diffs => remove(diffs).void,
          change = diffs => change(diffs).void,
        )
      }

      def syncEach[R: Monoid](
        add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] =
        sync(
          add = nel => DBIO.sequence(nel.toList.map(add)).map(_.combineAll),
          remove = nel => DBIO.sequence(nel.toList.map(remove)).map(_.combineAll),
          change = nel => DBIO.sequence(nel.toList.map(change)).map(_.combineAll),
        )

      def syncEachDiscard(
        add: Difference.Added[A] => DBIOAction[Any, NoStream, Effect.Write],
        remove: Difference.Removed[A] => DBIOAction[Any, NoStream, Effect.Write],
        change: Difference.Changed[A] => DBIOAction[Any, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[Unit, NoStream, Effect.Write] =
        syncEach(
          add = d => add(d).void,
          remove = d => remove(d).void,
          change = d => change(d).void,
        )
    }

  }
}
