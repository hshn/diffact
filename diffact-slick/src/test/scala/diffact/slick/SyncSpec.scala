package diffact.slick

import zio.*
import zio.test.*

import cats.implicits.*
import slick.jdbc.H2Profile

import diffact.Difference

object SyncSpec extends SlickZIOSpec("sync-test") {

  object TestProfile extends H2Profile with DifferComponent {
    object TestApi extends JdbcAPI with DifferApi
  }
  import TestProfile.TestApi.*

  private val allHandlers = Sync[Int]
    .added(d => DBIO.successful(s"added:${d.value}"))
    .removed(d => DBIO.successful(s"removed:${d.value}"))
    .changed(d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"))

  override def spec = suiteAll("Sync") {

    suiteAll("apply(Difference[A])") {
      test("dispatches Added to added handler") {
        run {
          allHandlers(Difference.Added(1))
            .map(result => assertTrue(result == "added:1"))
        }
      }
      test("dispatches Removed to removed handler") {
        run {
          allHandlers(Difference.Removed(2))
            .map(result => assertTrue(result == "removed:2"))
        }
      }
      test("dispatches Changed to changed handler") {
        run {
          allHandlers(Difference.Changed(1, 2))
            .map(result => assertTrue(result == "changed:1->2"))
        }
      }
    }

    suiteAll("void") {
      test("unifies distinct handler return types to Unit") {
        val handler = Sync[Int]
          .added(d => DBIO.successful(d.value))          // RA = Int
          .removed(d => DBIO.successful(s"${d.value}"))  // RR = String
          .changed(d => DBIO.successful(d.oldValue > 0)) // RC = Boolean
          .void                                          // Sync[Int, Unit, Unit, Unit]
        run {
          handler(Difference.Added(1))
            .map(result => assertTrue(result == ()))
        }
      }
      test("enables selective handlers") {
        val handler = Sync[Int]
          .changed(d => DBIO.successful(d.newValue))
          .void
        run {
          handler(Difference.Changed(1, 2))
            .map(result => assertTrue(result == ()))
        }
      }
    }

    suiteAll("unhandled cases") {
      test("fails with message identifying Added") {
        val handler = Sync[Int].changed(d => DBIO.successful(d.newValue)).void
        for {
          error <- run { handler(Difference.Added(1): Difference[Int]) }.flip
        } yield assertTrue(error.getMessage == "No Sync handler for Added")
      }
      test("fails with message identifying Removed") {
        val handler = Sync[Int].changed(d => DBIO.successful(d.newValue)).void
        for {
          error <- run { handler(Difference.Removed(1): Difference[Int]) }.flip
        } yield assertTrue(error.getMessage == "No Sync handler for Removed")
      }
      test("fails with message identifying Changed") {
        val handler = Sync[Int].added(d => DBIO.successful(d.value)).void
        for {
          error <- run { handler(Difference.Changed(1, 2): Difference[Int]) }.flip
        } yield assertTrue(error.getMessage == "No Sync handler for Changed")
      }
    }

    suiteAll("builder replaces handler") {
      test("later added handler overrides earlier one") {
        val handler = Sync[Int]
          .added(d => DBIO.successful(s"first:${d.value}"))
          .removed(d => DBIO.successful(s"removed:${d.value}"))
          .changed(d => DBIO.successful(s"changed"))
          .added(d => DBIO.successful(s"second:${d.value}"))

        for {
          result <- run { handler(Difference.Added(1)) }
        } yield {
          assertTrue(result == "second:1")
        }
      }
    }

    suiteAll("void with Tracked.Replaced on selective handler") {
      test("dispatches Replaced through voided selective handler with distinct types") {
        val handler = Sync[Int]
          .added(d => DBIO.successful(d.value.toLong))  // RA = Long
          .removed(d => DBIO.successful(s"${d.value}")) // RR = String
          .void
        run {
          handler(Difference.Tracked.Replaced(1, 2))
            .map(result => assertTrue(result == ()))
        }
      }
    }

    suiteAll("apply(Option[Difference[A]])") {
      test("dispatches Some to correct handler") {
        run {
          allHandlers(Some(Difference.Added(1)): Option[Difference[Int]])
            .map(result => assertTrue(result == "added:1"))
        }
      }
      test("returns Monoid.empty for None") {
        run {
          allHandlers(None: Option[Difference[Int]])
            .map(result => assertTrue(result == ""))
        }
      }
    }

    suiteAll("apply(Difference.Tracked[A])") {
      test("returns Monoid.empty for Unchanged") {
        run {
          allHandlers(Difference.Tracked.Unchanged: Difference.Tracked[Int])
            .map(result => assertTrue(result == ""))
        }
      }
      test("dispatches Tracked.Added") {
        run {
          allHandlers(Difference.Tracked.Added(1))
            .map(result => assertTrue(result == "added:1"))
        }
      }
      test("dispatches Tracked.Removed") {
        run {
          allHandlers(Difference.Tracked.Removed(1))
            .map(result => assertTrue(result == "removed:1"))
        }
      }
      test("dispatches Tracked.Changed") {
        run {
          allHandlers(Difference.Tracked.Changed(1, 2))
            .map(result => assertTrue(result == "changed:1->2"))
        }
      }
      test("dispatches Tracked.Replaced as remove then add") {
        run {
          allHandlers(Difference.Tracked.Replaced(1, 2))
            .map(result => assertTrue(result == "removed:1added:2"))
        }
      }
    }

    suiteAll("apply(Seq[Difference[A]])") {
      test("groups and dispatches in remove→add→change order") {
        val diffs: Seq[Difference[Int]] = Seq(
          Difference.Added(1),
          Difference.Removed(2),
          Difference.Added(3),
          Difference.Changed(4, 5),
        )
        val handler = Sync[Int]
          .added(d => DBIO.successful(List(s"added:${d.value}")))
          .removed(d => DBIO.successful(List(s"removed:${d.value}")))
          .changed(d => DBIO.successful(List(s"changed:${d.oldValue}->${d.newValue}")))
        run {
          handler(diffs)
            .map(result => assertTrue(result == List("removed:2", "added:1", "added:3", "changed:4->5")))
        }
      }
      test("returns Monoid.empty for empty seq") {
        val handler = Sync[Int]
          .added(d => DBIO.successful(List(s"added:${d.value}")))
          .removed(d => DBIO.successful(List(s"removed:${d.value}")))
          .changed(d => DBIO.successful(List(s"changed")))
        run {
          handler(Seq.empty[Difference[Int]])
            .map(result => assertTrue(result.isEmpty))
        }
      }
    }
  }
}
