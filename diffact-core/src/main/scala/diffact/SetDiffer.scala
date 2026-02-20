package diffact

case class SetDiffer[A]() extends Differ[Set[A]] {
  override type DiffResult = Seq[Difference[A]]

  override def diff(oldValue: Set[A], newValue: Set[A]): DiffResult = {
    val added   = (newValue -- oldValue).toSeq.map(Difference.Added(_))
    val removed = (oldValue -- newValue).toSeq.map(Difference.Removed(_))
    added ++ removed
  }

  override def added(newValue: Set[A]): Seq[Difference[A]]   = newValue.toSeq.map(Difference.Added(_))
  override def removed(oldValue: Set[A]): Seq[Difference[A]] = oldValue.toSeq.map(Difference.Removed(_))
  override def none: Seq[Difference[A]]                      = Nil
}
