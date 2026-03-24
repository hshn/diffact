package diffact.slick

import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Monoid
import slick.jdbc.JdbcProfile

import diffact.*
import scala.concurrent.ExecutionContext

trait DifferComponent { self: JdbcProfile =>
  trait DifferApi { api: JdbcAPI =>
    class SyncBatch[A, RA, RR, RC] private[diffact] (
      add: NonEmptyList[Difference.Added[A]] => DBIOAction[RA, NoStream, Effect.Write],
      remove: NonEmptyList[Difference.Removed[A]] => DBIOAction[RR, NoStream, Effect.Write],
      change: NonEmptyList[Difference.Changed[A]] => DBIOAction[RC, NoStream, Effect.Write],
    ) {

      def apply(
        diffs: Seq[Difference[A]]
      )(using eq: Eq3[RA, RR, RC], m: Monoid[RA], ec: ExecutionContext): DBIOAction[RA | RR | RC, NoStream, Effect.Write] = {
        val empty                                    = DBIO.successful(m.empty)
        val (maybeAdded, maybeRemoved, maybeChanged) = diffs.groupNelByType

        for {
          r1 <- maybeRemoved.fold(empty)(nel => remove(nel).map(eq.toA(_)))
          r2 <- maybeAdded.fold(empty)(add)
          r3 <- maybeChanged.fold(empty)(nel => change(nel).map(eq.toA(_)))
        } yield r1 |+| r2 |+| r3
      }

      def void: SyncBatch[A, Unit, Unit, Unit] = new SyncBatch[A, Unit, Unit, Unit](
        add = v => this.add(v).void,
        remove = v => this.remove(v).void,
        change = v => this.change(v).void,
      )

      def added[RA1](f: NonEmptyList[Difference.Added[A]] => DBIOAction[RA1, NoStream, Effect.Write]): SyncBatch[A, RA1, RR, RC] =
        new SyncBatch[A, RA1, RR, RC](add = f, remove = this.remove, change = this.change)
      def removed[RR1](f: NonEmptyList[Difference.Removed[A]] => DBIOAction[RR1, NoStream, Effect.Write]): SyncBatch[A, RA, RR1, RC] =
        new SyncBatch[A, RA, RR1, RC](add = this.add, remove = f, change = this.change)
      def changed[RC1](f: NonEmptyList[Difference.Changed[A]] => DBIOAction[RC1, NoStream, Effect.Write]): SyncBatch[A, RA, RR, RC1] =
        new SyncBatch[A, RA, RR, RC1](add = this.add, remove = this.remove, change = f)
    }

    class Sync[A, RA, RR, RC] private[diffact] (
      add: Difference.Added[A] => DBIOAction[RA, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[RR, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[RC, NoStream, Effect.Write],
    ) {

      def apply[D](d: D)(using
        s: Syncable[D, A],
        eq: Eq3[RA, RR, RC],
        m: Monoid[RA],
        ec: ExecutionContext,
      ): DBIOAction[RA | RR | RC, NoStream, Effect.Write] =
        s.sync[RA](d)(
          add = this.add(_),
          remove = r => this.remove(r).map(eq.toA(_)),
          change = c => this.change(c).map(eq.toA(_)),
        )

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

      def batch[A]: SyncBatch[A, Nothing, Nothing, Nothing] = new SyncBatch[A, Nothing, Nothing, Nothing](
        add = _ => DBIO.failed(new IllegalStateException("No SyncBatch handler for Added")),
        remove = _ => DBIO.failed(new IllegalStateException("No SyncBatch handler for Removed")),
        change = _ => DBIO.failed(new IllegalStateException("No SyncBatch handler for Changed")),
      )
    }

  }
}
