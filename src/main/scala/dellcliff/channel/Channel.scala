package dellcliff.channel

import scala.concurrent.{Future, Promise}


trait Channel[T] {
  def isClosed: Boolean

  def close(): Unit

  def put(v: T): Future[Option[Failure]]

  def take(): Future[Either[Failure, T]]
}

private class PChannel[T](buffer: Buffer[T]) extends Channel[T] {
  private var closed = false
  private val lock = new Object

  override def isClosed: Boolean = lock.synchronized {
    closed
  }

  override def close(): Unit = lock.synchronized {
    closed = true

    var take: Option[ParkedTake[T]] = None
    while ( {
      take = buffer.dequeueTake()
      take.isDefined
    }) take.get.callback(Left(ChannelClosed))
  }

  override def put(v: T): Future[Option[Failure]] = lock.synchronized {
    closed match {
      case true => Promise.successful(Some(ChannelClosed)).future
      case false => buffer.dequeueTake() match {
        case None =>
          val promise = Promise[Option[Failure]]()
          val cb: Option[Failure] => Unit = { b =>
            promise.success(b)
          }
          buffer.enqueuePut(ParkedPut(v, cb)) match {
            case true => promise.future
            case false => Promise.successful(Some(DroppedFromBuffer)).future
          }
        case Some(waitingTake) =>
          waitingTake.callback(Right(v))
          Promise.successful(None).future
      }
    }
  }

  override def take(): Future[Either[Failure, T]] = lock.synchronized {
    buffer.dequeuePut() match {
      case None => closed match {
        case true => Promise.successful(Left(ChannelClosed)).future
        case false =>
          val p = Promise[Either[Failure, T]]()
          val cb: Either[Failure, T] => Unit = { v =>
            p.success(v)
          }
          buffer.enqueueTake(ParkedTake(cb)) match {
            case true => p.future
            case false => Promise.successful(Left(DroppedFromBuffer)).future
          }
      }
      case Some(waitingPut) =>
        val promise = Promise.successful(Right(waitingPut.value))
        waitingPut.callback(None)
        promise.future
    }
  }
}

object Channel {
  def apply[T]() = chan[T]()

  def chan[T](): Channel[T] = new PChannel[T](new DroppingBuffer(1))

  def chan[T](size: Long): Channel[T] = new PChannel[T](new DroppingBuffer(size))

  def chan[T](buffer: Buffer[T]): Channel[T] = new PChannel[T](buffer)

  def slidingBuffer[T](size: Long): Buffer[T] = new SlidingBuffer[T](size)

  def droppingBuffer[T](size: Long): Buffer[T] = new DroppingBuffer[T](size)
}
