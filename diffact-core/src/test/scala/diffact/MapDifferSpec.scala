package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object MapDifferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("MapDiffer") {
    suiteAll("Differ.diff[Map[String, Int]]") {
      test("detects added entries") {
        assertTrue(
          Differ.diff(Map("a" -> 1, "b" -> 2)).from(Map("a" -> 1)).toSet == Set(
            Difference.Added(2)
          )
        )
      }
      test("detects removed entries") {
        assertTrue(
          Differ.diff(Map("a" -> 1)).from(Map("a" -> 1, "b" -> 2)).toSet == Set(
            Difference.Removed(2)
          )
        )
      }
      test("detects changed values") {
        assertTrue(
          Differ.diff(Map("a" -> 2)).from(Map("a" -> 1)).toSet == Set(
            Difference.Changed(oldValue = 1, newValue = 2)
          )
        )
      }
      test("detects additions, removals, and changes simultaneously") {
        assertTrue(
          Differ.diff(Map("a" -> 10, "c" -> 3)).from(Map("a" -> 1, "b" -> 2)).toSet == Set(
            Difference.Added(3),
            Difference.Removed(2),
            Difference.Changed(oldValue = 1, newValue = 10),
          )
        )
      }
      test("returns no difference for identical Maps") {
        assertTrue(
          Differ.diff(Map("a" -> 1, "b" -> 2)).from(Map("a" -> 1, "b" -> 2)).isEmpty
        )
      }
      test("returns no difference for empty Maps") {
        assertTrue(
          Differ.diff(Map.empty[String, Int]).from(Map.empty[String, Int]).isEmpty
        )
      }
    }
    suiteAll("added / removed / none") {
      test("added") {
        val differ = summon[MapDiffer[String, Int]]
        assertTrue(
          differ.added(Map("a" -> 1, "b" -> 2)).toSet == Set(Difference.Added(1), Difference.Added(2)),
          differ.added(Map.empty[String, Int]).isEmpty,
        )
      }
      test("removed") {
        val differ = summon[MapDiffer[String, Int]]
        assertTrue(
          differ.removed(Map("a" -> 1, "b" -> 2)).toSet == Set(Difference.Removed(1), Difference.Removed(2)),
          differ.removed(Map.empty[String, Int]).isEmpty,
        )
      }
      test("none") {
        val differ = summon[MapDiffer[String, Int]]
        assertTrue(
          differ.none.isEmpty
        )
      }
    }
  }
}
