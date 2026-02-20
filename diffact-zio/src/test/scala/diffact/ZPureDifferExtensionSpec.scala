package diffact

import zio.*
import zio.prelude.fx.ZPure
import zio.test.*

object ZPureDifferExtensionSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("ZPureDifferExtension") {
    suiteAll("runAllStateDiff") {
      test("returns diff when state changes") {
        val zpure  = ZPure.update[Int, Int](_ => 2).map(_ => "result")
        val result = zpure.runAllStateDiff(1)
        assertTrue(
          result == Right((Chunk.empty, Some(Difference.Changed(oldValue = 1, newValue = 2)), "result")),
        )
      }
      test("returns no diff when state is unchanged") {
        val zpure  = ZPure.get[Int].map(_ => "result")
        val result = zpure.runAllStateDiff(1)
        assertTrue(
          result == Right((Chunk.empty, None, "result")),
        )
      }
      test("returns Left on error") {
        val zpure  = ZPure.get[Int] *> ZPure.fail("error")
        val result = zpure.runAllStateDiff(1)
        assertTrue(
          result == Left("error"),
        )
      }
      test("preserves logs") {
        val zpure: ZPure[String, Int, Int, Any, Nothing, Unit] = for {
          _ <- ZPure.get[Int]
          _ <- ZPure.log("log1")
          _ <- ZPure.log("log2")
          _ <- ZPure.update[Int, Int](_ => 2)
        } yield ()
        val result = zpure.runAllStateDiff(1)
        assertTrue(
          result == Right((Chunk("log1", "log2"), Some(Difference.Changed(oldValue = 1, newValue = 2)), ())),
        )
      }
    }
  }
}
