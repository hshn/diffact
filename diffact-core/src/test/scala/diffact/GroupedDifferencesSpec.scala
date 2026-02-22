package diffact

import diffact.GroupedDifferences.*
import zio.Scope
import zio.test.*

object GroupedDifferencesSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("GroupedDifferences") {
    suiteAll("groupByType") {
      test("groups differences by type") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Changed(3, 4),
          Difference.Added(5),
        )
        assertTrue(
          diffs.groupByType == GroupedDifferences(
            added = Seq(Difference.Added(1), Difference.Added(5)),
            removed = Seq(Difference.Removed(2)),
            changed = Seq(Difference.Changed(3, 4)),
          )
        )
      }
      test("returns empty seqs for empty input") {
        assertTrue(
          Seq.empty[Difference[Int]].groupByType == GroupedDifferences(
            added = Seq.empty,
            removed = Seq.empty,
            changed = Seq.empty,
          )
        )
      }
    }
  }
}
