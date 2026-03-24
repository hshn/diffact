package diffact.slick

import cats.Id
import cats.data.NonEmptyList
import cats.implicits.*
import cats.kernel.Monoid
import slick.dbio.*

import diffact.*
import scala.concurrent.ExecutionContext

trait Syncable[-D, A, F[_]] {
  def sync[R: Monoid](difference: D)(
    add: F[Difference.Added[A]] => DBIOAction[R, NoStream, Effect.Write],
    remove: F[Difference.Removed[A]] => DBIOAction[R, NoStream, Effect.Write],
    change: F[Difference.Changed[A]] => DBIOAction[R, NoStream, Effect.Write],
  )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write]
}

object Syncable {

  given syncableDifference[A]: Syncable[Difference[A], A, Id] with {
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

  given syncableOption[A](using base: Syncable[Difference[A], A, Id]): Syncable[Option[Difference[A]], A, Id] with {
    def sync[R: Monoid](difference: Option[Difference[A]])(
      add: Difference.Added[A] => DBIOAction[R, NoStream, Effect.Write],
      remove: Difference.Removed[A] => DBIOAction[R, NoStream, Effect.Write],
      change: Difference.Changed[A] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = difference match {
      case Some(d) => base.sync(d)(add = add, remove = remove, change = change)
      case None    => DBIO.successful(Monoid[R].empty)
    }
  }

  given syncableTracked[A]: Syncable[Difference.Tracked[A], A, Id] with {
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

  given syncableSeq[A]: Syncable[Seq[Difference[A]], A, Id] with {
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

  given syncableSeqNonEmptyList[A]: Syncable[Seq[Difference[A]], A, NonEmptyList] with {
    override def sync[R: Monoid](difference: Seq[Difference[A]])(
      add: NonEmptyList[Difference.Added[A]] => DBIOAction[R, NoStream, Effect.Write],
      remove: NonEmptyList[Difference.Removed[A]] => DBIOAction[R, NoStream, Effect.Write],
      change: NonEmptyList[Difference.Changed[A]] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = {
      val empty                                    = DBIO.successful(Monoid[R].empty)
      val (maybeAdded, maybeRemoved, maybeChanged) = difference.groupNelByType

      for {
        r1 <- maybeRemoved.fold(empty)(remove)
        r2 <- maybeAdded.fold(empty)(add)
        r3 <- maybeChanged.fold(empty)(change)
      } yield r1 |+| r2 |+| r3
    }
  }

  given syncableSeqSeq[A]: Syncable[Seq[Difference[A]], A, Seq] with {
    override def sync[R: Monoid](difference: Seq[Difference[A]])(
      add: Seq[Difference.Added[A]] => DBIOAction[R, NoStream, Effect.Write],
      remove: Seq[Difference.Removed[A]] => DBIOAction[R, NoStream, Effect.Write],
      change: Seq[Difference.Changed[A]] => DBIOAction[R, NoStream, Effect.Write],
    )(using ExecutionContext): DBIOAction[R, NoStream, Effect.Write] = {
      val empty                                    = DBIO.successful(Monoid[R].empty)
      val (maybeAdded, maybeRemoved, maybeChanged) = difference.groupNelByType

      for {
        r1 <- maybeRemoved.fold(empty)(nel => remove(nel.toList))
        r2 <- maybeAdded.fold(empty)(nel => add(nel.toList))
        r3 <- maybeChanged.fold(empty)(nel => change(nel.toList))
      } yield r1 |+| r2 |+| r3
    }
  }
}
