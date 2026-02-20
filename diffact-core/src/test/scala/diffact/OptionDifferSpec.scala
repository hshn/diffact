package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object OptionDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("OptionDiffer") {
    suiteAll("Differ.diff[Option[Foo]]") {
      val one   = Foo(bar = "1")
      val two   = Foo(bar = "2")
      val empty = Option.empty[Foo]

      test("returns difference when values differ") {
        assertTrue(
          Differ.diff(Option(one)).from(Option(two)) == Some(Difference.Changed(oldValue = two, newValue = one)),
          Differ.diff(Option(two)).from(Option(one)) == Some(Difference.Changed(oldValue = one, newValue = two)),
          Differ.diff(Option(one)).from(empty) == Some(Difference.Added(one)),
          Differ.diff(empty).from(Option(one)) == Some(Difference.Removed(one)),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff(Option(one)).from(Option(one)).isEmpty,
          Differ.diff(Option(two)).from(Option(two)).isEmpty,
          Differ.diff(empty).from(empty).isEmpty,
        )
      }
    }
    suiteAll("added / removed / none") {
      test("added(Some)") {
        val differ = summon[OptionDiffer[Int]]
        assertTrue(
          differ.added(Some(1)) == Some(Difference.Added(1))
        )
      }
      test("added(None)") {
        val differ = summon[OptionDiffer[Int]]
        assertTrue(
          differ.added(None).isEmpty
        )
      }
      test("removed") {
        val differ = summon[OptionDiffer[Int]]
        assertTrue(
          differ.removed(Some(1)) == Some(Difference.Removed(1)),
          differ.removed(None).isEmpty,
        )
      }
      test("none") {
        val differ = summon[OptionDiffer[Int]]
        assertTrue(
          differ.none.isEmpty
        )
      }
    }
  }

  case class Foo(bar: String)
}
