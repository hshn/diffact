package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object TrackedValueOptionDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("OptionDiffer with TrackedValueDiffer") {
    val differ: OptionDiffer[Plan, Difference.Tracked[Plan]] = ValueDiffer[Plan].trackBy(_.id).toOption

    test("DiffResult is statically typed as Difference.Tracked[Plan]") {
      val diff: Difference.Tracked[Plan] = differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Pro")))
      assertTrue(
        diff == Difference.Tracked.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
      )
    }
    test("returns Changed when values differ with same identity") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Pro"))) ==
          Difference.Tracked.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
      )
    }
    test("returns Unchanged when values are equal with same identity") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Basic"))) == Difference.Tracked.Unchanged
      )
    }
    test("returns Replaced when identity differs") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p2", "Enterprise"))) ==
          Difference.Tracked.Replaced(Plan("p1", "Basic"), Plan("p2", "Enterprise"))
      )
    }
    test("returns Removed for Some to None") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), None) ==
          Difference.Tracked.Removed(Plan("p1", "Basic"))
      )
    }
    test("returns Added for None to Some") {
      assertTrue(
        differ.diff(None, Some(Plan("p1", "Basic"))) ==
          Difference.Tracked.Added(Plan("p1", "Basic"))
      )
    }
    test("returns Unchanged for None to None") {
      assertTrue(
        differ.diff(None, None) == Difference.Tracked.Unchanged
      )
    }
    test("added(Some)") {
      assertTrue(
        differ.added(Some(Plan("p1", "Basic"))) ==
          Difference.Tracked.Added(Plan("p1", "Basic"))
      )
    }
    test("added(None)") {
      assertTrue(
        differ.added(None) == Difference.Tracked.Unchanged
      )
    }
    test("removed(Some)") {
      assertTrue(
        differ.removed(Some(Plan("p1", "Basic"))) ==
          Difference.Tracked.Removed(Plan("p1", "Basic"))
      )
    }
    test("removed(None)") {
      assertTrue(
        differ.removed(None) == Difference.Tracked.Unchanged
      )
    }
    test("none") {
      assertTrue(
        differ.none == Difference.Tracked.Unchanged
      )
    }
  }

  case class Plan(id: String, name: String)
}
