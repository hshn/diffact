package diffact.slick

import cats.Id
import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Monoid
import slick.jdbc.JdbcProfile

import diffact.*
import scala.concurrent.ExecutionContext

trait DifferComponent { self: JdbcProfile =>
  trait DifferApi { api: JdbcAPI =>
    class Sync[A, RA, RR, RC, F[_]] private[diffact] (
      add: F[Difference.Added[A]] => DBIOAction[RA, NoStream, Effect.Write],
      remove: F[Difference.Removed[A]] => DBIOAction[RR, NoStream, Effect.Write],
      change: F[Difference.Changed[A]] => DBIOAction[RC, NoStream, Effect.Write],
    ) {

      def apply[D](d: D)(using
        s: Syncable[D, A, F],
        eq: Eq3[RA, RR, RC],
        m: Monoid[RA],
        ec: ExecutionContext,
      ): DBIOAction[RA | RR | RC, NoStream, Effect.Write] =
        s.sync[RA](d)(
          add = this.add(_),
          remove = r => this.remove(r).map(eq.toA(_)),
          change = c => this.change(c).map(eq.toA(_)),
        )

      def void: Sync[A, Unit, Unit, Unit, F] = new Sync[A, Unit, Unit, Unit, F](
        add = v => this.add(v).void,
        remove = v => this.remove(v).void,
        change = v => this.change(v).void,
      )

      def added[RA1](f: F[Difference.Added[A]] => DBIOAction[RA1, NoStream, Effect.Write]): Sync[A, RA1, RR, RC, F] =
        new Sync[A, RA1, RR, RC, F](add = f, remove = this.remove, change = this.change)

      def removed[RR1](f: F[Difference.Removed[A]] => DBIOAction[RR1, NoStream, Effect.Write]): Sync[A, RA, RR1, RC, F] =
        new Sync[A, RA, RR1, RC, F](add = this.add, remove = f, change = this.change)

      def changed[RC1](f: F[Difference.Changed[A]] => DBIOAction[RC1, NoStream, Effect.Write]): Sync[A, RA, RR, RC1, F] =
        new Sync[A, RA, RR, RC1, F](add = this.add, remove = this.remove, change = f)
    }

    object Sync {
      def apply[A]: Sync[A, Nothing, Nothing, Nothing, Id] = new Sync[A, Nothing, Nothing, Nothing, Id](
        add = _ => DBIO.failed(new IllegalStateException("No Sync handler for Added")),
        remove = _ => DBIO.failed(new IllegalStateException("No Sync handler for Removed")),
        change = _ => DBIO.failed(new IllegalStateException("No Sync handler for Changed")),
      )

      def batch[A]: Sync[A, Nothing, Nothing, Nothing, NonEmptyList] = new Sync[A, Nothing, Nothing, Nothing, NonEmptyList](
        add = _ => DBIO.failed(new IllegalStateException("No Sync handler for Added")),
        remove = _ => DBIO.failed(new IllegalStateException("No Sync handler for Removed")),
        change = _ => DBIO.failed(new IllegalStateException("No Sync handler for Changed")),
      )
    }
  }
}
