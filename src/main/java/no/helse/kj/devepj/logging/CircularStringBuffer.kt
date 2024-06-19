package no.helse.kj.devepj.logging

import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * (Copied from https://bitbucket.atlassian.nhn.no/projects/KJERNEJOURNAL/repos/api-critical-information/browse/src/main/java/no/helse/kj/criticalinformation/core/util/CircularStringBuffer.java)
 * Implementation of a minimal thread-safe lockless CircularStringBuffer
 * Implementing this here because:
 * a) Importing existing Circular buffers require us to include huge libraries we don't need
 * b) Most Circular buffer implementations (like commons.collections CircularFifoBuffer)
 * are NOT thread-safe, like this implementation
 *
 *
 * The threadsafe part of this class is the add function. It is threadsafe since it uses
 * an atomicInteger for determining the position of the added item in the circular buffer.
 *
 *
 * The only way to get items from the list is to use the forEach() method. The
 * method does NOT take any lock. That makes it possible in theory to overwrite the
 * list from other threads while extracting items from the list. This is by design, and
 * it will not cause error if that happens. If it happens the output may be out of order,
 * but that is not possible to prevent without adding locking of the complete log system.
 * Even if several threads add items to the circular buffer while items are extracted
 * with forEach(), the output is likely not affected, since the new ones will be put in
 * the extraItems part of the list.
 */
const val EXTRA_ITEMS: Int = 10
class CircularStringBuffer(length: Int) {
  val contentArray: Array<String?>
  private val length: Int
  private val bufferLength: Int
  var prevIndex: AtomicInteger = AtomicInteger(-1)

  init {
    require(length > 0)

    this.length = length
    this.bufferLength = length + EXTRA_ITEMS
    this.contentArray = arrayOfNulls(bufferLength)
  }

  private val numberOfItemsAdded: Int
    get() = prevIndex.get() + 1

  private val numberOfItemsAvailable: Int
    get() {
      val itemsAdded = numberOfItemsAdded
      return if ((itemsAdded > length)) length else itemsAdded
    }

  private fun wrappedPos(pos: Int): Int {
    return pos % bufferLength
  }

  fun add(text: String?): Int {
    val rawPosNew = prevIndex.incrementAndGet()
    val wrappedPosNew = wrappedPos(rawPosNew)
    atomicOverflowCheck(rawPosNew, wrappedPosNew)
    contentArray[wrappedPosNew] = text
    return rawPosNew
  }

  fun forEach(action: Consumer<in String?>) {
    Objects.requireNonNull(action)

    val rawPos = prevIndex.get()
    val size = numberOfItemsAvailable
    if (size == 0) {
      return
    }
    val startIndex = rawPos - (size - 1)
    val endIndex = rawPos
    for (i in startIndex..endIndex) {
      val wrappedPos = wrappedPos(i)
      val text = contentArray[wrappedPos]
      action.accept(text)
    }
  }


  // When the atomic integer gets close to its max value, we reset it
  // to buffer length so that wrapped position extracted from this
  // value will be 0 both before and after the reset.
  // If many threads are doing this concurrently, we risk of loosing
  // one add element each time this reset occurs.
  // This is not a problem for logging, and will  only happen if the
  // application log a lot, and run without restart for more than 100000 years
  // Still the implementation handle this event gracefully.
  private fun atomicOverflowCheck(rawPosNew: Int, wrappedPosNew: Int) {
    if ((wrappedPosNew == 0) && (rawPosNew > Int.MAX_VALUE - bufferLength * 2)) {
      prevIndex.set(bufferLength)
    }
  }
}
