package dellcliff.channel


sealed trait Failure

case object ChannelClosed extends Failure

case object DroppedFromBuffer extends Failure


sealed trait ParkedOperation

sealed case class ParkedTake[T](callback: Either[Failure, T] => Unit)
  extends ParkedOperation

sealed case class ParkedPut[T](value: T, callback: Option[Failure] => Unit)
  extends ParkedOperation
