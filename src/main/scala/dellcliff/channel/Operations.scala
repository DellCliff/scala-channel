package dellcliff.channel


trait WaitingOperation

case class WaitingTake[T](take: T => Unit) extends WaitingOperation

case class WaitingPut[T](value: T, put: Boolean => Unit) extends WaitingOperation
