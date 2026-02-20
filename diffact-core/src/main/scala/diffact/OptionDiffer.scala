package diffact

case class OptionDiffer[A](
  differ: ValueDiffer[A]
) extends Differ[Option[A]] {
  override type DiffResult = Option[Difference[A]]
  override def diff(oldValue: Option[A], newValue: Option[A]): Option[Difference[A]] = {
    (oldValue, newValue) match {
      case (Some(oldValue), Some(newValue)) => differ.diff(oldValue = oldValue, newValue = newValue)
      case (Some(oldValue), None)           => differ.removed(oldValue)
      case (None, Some(newValue))           => differ.added(newValue)
      case (None, None)                     => differ.none
    }
  }
  override def added(newValue: Option[A]): Option[Difference[A]]   = newValue.map(Difference.Added(_))
  override def removed(oldValue: Option[A]): Option[Difference[A]] = oldValue.map(Difference.Removed(_))
  override def none: Option[Difference[A]]                         = None
}
