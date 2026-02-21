package diffact.slick

import zio.Scope
import zio.test.*

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object EitherDBIOComponentSpec extends ZIOSpecDefault {

  object TestProfile extends _root_.slick.jdbc.H2Profile with EitherDBIOComponent {
    object TestApi extends JdbcAPI with EitherDBIOApi
  }
  import TestProfile.TestApi.*

  private val db = Database.forURL("jdbc:h2:mem:either_dbio_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private def run[R](action: DBIO[R]): R =
    Await.result(db.run(action), Duration(5, "seconds"))

  class Counter(tag: Tag) extends Table[Int](tag, "counter") {
    def value = column[Int]("value")
    def *     = value
  }
  private val counter = TableQuery[Counter]

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("EitherDBIOComponent") {

    suiteAll("semiflatMap") {
      test("applies f on Right") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Right(1))
        val result = run(action.semiflatMap(r => DBIO.successful(r + 10)))
        assertTrue(result == Right(11))
      }
      test("short-circuits on Left") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Left("err"))
        var called = false
        val result = run(action.semiflatMap { r =>
          called = true
          DBIO.successful(r + 10)
        })
        assertTrue(result == Left("err"), !called)
      }
    }

    suiteAll("subflatMap") {
      test("applies f on Right returning Right") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Right(5))
        val result = run(action.subflatMap(r => Right(r * 2)))
        assertTrue(result == Right(10))
      }
      test("applies f on Right returning Left") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Right(5))
        val result = run(action.subflatMap(_ => Left("from f")))
        assertTrue(result == Left("from f"))
      }
      test("short-circuits on Left") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Left("err"))
        val result = run(action.subflatMap(r => Right(r * 2)))
        assertTrue(result == Left("err"))
      }
    }

    suiteAll("flatMapF") {
      test("applies f on Right returning Right") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Right(3))
        val result = run(action.flatMapF(r => DBIO.successful(Right(r + 7))))
        assertTrue(result == Right(10))
      }
      test("applies f on Right returning Left") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Right(3))
        val result = run(action.flatMapF(_ => DBIO.successful(Left("from f"))))
        assertTrue(result == Left("from f"))
      }
      test("short-circuits on Left") {
        val action: DBIO[Either[String, Int]] = DBIO.successful(Left("err"))
        var called = false
        val result = run(action.flatMapF { r =>
          called = true
          DBIO.successful(Right(r))
        })
        assertTrue(result == Left("err"), !called)
      }
    }

    suiteAll("right") {
      test("extracts Right when Left is Nothing") {
        val action: DBIO[Either[Nothing, Int]] = DBIO.successful(Right(42))
        val result = run(action.right)
        assertTrue(result == 42)
      }
    }

    suiteAll("rollbackOnLeft.transactionally") {
      test("commits on Right") {
        run(counter.schema.createIfNotExists)
        run(counter.delete)
        val inner: DBIO[Either[String, Int]] = (counter += 99).flatMap(_ =>
          DBIO.successful(Right(1)): DBIO[Either[String, Int]]
        )
        val result = run(inner.rollbackOnLeft.transactionally)
        val rows   = run(counter.result)
        assertTrue(result == Right(1), rows == Seq(99))
      }
      test("rolls back on Left") {
        run(counter.schema.createIfNotExists)
        run(counter.delete)
        val inner: DBIO[Either[String, Int]] = (counter += 99).flatMap(_ =>
          DBIO.successful(Left("domain error")): DBIO[Either[String, Int]]
        )
        val result = run(inner.rollbackOnLeft.transactionally)
        val rows   = run(counter.result)
        assertTrue(result == Left("domain error"), rows.isEmpty)
      }
      test("propagates non-Left exceptions and rolls back") {
        run(counter.schema.createIfNotExists)
        run(counter.delete)
        val inner: DBIO[Either[String, Int]] = (counter += 99).flatMap(_ =>
          DBIO.failed(new RuntimeException("boom")): DBIO[Either[String, Int]]
        )
        val result = scala.util.Try(run(inner.rollbackOnLeft.transactionally))
        val rows   = run(counter.result)
        assertTrue(result.isFailure, rows.isEmpty)
      }
    } @@ TestAspect.sequential
  }
}
