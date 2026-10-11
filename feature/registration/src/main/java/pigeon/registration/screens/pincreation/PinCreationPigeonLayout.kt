package pigeon.registration.screens.pincreation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.screens.pincreation.PinCreationScreenEvents
import org.signal.registration.screens.pincreation.PinCreationState
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus
import pigeon.compose.PigeonTextField

/**
 * PIGEON: Compact PIN creation flow for the MP02. The Signal two-step logic stays in the view model;
 * this layout only swaps the dense Material scaffold for a D-pad friendly list of text actions.
 */
@Composable
internal fun PinCreationPigeonLayout(
  state: PinCreationState,
  onEvent: (PinCreationScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val pinFocus = remember { FocusRequester() }
  val nextFocus = remember { FocusRequester() }
  var showOptOutDialog by rememberSaveable { mutableStateOf(false) }
  var pin by rememberSaveable(state.isConfirmEnabled) { mutableStateOf("") }
  val canSubmitPin = pin.length >= 4
  val submit = {
    if (canSubmitPin && !state.loading) {
      onEvent(PinCreationScreenEvents.PinSubmitted(pin))
    }
  }

  PigeonRequestFocus(pinFocus, key = state.isConfirmEnabled)

  if (showOptOutDialog) {
    Dialogs.SimpleAlertDialog(
      title = stringResource(R.string.PinCreationScreen__warning),
      body = stringResource(R.string.PinCreationScreen__disable_pin_warning),
      confirm = stringResource(R.string.PinCreationScreen__disable_pin),
      dismiss = stringResource(R.string.PinCreationScreen__cancel),
      onConfirm = {
        showOptOutDialog = false
        onEvent(PinCreationScreenEvents.OptOut)
      },
      onDismiss = { showOptOutDialog = false }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.PIN_CREATION_SCREEN)
  ) {
    Text(
      text = if (state.isConfirmEnabled) {
        stringResource(R.string.PinCreationScreen__confirm_your_pin)
      } else {
        stringResource(R.string.PinCreationScreen__create_your_pin)
      },
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 12.dp, end = 10.dp)
    )

    Text(
      text = if (state.isConfirmEnabled) {
        stringResource(R.string.PinCreationScreen__reenter_pin_description)
      } else {
        stringResource(R.string.PinCreationScreen__pins_can_help)
      },
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 4.dp)
    )

    PigeonTextField(
      value = pin,
      onValueChange = { pin = it },
      placeholder = if (state.isConfirmEnabled) {
        stringResource(R.string.PinCreationScreen__reenter_pin)
      } else {
        stringResource(R.string.PinCreationScreen__create_your_pin)
      },
      enabled = !state.loading,
      downFocus = nextFocus,
      keyboardOptions = KeyboardOptions(
        keyboardType = if (state.isAlphanumericKeyboard) KeyboardType.Text else KeyboardType.Number,
        imeAction = ImeAction.Done
      ),
      keyboardActions = KeyboardActions(onDone = { submit() }),
      modifier = Modifier
        .focusRequester(pinFocus)
        .testTag(if (state.isConfirmEnabled) TestTags.PIN_CREATION_CONFIRM_INPUT else TestTags.PIN_CREATION_INPUT)
    )

    val infoText = when {
      state.isConfirmEnabled -> stringResource(R.string.PinCreationScreen__reenter_pin)
      state.pinMatchesVerificationCode -> stringResource(R.string.PinCreationScreen__reentered_verification_code)
      state.pinTooWeak -> stringResource(R.string.PinCreationScreen__choose_a_stronger_pin)
      state.pinMismatch -> stringResource(R.string.PinCreationScreen__pins_dont_match)
      state.isAlphanumericKeyboard -> stringResource(R.string.PinCreationScreen__pin_at_least_4_characters)
      else -> stringResource(R.string.PinCreationScreen__pin_at_least_4_digits)
    }

    Text(
      text = infoText,
      fontSize = 14.sp,
      fontWeight = FontWeight.Normal,
      color = if (!state.isConfirmEnabled && (state.pinMatchesVerificationCode || state.pinTooWeak || state.pinMismatch)) {
        MaterialTheme.colorScheme.error
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      modifier = Modifier.padding(horizontal = 25.dp, vertical = 4.dp)
    )

    Rows.TextRow(
      text = stringResource(R.string.PinCreationScreen__next),
      onClick = submit,
      enabled = canSubmitPin && !state.loading,
      modifier = Modifier
        .focusRequester(nextFocus)
        .testTag(TestTags.PIN_CREATION_NEXT_BUTTON)
    )

    Rows.TextRow(
      text = if (state.isAlphanumericKeyboard) {
        stringResource(R.string.PinCreationScreen__switch_to_numeric)
      } else {
        stringResource(R.string.PinCreationScreen__switch_to_alphanumeric)
      },
      onClick = { onEvent(PinCreationScreenEvents.ToggleKeyboard) },
      enabled = !state.loading,
      modifier = Modifier.testTag(TestTags.PIN_CREATION_TOGGLE_KEYBOARD_BUTTON)
    )

    Rows.TextRow(
      text = stringResource(R.string.PinCreationScreen__disable_pin),
      onClick = { showOptOutDialog = true },
      enabled = !state.loading,
      modifier = Modifier.testTag(TestTags.PIN_CREATION_DISABLE_PIN_MENU_ITEM)
    )
  }
}



