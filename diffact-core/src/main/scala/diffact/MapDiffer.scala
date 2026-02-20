package diffact

case class MapDiffer[K, V](differ: ValueDiffer[V]) extends Differ[Map[K, V]] {
  override type DiffResult = Seq[Difference[V]]

  override def diff(oldValue: Map[K, V], newValue: Map[K, V]): DiffResult = {
    val added   = (newValue -- oldValue.keys).values.toSeq.map(Difference.Added(_))
    val removed = (oldValue -- newValue.keys).values.toSeq.map(Difference.Removed(_))
    val changed = (newValue.keySet & oldValue.keySet).toSeq
      .flatMap(key => differ.diff(oldValue = oldValue(key), newValue = newValue(key)))
    added ++ removed ++ changed
  }

  override def added(newValue: Map[K, V]): Seq[Difference[V]]   = newValue.values.toSeq.map(Difference.Added(_))
  override def removed(oldValue: Map[K, V]): Seq[Difference[V]] = oldValue.values.toSeq.map(Difference.Removed(_))
  override def none: Seq[Difference[V]]                         = Nil
}
