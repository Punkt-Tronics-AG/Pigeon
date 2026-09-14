package org.signal.registration.screens.captcha

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus

private const val CURSOR_STEP_PX = 7
private const val SCROLL_STEP_PX = 10
private const val CURSOR_SIZE_PX = 20
private const val CURSOR_TIP_X_PX = 4
private const val CURSOR_TIP_Y_PX = 7

/**
 * PIGEON: The MP02 has no touch screen, so the captcha WebView is driven with an on-screen cursor:
 * 2/4/6/8 (or the D-pad) move it, 5 / OK taps, `*` grabs / drops (drag puzzles: grab, move, drop), 0 reloads.
 * Back leaves the screen, there is no Cancel button.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PigeonCaptchaLayout(
  state: CaptchaState,
  onEvent: (CaptchaScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  var loadState by remember { mutableStateOf(state.loadState) }
  var webView by remember { mutableStateOf<WebView?>(null) }
  var cursor by remember { mutableStateOf(IntOffset(31, 103)) }
  var dragging by remember { mutableStateOf(false) }
  var gestureDownTime by remember { mutableStateOf(0L) }
  var size by remember { mutableStateOf(IntSize.Zero) }
  val focusRequester = remember { FocusRequester() }

  PigeonRequestFocus(focusRequester)

  Box(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged { size = it }
      .testTag(TestTags.CAPTCHA_SCREEN)
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        val view = webView ?: return@onKeyEvent false
        val maxX = size.width - CURSOR_SIZE_PX
        val maxY = size.height - CURSOR_SIZE_PX
        val isDown = event.type == KeyEventType.KeyDown

        fun touch(action: Int) {
          val now = SystemClock.uptimeMillis()
          // A drag is one gesture: DOWN/MOVE/UP must share the same downTime or the WebView drops the moves.
          if (action == MotionEvent.ACTION_DOWN) gestureDownTime = now
          val motion = MotionEvent.obtain(gestureDownTime, now, action, (cursor.x + CURSOR_TIP_X_PX).toFloat(), (cursor.y + CURSOR_TIP_Y_PX).toFloat(), 0)
          view.dispatchTouchEvent(motion)
          motion.recycle()
        }

        fun move(dx: Int, dy: Int): Boolean {
          if (!isDown) return true
          var x = cursor.x + dx
          var y = cursor.y + dy
          x = x.coerceIn(-CURSOR_TIP_X_PX, maxX)
          if (y < -CURSOR_TIP_Y_PX) {
            if (!dragging) view.scrollBy(0, -SCROLL_STEP_PX)
            y = -CURSOR_TIP_Y_PX
          } else if (y > maxY) {
            if (!dragging) view.scrollBy(0, SCROLL_STEP_PX)
            y = maxY
          }
          cursor = IntOffset(x, y)
          if (dragging) touch(MotionEvent.ACTION_MOVE)
          return true
        }

        when (event.key.nativeKeyCode) {
          KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_DPAD_UP -> move(0, -CURSOR_STEP_PX)
          KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_DPAD_DOWN -> move(0, CURSOR_STEP_PX)
          KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_DPAD_LEFT -> move(-CURSOR_STEP_PX, 0)
          KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_DPAD_RIGHT -> move(CURSOR_STEP_PX, 0)
          KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
            if (dragging) {
              if (isDown) {
                touch(MotionEvent.ACTION_UP)
                dragging = false
              }
            } else {
              touch(if (isDown) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP)
            }
            true
          }
          KeyEvent.KEYCODE_STAR -> {
            if (isDown) {
              touch(if (dragging) MotionEvent.ACTION_UP else MotionEvent.ACTION_DOWN)
              dragging = !dragging
            }
            true
          }
          KeyEvent.KEYCODE_0 -> {
            if (isDown) view.reload()
            true
          }
          else -> false
        }
      }
  ) {
    AndroidView(
      factory = { context ->
        WebView(context).apply {
          layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
          // Keys must stay with the Compose host so the cursor can be driven.
          isFocusable = false
          isFocusableInTouchMode = false
          settings.javaScriptEnabled = true
          clearCache(true)
          webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
              if (url.startsWith(state.captchaScheme)) {
                onEvent(CaptchaScreenEvents.CaptchaCompleted(url.substring(state.captchaScheme.length)))
                return true
              }
              return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              loadState = CaptchaLoadState.Loaded
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
              super.onReceivedError(view, errorCode, description, failingUrl)
              loadState = CaptchaLoadState.Error
            }
          }
          loadUrl(state.captchaUrl)
          webView = this
        }
      },
      modifier = Modifier.fillMaxSize()
    )

    when (loadState) {
      CaptchaLoadState.Loaded -> Unit
      CaptchaLoadState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
      }
      CaptchaLoadState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.CaptchaScreen__failed_to_load_captcha),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.error
        )
      }
    }

    if (dragging) {
      Text(
        text = stringResource(R.string.Pigeon_CaptchaScreen__dragging_hint),
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      )
    }

    // Arrow while pointing, four-way "move" glyph (red) while an element is grabbed.
    Text(
      text = if (dragging) "\u2725" else "\u27A4",
      color = if (dragging) Color.Red else Color.Black,
      fontSize = 20.sp,
      modifier = Modifier
        .offset { cursor }
        .rotate(if (dragging) 0f else 225f)
    )
  }
}
