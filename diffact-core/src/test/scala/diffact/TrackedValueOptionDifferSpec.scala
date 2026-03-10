package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object TrackedValueOptionDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("OptionDiffer with TrackedValueDiffer") {
    val differ: OptionDiffer[Plan, Seq[Difference[Plan]]] = ValueDiffer[Plan].trackBy(_.id).toOption

    test("DiffResult is statically typed as Seq[Difference[Plan]]") {
      val diffs: Seq[Difference[Plan]] = differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Pro")))
      assertTrue(diffs.headOption == Some(Difference.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))))
    }
    test("returns Changed when values differ with same identity") {
      assertTrue(
        differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Pro"))) == Seq(
          Difference.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
        )
      )
    }
    test("returns no difference when values are equal with same identity") {
      val isEmpty = differ.diff(Some(Plan("p1", "Basic")), Some(Plan("p1", "Basic"))).equals(Nil)
      assertTrue(isEmpty)
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
      val isEmpty = differ.diff(None, None).equals(Nil)
      assertTrue(isEmpty)
    }
    test("added(Some)") {
      assertTrue(
        differ.added(Some(Plan("p1", "Basic"))) == Seq(Difference.Added(Plan("p1", "Basic")))
      )
    }
    test("added(None)") {
      val isEmpty = differ.added(None).equals(Nil)
      assertTrue(isEmpty)
    }
    test("removed(Some)") {
      assertTrue(
        differ.removed(Some(Plan("p1", "Basic"))) == Seq(Difference.Removed(Plan("p1", "Basic")))
      )
    }
    test("removed(None)") {
      val isEmpty = differ.removed(None).equals(Nil)
      assertTrue(isEmpty)
    }
    test("none") {
      val isEmpty = differ.none.equals(Nil)
      assertTrue(isEmpty)
    }
  }

  case class Plan(id: String, name: String)
}
