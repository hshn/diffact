package diffact.slick

import diffact.*
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import zio.*
import zio.prelude.fx.ZPure
import zio.test.*

object ZPureDifferSlickComponentSpec extends ZIOSpecDefault {

  object TestProfile extends _root_.slick.jdbc.H2Profile with ZPureDifferSlickComponent {
    object TestApi extends JdbcAPI with ZPureDifferSlickApi
  }
  import TestProfile.TestApi.*

  private val db = Database.forURL("jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  private def run[R](action: DBIO[R]): R =
    Await.result(db.run(action), Duration(5, "seconds"))

  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("ZPureDifferSlickComponent") {
    suiteAll("runAllStateAsDBIO") {
      test("wraps successful result in DBIO") {
        val zpure  = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        val result = run(zpure.runAllStateAsDBIO(1))
        assertTrue(
          result == Right((Chunk.empty, Some(Difference.Changed(oldValue = 1, newValue = 2)), "result"))
        )
      }
      test("wraps no-change result in DBIO") {
        val zpure  = ZPure.get[Int].map(_ => "result")
        val result = run(zpure.runAllStateAsDBIO(1))
        assertTrue(
          result == Right((Chunk.empty, None, "result"))
        )
      }
      test("wraps error result in DBIO") {
        val zpure  = ZPure.get[Int] *> ZPure.fail("error")
        val result = run(zpure.runAllStateAsDBIO(1))
        assertTrue(
          result == Left("error")
        )
      }
    }
  }
}
