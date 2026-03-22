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

  def map[B](f: A => B)(using differ: Differ[B]): differ.DiffResult = this match {
    case Difference.Added(value)                => differ.added(f(value))
    case Difference.Removed(value)              => differ.removed(f(value))
    case Difference.Changed(oldValue, newValue) => differ.diff(oldValue = f(oldValue), newValue = f(newValue))
  }
}
object Difference {
  case class Added[+A](value: A)                   extends Difference[A]
  case class Removed[+A](value: A)                 extends Difference[A]
  case class Changed[+A](oldValue: A, newValue: A) extends Difference[A]

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

  extension [A](diff: Tracked[A]) {
    def show(using s: Show[A]): String = diff match {
      case Tracked.Unchanged                    => "(no change)"
      case Tracked.Added(value)                 => s"+${s.show(value)}"
      case Tracked.Removed(value)               => s"-${s.show(value)}"
      case Tracked.Changed(oldValue, newValue)  => s"${s.show(oldValue)} → ${s.show(newValue)}"
      case Tracked.Replaced(removedValue, addedValue) => s"-${s.show(removedValue)}\n+${s.show(addedValue)}"
    }
  }

  sealed trait Tracked[+A] {
    def toDifferences: Seq[Difference[A]] = this match {
      case Tracked.Unchanged                    => Nil
      case Tracked.Added(value)                 => Seq(Difference.Added(value))
      case Tracked.Removed(value)               => Seq(Difference.Removed(value))
      case Tracked.Changed(oldValue, newValue)  => Seq(Difference.Changed(oldValue, newValue))
      case Tracked.Replaced(removedValue, addedValue) => Seq(Difference.Removed(removedValue), Difference.Added(addedValue))
    }
  }

  object Tracked {
    case object Unchanged                                      extends Tracked[Nothing]
    case class Added[+A](value: A)                             extends Tracked[A]
    case class Removed[+A](value: A)                           extends Tracked[A]
    case class Changed[+A](oldValue: A, newValue: A)           extends Tracked[A]
    case class Replaced[+A](removedValue: A, addedValue: A)     extends Tracked[A]
  }
}
