package diffact

sealed trait Difference[+A] extends Product with Serializable {
  def fold[B](
    added: A => B,
    removed: A => B,
    changed: (A, A) => B,
  ): B = this match {
    case Difference.Added(value)                => added(value)
    case Difference.Removed(value)              => removed(value)
    case Difference.Changed(oldValue, newValue) => changed(oldValue, newValue)
  }
}
object Difference {
  case class Added[+A](value: A)                   extends Difference[A]
  case class Removed[+A](value: A)                 extends Difference[A]
  case class Changed[+A](oldValue: A, newValue: A) extends Difference[A] {
    def map[B](f: A => B)(using differ: Differ[B]): differ.DiffResult = differ.diff(oldValue = f(oldValue), newValue = f(newValue))
  }
}
