package pigeon.compose

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * PIGEON: Full-screen scrollable block of text built from [parts] (concatenated string resources).
 * Takes focus on entry so D-pad up/down scrolls it on the MP02.
 */
@Composable
fun PigeonScrollableText(
  parts: List<Int>,
  modifier: Modifier = Modifier
) {
  val text = parts.map { stringResource(it) }.joinToString(separator = "")
  PigeonScrollableText(
    text = text,
    modifier = modifier
  )
}

@Composable
fun PigeonScrollableText(
  text: String,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val scrollStep = with(LocalDensity.current) { 48.dp.toPx() }

  PigeonRequestFocus(focusRequester)

  Text(
    text = text,
    fontSize = 24.sp,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.key.nativeKeyCode) {
          KeyEvent.KEYCODE_DPAD_DOWN -> scope.launch { scrollState.animateScrollBy(scrollStep) }
          KeyEvent.KEYCODE_DPAD_UP -> scope.launch { scrollState.animateScrollBy(-scrollStep) }
          else -> return@onKeyEvent false
        }
        true
      }
      .padding(horizontal = 10.dp, vertical = 20.dp)
  )
}
