package com.example.engine

import android.content.Context
import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Dedicated 60 FPS SurfaceView game loop.
 * Executes game physics and canvas rendering on a background render thread,
 * preserving pause state and preventing delta-time spikes across surface recreation.
 */
class GameSurfaceView(
    context: Context,
    val engine: ChaosGameEngine,
    val renderer: ChaosGameRenderer
) : SurfaceView(context), SurfaceHolder.Callback {

  private var renderThread: GameRenderThread? = null

  @Volatile
  private var surfacePaused: Boolean = false

  @Volatile
  private var isReleased: Boolean = false

  init {
    holder.addCallback(this)
    holder.setFormat(PixelFormat.OPAQUE)
    isFocusable = false
    isFocusableInTouchMode = false
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    if (!isReleased) {
      startThread(holder)
    }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    renderThread?.requestDraw()
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    stopThread()
  }

  fun setPaused(paused: Boolean) {
    surfacePaused = paused
    renderThread?.setPausedState(paused)
  }

  fun pause() {
    setPaused(true)
  }

  fun resume() {
    setPaused(false)
  }

  fun release() {
    isReleased = true
    stopThread()
    renderer.expansionRenderer.release()
    try {
      holder.removeCallback(this)
    } catch (_: Exception) {}
  }

  fun isRenderThreadAlive(): Boolean = renderThread?.isAlive == true

  private fun startThread(holder: SurfaceHolder) {
    stopThread()
    renderThread = GameRenderThread(holder, engine, renderer, initialPaused = surfacePaused).apply {
      isRunning = true
      start()
    }
  }

  private fun stopThread() {
    renderThread?.let {
      it.isRunning = false
      it.interrupt()
      var retry = true
      var attempts = 0
      while (retry && attempts < 5) {
        try {
          it.join(200)
          retry = false
        } catch (_: InterruptedException) {
          attempts++
        }
      }
    }
    renderThread = null
  }

  private class GameRenderThread(
      val surfaceHolder: SurfaceHolder,
      val engine: ChaosGameEngine,
      val renderer: ChaosGameRenderer,
      initialPaused: Boolean
  ) : Thread("ChaosRunRenderThread") {
    @Volatile var isRunning = false
    @Volatile private var isPaused = initialPaused
    @Volatile private var needsDrawWhilePaused = true
    @Volatile private var resetTimingOnNextUnpause = true

    private val targetFrameTimeNs = 16_666_667L // 60 FPS in nanoseconds

    fun setPausedState(paused: Boolean) {
      val wasPaused = isPaused
      isPaused = paused
      if (paused && !wasPaused) {
        needsDrawWhilePaused = true
      } else if (!paused && wasPaused) {
        resetTimingOnNextUnpause = true
      }
    }

    fun requestDraw() {
      needsDrawWhilePaused = true
    }

    override fun run() {
      var lastTimeNs = System.nanoTime()
      var accumulatorNs = 0L

      while (isRunning) {
        val nowNs = System.nanoTime()

        if (!isPaused) {
          if (resetTimingOnNextUnpause) {
            lastTimeNs = nowNs
            accumulatorNs = 0L
            resetTimingOnNextUnpause = false
          }
          val elapsedNs = (nowNs - lastTimeNs).coerceAtMost(100_000_000L)
          lastTimeNs = nowNs
          accumulatorNs += elapsedNs

          while (accumulatorNs >= targetFrameTimeNs) {
            engine.update()
            accumulatorNs -= targetFrameTimeNs
          }

          drawFrame()
        } else {
          lastTimeNs = nowNs
          accumulatorNs = 0L
          if (needsDrawWhilePaused) {
            drawFrame()
            needsDrawWhilePaused = false
          }
        }

        val frameDurationNs = System.nanoTime() - nowNs
        val sleepNs = targetFrameTimeNs - frameDurationNs
        if (sleepNs > 1_000_000L) {
          try {
            sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
          } catch (_: InterruptedException) {}
        } else if (sleepNs > 0L) {
          java.util.concurrent.locks.LockSupport.parkNanos(sleepNs)
        }
      }
    }

    private fun drawFrame() {
      var canvas: android.graphics.Canvas? = null
      try {
        canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
          val w = canvas.width.toFloat()
          val h = canvas.height.toFloat()
          renderer.render(canvas, engine, w, h)
        }
      } catch (_: Exception) {
        // Safe canvas handling
      } finally {
        if (canvas != null) {
          try {
            surfaceHolder.unlockCanvasAndPost(canvas)
          } catch (_: Exception) {}
        }
      }
    }
  }
}
