package diffact

import zio.Scope
import zio.test.Spec
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object DifferenceFoldSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = suiteAll("Difference#fold") {
    test("dispatches Added to added handler") {
      val diff: Difference[Int] = Difference.Added(1)
      val result                = diff.fold(
        added = v => s"added:$v",
        removed = v => s"removed:$v",
        changed = (o, n) => s"changed:$o->$n",
      )
      assertTrue(result == "added:1")
    }
    test("dispatches Removed to removed handler") {
      val diff: Difference[Int] = Difference.Removed(2)
      val result                = diff.fold(
        added = v => s"added:$v",
        removed = v => s"removed:$v",
        changed = (o, n) => s"changed:$o->$n",
      )
      assertTrue(result == "removed:2")
    }
    test("dispatches Changed to changed handler") {
      val diff: Difference[Int] = Difference.Changed(3, 4)
      val result                = diff.fold(
        added = v => s"added:$v",
        removed = v => s"removed:$v",
        changed = (o, n) => s"changed:$o->$n",
      )
      assertTrue(result == "changed:3->4")
    }
  }
}
