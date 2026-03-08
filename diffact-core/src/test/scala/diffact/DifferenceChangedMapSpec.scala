package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object DifferenceChangedMapSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Difference#map") {
    suiteAll("with TrackedValueDiffer") {
      given TrackedValueDiffer[Plan, String] = ValueDiffer[Plan].trackBy(_.id)

      test("returns Changed when identity is the same") {
        val diff = Difference.Changed(
          oldValue = Parent("x", Plan("p1", "Basic")),
          newValue = Parent("x", Plan("p1", "Pro")),
        )
        assertTrue(
          diff.map(_.plan) == Seq(
            Difference.Changed(oldValue = Plan("p1", "Basic"), newValue = Plan("p1", "Pro"))
          )
        )
      }
      test("returns Removed and Added when identity differs") {
        val diff = Difference.Changed(
          oldValue = Parent("x", Plan("p1", "Basic")),
          newValue = Parent("x", Plan("p2", "Enterprise")),
        )
        assertTrue(
          diff.map(_.plan) == Seq(
            Difference.Removed(Plan("p1", "Basic")),
            Difference.Added(Plan("p2", "Enterprise")),
          )
        )
      }
    }
    suiteAll("with SeqDiffer") {
      test("returns differences when values differ") {
        val diff = Difference.Changed(
          oldValue = Foo(
            bar = "1",
            baz = Seq(
              Baz(id = "b1", qux = "q1"),
              Baz(id = "b2", qux = "q2"),
              Baz(id = "b3", qux = "q3"),
            ),
          ),
          newValue = Foo(
            bar = "1",
            baz = Seq(
              Baz(id = "b1", qux = "q1"),
              Baz(id = "b2", qux = "q2222"),
            ),
          ),
        )

        assertTrue(
          diff.map(_.baz) == Seq(
            Difference.Removed(Baz(id = "b3", qux = "q3")),
            Difference.Changed(oldValue = Baz(id = "b2", qux = "q2"), newValue = Baz(id = "b2", qux = "q2222")),
          )
        )
      }
      test("returns differences when values differ (TrackById)") {
        given SeqDiffer[Baz, String] = ValueDiffer[Baz].trackBy(_.id).toSeq

        val diff = Difference.Changed(
          oldValue = Foo(
            bar = "1",
            baz = Seq(
              Baz(id = "b1", qux = "q1"),
              Baz(id = "b2", qux = "q2"),
              Baz(id = "b3", qux = "q3"),
            ),
          ),
          newValue = Foo(
            bar = "1",
            baz = Seq(
              Baz(id = "b2", qux = "q2222"),
              Baz(id = "b1", qux = "q1"),
            ),
          ),
        )

        assertTrue(
          diff.map(_.baz) == Seq(
            Difference.Removed(Baz(id = "b3", qux = "q3")),
            Difference.Changed(oldValue = Baz(id = "b2", qux = "q2"), newValue = Baz(id = "b2", qux = "q2222")),
          )
        )
      }
    }
    suiteAll("Added#map") {
      test("delegates to differ.added") {
        val diff: Difference[Parent] = Difference.Added(Parent("x", Plan("p1", "Basic")))
        assertTrue(
          diff.map(_.plan) == Some(Difference.Added(Plan("p1", "Basic")))
        )
      }
      test("with SeqDiffer") {
        val diff: Difference[Foo] = Difference.Added(
          Foo(bar = "1", baz = Seq(Baz(id = "b1", qux = "q1"), Baz(id = "b2", qux = "q2")))
        )
        assertTrue(
          diff.map(_.baz) == Seq(Difference.Added(Baz(id = "b1", qux = "q1")), Difference.Added(Baz(id = "b2", qux = "q2")))
        )
      }
    }
    suiteAll("Removed#map") {
      test("delegates to differ.removed") {
        val diff: Difference[Parent] = Difference.Removed(Parent("x", Plan("p1", "Basic")))
        assertTrue(
          diff.map(_.plan) == Some(Difference.Removed(Plan("p1", "Basic")))
        )
      }
      test("with SeqDiffer") {
        val diff: Difference[Foo] = Difference.Removed(
          Foo(bar = "1", baz = Seq(Baz(id = "b1", qux = "q1"), Baz(id = "b2", qux = "q2")))
        )
        assertTrue(
          diff.map(_.baz) == Seq(Difference.Removed(Baz(id = "b1", qux = "q1")), Difference.Removed(Baz(id = "b2", qux = "q2")))
        )
      }
    }
  }

  case class Parent(name: String, plan: Plan)
  case class Foo(bar: String, baz: Seq[Baz] = Nil)
  case class Baz(id: String, qux: String)
  case class Plan(id: String, name: String)
}
