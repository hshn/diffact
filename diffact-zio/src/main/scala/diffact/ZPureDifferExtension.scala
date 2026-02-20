package diffact

import zio.Chunk
import zio.prelude.fx.ZPure

import cats.implicits.*

extension [W, S1, S2, R, E, A](zpure: ZPure[W, S1, S2, R, E, A]) {
  def runAllStateDiff(
    s1: S1
  )(using ev1: Any <:< R, ev2: S2 <:< S1, differ: Differ[S1]): Either[E, (Chunk[W], differ.DiffResult, A)] = {
    zpure.runAll(s1) match {
      case (_, Left(e))        => e.asLeft
      case (w, Right((s2, a))) => (w, differ.diff(oldValue = s1, newValue = s2), a).asRight
    }
  }
}
