package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object SeqDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("SeqDiffer") {
    suiteAll("Differ.diff[Seq[Int]]") {
      test("returns differences when values differ") {
        assertTrue(
          Differ.diff(Seq.empty[Int]).from(Seq(1)) == Seq(
            Difference.Removed(1)
          ),
          Differ.diff(Seq(2)).from(Nil) == Seq(
            Difference.Added(2)
          ),
          Differ.diff(Seq(1)).from(Seq(2)) == Seq(
            Difference.Changed(oldValue = 2, newValue = 1)
          ),
          Differ.diff(Seq(2)).from(Seq(1)) == Seq(
            Difference.Changed(oldValue = 1, newValue = 2)
          ),
          Differ.diff(Seq(1, 2)).from(Seq(2, 1)) == Seq(
            Difference.Changed(oldValue = 2, newValue = 1),
            Difference.Changed(oldValue = 1, newValue = 2),
          ),
          Differ.diff(Seq(1, 2, 3)).from(Seq(3, 2, 1, 4)) == Seq(
            Difference.Removed(4),
            Difference.Changed(oldValue = 3, newValue = 1),
            Difference.Changed(oldValue = 1, newValue = 3),
          ),
          Differ.diff(Seq(1, 2, 3, 4)).from(Seq(3, 2, 1)) == Seq(
            Difference.Added(4),
            Difference.Changed(oldValue = 3, newValue = 1),
            Difference.Changed(oldValue = 1, newValue = 3),
          ),
        )
      }
      test("returns no differences when values are equal") {
        assertTrue(
          Differ.diff(Seq()).from(Seq()).isEmpty,
          Differ.diff(Seq(1)).from(Seq(1)).isEmpty,
          Differ.diff(Seq(2, 1)).from(Seq(2, 1)).isEmpty,
        )
      }
    }
    suiteAll("added / removed / none") {
      test("added") {
        val differ = summon[SeqDiffer[Int, Int]]
        assertTrue(
          differ.added(Seq(1, 2)) == Seq(Difference.Added(1), Difference.Added(2)),
          differ.added(Nil).isEmpty,
        )
      }
      test("removed") {
        val differ = summon[SeqDiffer[Int, Int]]
        assertTrue(
          differ.removed(Seq(1, 2)) == Seq(Difference.Removed(1), Difference.Removed(2)),
          differ.removed(Nil).isEmpty,
        )
      }
      test("none") {
        val differ = summon[SeqDiffer[Int, Int]]
        assertTrue(
          differ.none.isEmpty
        )
      }
    }
    suiteAll("duplicate tracking keys") {
      test("throws exception when duplicate tracking keys exist") {
        import scala.util.Try

        given SeqDiffer[Baz, String] = Differ[Baz].trackBy(_.id).toSeq

        val duplicates = Seq(Baz(id = "b1", qux = "q1"), Baz(id = "b1", qux = "q2"))
        assertTrue(
          Try(Differ.diff(duplicates).from(Nil)).failed.get.isInstanceOf[IllegalArgumentException],
          Try(Differ.diff(Nil: Seq[Baz]).from(duplicates)).failed.get.isInstanceOf[IllegalArgumentException],
        )
      }
    }
  }

  case class Baz(id: String, qux: String)
}
