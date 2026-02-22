package diffact

case class GroupedDifferences[A](
  added: Seq[Difference.Added[A]],
  removed: Seq[Difference.Removed[A]],
  changed: Seq[Difference.Changed[A]],
)

object GroupedDifferences {
  extension [A](diffs: Seq[Difference[A]]) {
    def groupByType: GroupedDifferences[A] = {
      val added   = Seq.newBuilder[Difference.Added[A]]
      val removed = Seq.newBuilder[Difference.Removed[A]]
      val changed = Seq.newBuilder[Difference.Changed[A]]
      diffs.foreach {
        case diff: Difference.Added[A]   => added += diff
        case diff: Difference.Removed[A] => removed += diff
        case diff: Difference.Changed[A] => changed += diff
      }
      GroupedDifferences(added.result(), removed.result(), changed.result())
    }
  }
}
