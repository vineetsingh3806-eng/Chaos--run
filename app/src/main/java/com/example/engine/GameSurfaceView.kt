package com.example.engine

import android.content.Context
import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Dedicated 60 FPS SurfaceView game loop.
 * Executes game physics and canvas rendering on a background render thread.
 */
class GameSurfaceView(
    context: Context,
    val engine: ChaosGameEngine,
    val renderer: ChaosGameRenderer
) : SurfaceView(context), SurfaceHolder.Callback {

  private var renderThread: GameRenderThread? = null

  init {
    holder.addCallback(this)
    holder.setFormat(PixelFormat.OPAQUE)
    isFocusable = true
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    startThread(holder)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    // Surface size configured
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    stopThread()
  }

  fun pause() {
    renderThread?.isPaused = true
  }

  fun resume() {
    renderThread?.isPaused = false
  }

  private fun startThread(holder: SurfaceHolder) {
    stopThread()
    renderThread = GameRenderThread(holder, engine, renderer).apply {
      isRunning = true
      start()
    }
  }

  private fun stopThread() {
    renderThread?.let {
      it.isRunning = false
      var retry = true
      while (retry) {
        try {
          it.join(300)
          retry = false
        } catch (_: InterruptedException) {}
      }
    }
    renderThread = null
  }

  private class GameRenderThread(
      val surfaceHolder: SurfaceHolder,
      val engine: ChaosGameEngine,
      val renderer: ChaosGameRenderer
  ) : Thread("ChaosRunRenderThread") {
    @Volatile var isRunning = false
    @Volatile var isPaused = false

    private val targetFrameTimeNs = 16_666_667L // 60 FPS in nanoseconds

    override fun run() {
      var lastTimeNs = System.nanoTime()
      var accumulatorNs = 0L

      while (isRunning) {
        val nowNs = System.nanoTime()
        val elapsedNs = (nowNs - lastTimeNs).coerceAtMost(100_000_000L) // cap spiral of death at 100ms
        lastTimeNs = nowNs

        if (!isPaused) {
          accumulatorNs += elapsedNs

          // Fixed-timestep simulation update (prevents tunneling if frame drops occur)
          while (accumulatorNs >= targetFrameTimeNs) {
            engine.update()
            accumulatorNs -= targetFrameTimeNs
          }

          // Render frame to SurfaceView Canvas
          var canvas = null as android.graphics.Canvas?
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

        // Frame timing throttle
        val frameDurationNs = System.nanoTime() - nowNs
        val sleepNs = targetFrameTimeNs - frameDurationNs
        if (sleepNs > 1_000_000L) {
          try {
            sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
          } catch (_: InterruptedException) {}
        }
      }
    }
  }
}
