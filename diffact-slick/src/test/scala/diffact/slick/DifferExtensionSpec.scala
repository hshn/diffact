package diffact.slick

import cats.data.NonEmptyList
import diffact.Difference
import zio.Scope
import zio.test.*

object DifferExtensionSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("DifferExtension") {
    suiteAll("groupByType") {
      test("groups differences by type") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Changed(3, 4),
          Difference.Added(5),
        )
        val (added, removed, changed) = diffs.groupByType
        assertTrue(
          added == Seq(Difference.Added(1), Difference.Added(5)),
          removed == Seq(Difference.Removed(2)),
          changed == Seq(Difference.Changed(3, 4)),
        )
      }
      test("returns empty seqs for empty input") {
        val (added, removed, changed) = Seq.empty[Difference[Int]].groupByType
        assertTrue(
          added.isEmpty,
          removed.isEmpty,
          changed.isEmpty,
        )
      }
    }
    suiteAll("groupNelByType") {
      test("groups differences as NonEmptyList options") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Changed(3, 4),
        )
        val (added, removed, changed) = diffs.groupNelByType
        assertTrue(
          added == Some(NonEmptyList.one(Difference.Added(1))),
          removed == Some(NonEmptyList.one(Difference.Removed(2))),
          changed == Some(NonEmptyList.one(Difference.Changed(3, 4))),
        )
      }
      test("returns None for missing groups") {
        val diffs: Seq[Difference[Int]] = Seq(Difference.Added(1))
        val (added, removed, changed)   = diffs.groupNelByType
        assertTrue(
          added == Some(NonEmptyList.one(Difference.Added(1))),
          removed.isEmpty,
          changed.isEmpty,
        )
      }
      test("returns all None for empty input") {
        val (added, removed, changed) = Seq.empty[Difference[Int]].groupNelByType
        assertTrue(
          added.isEmpty,
          removed.isEmpty,
          changed.isEmpty,
        )
      }
    }
  }
}
