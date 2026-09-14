package pigeon.compose

import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIGEON: Borderless text field for the MP02. D-pad up/down hand focus over to [upFocus] / [downFocus]
 * (when given) so the hardware keypad can move between the field and the surrounding rows.
 */
@Composable
fun PigeonTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  enabled: Boolean = true,
  upFocus: FocusRequester? = null,
  downFocus: FocusRequester? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  // Keep the cursor at the end whenever the text is replaced from outside (e.g. re-formatted by a view model).
  var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
  if (fieldValue.text != value) {
    fieldValue = TextFieldValue(value, TextRange(value.length))
  }

  PigeonTextField(
    value = fieldValue,
    onValueChange = {
      fieldValue = it
      if (it.text != value) onValueChange(it.text)
    },
    modifier = modifier,
    placeholder = placeholder,
    enabled = enabled,
    upFocus = upFocus,
    downFocus = downFocus,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions
  )
}

@Composable
fun PigeonTextField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  enabled: Boolean = true,
  upFocus: FocusRequester? = null,
  downFocus: FocusRequester? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    singleLine = true,
    placeholder = placeholder?.let { { Text(text = it, color = Color.Gray, fontSize = 20.sp) } },
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = Color.Transparent,
      unfocusedBorderColor = Color.Transparent,
      disabledBorderColor = Color.Transparent,
      focusedTextColor = Color.White,
      unfocusedTextColor = Color.White,
      disabledTextColor = Color.White
    ),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp)
      .onPreviewKeyEvent { event ->
        val target = when (event.key.nativeKeyCode) {
          KeyEvent.KEYCODE_DPAD_DOWN -> downFocus
          KeyEvent.KEYCODE_DPAD_UP -> upFocus
          else -> null
        } ?: return@onPreviewKeyEvent false
        // Intercept before the text field turns the arrow into a cursor move; act on down, swallow the matching up.
        if (event.type == KeyEventType.KeyDown) {
          runCatching { target.requestFocus() }
        }
        true
      }
  )
}
