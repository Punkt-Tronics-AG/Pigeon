package pigeon.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.view.KeyEvent.KEYCODE_DEL
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.ComposeDialogFragment
import org.thoughtcrime.securesms.R

class PigeonBackupTimePickerDialog : ComposeDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState).apply {
      window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(STYLE_NORMAL, R.style.Pigeon_MaterialAlertDialog)
  }

  @Composable
  override fun DialogContent() {
    val initialHour = requireArguments().getInt(ARG_HOUR)
    val initialMinute = requireArguments().getInt(ARG_MINUTE)

    PigeonBackupTimePickerContent(
      initialHour = initialHour,
      initialMinute = initialMinute,
      onCancel = ::dismiss,
      onConfirm = { hour, minute ->
        parentFragmentManager.setFragmentResult(
          REQUEST_KEY,
          Bundle().apply {
            putInt(RESULT_HOUR, hour)
            putInt(RESULT_MINUTE, minute)
          }
        )
        dismiss()
      }
    )
  }

  companion object {
    private const val TAG = "PigeonBackupTimePicker"
    private const val REQUEST_KEY = "pigeon_backup_time_picker_result"
    private const val ARG_HOUR = "arg_hour"
    private const val ARG_MINUTE = "arg_minute"
    private const val RESULT_HOUR = "result_hour"
    private const val RESULT_MINUTE = "result_minute"

    fun interface OnTimePickedListener {
      fun onTimePicked(hour: Int, minute: Int)
    }

    @JvmStatic
    fun show(
      fragmentManager: FragmentManager,
      lifecycleOwner: LifecycleOwner,
      hour: Int,
      minute: Int,
      listener: OnTimePickedListener
    ) {
      fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, bundle ->
        listener.onTimePicked(bundle.getInt(RESULT_HOUR), bundle.getInt(RESULT_MINUTE))
      }

      PigeonBackupTimePickerDialog().apply {
        arguments = Bundle().apply {
          putInt(ARG_HOUR, hour)
          putInt(ARG_MINUTE, minute)
        }
      }.show(fragmentManager, TAG)
    }
  }
}

/**
 * Test tags for the Pigeon backup time picker so instrumentation tests can drive the
 * digit-by-digit input and DPAD navigation reliably.
 */
object PigeonBackupTimePickerTestTags {
  const val HOUR_TENS = "pigeon_backup_time_hour_tens"
  const val HOUR_ONES = "pigeon_backup_time_hour_ones"
  const val MINUTE_TENS = "pigeon_backup_time_minute_tens"
  const val MINUTE_ONES = "pigeon_backup_time_minute_ones"
  const val OK_BUTTON = "pigeon_backup_time_ok"
  const val CANCEL_BUTTON = "pigeon_backup_time_cancel"
  const val HOUR_ERROR = "pigeon_backup_time_hour_error"
  const val MINUTE_ERROR = "pigeon_backup_time_minute_error"
}

