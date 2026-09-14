package org.signal.registration.screens.countrycode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonTextField
import pigeon.compose.PigeonRequestFocus

/**
 * PIGEON: D-pad friendly country picker for the MP02 – a search field on top and a plain focusable list below.
 * Selecting a row returns the country to the phone number screen.
 */
@Composable
internal fun PigeonCountryCodePickerLayout(
  state: CountryCodeState,
  onEvent: (CountryCodePickerScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val searchFocus = remember { FocusRequester() }
  val firstItemFocus = remember { FocusRequester() }
  val listState = rememberLazyListState()

  val countries = when {
    state.query.isNotEmpty() -> state.filteredList
    else -> state.commonCountryList + state.countryList
  }

  PigeonRequestFocus(searchFocus)

  // Typing on the keypad can start before the countries are loaded; re-run the query once they are.
  LaunchedEffect(state.countryList.size) {
    if (state.countryList.isNotEmpty() && state.query.isNotEmpty()) {
      onEvent(CountryCodePickerScreenEvents.Search(state.query))
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag(TestTags.COUNTRY_CODE_PICKER_SCREEN)
  ) {
    PigeonTextField(
      value = state.query,
      onValueChange = { onEvent(CountryCodePickerScreenEvents.Search(it)) },
      placeholder = stringResource(R.string.CountryCodeSelectScreen__search_by),
      downFocus = firstItemFocus,
      modifier = Modifier.focusRequester(searchFocus)
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      items(countries) { country ->
        Rows.TextRow(
          text = "${country.name.ifEmpty { stringResource(R.string.CountryCodeSelectScreen__unknown_country) }} (+${country.countryCode})",
          onClick = { onEvent(CountryCodePickerScreenEvents.CountrySelected(country)) },
          modifier = if (country == countries.firstOrNull()) Modifier.focusRequester(firstItemFocus) else Modifier
        )
      }
    }
  }
}
