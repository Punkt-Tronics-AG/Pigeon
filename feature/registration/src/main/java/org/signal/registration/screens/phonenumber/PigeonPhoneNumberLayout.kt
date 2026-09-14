package org.signal.registration.screens.phonenumber

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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonTextField
import pigeon.compose.PigeonRequestFocus

/**
 * PIGEON: D-pad friendly phone number entry for the MP02. Country row (focused first) opens the picker, the number is typed on the
 * hardware keypad and "Next" submits. Account-ID / link-device / register-without-number paths are not offered here.
 */
@Composable
internal fun PigeonPhoneNumberLayout(
  state: PhoneNumberEntryState,
  onEvent: (PhoneNumberEntryScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val countryFocus = remember { FocusRequester() }
  val numberFocus = remember { FocusRequester() }
  val nextFocus = remember { FocusRequester() }
  val hasValidCountry = state.countryName.isNotEmpty()

  var number by remember { mutableStateOf(state.formattedNumber) }
  LaunchedEffect(state.formattedNumber) {
    number = state.formattedNumber
  }

  // Country code first; once the user has picked one, continue with the number.
  var countryPicked by rememberSaveable { mutableStateOf(false) }
  PigeonRequestFocus(if (countryPicked && hasValidCountry) numberFocus else countryFocus, key = countryPicked to state.countryCode)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.PHONE_NUMBER_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.Pigeon_PhoneNumberScreen__mobile_phone),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    Rows.TextRow(
      text = if (hasValidCountry) "+${state.countryCode} ${state.countryName}" else stringResource(R.string.RegistrationActivity_select_a_country),
      onClick = {
        countryPicked = true
        onEvent(PhoneNumberEntryScreenEvents.CountryPicker)
      },
      enabled = !state.showSpinner,
      modifier = Modifier
        .focusRequester(countryFocus)
        .testTag(TestTags.PHONE_NUMBER_COUNTRY_CODE_FIELD)
    )

    PigeonTextField(
      value = number,
      onValueChange = { newValue ->
        onEvent(PhoneNumberEntryScreenEvents.NationalNumberChanged(oldValue = number, newValue = newValue))
        number = newValue
      },
      placeholder = stringResource(R.string.RegistrationActivity_phone_number_description),
      enabled = !state.showSpinner,
      upFocus = countryFocus,
      downFocus = nextFocus,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { if (state.isNextEnabled) onEvent(PhoneNumberEntryScreenEvents.NextClicked) }),
      modifier = Modifier
        .focusRequester(numberFocus)
        .testTag(TestTags.PHONE_NUMBER_PHONE_FIELD)
    )

    if (state.isNumberInvalid) {
      Text(
        text = stringResource(R.string.RegistrationActivity_not_a_valid_phone_number),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 25.dp)
      )
    }

    // Stays focusable even while the number is incomplete so D-pad navigation never dead-ends.
    Rows.TextRow(
      text = stringResource(R.string.RegistrationActivity_next),
      onClick = { if (state.isNextEnabled) onEvent(PhoneNumberEntryScreenEvents.NextClicked) },
      modifier = Modifier
        .focusRequester(nextFocus)
        .testTag(TestTags.PHONE_NUMBER_NEXT_BUTTON)
    )
  }
}