@Composable
internal fun PigeonBackupTimePickerContent(
  initialHour: Int,
  initialMinute: Int,
  onConfirm: (Int, Int) -> Unit,
  onCancel: () -> Unit
) {
  val hourTensRequester = remember { FocusRequester() }
  val hourOnesRequester = remember { FocusRequester() }
  val minuteTensRequester = remember { FocusRequester() }
  val minuteOnesRequester = remember { FocusRequester() }
  val okRequester = remember { FocusRequester() }
  val cancelRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  val safeHour = initialHour.coerceIn(0, 23)
  val safeMinute = initialMinute.coerceIn(0, 59)

  var hourTens by remember { mutableStateOf(safeHour.padWithZero()[0].digitAsString()) }
  var hourOnes by remember { mutableStateOf(safeHour.padWithZero()[1].digitAsString()) }
  var minuteTens by remember { mutableStateOf(safeMinute.padWithZero()[0].digitAsString()) }
  var minuteOnes by remember { mutableStateOf(safeMinute.padWithZero()[1].digitAsString()) }
  var hourError by remember { mutableStateOf<String?>(null) }
  var minuteError by remember { mutableStateOf<String?>(null) }

  fun submitIfValid() {
    val hourText = hourTens + hourOnes
    val minuteText = minuteTens + minuteOnes
    val hour = hourText.toValidatedTimeValue(23)
    val minute = minuteText.toValidatedTimeValue(59)

    hourError = validateHourFormat(hourText)
    minuteError = validateMinuteFormat(minuteText)

    if (hour != null && minute != null) {
      keyboardController?.hide()
      onConfirm(hour, minute)
    } else if (hour == null) {
      hourTensRequester.requestFocus()
    } else {
      minuteTensRequester.requestFocus()
    }
  }

  LaunchedEffect(Unit) {
    hourTensRequester.requestFocus()
  }

  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    modifier = Modifier
      .padding(12.dp)
      .widthIn(min = 280.dp, max = 320.dp)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Text(
        text = stringResource(R.string.BackupsPreferenceFragment__set_backup_time),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        TimeDigitField(
          value = hourTens,
          testTag = PigeonBackupTimePickerTestTags.HOUR_TENS,
          requester = hourTensRequester,
          nextRequester = hourOnesRequester,
          previousRequester = cancelRequester,
          upRequester = cancelRequester,
          downRequester = hourOnesRequester,
          onValueChange = { value ->
            hourTens = value
            hourError = null
          },
          onBackspaceFromEmpty = {
            cancelRequester.requestFocus()
          }
        )

        TimeDigitField(
          value = hourOnes,
          testTag = PigeonBackupTimePickerTestTags.HOUR_ONES,
          requester = hourOnesRequester,
          nextRequester = minuteTensRequester,
          previousRequester = hourTensRequester,
          upRequester = hourTensRequester,
          downRequester = minuteTensRequester,
          onValueChange = { value ->
            hourOnes = value
            hourError = null
          },
          onBackspaceFromEmpty = {
            hourTensRequester.requestFocus()
          }
        )

        Text(
          text = ":",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .padding(horizontal = 12.dp)
            .focusProperties { canFocus = false }
        )

        TimeDigitField(
          value = minuteTens,
          testTag = PigeonBackupTimePickerTestTags.MINUTE_TENS,
          requester = minuteTensRequester,
          nextRequester = minuteOnesRequester,
          previousRequester = hourOnesRequester,
          upRequester = hourOnesRequester,
          downRequester = minuteOnesRequester,
          onValueChange = { value ->
            minuteTens = value
            minuteError = null
          },
          onBackspaceFromEmpty = {
            hourOnesRequester.requestFocus()
          }
        )

        TimeDigitField(
          value = minuteOnes,
          testTag = PigeonBackupTimePickerTestTags.MINUTE_ONES,
          requester = minuteOnesRequester,
          nextRequester = okRequester,
          previousRequester = minuteTensRequester,
          upRequester = minuteTensRequester,
          downRequester = okRequester,
          onValueChange = { value ->
            minuteOnes = value
            minuteError = null
          },
          onBackspaceFromEmpty = {
            minuteTensRequester.requestFocus()
          }
        )
      }

      if (hourError != null || minuteError != null) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          if (hourError != null) {
            Text(
              text = hourError.orEmpty(),
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(PigeonBackupTimePickerTestTags.HOUR_ERROR)
            )
          }
          if (minuteError != null) {
            Text(
              text = minuteError.orEmpty(),
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.testTag(PigeonBackupTimePickerTestTags.MINUTE_ERROR)
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onCancel,
          modifier = Modifier
            .testTag(PigeonBackupTimePickerTestTags.CANCEL_BUTTON)
            .focusRequester(cancelRequester)
            .onPreviewKeyEvent { event ->
              if (event.nativeKeyEvent.action == ACTION_DOWN) {
                when (event.nativeKeyEvent.keyCode) {
                  KEYCODE_DPAD_LEFT,
                  KEYCODE_DPAD_UP -> {
                    minuteOnesRequester.requestFocus()
                    return@onPreviewKeyEvent true
                  }
                  KEYCODE_DPAD_RIGHT,
                  KEYCODE_DPAD_DOWN -> {
                    okRequester.requestFocus()
                    return@onPreviewKeyEvent true
                  }
                }
              }

              false
            }
            .focusProperties {
              left = hourTensRequester
              previous = hourTensRequester
              right = okRequester
              next = okRequester
              down = okRequester
              up = minuteOnesRequester
            }
        ) {
          Text(text = stringResource(android.R.string.cancel))
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(
          onClick = { submitIfValid() },
          modifier = Modifier
            .testTag(PigeonBackupTimePickerTestTags.OK_BUTTON)
            .focusRequester(okRequester)
            .onPreviewKeyEvent { event ->
              if (event.nativeKeyEvent.action == ACTION_DOWN) {
                when (event.nativeKeyEvent.keyCode) {
                  KEYCODE_DPAD_LEFT,
                  KEYCODE_DPAD_UP -> {
                    cancelRequester.requestFocus()
                    return@onPreviewKeyEvent true
                  }
                  KEYCODE_DPAD_RIGHT,
                  KEYCODE_DPAD_DOWN -> {
                    hourTensRequester.requestFocus()
                    return@onPreviewKeyEvent true
                  }
                }
              }

              false
            }
            .focusProperties {
              left = cancelRequester
              previous = cancelRequester
              right = hourTensRequester
              next = hourTensRequester
              down = hourTensRequester
              up = minuteOnesRequester
            }
        ) {
          Text(text = stringResource(android.R.string.ok))
        }
      }
    }
  }
}

