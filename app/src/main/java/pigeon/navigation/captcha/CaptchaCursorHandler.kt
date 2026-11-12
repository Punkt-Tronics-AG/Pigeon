package pigeon.navigation.captcha

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

class CaptchaCursorHandler(private val captchaWebView: WebView?, private val mouseCursor: View?) {

  private val cursorMarginX = 4f
  private val cursorMarginY = 7f
  private val cursorMove = 7f
  private val viewCursorStep = 10
  private val fallbackCursorSize = 20f
  private val fallbackWebViewWidth = 240f
  private val fallbackWebViewHeight = 320f

  private var dragging = false
  private var gestureDownTime = 0L
  private var selectPressedAt = 0L

  fun onKeyDown(keyCode: Int, action: Int) {
    if (captchaWebView == null) {
      return
    }

    if (mouseCursor == null) {
      return
    }

    fun touch(motionAction: Int) {
      val now = SystemClock.uptimeMillis()
      if (motionAction == MotionEvent.ACTION_DOWN) {
        gestureDownTime = now
      }

      val motionEvent = MotionEvent.obtain(
        gestureDownTime,
        now,
        motionAction,
        mouseCursor.x + cursorMarginX,
        mouseCursor.y + cursorMarginY,
        0
      )

      captchaWebView.dispatchTouchEvent(motionEvent)
      motionEvent.recycle()
    }

    fun cursorWidth(): Float = mouseCursor.width.takeIf { it > 0 }?.toFloat() ?: fallbackCursorSize

    fun cursorHeight(): Float = mouseCursor.height.takeIf { it > 0 }?.toFloat() ?: fallbackCursorSize

    fun webViewWidth(): Float = captchaWebView.width.takeIf { it > 0 }?.toFloat() ?: fallbackWebViewWidth

    fun webViewHeight(): Float = captchaWebView.height.takeIf { it > 0 }?.toFloat() ?: fallbackWebViewHeight

    fun move(dx: Float, dy: Float) {
      if (action != KeyEvent.ACTION_DOWN) return

      var x = mouseCursor.x + dx
      var y = mouseCursor.y + dy
      val maxX = (webViewWidth() - cursorWidth()).coerceAtLeast(-cursorMarginX)
      val maxY = (webViewHeight() - cursorHeight()).coerceAtLeast(-cursorMarginY)

      x = x.coerceIn(-cursorMarginX, maxX)

      if (y < -cursorMarginY) {
        if (!dragging) {
          captchaWebView.scrollBy(0, -viewCursorStep)
        }
        y = -cursorMarginY
      } else if (y > maxY) {
        if (!dragging) {
          captchaWebView.scrollBy(0, viewCursorStep)
        }
        y = maxY
      }

      mouseCursor.x = x
      mouseCursor.y = y

      if (dragging) {
        touch(MotionEvent.ACTION_MOVE)
      }
    }

    when (keyCode) {
      KeyEvent.KEYCODE_0 -> {
        if (action == KeyEvent.ACTION_DOWN) {
          captchaWebView.reload()
        }
      }
      KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_DPAD_UP -> {
        move(0f, -cursorMove)
      }
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_DPAD_LEFT -> {
        move(-cursorMove, 0f)
      }
      KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        move(cursorMove, 0f)
      }
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_DPAD_DOWN -> {
        move(0f, cursorMove)
      }
      KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
        if (dragging) {
          if (action == KeyEvent.ACTION_UP) {
            touch(MotionEvent.ACTION_UP)
            dragging = false
          }
        } else {
          if (action == KeyEvent.ACTION_DOWN) {
            if (selectPressedAt == 0L) {
              selectPressedAt = SystemClock.uptimeMillis()
            }
          } else if (action == KeyEvent.ACTION_UP) {
            val pressStartedAt = selectPressedAt.takeIf { it != 0L } ?: SystemClock.uptimeMillis()
            selectPressedAt = 0L

            if (SystemClock.uptimeMillis() - pressStartedAt >= LONG_PRESS_MS) {
              touch(MotionEvent.ACTION_DOWN)
              dragging = true
            } else {
              touch(MotionEvent.ACTION_DOWN)
              touch(MotionEvent.ACTION_UP)
            }
          }
        }
      }
      KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> {
        if (action == KeyEvent.ACTION_DOWN) {
          touch(if (dragging) MotionEvent.ACTION_UP else MotionEvent.ACTION_DOWN)
          dragging = !dragging
        }
      }
      KeyEvent.KEYCODE_APOSTROPHE -> {
        if (action == KeyEvent.ACTION_DOWN && captchaWebView.canGoBack()) {
          captchaWebView.goBack()
        }
      }
    }
  }
}

private const val LONG_PRESS_MS = 450L
