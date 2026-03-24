package diffact.slick

import zio.*
import zio.test.*

import cats.implicits.*
import slick.jdbc.H2Profile

import diffact.Difference

object SyncBatchSpec extends SlickZIOSpec("sync-batch-test") {

  object TestProfile extends H2Profile with DifferComponent {
    object TestApi extends JdbcAPI with DifferApi
  }
  import TestProfile.TestApi.*

  override def spec = suiteAll("SyncBatch") {

    val nelHandlers = Sync
      .batchNel[Int]
      .added(nel => DBIO.successful(nel.map(d => s"added:${d.value}").toList))
      .removed(nel => DBIO.successful(nel.map(d => s"removed:${d.value}").toList))
      .changed(nel => DBIO.successful(nel.map(d => s"changed:${d.oldValue}->${d.newValue}").toList))

    suiteAll("batchNel") {

      suiteAll("apply(Seq[Difference[A]])") {
        test("groups and dispatches in remove→add→change order") {
          val diffs: Seq[Difference[Int]] = Seq(
            Difference.Added(1),
            Difference.Removed(2),
            Difference.Added(3),
            Difference.Changed(4, 5),
          )
          run {
            nelHandlers(diffs)
              .map(result => assertTrue(result == List("removed:2", "added:1", "added:3", "changed:4->5")))
          }
        }
        test("returns Monoid.empty for empty seq") {
          run {
            nelHandlers(Seq.empty[Difference[Int]])
              .map(result => assertTrue(result.isEmpty))
          }
        }
      }

      suiteAll("void") {
        test("unifies distinct handler return types to Unit") {
          val handler = Sync
            .batchNel[Int]
            .added(nel => DBIO.successful(nel.size))         // RA = Int
            .removed(nel => DBIO.successful(s"${nel.size}")) // RR = String
            .changed(nel => DBIO.successful(nel.size > 0))   // RC = Boolean
            .void
          run {
            handler(Seq(Difference.Added(1)))
              .map(result => assertTrue(result == ()))
          }
        }
        test("enables selective handlers") {
          val handler = Sync
            .batchNel[Int]
            .changed(nel => DBIO.successful(nel.size))
            .void
          run {
            handler(Seq(Difference.Changed(1, 2)))
              .map(result => assertTrue(result == ()))
          }
        }
      }

      suiteAll("unhandled cases") {
        test("fails with message identifying Added") {
          val handler = Sync.batchNel[Int].changed(nel => DBIO.successful(nel.size)).void
          for {
            error <- run { handler(Seq(Difference.Added(1))) }.flip
          } yield assertTrue(error.getMessage == "No SyncBatch handler for Added")
        }
        test("fails with message identifying Removed") {
          val handler = Sync.batchNel[Int].changed(nel => DBIO.successful(nel.size)).void
          for {
            error <- run { handler(Seq(Difference.Removed(1))) }.flip
          } yield assertTrue(error.getMessage == "No SyncBatch handler for Removed")
        }
        test("fails with message identifying Changed") {
          val handler = Sync.batchNel[Int].added(nel => DBIO.successful(nel.size)).void
          for {
            error <- run { handler(Seq(Difference.Changed(1, 2))) }.flip
          } yield assertTrue(error.getMessage == "No SyncBatch handler for Changed")
        }
      }

      suiteAll("builder replaces handler") {
        test("later added handler overrides earlier one") {
          val handler = Sync
            .batchNel[Int]
            .added(nel => DBIO.successful(nel.map(d => s"first:${d.value}").toList))
            .removed(nel => DBIO.successful(nel.map(d => s"removed:${d.value}").toList))
            .changed(nel => DBIO.successful(List("changed")))
            .added(nel => DBIO.successful(nel.map(d => s"second:${d.value}").toList))

          for {
            result <- run { handler(Seq(Difference.Added(1))) }
          } yield assertTrue(result == List("second:1"))
        }
      }
    }

    suiteAll("batchSeq") {

      val seqHandlers = Sync
        .batchSeq[Int]
        .added(ds => DBIO.successful(ds.map(d => s"added:${d.value}").toList))
        .removed(ds => DBIO.successful(ds.map(d => s"removed:${d.value}").toList))
        .changed(ds => DBIO.successful(ds.map(d => s"changed:${d.oldValue}->${d.newValue}").toList))

      suiteAll("apply(Seq[Difference[A]])") {
        test("groups and dispatches in remove→add→change order") {
          val diffs: Seq[Difference[Int]] = Seq(
            Difference.Added(1),
            Difference.Removed(2),
            Difference.Added(3),
            Difference.Changed(4, 5),
          )
          run {
            seqHandlers(diffs)
              .map(result => assertTrue(result == List("removed:2", "added:1", "added:3", "changed:4->5")))
          }
        }
        test("returns Monoid.empty for empty seq") {
          run {
            seqHandlers(Seq.empty[Difference[Int]])
              .map(result => assertTrue(result.isEmpty))
          }
        }
      }

      suiteAll("void") {
        test("unifies distinct handler return types to Unit") {
          val handler = Sync
            .batchSeq[Int]
            .added(ds => DBIO.successful(ds.size))         // RA = Int
            .removed(ds => DBIO.successful(s"${ds.size}")) // RR = String
            .changed(ds => DBIO.successful(ds.size > 0))   // RC = Boolean
            .void
          run {
            handler(Seq(Difference.Added(1)))
              .map(result => assertTrue(result == ()))
          }
        }
      }

      test("does not call handlers when input is empty") {
        val handler = Sync.batchSeq[Int].void
        run {
          handler(Seq.empty[Difference[Int]])
            .map(result => assertTrue(result == ()))
        }
      }

      suiteAll("unhandled cases") {
        test("fails with message identifying Added") {
          val handler = Sync.batchSeq[Int].changed(ds => DBIO.successful(ds.size)).void
          for {
            error <- run { handler(Seq(Difference.Added(1))) }.flip
          } yield assertTrue(error.getMessage == "No SyncBatch handler for Added")
        }
      }
    }
  }
}
