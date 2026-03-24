package diffact.slick

import cats.implicits.*
import cats.kernel.Monoid
import slick.dbio.*

import diffact.*
import scala.concurrent.ExecutionContext

trait Syncable[-D, A] {
  def sync[R: Monoid](difference: D)(
    add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
    remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
    change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
  )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write]
}

object Syncable {

  given syncableDifference[A]: Syncable[Difference[A], A] with {
    def sync[R: Monoid](difference: Difference[A])(
      add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = difference match {
      case d: Difference.Added[A]   => add(d)
      case d: Difference.Removed[A] => remove(d)
      case d: Difference.Changed[A] => change(d)
    }
  }

  given syncableOption[A](using base: Syncable[Difference[A], A]): Syncable[Option[Difference[A]], A] with {
    def sync[R: Monoid](difference: Option[Difference[A]])(
      add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = difference match {
      case Some(d) => base.sync(d)(add = add, remove = remove, change = change)
      case None    => DBIO.successful(Monoid[R].empty)
    }
  }

  given syncableTracked[A]: Syncable[Difference.Tracked[A], A] with {
    def sync[R: Monoid](difference: Difference.Tracked[A])(
      add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = difference match {
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
  }

  given syncableSeq[A]: Syncable[Seq[Difference[A]], A] with {
    def sync[R: Monoid](difference: Seq[Difference[A]])(
      add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = {
      val empty                                    = DBIO.successful(Monoid[R].empty)
      val (maybeAdded, maybeRemoved, maybeChanged) = difference.groupNelByType

      for {
        r1 <- maybeRemoved.fold(empty)(nel => DBIO.sequence(nel.toList.map(remove)).map(_.combineAll))
        r2 <- maybeAdded.fold(empty)(nel => DBIO.sequence(nel.toList.map(add)).map(_.combineAll))
        r3 <- maybeChanged.fold(empty)(nel => DBIO.sequence(nel.toList.map(change)).map(_.combineAll))
      } yield r1 |+| r2 |+| r3
    }
  }
}
