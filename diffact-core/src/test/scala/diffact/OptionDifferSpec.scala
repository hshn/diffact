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
        val sameOne  = Differ.diff(Option(one)).from(Option(one)).equals(None)
        val sameTwo  = Differ.diff(Option(two)).from(Option(two)).equals(None)
        val bothNone = Differ.diff(empty).from(empty).equals(None)
        assertTrue(sameOne, sameTwo, bothNone)
      }
    }
    suiteAll("given instance") {
      test("resolves OptionDiffer with concrete DiffResult type") {
        val differ: OptionDiffer[Int, Option[Difference[Int]]] = summon
        assertTrue(differ.diff(Some(1), Some(2)) == Some(Difference.Changed(1, 2)))
      }
    }
    suiteAll("added / removed / none") {
      test("added(Some)") {
        val differ = ValueDiffer[Int].toOption
        assertTrue(
          differ.added(Some(1)) == Some(Difference.Added(1))
        )
      }
      test("added(None)") {
        val differ  = ValueDiffer[Int].toOption
        val isEmpty = differ.added(None).equals(None)
        assertTrue(isEmpty)
      }
      test("removed") {
        val differ      = ValueDiffer[Int].toOption
        val noneIsEmpty = differ.removed(None).equals(None)
        assertTrue(
          differ.removed(Some(1)) == Some(Difference.Removed(1)),
          noneIsEmpty,
        )
      }
      test("none") {
        val differ  = ValueDiffer[Int].toOption
        val isEmpty = differ.none.equals(None)
        assertTrue(isEmpty)
      }
    }
    suiteAll("with MapDiffer") {
      val differ = summon[MapDiffer[String, Int]].toOption

      test("diff Some to Some") {
        assertTrue(
          differ.diff(Some(Map("a" -> 1)), Some(Map("a" -> 2))) == Seq("a" -> Difference.Changed(1, 2))
        )
      }
      test("diff Some to None") {
        assertTrue(
          differ.diff(Some(Map("a" -> 1)), None) == Seq("a" -> Difference.Removed(1))
        )
      }
      test("diff None to Some") {
        assertTrue(
          differ.diff(None, Some(Map("a" -> 1))) == Seq("a" -> Difference.Added(1))
        )
      }
      test("diff None to None") {
        val isEmpty = differ.diff(None, None).equals(Nil)
        assertTrue(isEmpty)
      }
    }
    suiteAll("with SeqDiffer") {
      val differ = summon[SeqDiffer[Int, Int]].toOption

      test("diff Some to Some") {
        assertTrue(
          differ.diff(Some(Seq(1, 2)), Some(Seq(1, 3))) == Seq(Difference.Changed(2, 3))
        )
      }
      test("diff Some to None") {
        assertTrue(
          differ.diff(Some(Seq(1)), None) == Seq(Difference.Removed(1))
        )
      }
      test("diff None to Some") {
        assertTrue(
          differ.diff(None, Some(Seq(1))) == Seq(Difference.Added(1))
        )
      }
      test("diff None to None") {
        val isEmpty = differ.diff(None, None).equals(Nil)
        assertTrue(isEmpty)
      }
    }
    suiteAll("with SetDiffer") {
      val differ = summon[SetDiffer[Int]].toOption

      test("diff Some to Some") {
        assertTrue(
          differ.diff(Some(Set(1, 2)), Some(Set(2, 3))) == Seq(Difference.Added(3), Difference.Removed(1))
        )
      }
      test("diff Some to None") {
        assertTrue(
          differ.diff(Some(Set(1)), None) == Seq(Difference.Removed(1))
        )
      }
      test("diff None to Some") {
        assertTrue(
          differ.diff(None, Some(Set(1))) == Seq(Difference.Added(1))
        )
      }
      test("diff None to None") {
        val isEmpty = differ.diff(None, None).equals(Nil)
        assertTrue(isEmpty)
      }
    }
  }

  case class Foo(bar: String)
}
