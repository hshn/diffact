package diffact.slick

import cats.data.NonEmptyList
import diffact.Difference
import diffact.GroupedDifferences.*

extension [A](diffs: Seq[Difference[A]]) {
  private[slick] def groupNelByType: (
    Option[NonEmptyList[Difference.Added[A]]],
    Option[NonEmptyList[Difference.Removed[A]]],
    Option[NonEmptyList[Difference.Changed[A]]],
  ) = {
    val grouped = diffs.groupByType
    (
      NonEmptyList.fromList(grouped.added.toList),
      NonEmptyList.fromList(grouped.removed.toList),
      NonEmptyList.fromList(grouped.changed.toList),
    )
  }
}
