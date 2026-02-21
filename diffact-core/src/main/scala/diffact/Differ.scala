package diffact

trait Differ[A] {
  type DiffResult

  def diff(oldValue: A, newValue: A): DiffResult

  def added(newValue: A): DiffResult
  def removed(oldValue: A): DiffResult
  def none: DiffResult
}

object Differ extends DifferInstances {
  def apply[A](using differ: ValueDiffer[A]): ValueDiffer[A] = differ

  def diff[A](oldValue: A, newValue: A)(using differ: Differ[A]): differ.DiffResult =
    differ.diff(oldValue = oldValue, newValue = newValue)

  def diff[A](value: A): DiffPartiallyApplied[A] = new DiffPartiallyApplied[A](value)

  class DiffPartiallyApplied[A](private val value: A) extends AnyVal {
    def from(oldValue: A)(using differ: Differ[A]): differ.DiffResult =
      differ.diff(oldValue = oldValue, newValue = value)
    def to(newValue: A)(using differ: Differ[A]): differ.DiffResult =
      differ.diff(oldValue = value, newValue = newValue)
  }
}

trait DifferInstances extends DifferInstances0 {
  given [A: ValueDiffer]: SeqDiffer[A, Int]  = Differ[A].toSeq
  given [A: ValueDiffer]: OptionDiffer[A]    = Differ[A].toOption
  given [A]: SetDiffer[A]                    = SetDiffer()
  given [K, V: ValueDiffer]: MapDiffer[K, V] = Differ[V].toMap
}

trait DifferInstances0 {
  private case object DefaultDiffer extends ValueDiffer[Any] {
    override def diff(oldValue: Any, newValue: Any): Option[Difference.Changed[Any]] = {
      if (oldValue == newValue) None
      else Some(Difference.Changed(oldValue, newValue))
    }
    override def added(newValue: Any): Some[Difference.Added[Any]]     = Some(Difference.Added(newValue))
    override def removed(oldValue: Any): Some[Difference.Removed[Any]] = Some(Difference.Removed(oldValue))
    override def none: None.type                                       = None
  }

  given [A]: ValueDiffer[A] = DefaultDiffer.asInstanceOf[ValueDiffer[A]]
}
