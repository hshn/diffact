package diffact

trait ValueDiffer[A] extends Differ[A] { self =>
  override type DiffResult = Option[Difference[A]]

  override def diff(oldValue: A, newValue: A): Option[Difference.Changed[A]]
  override def added(newValue: A): Some[Difference.Added[A]]
  override def removed(oldValue: A): Some[Difference.Removed[A]]
  override def none: None.type

  def trackBy[T](tracker: A => T): TrackedValueDiffer[A, T] = TrackedValueDiffer(tracker, this)

  def toSeq: SeqDiffer[A, Int]  = SeqDiffer((_, index) => index, this)
  def toOption: OptionDiffer[A] = OptionDiffer(this)
  def toMap[K]: MapDiffer[K, A] = MapDiffer(this)

  def contramap[B](f: B => A): ValueDiffer[B] = new ValueDiffer[B] {
    override def diff(oldValue: B, newValue: B): Option[Difference.Changed[B]] =
      self.diff(f(oldValue), f(newValue)).map(_ => Difference.Changed(oldValue, newValue))
    override def added(newValue: B): Some[Difference.Added[B]]     = Some(Difference.Added(newValue))
    override def removed(oldValue: B): Some[Difference.Removed[B]] = Some(Difference.Removed(oldValue))
    override def none: None.type                                    = None
  }
}
