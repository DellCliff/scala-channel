package dellcliff.channel


trait Buffer[T] {
  def dequeueTake(): Option[ParkedTake[T]]

  def dequeuePut(): Option[ParkedPut[T]]

  def enqueueTake(take: ParkedTake[T]): Boolean

  def enqueuePut(put: ParkedPut[T]): Boolean
}

private abstract class QueueBuffer[T] extends Buffer[T] {
  protected val buffer = scala.collection.mutable.Queue[ParkedOperation]()
  protected val lock = new Object

  override def dequeueTake(): Option[ParkedTake[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[ParkedTake[_]])
      .map(_.asInstanceOf[ParkedTake[T]])
  }

  override def dequeuePut(): Option[ParkedPut[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[ParkedPut[_]])
      .map(_.asInstanceOf[ParkedPut[T]])
  }
}

private class SlidingBuffer[T](size: Long) extends QueueBuffer[T] {
  override def enqueuePut(p: ParkedPut[T]): Boolean = lock.synchronized {
    if (size == 0) false
    else {
      while (buffer.length >= size) {
        buffer.dequeue() match {
          case ParkedPut(_, callback) => callback(Some(DroppedFromBuffer))
          case ParkedTake(callback) => callback(Left(DroppedFromBuffer))
          case ignore =>
        }
      }
      buffer.enqueue(p)
      true
    }
  }

  override def enqueueTake(t: ParkedTake[T]): Boolean = lock.synchronized {
    if (size == 0) false
    else {
      while (buffer.length >= size) {
        buffer.dequeue() match {
          case ParkedPut(value, callback) => callback(Some(DroppedFromBuffer))
          case ParkedTake(callback) => callback(Left(DroppedFromBuffer))
          case ignore =>
        }
      }
      buffer.enqueue(t)
      true
    }
  }
}

private class DroppingBuffer[T](size: Long) extends QueueBuffer[T] {
  override def enqueuePut(p: ParkedPut[T]): Boolean = lock.synchronized {
    if (size == 0 || buffer.length >= size) false
    else {
      buffer.enqueue(p)
      true
    }
  }

  override def enqueueTake(t: ParkedTake[T]): Boolean = lock.synchronized {
    if (size == 0 || buffer.length >= size) false
    else {
      buffer.enqueue(t)
      true
    }
  }
}