@DayNightPreviews
@Composable
private fun PigeonBackupTimePickerContentPreview() {
  Previews.Preview {
    PigeonBackupTimePickerContent(
      initialHour = 5,
      initialMinute = 7,
      onConfirm = { _, _ -> },
      onCancel = {}
    )
  }
}

private fun Int.padWithZero(): String {
  return toString().padStart(2, '0')
}

private fun String.toValidatedTimeValue(maxValue: Int): Int? {
  if (!isValidTimeFormat(maxValue)) {
    return null
  }

  return toIntOrNull()
}

private fun validateHourFormat(value: String): String? {
  return validateTimeFormat(value, 23, "Hour")
}

private fun validateMinuteFormat(value: String): String? {
  return validateTimeFormat(value, 59, "Minute")
}

private fun validateTimeFormat(value: String, maxValue: Int, label: String): String? {
  if (value.isBlank()) {
    return "$label is required"
  }

  if (!value.isValidTimeFormat(maxValue)) {
    return "$label must be between 00 and ${maxValue.toString().padStart(2, '0')}"
  }

  return null
}

private fun String.isValidTimeFormat(maxValue: Int): Boolean {
  if (!isCompleteTimeInput()) {
    return false
  }

  val parsed = toIntOrNull() ?: return false
  return parsed in 0..maxValue
}

private fun String.isCompleteTimeInput(): Boolean {
  return length == 2 && all(Char::isDigit)
}

@Composable
private fun TimeDigitField(
  value: String,
  testTag: String,
  requester: FocusRequester,
  nextRequester: FocusRequester,
  previousRequester: FocusRequester,
  upRequester: FocusRequester,
  downRequester: FocusRequester,
  onValueChange: (String) -> Unit,
  onBackspaceFromEmpty: () -> Unit,
) {
  var fieldValue by remember {
    mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
  }

  LaunchedEffect(value) {
    if (fieldValue.text != value) {
      fieldValue = TextFieldValue(value, selection = TextRange(value.length))
    }
  }

  OutlinedTextField(
    value = fieldValue,
    onValueChange = { newValue ->
      val typedDigit = newValue.text.lastOrNull()?.takeIf(Char::isDigit)
      when {
        typedDigit != null -> {
          // Overwrite whatever was in the box with the digit that was just typed and move on.
          val digit = typedDigit.digitAsString()
          fieldValue = TextFieldValue(digit, selection = TextRange(digit.length))
          onValueChange(digit)
          nextRequester.requestFocus()
        }

        newValue.text.isEmpty() -> {
          fieldValue = TextFieldValue("", selection = TextRange.Zero)
          onValueChange("")
        }

        else -> {
          // Non-digit input: ignore it and keep the current digit.
          fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
        }
      }
    },
    singleLine = true,
    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
    // The focused box border already shows where input goes; a blinking cursor is just noise on MP02.
    colors = OutlinedTextFieldDefaults.colors(
      cursorColor = ComposeColor.Transparent,
      errorCursorColor = ComposeColor.Transparent
    ),
    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
      keyboardType = KeyboardType.Number,
      imeAction = ImeAction.Next,
      capitalization = KeyboardCapitalization.None,
      autoCorrectEnabled = false
    ),
    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
      onNext = { nextRequester.requestFocus() },
      onDone = { nextRequester.requestFocus() }
    ),
    modifier = Modifier
      .width(56.dp)
      .testTag(testTag)
      .focusRequester(requester)
      .onFocusChanged { focusState ->
        if (!focusState.isFocused && fieldValue.text.isNotEmpty()) {
          fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
        }
      }
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) {
          when (event.nativeKeyEvent.keyCode) {
            KEYCODE_DEL -> {
              if (fieldValue.text.isEmpty()) {
                onBackspaceFromEmpty()
              } else {
                fieldValue = TextFieldValue("", selection = TextRange.Zero)
                onValueChange("")
              }
              return@onPreviewKeyEvent true
            }

            KEYCODE_DPAD_LEFT -> {
              previousRequester.requestFocus()
              return@onPreviewKeyEvent true
            }

            KEYCODE_DPAD_RIGHT,
            KEYCODE_DPAD_DOWN -> {
              downRequester.requestFocus()
              return@onPreviewKeyEvent true
            }

            KEYCODE_DPAD_UP -> {
              upRequester.requestFocus()
              return@onPreviewKeyEvent true
            }
          }
        }

        false
      }
      .focusProperties {
        previous = previousRequester
        next = nextRequester
        left = previousRequester
        right = nextRequester
        up = upRequester
        down = downRequester
      }
  )
}


private fun Char.digitAsString(): String = toString()

