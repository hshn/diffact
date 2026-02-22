package diffact

object DifferenceShow {
  extension [A](diff: Difference[A]) {
    def show(using s: Show[A]): String = diff match {
      case Difference.Added(value)                => s"+${s.show(value)}"
      case Difference.Removed(value)              => s"-${s.show(value)}"
      case Difference.Changed(oldValue, newValue) => s"${s.show(oldValue)} → ${s.show(newValue)}"
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
