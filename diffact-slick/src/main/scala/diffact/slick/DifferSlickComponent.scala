package diffact.slick

import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Monoid
import diffact.*
import scala.concurrent.ExecutionContext

trait DifferSlickComponent { self: _root_.slick.jdbc.JdbcProfile =>
  trait DifferSlickApi { api: JdbcAPI =>

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

    extension [A](diffs: Seq[Difference[A]]) {
      def sync[R: Monoid](
        add: NonEmptyList[Difference.Added[A]] => DBIOAction[R, NoStream, Effect.Write],
        remove: NonEmptyList[Difference.Removed[A]] => DBIOAction[R, NoStream, Effect.Write],
        change: NonEmptyList[Difference.Changed[A]] => DBIOAction[R, NoStream, Effect.Write],
      )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = {
        val empty                                    = DBIO.successful(Monoid[R].empty)
        val (maybeAdded, maybeRemoved, maybeChanged) = diffs.groupNelByType

        for {
          r1 <- maybeAdded.map(add).getOrElse(empty)
          r2 <- maybeRemoved.map(remove).getOrElse(empty)
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
    }

  }
}
