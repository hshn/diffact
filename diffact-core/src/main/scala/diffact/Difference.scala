package diffact

sealed trait Difference[+A] extends Product with Serializable
object Difference {
  case class Added[+A](value: A)                   extends Difference[A]
  case class Removed[+A](value: A)                 extends Difference[A]
  case class Changed[+A](oldValue: A, newValue: A) extends Difference[A] {
    def map[B](f: A => B)(using differ: Differ[B]): differ.DiffResult = differ.diff(oldValue = f(oldValue), newValue = f(newValue))
  }
}
