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
  }
}
