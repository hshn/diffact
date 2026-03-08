package diffact

import zio.Scope
import zio.test.Gen
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.check
import zio.test.assertTrue

object DifferPropertySpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Differ properties") {
    suiteAll("ValueDiffer") {
      val gen = Gen.int(-1000, 1000)

      test("reflexivity: diff(a, a) == none") {
        check(gen) { a =>
          assertTrue(ValueDiffer[Int].diff(a, a).isEmpty)
        }
      }
      test("changed means different: diff(a, b) != none implies a != b") {
        check(gen, gen) { (a, b) =>
          val result = ValueDiffer[Int].diff(a, b)
          assertTrue(result.nonEmpty == (a != b))
        }
      }
      test("added is not none") {
        check(gen) { a =>
          assertTrue(ValueDiffer[Int].added(a).nonEmpty)
        }
      }
      test("removed is not none") {
        check(gen) { a =>
          assertTrue(ValueDiffer[Int].removed(a).nonEmpty)
        }
      }
    }
    suiteAll("SeqDiffer") {
      val gen = Gen.listOfBounded(0, 10)(Gen.int(-100, 100)).map(_.toSeq)

      test("reflexivity: diff(s, s).isEmpty") {
        check(gen) { s =>
          val differ = summon[SeqDiffer[Int, Int]]
          assertTrue(differ.diff(s, s).isEmpty)
        }
      }
    }
    suiteAll("MapDiffer") {
      val gen = Gen.mapOfBounded(0, 10)(Gen.string, Gen.int(-100, 100))

      test("reflexivity: diff(m, m).isEmpty") {
        check(gen) { m =>
          val differ = summon[MapDiffer[String, Int]]
          assertTrue(differ.diff(m, m).isEmpty)
        }
      }
    }
    suiteAll("SetDiffer") {
      val gen = Gen.setOfBounded(0, 10)(Gen.int(-100, 100))

      test("reflexivity: diff(s, s).isEmpty") {
        check(gen) { s =>
          val differ = summon[SetDiffer[Int]]
          assertTrue(differ.diff(s, s).isEmpty)
        }
      }
    }
    suiteAll("Difference#map") {
      val gen = Gen.int(-1000, 1000)

      test("Changed.map(identity) == differ.diff(old, new)") {
        check(gen, gen) { (a, b) =>
          val changed: Difference[Int] = Difference.Changed(a, b)
          assertTrue(changed.map(identity) == ValueDiffer[Int].diff(a, b))
        }
      }
      test("Added.map(identity) == differ.added(value)") {
        check(gen) { a =>
          val added: Difference[Int] = Difference.Added(a)
          assertTrue(added.map(identity) == ValueDiffer[Int].added(a))
        }
      }
      test("Removed.map(identity) == differ.removed(value)") {
        check(gen) { a =>
          val removed: Difference[Int] = Difference.Removed(a)
          assertTrue(removed.map(identity) == ValueDiffer[Int].removed(a))
        }
      }
    }
  }
}
