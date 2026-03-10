package diffact.slick

import zio.*
import zio.prelude.fx.ZPure
import zio.test.*

import cats.implicits.*
import slick.jdbc.H2Profile

import diffact.*

object DifferZPureComponentSpec extends SlickZIOSpec("test2") {

  object TestProfile extends H2Profile with DifferZPureComponent with DifferComponent {
    object TestApi extends JdbcAPI with DifferZPureApi with DifferApi
  }
  import TestProfile.TestApi.*

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("DifferZPureComponent") {
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
    suiteAll("runAllStateAsDBIO + sync") {
      test("state change diffResult dispatches to change handler") {
        val zpure = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        run {
          for {
            either <- zpure.runAllStateAsDBIO(1)
            result <- either match {
              case Right((_, diffResult, _)) =>
                diffResult.sync[String](
                  add = d => DBIO.successful(s"added:${d.value}"),
                  remove = d => DBIO.successful(s"removed:${d.value}"),
                  change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
                )
              case Left(_) => DBIO.successful("error")
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
              case Right((_, diffResult, _)) =>
                diffResult.sync[String](
                  add = d => DBIO.successful(s"added:${d.value}"),
                  remove = d => DBIO.successful(s"removed:${d.value}"),
                  change = d => DBIO.successful(s"changed:${d.oldValue}->${d.newValue}"),
                )
              case Left(_) => DBIO.successful("error")
            }
          } yield {
            assertTrue(result == "")
          }
        }
      }
    }
    suiteAll("runAllStateAsDBIO + syncDiscard") {
      test("state change diffResult returns Unit") {
        val zpure = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        run {
          for {
            either <- zpure.runAllStateAsDBIO(1)
            result <- either match {
              case Right((_, diffResult, _)) =>
                diffResult.syncDiscard(
                  add = _ => DBIO.successful(()),
                  remove = _ => DBIO.successful(()),
                  change = _ => DBIO.successful(()),
                )
              case Left(_) => DBIO.successful(())
            }
          } yield {
            assertTrue(result == ())
          }
        }
      }
    }
  }
}
