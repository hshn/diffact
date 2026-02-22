package diffact

sealed trait Difference[+A] extends Product with Serializable
object Difference {
  case class Added[+A](value: A)                   extends Difference[A]
  case class Removed[+A](value: A)                 extends Difference[A]
  case class Changed[+A](oldValue: A, newValue: A) extends Difference[A] {
    def map[B](f: A => B)(using differ: Differ[B]): differ.DiffResult = differ.diff(oldValue = f(oldValue), newValue = f(newValue))
  }

  extension [A](diff: Difference[A]) {
    def show(using s: Show[A]): String = diff match {
      case Added(value)                => s"+${s.show(value)}"
      case Removed(value)              => s"-${s.show(value)}"
      case Changed(oldValue, newValue) => s"${s.show(oldValue)} → ${s.show(newValue)}"
    }
  }

  extension [A](diff: Option[Difference[A]]) {
    def show(using Show[A]): String = diff match {
      case Some(d) => d.show
      case None    => "(no change)"
    }
  }

  extension [A](diffs: Seq[Difference[A]]) {
    def show(using Show[A]): String = diffs.map(_.show).mkString("\n")
  }
}
