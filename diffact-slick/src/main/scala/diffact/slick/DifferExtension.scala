package diffact.slick

import cats.data.NonEmptyList
import diffact.Difference

extension [A](diffs: Seq[Difference[A]]) {
  private[slick] def groupByType: (
    Seq[Difference.Added[A]],
    Seq[Difference.Removed[A]],
    Seq[Difference.Changed[A]],
  ) = {
    diffs.foldLeft((Seq.empty[Difference.Added[A]], Seq.empty[Difference.Removed[A]], Seq.empty[Difference.Changed[A]])) {
      case ((added, removed, changed), diff: Difference.Added[A])   => (added :+ diff, removed, changed)
      case ((added, removed, changed), diff: Difference.Removed[A]) => (added, removed :+ diff, changed)
      case ((added, removed, changed), diff: Difference.Changed[A]) => (added, removed, changed :+ diff)
    }
  }

  private[slick] def groupNelByType: (
    Option[NonEmptyList[Difference.Added[A]]],
    Option[NonEmptyList[Difference.Removed[A]]],
    Option[NonEmptyList[Difference.Changed[A]]],
  ) = {
    val (added, removed, changed) = groupByType
    (NonEmptyList.fromList(added.toList), NonEmptyList.fromList(removed.toList), NonEmptyList.fromList(changed.toList))
  }
}
