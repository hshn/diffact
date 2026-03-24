package diffact.slick

trait Eq3[A, B, C] {
  def toA(nonA: B | C): A
  def toB(nonB: A | C): B
  def toC(nonC: A | B): C
}

object Eq3 {
  given [A]: Eq3[A, A, A] with {
    override def toA(nonA: A | A): A = nonA
    override def toB(nonB: A | A): A = nonB
    override def toC(nonC: A | A): A = nonC
  }
}
