package dellcliff.channel

import scala.concurrent.{Future, Promise}


trait Channel[T] {
  def close(): Unit

  def put(v: T): Future[Boolean]

  def take(): Future[Option[T]]
}

private object PChannel {
  private val falseFuture = Promise.successful(false).future
  private val trueFuture = Promise.successful(true).future
}

private class PChannel[T](buffer: Buffer[T]) extends Channel[T] {
  private val noneFuture = Promise.successful[Option[T]](None).future
  private var closed = false
  private val lock = new Object

  override def close(): Unit = lock.synchronized {
    closed = true
  }

  override def put(v: T): Future[Boolean] = lock.synchronized {
    closed match {
      case true => PChannel.falseFuture
      case false => buffer.dequeueWaitingTake() match {
        case None =>
          val promise = Promise[Boolean]()
          val cb: Boolean => Unit = { b =>
            promise.success(b)
          }
          buffer.enqueueWaitingPut(WaitingPut(v, cb)) match {
            case true => promise.future
            case false => PChannel.falseFuture
          }
        case Some(waitingTake) =>
          waitingTake.take(v)
          PChannel.trueFuture
      }
    }
  }

  override def take(): Future[Option[T]] = lock.synchronized {
    buffer.dequeueWaitingPut() match {
      case None => closed match {
        case true => noneFuture
        case false =>
          val p = Promise[Option[T]]()
          val cb: T => Unit = { v =>
            p.success(Some(v))
          }
          buffer.enqueueWaitingTake(WaitingTake(cb)) match {
            case true => p.future
            case false => noneFuture
          }
      }
      case Some(waitingPut) =>
        val promise = Promise.successful(Some(waitingPut.value))
        waitingPut.put(true)
        promise.future
    }
  }
}

object Channel {
  def apply[T]() = chan[T]()

  def chan[T](): Channel[T] = new PChannel[T](new FixedSize(1))

  def chan[T](size: Long): Channel[T] = new PChannel[T](new FixedSize(size))

  def chan[T](buffer: Buffer[T]): Channel[T] = new PChannel[T](buffer)

  def slidingBuffer[T](size: Long): Buffer[T] = new SlidingBuffer[T](size)

  def droppingBuffer[T](size: Long): Buffer[T] = new DroppingBuffer[T](size)
}
