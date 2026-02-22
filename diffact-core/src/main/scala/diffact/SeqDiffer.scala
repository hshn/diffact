package diffact

case class SeqDiffer[A, T](
  tracker: (A, Int) => T,
  differ: ValueDiffer[A],
) extends Differ[Seq[A]] {
  override type DiffResult = Seq[Difference[A]]

  override def diff(
    oldValue: Seq[A],
    newValue: Seq[A],
  ): DiffResult = {
    val oldValueMap = toMap(oldValue)
    val newValueMap = toMap(newValue)

    val added = newValue.zipWithIndex.collect {
      case (v, i) if !oldValueMap.contains(tracker(v, i)) => Difference.Added(v)
    }
    val removed = oldValue.zipWithIndex.collect {
      case (v, i) if !newValueMap.contains(tracker(v, i)) => Difference.Removed(v)
    }
    val changed = newValue.zipWithIndex.flatMap { case (v, i) =>
      val key = tracker(v, i)
      oldValueMap.get(key).flatMap(oldV => differ.diff(oldValue = oldV, newValue = v))
    }

    added ++ removed ++ changed
  }

  override def added(newValue: Seq[A]): Seq[Difference[A]]   = newValue.map(Difference.Added(_))
  override def removed(oldValue: Seq[A]): Seq[Difference[A]] = oldValue.map(Difference.Removed(_))
  override def none: Seq[Difference[A]]                      = Nil

  private def toMap(values: Seq[A]): Map[T, A] = {
    val entries = values.zipWithIndex.map { case (value, index) => tracker(value, index) -> value }
    val map     = entries.toMap
    require(map.size == entries.size, "Duplicate tracking keys detected: tracking keys must be unique within the sequence")
    map
  }
}
