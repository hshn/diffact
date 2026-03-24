package diffact.slick

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Sync handler return types must all be equal, but got RA=${A}, RR=${B}, RC=${C}. " +
    "Call .void before dispatching to unify them to Unit."
)
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
