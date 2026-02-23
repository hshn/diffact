package diffact

trait Show[A] {
  def show(value: A): String
}

object Show extends ShowInstances0 {
  def apply[A](using show: Show[A]): Show[A] = show
}

private[diffact] trait ShowInstances0 {
  given [A]: Show[A] with {
    def show(value: A): String = value.toString
  }
}
