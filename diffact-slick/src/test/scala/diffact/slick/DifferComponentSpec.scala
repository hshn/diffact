package diffact.slick

import zio.*
import zio.test.*

import cats.implicits.*
import slick.jdbc.H2Profile

import diffact.Difference

object DifferComponentSpec extends SlickZIOSpec("test") {

  object TestProfile extends H2Profile with DifferComponent {
    object TestApi extends JdbcAPI with DifferApi
  }
  import TestProfile.TestApi.*

  override def spec = suiteAll("DifferComponent") {
    suiteAll("Difference[A].sync") {
      test("dispatches Added to add handler") {
        val diff: Difference[Int] = Difference.Added(1)
        run {
          diff
            .sync(
              add = d => DBIO.successful(s"added:${d.value}"),
              remove = d => DBIO.successful(s"removed:${d.value}"),
              change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
            )
            .map(result => assertTrue(result == "added:1"))
        }
      }
      test("dispatches Removed to remove handler") {
        val diff: Difference[Int] = Difference.Removed(2)
        run {
          diff
            .sync(
              add = d => DBIO.successful(s"added:${d.value}"),
              remove = d => DBIO.successful(s"removed:${d.value}"),
              change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
            )
            .map(result => assertTrue(result == "removed:2"))
        }
      }
      test("dispatches Changed to change handler") {
        val diff: Difference[Int] = Difference.Changed(1, 2)
        run {
          diff
            .sync(
              add = d => DBIO.successful(s"added:${d.value}"),
              remove = d => DBIO.successful(s"removed:${d.value}"),
              change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
            )
            .map(result => assertTrue(result == "changed:1->2"))
        }
      }
    }
    suiteAll("Difference[A].syncDiscard") {
      test("returns Unit") {
        val diff: Difference[Int] = Difference.Added(1)
        run {
          diff
            .syncDiscard(
              add = d => DBIO.successful(d.value),
              remove = d => DBIO.successful(d.value),
              change = d => DBIO.successful(d.oldValue),
            )
            .map(result => assertTrue(result == ()))
        }
      }
    }
    suiteAll("Option[Difference[A]].sync") {
      test("dispatches Some to correct handler") {
        val diff: Option[Difference[Int]] = Some(Difference.Added(1))
        run {
          diff
            .sync(
              add = d => DBIO.successful(s"added:${d.value}"),
              remove = d => DBIO.successful(s"removed:${d.value}"),
              change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
            )
            .map(result => assertTrue(result == "added:1"))
        }
      }
      test("returns Monoid.empty for None") {
        val diff: Option[Difference[Int]] = None
        run {
          diff
            .sync(
              add = d => DBIO.successful(s"added:${d.value}"),
              remove = d => DBIO.successful(s"removed:${d.value}"),
              change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
            )
            .map(result => assertTrue(result == ""))
        }
      }
    }
    suiteAll("Option[Difference[A]].syncDiscard") {
      test("returns Unit for None") {
        val diff: Option[Difference[Int]] = None
        run {
          diff
            .syncDiscard(
              add = d => DBIO.successful(d.value),
              remove = d => DBIO.successful(d.value),
              change = d => DBIO.successful(d.oldValue),
            )
            .map(result => assertTrue(result == ()))
        }
      }
    }
    suiteAll("Seq[Difference[A]].sync") {
      test("groups and dispatches differences") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Added(3),
          Difference.Changed(4, 5),
        )
        run {
          diffs
            .sync(
              add = nel => DBIO.successful(nel.map(d => s"added:${d.value}").toList),
              remove = nel => DBIO.successful(nel.map(d => s"removed:${d.value}").toList),
              change = nel => DBIO.successful(nel.map(d => s"changed:${d.oldValue}->${d.newValue}").toList),
            )
            .map(result => assertTrue(result == List("removed:2", "added:1", "added:3", "changed:4->5")))
        }
      }
      test("returns Monoid.empty for empty input") {
        val diffs: Seq[Difference[Int]] = Seq.empty
        run {
          diffs
            .sync(
              add = nel => DBIO.successful(nel.map(d => s"added:${d.value}").toList),
              remove = nel => DBIO.successful(nel.map(d => s"removed:${d.value}").toList),
              change = nel => DBIO.successful(nel.map(d => s"changed:${d.oldValue}->${d.newValue}").toList),
            )
            .map(result => assertTrue(result.isEmpty))
        }
      }
    }
    suiteAll("Seq[Difference[A]].syncDiscard") {
      test("returns Unit") {
        val diffs: Seq[Difference[Int]] = Seq(Difference.Added(1), Difference.Removed(2))
        run {
          diffs
            .syncDiscard(
              add = nel => DBIO.successful(nel.size),
              remove = nel => DBIO.successful(nel.size),
              change = nel => DBIO.successful(nel.size),
            )
            .map(result => assertTrue(result == ()))
        }
      }
    }
    suiteAll("Seq[Difference[A]].syncEach") {
      test("dispatches each difference individually in remove→add→change order") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Added(3),
          Difference.Changed(4, 5),
        )
        run {
          diffs
            .syncEach(
              add = d => DBIO.successful(List(s"added:${d.value}")),
              remove = d => DBIO.successful(List(s"removed:${d.value}")),
              change = d => DBIO.successful(List(s"changed:${d.oldValue}->${d.newValue}")),
            )
            .map(result => assertTrue(result == List("removed:2", "added:1", "added:3", "changed:4->5")))
        }
      }
      test("returns Monoid.empty for empty input") {
        val diffs: Seq[Difference[Int]] = Seq.empty
        run {
          diffs
            .syncEach(
              add = d => DBIO.successful(List(s"added:${d.value}")),
              remove = d => DBIO.successful(List(s"removed:${d.value}")),
              change = d => DBIO.successful(List(s"changed:${d.oldValue}->${d.newValue}")),
            )
            .map(result => assertTrue(result.isEmpty))
        }
      }
    }
    suiteAll("Seq[Difference[A]].syncEachDiscard") {
      test("returns Unit") {
        val diffs: Seq[Difference[Int]] = Seq(Difference.Added(1), Difference.Removed(2))
        run {
          diffs
            .syncEachDiscard(
              add = d => DBIO.successful(d.value),
              remove = d => DBIO.successful(d.value),
              change = d => DBIO.successful(d.oldValue),
            )
            .map(result => assertTrue(result == ()))
        }
      }
    }
  }
}
