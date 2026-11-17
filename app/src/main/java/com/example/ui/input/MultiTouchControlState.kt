package com.example.ui.input

import com.example.engine.ChaosGameEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Logical on-screen control buttons mapped cleanly by active touch Pointer IDs.
 */
enum class GameControlButton {
  LEFT,
  RIGHT,
  JUMP
}

/**
 * Thread-safe, pointer-ID-isolated multi-touch input controller.
 *
 * Guarantees:
 * - Multiple pointers can simultaneously own different buttons (e.g. Pointer 0 -> RIGHT, Pointer 1 -> JUMP).
 * - Releasing or cancelling one pointer NEVER clears another button still held by a different pointer.
 * - Death, respawn, landing, and checkpoint triggers inside [ChaosGameEngine] NEVER reset held pointers.
 * - [bindEngine] seamlessly transfers currently held physical touches across engine restarts if needed.
 */
class MultiTouchControlState {
  private val lock = Any()
  private val activePointers = HashMap<Long, GameControlButton>(8)

  @Volatile var isLeftPressed: Boolean = false
    private set
  @Volatile var isRightPressed: Boolean = false
    private set
  @Volatile var isJumpPressed: Boolean = false
    private set

  private val _resetEpoch = MutableStateFlow(0)
  val resetEpoch: StateFlow<Int> = _resetEpoch.asStateFlow()

  @Volatile private var boundEngine: ChaosGameEngine? = null

  fun bindEngine(engine: ChaosGameEngine?) {
    synchronized(lock) {
      boundEngine = engine
      syncToEngineLocked()
    }
  }

  fun onPointerDown(pointerId: Long, button: GameControlButton) {
    synchronized(lock) {
      val prev = activePointers[pointerId]
      if (prev != button) {
        activePointers[pointerId] = button
        recomputeLocked()
      }
    }
  }

  fun onPointerUp(pointerId: Long, button: GameControlButton? = null) {
    synchronized(lock) {
      val owner = activePointers[pointerId] ?: return
      if (button == null || owner == button) {
        activePointers.remove(pointerId)
        recomputeLocked()
      }
    }
  }

  fun onPointerCancel(pointerId: Long, button: GameControlButton? = null) {
    onPointerUp(pointerId, button)
  }

  /**
   * Releases ONLY pointers currently mapped to [button] (used on composable dispose/scope cancel),
   * never affecting unrelated buttons still held by other fingers.
   */
  fun releaseButtonPointers(button: GameControlButton) {
    synchronized(lock) {
      val iter = activePointers.entries.iterator()
      var changed = false
      while (iter.hasNext()) {
        if (iter.next().value == button) {
          iter.remove()
          changed = true
        }
      }
      if (changed) {
        recomputeLocked()
      }
    }
  }

  fun clearAll() {
    synchronized(lock) {
      activePointers.clear()
      isLeftPressed = false
      isRightPressed = false
      isJumpPressed = false
      syncToEngineLocked()
    }
    _resetEpoch.value = _resetEpoch.value + 1
  }

  fun activePointerCount(): Int {
    synchronized(lock) {
      return activePointers.size
    }
  }

  private fun recomputeLocked() {
    var left = false
    var right = false
    var jump = false
    for (btn in activePointers.values) {
      when (btn) {
        GameControlButton.LEFT -> left = true
        GameControlButton.RIGHT -> right = true
        GameControlButton.JUMP -> jump = true
      }
    }
    isLeftPressed = left
    isRightPressed = right
    isJumpPressed = jump
    syncToEngineLocked()
  }

  private fun syncToEngineLocked() {
    boundEngine?.let { engine ->
      engine.inputLeft = isLeftPressed
      engine.inputRight = isRightPressed
      engine.inputJump = isJumpPressed
    }
  }
}
