package dellcliff.channel


trait Buffer[T] {
  def dequeueWaitingTake(): Option[WaitingTake[T]]

  def dequeueWaitingPut(): Option[WaitingPut[T]]

  def enqueueWaitingTake(t: WaitingTake[T]): Boolean

  def enqueueWaitingPut(p: WaitingPut[T]): Boolean
}

private class FixedSize[T](size: Long) extends Buffer[T] {
  private val buffer = scala.collection.mutable.Queue[WaitingOperation]()
  private val lock = new Object

  override def dequeueWaitingTake(): Option[WaitingTake[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingTake[_]])
      .map(_.asInstanceOf[WaitingTake[T]])
  }

  override def dequeueWaitingPut(): Option[WaitingPut[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingPut[_]])
      .map(_.asInstanceOf[WaitingPut[T]])
  }

  override def enqueueWaitingPut(p: WaitingPut[T]): Boolean = lock.synchronized {
    buffer.length >= size match {
      case true => false
      case false =>
        buffer.enqueue(p)
        true
    }
  }

  override def enqueueWaitingTake(t: WaitingTake[T]): Boolean = lock.synchronized {
    buffer.length >= size match {
      case true => false
      case false =>
        buffer.enqueue(t)
        true
    }
  }
}

private class SlidingBuffer[T](size: Long) extends Buffer[T] {
  private val buffer = scala.collection.mutable.Queue[WaitingOperation]()
  private val lock = new Object

  override def dequeueWaitingTake(): Option[WaitingTake[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingTake[_]])
      .map(_.asInstanceOf[WaitingTake[T]])
  }

  override def dequeueWaitingPut(): Option[WaitingPut[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingPut[_]])
      .map(_.asInstanceOf[WaitingPut[T]])
  }

  override def enqueueWaitingPut(p: WaitingPut[T]): Boolean = lock.synchronized {
    if (size == 0) false
    else {
      while (buffer.length >= size && size > 0)
        buffer.dequeue()
      buffer.enqueue(p)
      true
    }
  }

  override def enqueueWaitingTake(t: WaitingTake[T]): Boolean = lock.synchronized {
    if (size == 0) false
    else {
      while (buffer.length >= size && size > 0)
        buffer.dequeue()
      buffer.enqueue(t)
      true
    }
  }
}

private class DroppingBuffer[T](size: Long) extends Buffer[T] {
  private val buffer = scala.collection.mutable.Queue[WaitingOperation]()
  private val lock = new Object

  override def dequeueWaitingTake(): Option[WaitingTake[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingTake[_]])
      .map(_.asInstanceOf[WaitingTake[T]])
  }

  override def dequeueWaitingPut(): Option[WaitingPut[T]] = lock.synchronized {
    buffer.dequeueFirst(_.isInstanceOf[WaitingPut[_]])
      .map(_.asInstanceOf[WaitingPut[T]])
  }

  override def enqueueWaitingPut(p: WaitingPut[T]): Boolean = lock.synchronized {
    if (size == 0 || buffer.length >= size) false
    else {
      buffer.enqueue(p)
      true
    }
  }

  override def enqueueWaitingTake(t: WaitingTake[T]): Boolean = lock.synchronized {
    if (size == 0 || buffer.length >= size) false
    else {
      buffer.enqueue(t)
      true
    }
  }
}
