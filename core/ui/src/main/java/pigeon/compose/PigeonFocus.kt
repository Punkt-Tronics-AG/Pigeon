package pigeon.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

/**
 * PIGEON: Gives [focusRequester] the initial focus once the screen is ready. Screens hosted in an animated
 * NavDisplay are not always able to take focus on the first frame, so the request is retried briefly.
 */
@Composable
fun PigeonRequestFocus(focusRequester: FocusRequester, key: Any? = Unit) {
  val view = LocalView.current
  LaunchedEffect(key) {
    repeat(MAX_ATTEMPTS) {
      // Compose refuses focus while the hosting ComposeView itself is not the focused View (first screen after launch).
      if (!view.hasFocus()) view.requestFocus()
      if (runCatching { focusRequester.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
      delay(RETRY_DELAY_MS)
    }
  }
}

private const val MAX_ATTEMPTS = 15
private const val RETRY_DELAY_MS = 100L
