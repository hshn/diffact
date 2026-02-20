package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object SetDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("SetDiffer") {
    suiteAll("Differ.diff[Set[Int]]") {
      test("detects added elements") {
        assertTrue(
          Differ.diff(Set(1, 2, 3)).from(Set(1, 2)).toSet == Set(
            Difference.Added(3),
          ),
        )
      }
      test("detects removed elements") {
        assertTrue(
          Differ.diff(Set(1)).from(Set(1, 2, 3)).toSet == Set(
            Difference.Removed(2),
            Difference.Removed(3),
          ),
        )
      }
      test("detects additions and removals simultaneously") {
        assertTrue(
          Differ.diff(Set(2, 3)).from(Set(1, 2)).toSet == Set(
            Difference.Added(3),
            Difference.Removed(1),
          ),
        )
      }
      test("returns no difference for identical Sets") {
        assertTrue(
          Differ.diff(Set(1, 2)).from(Set(1, 2)).isEmpty,
        )
      }
      test("returns no difference for empty Sets") {
        assertTrue(
          Differ.diff(Set.empty[Int]).from(Set.empty[Int]).isEmpty,
        )
      }
    }
    suiteAll("added / removed / none") {
      test("added") {
        val differ = summon[SetDiffer[Int]]
        assertTrue(
          differ.added(Set(1, 2)).toSet == Set(Difference.Added(1), Difference.Added(2)),
          differ.added(Set.empty[Int]).isEmpty,
        )
      }
      test("removed") {
        val differ = summon[SetDiffer[Int]]
        assertTrue(
          differ.removed(Set(1, 2)).toSet == Set(Difference.Removed(1), Difference.Removed(2)),
          differ.removed(Set.empty[Int]).isEmpty,
        )
      }
      test("none") {
        val differ = summon[SetDiffer[Int]]
        assertTrue(
          differ.none.isEmpty,
        )
      }
    }
  }
}
