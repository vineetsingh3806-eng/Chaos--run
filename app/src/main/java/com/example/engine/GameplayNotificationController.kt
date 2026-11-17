package com.example.engine

import com.example.data.GameplayNotification
import com.example.data.NotificationPriority

/**
 * Priority-ordered, deduplicated, single-slot gameplay notification controller.
 *
 * Guarantees:
 * - Never stacks multiple banners: at most one [activeNotification] is visible at a time.
 * - Priority order: CRITICAL > DISCOVERY > STORY > INFO.
 * - Higher-priority notifications preempt lower-priority ones; interrupted DISCOVERY/STORY
 *   confirmations resume from the queue once the urgent alert completes.
 * - Lower- or equal-priority notifications queue cleanly behind active higher-priority ones.
 * - Duplicate keys and identical active/queued messages are suppressed.
 * - Checkpoint rollback and level restart immediately clear active and queued notifications so
 *   stale pre-death messages never reappear.
 */
class GameplayNotificationController {

  companion object {
    const val MAX_QUEUE_SIZE = 5
  }

  private data class QueuedEntry(
      val notification: GameplayNotification,
      val remainingFrames: Int,
      val sequenceId: Long
  )

  private val shownKeys = LinkedHashSet<String>()
  private val pendingQueue = ArrayList<QueuedEntry>(8)
  private var nextSequenceId = 0L

  var activeNotification: GameplayNotification? = null
    private set

  var remainingFrames: Int = 0
    private set

  var elapsedFrames: Int = 0
    private set

  val queuedCount: Int
    get() = pendingQueue.size

  fun peekQueued(): List<GameplayNotification> = pendingQueue.map { it.notification }

  fun hasShownKey(eventKey: String): Boolean = shownKeys.contains(eventKey)

  fun post(notification: GameplayNotification): Boolean {
    val normalizedMessage = notification.message.trim()
    if (normalizedMessage.isEmpty()) return false

    if (shownKeys.contains(notification.eventKey)) return false
    if (activeNotification?.eventKey == notification.eventKey) return false
    if (activeNotification?.message?.trim() == normalizedMessage) return false
    if (pendingQueue.any {
      it.notification.eventKey == notification.eventKey ||
          it.notification.message.trim() == normalizedMessage
    }) {
      return false
    }

    shownKeys.add(notification.eventKey)

    if (notification.supersedesKeys.isNotEmpty()) {
      pendingQueue.removeAll { notification.supersedesKeys.contains(it.notification.eventKey) }
    }

    val current = activeNotification
    if (current == null) {
      activate(notification, notification.durationFrames)
      return true
    }

    // If the incoming notification explicitly resolves/supersedes the active warning, replace immediately.
    if (notification.supersedesKeys.contains(current.eventKey)) {
      activate(notification, notification.durationFrames)
      return true
    }

    // Higher-priority incoming message preempts lower-priority active message.
    if (notification.priority.rank > current.priority.rank) {
      if (current.priority != NotificationPriority.INFO && remainingFrames >= 30) {
        enqueueSorted(
            QueuedEntry(
                notification = current,
                remainingFrames = remainingFrames,
                sequenceId = ++nextSequenceId
            )
        )
      }
      activate(notification, notification.durationFrames)
      return true
    }

    // Otherwise queue behind active notification in priority order.
    enqueueSorted(
        QueuedEntry(
            notification = notification,
            remainingFrames = notification.durationFrames,
            sequenceId = ++nextSequenceId
        )
    )
    return true
  }

  fun tick() {
    if (activeNotification == null) {
      promoteNextFromQueue()
      return
    }
    elapsedFrames++
    remainingFrames--
    if (remainingFrames <= 0) {
      activeNotification = null
      remainingFrames = 0
      elapsedFrames = 0
      promoteNextFromQueue()
    }
  }

  fun clearActiveAndQueue(clearShownKeys: Boolean) {
    activeNotification = null
    remainingFrames = 0
    elapsedFrames = 0
    pendingQueue.clear()
    if (clearShownKeys) {
      shownKeys.clear()
    }
  }

  fun invalidateKeys(keys: Collection<String>) {
    if (keys.isEmpty()) return
    shownKeys.removeAll(keys.toSet())
    pendingQueue.removeAll { keys.contains(it.notification.eventKey) }
    val current = activeNotification
    if (current != null && keys.contains(current.eventKey)) {
      activeNotification = null
      remainingFrames = 0
      elapsedFrames = 0
      promoteNextFromQueue()
    }
  }

  private fun activate(notification: GameplayNotification, frames: Int) {
    activeNotification = notification
    remainingFrames = frames.coerceAtLeast(1)
    elapsedFrames = 0
  }

  private fun promoteNextFromQueue() {
    if (pendingQueue.isEmpty()) return
    val next = pendingQueue.removeAt(0)
    activate(next.notification, next.remainingFrames)
  }

  private fun enqueueSorted(entry: QueuedEntry) {
    pendingQueue.add(entry)
    pendingQueue.sortWith(
        compareByDescending<QueuedEntry> { it.notification.priority.rank }
            .thenBy { it.sequenceId }
    )
    if (pendingQueue.size > MAX_QUEUE_SIZE) {
      // Evict lowest priority, oldest entry (preferring to keep CRITICAL/DISCOVERY).
      var evictIndex = pendingQueue.lastIndex
      for (i in pendingQueue.indices.reversed()) {
        if (pendingQueue[i].notification.priority != NotificationPriority.CRITICAL) {
          evictIndex = i
          break
        }
      }
      pendingQueue.removeAt(evictIndex)
    }
  }
}
