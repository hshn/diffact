package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object TrackedValueDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("TrackedValueDiffer") {
    suiteAll("diff") {
      val differ: TrackedValueDiffer[Plan, String] = ValueDiffer[Plan].trackBy(_.id)

      test("returns Changed when values differ with same identity") {
        assertTrue(
          differ.diff(Plan("p1", "Basic"), Plan("p1", "Pro")) ==
            Difference.Tracked.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
        )
      }
      test("returns Unchanged when values are equal with same identity") {
        assertTrue(
          differ.diff(Plan("p1", "Basic"), Plan("p1", "Basic")) == Difference.Tracked.Unchanged
        )
      }
      test("returns Replaced when identity differs") {
        assertTrue(
          differ.diff(Plan("p1", "Basic"), Plan("p2", "Enterprise")) ==
            Difference.Tracked.Replaced(Plan("p1", "Basic"), Plan("p2", "Enterprise"))
        )
      }
      test("added") {
        assertTrue(
          differ.added(Plan("p1", "Basic")) == Difference.Tracked.Added(Plan("p1", "Basic"))
        )
      }
      test("removed") {
        assertTrue(
          differ.removed(Plan("p1", "Basic")) == Difference.Tracked.Removed(Plan("p1", "Basic"))
        )
      }
      test("none") {
        assertTrue(
          differ.none == Difference.Tracked.Unchanged
        )
      }
      test("converts to SeqDiffer via toSeq") {
        val seqDiffer: SeqDiffer[Plan, String] = differ.toSeq

        val oldPlans = Seq(Plan("p1", "Basic"), Plan("p2", "Pro"))
        val newPlans = Seq(Plan("p1", "Basic"), Plan("p3", "Enterprise"))

        assertTrue(
          seqDiffer.diff(oldPlans, newPlans) == Seq(
            Difference.Added(Plan("p3", "Enterprise")),
            Difference.Removed(Plan("p2", "Pro")),
          )
        )
      }
    }
    suiteAll("toDifferences") {
      test("Unchanged") {
        assertTrue(
          (Difference.Tracked.Unchanged: Difference.Tracked[Int]).toDifferences == Nil
        )
      }
      test("Added") {
        assertTrue(
          Difference.Tracked.Added(1).toDifferences == Seq(Difference.Added(1))
        )
      }
      test("Replaced") {
        assertTrue(
          Difference.Tracked.Replaced(1, 2).toDifferences == Seq(Difference.Removed(1), Difference.Added(2))
        )
      }
    }
    suiteAll("fluent API") {
      given TrackedValueDiffer[Plan, String] = ValueDiffer[Plan].trackBy(_.id)

      test("resolves given instance via Differ.diff(x).from(y)") {
        assertTrue(
          Differ.diff(Plan("p1", "Pro")).from(Plan("p1", "Basic")) ==
            Difference.Tracked.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro")),
          Differ.diff(Plan("p2", "Enterprise")).from(Plan("p1", "Basic")) ==
            Difference.Tracked.Replaced(Plan("p1", "Basic"), Plan("p2", "Enterprise")),
        )
      }
    }
  }

  case class Plan(id: String, name: String)
}
