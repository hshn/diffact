package diffact.slick

import zio.*
import zio.test.*

import scala.concurrent.ExecutionContext

abstract class SlickZIOSpec(dbName: String) extends ZIOSpecDefault {
  import slick.jdbc.H2Profile.api.Database
  import slick.jdbc.H2Profile.api.DBIO

  protected val db: Database =
    Database.forURL(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

  protected def run[R](action: ExecutionContext ?=> DBIO[R]): Task[R] =
    ZIO.fromFuture(ec => db.run(action(using ec)))
}
