package diffact.slick

import zio.*
import zio.prelude.fx.ZPure
import zio.test.*

import cats.implicits.*
import slick.jdbc.H2Profile

import diffact.*

object DiffactZPureComponentSpec extends SlickZIOSpec("test2") {

  object TestProfile extends H2Profile with DiffactZPureComponent with DiffactComponent {
    object TestApi extends JdbcAPI with DiffactZPureApi with DiffactApi
  }
  import TestProfile.TestApi.*

  override def spec = suiteAll("DiffactZPureComponent") {
    suiteAll("runAllStateAsDBIO") {
      test("wraps successful result in DBIO") {
        val zpure = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        run {
          zpure.runAllStateAsDBIO(1).map { result =>
            assertTrue(
              result == Right((Chunk.empty, Some(Difference.Changed(oldValue = 1, newValue = 2)), "result"))
            )
          }
        }
      }
      test("wraps no-change result in DBIO") {
        val zpure = ZPure.get[Int].map(_ => "result")
        run {
          zpure.runAllStateAsDBIO(1).map { result =>
            assertTrue(result == Right((Chunk.empty, None, "result")))
          }
        }
      }
      test("wraps error result in DBIO") {
        val zpure = ZPure.get[Int] *> ZPure.fail("error")
        run {
          zpure.runAllStateAsDBIO(1).map { result =>
            assertTrue(result == Left("error"))
          }
        }
      }
    }
    suiteAll("runAllStateAsDBIO + Sync") {
      val handler = Sync[Int]
        .added(d => DBIO.successful(s"added:${d.value}"))
        .removed(d => DBIO.successful(s"removed:${d.value}"))
        .changed(d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"))

      test("state change diffResult dispatches to change handler") {
        val zpure = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        run {
          for {
            either <- zpure.runAllStateAsDBIO(1)
            result <- either match {
              case Right((_, diffResult, _)) => handler(diffResult)
              case Left(_)                   => DBIO.successful("error")
            }
          } yield {
            assertTrue(result == "changed:1->2")
          }
        }
      }
      test("no-change diffResult returns Monoid.empty") {
        val zpure = ZPure.get[Int].map(_ => "result")
        run {
          for {
            either <- zpure.runAllStateAsDBIO(1)
            result <- either match {
              case Right((_, diffResult, _)) => handler(diffResult)
              case Left(_)                   => DBIO.successful("error")
            }
          } yield {
            assertTrue(result == "")
          }
        }
      }
      test("state change diffResult returns Unit via void") {
        val zpure = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        run {
          for {
            either <- zpure.runAllStateAsDBIO(1)
            result <- either match {
              case Right((_, diffResult, _)) => handler.void(diffResult)
              case Left(_)                   => DBIO.successful(())
            }
          } yield {
            assertTrue(result == ())
          }
        }
      }
    }
  }
}
