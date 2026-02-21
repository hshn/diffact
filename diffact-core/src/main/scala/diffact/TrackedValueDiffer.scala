package diffact

case class TrackedValueDiffer[A, T](
  tracker: A => T,
  differ: ValueDiffer[A],
) extends Differ[A] {
  override type DiffResult = Seq[Difference[A]]

  def toSeq: SeqDiffer[A, T]              = SeqDiffer((a, _) => tracker(a), differ)
  def toOption: TrackedOptionDiffer[A, T] = TrackedOptionDiffer(this)

  override def diff(oldValue: A, newValue: A): Seq[Difference[A]] = {
    if (tracker(oldValue) == tracker(newValue)) {
      differ.diff(oldValue, newValue).toSeq
    } else {
      Seq(Difference.Removed(oldValue), Difference.Added(newValue))
    }
  }

  override def added(newValue: A): Seq[Difference[A]]   = Seq(Difference.Added(newValue))
  override def removed(oldValue: A): Seq[Difference[A]] = Seq(Difference.Removed(oldValue))
  override def none: Seq[Difference[A]]                 = Nil
}
