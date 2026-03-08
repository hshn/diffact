package diffact.slick

import diffact.*
import zio.Chunk
import zio.prelude.fx.ZPure

trait DifferZPureComponent { self: _root_.slick.jdbc.JdbcProfile =>
  trait DifferZPureApi { api: JdbcAPI =>

    extension [W, S1, S2, R, E, A](zpure: ZPure[W, S1, S2, R, E, A]) {
      def runAllStateAsDBIO(
        s1: S1
      )(using
        ev1: Any <:< R,
        ev2: S2 <:< S1,
        differ: Differ[S1],
      ): DBIOAction[Either[E, (Chunk[W], differ.DiffResult, A)], NoStream, Effect] = {
        DBIO.successful(zpure.runAllStateDiff(s1))
      }
    }

  }
}
