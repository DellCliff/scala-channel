package dellcliff.channel


trait ParkedOperation

case class ParkedTake[T](take: Option[T] => Unit) extends ParkedOperation

case class ParkedPut[T](value: T, put: Boolean => Unit) extends ParkedOperation
