package org.signal.registration.screens.pinentry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus
import pigeon.compose.PigeonTextField

/**
 * PIGEON: D-pad friendly PIN entry for the MP02 – short description, the PIN field and plain rows for the actions.
 * The large headline is dropped to make everything fit on the 320x240 screen.
 */
@Composable
internal fun PinEntryPigeonLayout(
  state: PinEntryState,
  pin: String,
  canSubmitPin: Boolean,
  focusRequester: FocusRequester,
  onPinChanged: (String) -> Unit,
  onSkip: () -> Unit,
  onContactSupport: () -> Unit,
  onEvent: (PinEntryScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val continueFocus = remember { FocusRequester() }
  val submit = { if (canSubmitPin && !state.loading) onEvent(PinEntryScreenEvents.PinEntered(pin)) }

  PigeonRequestFocus(focusRequester)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.PIN_ENTRY_SCREEN)
  ) {
    Text(
      text = when (state.mode) {
        PinEntryState.Mode.RegistrationLock -> stringResource(R.string.PinEntryScreen__registration_lock)
        else -> stringResource(R.string.PinEntryScreen__enter_the_pin_you_created)
      },
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 12.dp, end = 10.dp)
    )

    PigeonTextField(
      value = pin,
      onValueChange = onPinChanged,
      placeholder = stringResource(R.string.PinEntryScreen__enter_your_pin),
      enabled = !state.loading,
      downFocus = continueFocus,
      keyboardOptions = KeyboardOptions(
        keyboardType = if (state.isAlphanumericKeyboard) KeyboardType.Text else KeyboardType.Number,
        imeAction = ImeAction.Done
      ),
      keyboardActions = KeyboardActions(onDone = { submit() }),
      modifier = Modifier
        .focusRequester(focusRequester)
        .testTag(TestTags.PIN_ENTRY_INPUT)
    )

    val error = when {
      state.enteredVerificationCode -> stringResource(R.string.PinEntryScreen__reentered_verification_code)
      state.triesRemaining != null -> pluralStringResource(R.plurals.PinEntryScreen__incorrect_pin, state.triesRemaining, state.triesRemaining)
      else -> null
    }
    if (error != null) {
      Text(
        text = error,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 25.dp)
      )
    }

    // Stays focusable while the PIN is empty so D-pad navigation never dead-ends.
    Rows.TextRow(
      text = stringResource(R.string.PinEntryScreen__continue),
      onClick = submit,
      modifier = Modifier
        .focusRequester(continueFocus)
        .testTag(TestTags.PIN_ENTRY_CONTINUE_BUTTON)
    )

    if (state.mode != PinEntryState.Mode.RegistrationLock) {
      Rows.TextRow(
        text = stringResource(R.string.PinEntryScreen__skip),
        onClick = onSkip,
        enabled = !state.loading,
        modifier = Modifier.testTag(TestTags.PIN_ENTRY_SKIP_BUTTON)
      )
    }

    Rows.TextRow(
      text = stringResource(R.string.PinEntryScreen__switch_keyboard),
      onClick = { onEvent(PinEntryScreenEvents.ToggleKeyboard) },
      enabled = !state.loading
    )

    if (state.showNeedHelp) {
      Rows.TextRow(
        text = stringResource(R.string.PinEntryScreen__need_help),
        onClick = onContactSupport,
        enabled = !state.loading
      )
    }
  }
}
