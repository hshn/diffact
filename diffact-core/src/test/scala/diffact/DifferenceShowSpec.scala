package diffact

import diffact.DifferenceShow.*
import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object DifferenceShowSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("DifferenceShow") {
    suiteAll("Difference.show") {
      test("Added") {
        assertTrue(
          Difference.Added(1).show == "+1"
        )
      }
      test("Removed") {
        assertTrue(
          Difference.Removed("alice").show == "-alice"
        )
      }
      test("Changed") {
        assertTrue(
          Difference.Changed(25, 26).show == "25 → 26"
        )
      }
    }
    suiteAll("Option[Difference].show") {
      test("Some") {
        val diff: Option[Difference[Int]] = Some(Difference.Changed(1, 2))
        assertTrue(
          diff.show == "1 → 2"
        )
      }
      test("None") {
        val diff: Option[Difference[Int]] = None
        assertTrue(
          diff.show == "(no change)"
        )
      }
    }
    suiteAll("Seq[Difference].show") {
      test("multiple differences") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Changed(3, 4),
        )
        assertTrue(
          diffs.show == "+1\n-2\n3 → 4"
        )
      }
      test("empty seq") {
        val diffs: Seq[Difference[Int]] = Seq.empty
        assertTrue(
          diffs.show == ""
        )
      }
      test("single difference") {
        val diffs: Seq[Difference[String]] = Seq(Difference.Added("alice"))
        assertTrue(
          diffs.show == "+alice"
        )
      }
    }
    suiteAll("custom Show instance") {
      given Show[Int] with {
        def show(value: Int): String = s"#$value"
      }

      test("uses custom Show for Difference") {
        assertTrue(
          Difference.Changed(1, 2).show == "#1 → #2",
          Difference.Added(3).show == "+#3",
          Difference.Removed(4).show == "-#4",
        )
      }
      test("propagates through Option[Difference]") {
        val some: Option[Difference[Int]] = Some(Difference.Changed(1, 2))
        val none: Option[Difference[Int]] = None
        assertTrue(
          some.show == "#1 → #2",
          none.show == "(no change)",
        )
      }
      test("propagates through Seq[Difference]") {
        val diffs: Seq[Difference[Int]] = Seq(Difference.Added(1), Difference.Removed(2))
        assertTrue(
          diffs.show == "+#1\n-#2"
        )
      }
    }
  }
}
