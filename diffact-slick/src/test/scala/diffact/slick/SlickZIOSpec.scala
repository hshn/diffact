package diffact.slick

import zio.*
import zio.test.*

import slick.jdbc.H2Profile.api.Database
import slick.jdbc.H2Profile.api.DBIO

import scala.concurrent.ExecutionContext

abstract class SlickZIOSpec(dbName: String) extends ZIOSpec[Database & TestEnvironment] {

  override val bootstrap: ZLayer[Any, Any, Database & TestEnvironment] =
    testEnvironment ++ ZLayer.scoped(
      ZIO.acquireRelease(
        ZIO.succeed(Database.forURL(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver"))
      )(db => ZIO.succeed(db.close()))
    )

  protected def run[R](action: ExecutionContext ?=> DBIO[R]): ZIO[Database, Throwable, R] =
    ZIO.serviceWithZIO[Database] { db =>
      ZIO.fromFuture(ec => db.run(action(using ec)))
    }
}
