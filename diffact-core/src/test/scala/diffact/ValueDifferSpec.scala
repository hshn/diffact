package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object ValueDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("ValueDiffer") {
    suiteAll("Differ.diff(old, new)") {
      test("returns difference when values differ") {
        assertTrue(
          Differ.diff(oldValue = 1, newValue = 2) == Some(Difference.Changed(oldValue = 1, newValue = 2)),
          Differ.diff(oldValue = 2, newValue = 1) == Some(Difference.Changed(oldValue = 2, newValue = 1)),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff(oldValue = 1, newValue = 1).isEmpty,
        )
      }
    }
    suiteAll("Differ.diff(x).to(y)") {
      test("returns difference when values differ") {
        assertTrue(
          Differ.diff(1).to(2) == Some(Difference.Changed(oldValue = 1, newValue = 2)),
          Differ.diff(2).to(1) == Some(Difference.Changed(oldValue = 2, newValue = 1)),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff(1).to(1).isEmpty,
        )
      }
    }
    suiteAll("Differ.diff(x).from(y)") {
      test("returns difference when values differ") {
        assertTrue(
          Differ.diff(1).from(2) == Some(Difference.Changed(oldValue = 2, newValue = 1)),
          Differ.diff(2).from(1) == Some(Difference.Changed(oldValue = 1, newValue = 2)),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff(1).from(1).isEmpty,
          Differ.diff(2).from(2).isEmpty,
        )
      }
    }
    suiteAll("Differ.diff[String]") {
      test("returns difference when values differ") {
        assertTrue(
          Differ.diff("1").from("2") == Some(Difference.Changed(oldValue = "2", newValue = "1")),
          Differ.diff("2").from("1") == Some(Difference.Changed(oldValue = "1", newValue = "2")),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff("1").from("1").isEmpty,
          Differ.diff("2").from("2").isEmpty,
        )
      }
    }
    suiteAll("Differ.diff[Foo]") {
      val one = Foo(bar = "1")
      val two = Foo(bar = "2")

      test("returns difference when values differ") {
        assertTrue(
          Differ.diff(one).from(two) == Some(Difference.Changed(oldValue = two, newValue = one)),
          Differ.diff(two).from(one) == Some(Difference.Changed(oldValue = one, newValue = two)),
        )
      }
      test("returns no difference when values are equal") {
        assertTrue(
          Differ.diff(one).from(one).isEmpty,
          Differ.diff(two).from(two).isEmpty,
        )
      }
    }
    suiteAll("added / removed / none") {
      test("added") {
        assertTrue(
          Differ[Int].added(1) == Some(Difference.Added(1)),
        )
      }
      test("removed") {
        assertTrue(
          Differ[Int].removed(1) == Some(Difference.Removed(1)),
        )
      }
      test("none") {
        assertTrue(
          Differ[Int].none.isEmpty,
        )
      }
    }
    suiteAll("contramap") {
      case class Wrapper(value: Int)

      test("compares by projected value and returns result in original type") {
        val differ: ValueDiffer[Wrapper] = Differ[Int].contramap(_.value)
        assertTrue(
          differ.diff(Wrapper(1), Wrapper(2)) == Some(Difference.Changed(Wrapper(1), Wrapper(2))),
          differ.diff(Wrapper(1), Wrapper(1)).isEmpty,
        )
      }
      test("added") {
        val differ: ValueDiffer[Wrapper] = Differ[Int].contramap(_.value)
        assertTrue(
          differ.added(Wrapper(1)) == Some(Difference.Added(Wrapper(1))),
        )
      }
      test("removed") {
        val differ: ValueDiffer[Wrapper] = Differ[Int].contramap(_.value)
        assertTrue(
          differ.removed(Wrapper(1)) == Some(Difference.Removed(Wrapper(1))),
        )
      }
      test("none") {
        val differ: ValueDiffer[Wrapper] = Differ[Int].contramap(_.value)
        assertTrue(
          differ.none.isEmpty,
        )
      }
      test("identity projection returns same result as default") {
        val differ: ValueDiffer[Int] = Differ[Int].contramap(identity)
        assertTrue(
          differ.diff(1, 2) == Some(Difference.Changed(1, 2)),
          differ.diff(1, 1).isEmpty,
        )
      }
      test("works with SeqDiffer") {
        case class Item(id: String, name: String)
        given ValueDiffer[Item] = Differ[String].contramap(_.name)
        given SeqDiffer[Item, String] = Differ[Item].trackBy(_.id).toSeq

        val oldItems = Seq(Item("1", "alice"), Item("2", "bob"))
        val newItems = Seq(Item("1", "alice"), Item("2", "BOB"))

        assertTrue(
          Differ.diff(newItems).from(oldItems) == Seq(
            Difference.Changed(oldValue = Item("2", "bob"), newValue = Item("2", "BOB")),
          ),
        )
      }
    }
  }

  case class Foo(bar: String)
}
