package diffact

case class MapDiffer[K, V](differ: ValueDiffer[V]) extends Differ[Map[K, V]] {
  override type DiffResult = Seq[(K, Difference[V])]

  override def diff(oldValue: Map[K, V], newValue: Map[K, V]): DiffResult = {
    val added   = (newValue -- oldValue.keys).toSeq.map((k, v) => k -> Difference.Added(v))
    val removed = (oldValue -- newValue.keys).toSeq.map((k, v) => k -> Difference.Removed(v))
    val changed = (newValue.keySet & oldValue.keySet).toSeq
      .flatMap(key => differ.diff(oldValue = oldValue(key), newValue = newValue(key)).map(d => key -> d))
    added ++ removed ++ changed
  }

  override def added(newValue: Map[K, V]): Seq[(K, Difference[V])]   = newValue.toSeq.map((k, v) => k -> Difference.Added(v))
  override def removed(oldValue: Map[K, V]): Seq[(K, Difference[V])] = oldValue.toSeq.map((k, v) => k -> Difference.Removed(v))
  override def none: Seq[(K, Difference[V])]                         = Nil
}
