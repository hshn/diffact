package diffact

case class TrackedOptionDiffer[A, T](
  tracked: TrackedValueDiffer[A, T],
) extends Differ[Option[A]] {
  override type DiffResult = Seq[Difference[A]]

  override def diff(oldValue: Option[A], newValue: Option[A]): Seq[Difference[A]] = {
    (oldValue, newValue) match {
      case (Some(oldValue), Some(newValue)) => tracked.diff(oldValue, newValue)
      case (Some(oldValue), None)           => tracked.removed(oldValue)
      case (None, Some(newValue))           => tracked.added(newValue)
      case (None, None)                     => tracked.none
    }
  }

  override def added(newValue: Option[A]): Seq[Difference[A]]   = newValue.map(Difference.Added(_)).toSeq
  override def removed(oldValue: Option[A]): Seq[Difference[A]] = oldValue.map(Difference.Removed(_)).toSeq
  override def none: Seq[Difference[A]]                          = Nil
}
