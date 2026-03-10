package diffact

case class OptionDiffer[A, D] private[diffact] (differ: Differ[A] { type DiffResult = D }) extends Differ[Option[A]] {
  override type DiffResult = D

  override def diff(oldValue: Option[A], newValue: Option[A]): DiffResult =
    (oldValue, newValue) match {
      case (Some(oldValue), Some(newValue)) => differ.diff(oldValue = oldValue, newValue = newValue)
      case (Some(oldValue), None)           => differ.removed(oldValue)
      case (None, Some(newValue))           => differ.added(newValue)
      case (None, None)                     => differ.none
    }

  override def added(newValue: Option[A]): DiffResult   = newValue.fold(differ.none)(differ.added)
  override def removed(oldValue: Option[A]): DiffResult = oldValue.fold(differ.none)(differ.removed)
  override def none: DiffResult                         = differ.none
}

object OptionDiffer {
  @scala.annotation.targetName("fromDiffer")
  def apply[A](differ: Differ[A]): OptionDiffer[A, differ.DiffResult] =
    new OptionDiffer[A, differ.DiffResult](differ)
}
