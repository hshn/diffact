package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object TrackedOptionDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("TrackedOptionDiffer") {
    val differ: TrackedOptionDiffer[Plan, String] = Differ[Plan].trackBy(_.id).toOption

    test("returns Changed when values differ with same identity") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Pro"))) == Seq(
          Difference.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
        )
      )
    }
    test("returns no difference when values are equal with same identity") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Basic"))).isEmpty
      )
    }
    test("returns Removed and Added when identity differs") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p2", "Enterprise"))) == Seq(
          Difference.Removed(Plan("p1", "Basic")),
          Difference.Added(Plan("p2", "Enterprise")),
        )
      )
    }
    test("returns Removed for Some to None") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), None) == Seq(
          Difference.Removed(Plan("p1", "Basic"))
        )
      )
    }
    test("returns Added for None to Some") {
      assertTrue(
        differ.diff(None, Some(Plan("p1", "Basic"))) == Seq(
          Difference.Added(Plan("p1", "Basic"))
        )
      )
    }
    test("returns no difference for None to None") {
      assertTrue(
        differ.diff(None, None).isEmpty
      )
    }
    test("added(Some)") {
      assertTrue(
        differ.added(Some(Plan("p1", "Basic"))) == Seq(Difference.Added(Plan("p1", "Basic")))
      )
    }
    test("added(None)") {
      assertTrue(
        differ.added(None).isEmpty
      )
    }
    test("removed(Some)") {
      assertTrue(
        differ.removed(Some(Plan("p1", "Basic"))) == Seq(Difference.Removed(Plan("p1", "Basic")))
      )
    }
    test("removed(None)") {
      assertTrue(
        differ.removed(None).isEmpty
      )
    }
    test("none") {
      assertTrue(
        differ.none.isEmpty
      )
    }
  }

  case class Plan(id: String, name: String)
}
