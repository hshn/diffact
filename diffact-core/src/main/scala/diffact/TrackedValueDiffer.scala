package diffact

case class TrackedValueDiffer[A, T](
  tracker: A => T,
  differ: ValueDiffer[A],
) extends Differ[A] {
  override type DiffResult = Difference.Tracked[A]

  def toSeq: SeqDiffer[A, T] = SeqDiffer((a, _) => tracker(a), differ)

  override def diff(oldValue: A, newValue: A): Difference.Tracked[A] = {
    if (tracker(oldValue) == tracker(newValue)) {
      differ.diff(oldValue, newValue) match {
        case Some(changed) => Difference.Tracked.Changed(changed.oldValue, changed.newValue)
        case None          => Difference.Tracked.Unchanged
      }
    } else {
      Difference.Tracked.Replaced(removedValue = oldValue, addedValue = newValue)
    }
  }

  override def added(newValue: A): Difference.Tracked[A]   = Difference.Tracked.Added(newValue)
  override def removed(oldValue: A): Difference.Tracked[A] = Difference.Tracked.Removed(oldValue)
  override def none: Difference.Tracked[A]                 = Difference.Tracked.Unchanged
}
