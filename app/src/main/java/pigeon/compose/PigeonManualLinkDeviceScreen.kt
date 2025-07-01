package pigeon.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R

@Composable
fun PigeonManualLinkDeviceScreen(
  uuid: String,
  onUuidChange: (String) -> Unit,
  pubKey: String,
  onPubKeyChange: (String) -> Unit,
  onLinkClicked: () -> Unit,
  isLinking: Boolean = false,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = dimensionResource(R.dimen.pigeon_start_margin))
  ) {
    Text(
      text = stringResource(R.string.Pigeon_uuid),
      style = MaterialTheme.typography.bodyMedium,
      color = Color.White,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = uuid,
      onValueChange = onUuidChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin)),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = colorResource(id = R.color.white_focus),
        unfocusedTextColor = colorResource(id = R.color.white_focus)
      ),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Ascii,
        autoCorrect = false
      )
    )

    Text(
      text = stringResource(R.string.Pigeon_pubkey),
      style = MaterialTheme.typography.bodyMedium,
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin))
    )

    OutlinedTextField(
      value = pubKey,
      onValueChange = onPubKeyChange,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin)),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedTextColor = colorResource(id = R.color.white_focus),
        unfocusedTextColor = colorResource(id = R.color.white_focus)
      ),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Ascii,
        autoCorrect = false
      )
    )

    Button(
      onClick = onLinkClicked,
      contentPadding = PaddingValues(
        0.dp
      ),
      colors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = Color.Transparent
      ),
      modifier = Modifier
        .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin)),
      enabled = !isLinking
    ) {
      if (isLinking) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = MaterialTheme.colorScheme.onPrimary
        )
      } else {
        org.signal.core.ui.compose.Rows.TextRow(text = stringResource(R.string.device_link_fragment__link_device))
      }
    }
  }
}

@Preview
@Composable
fun PigeonManualLinkDeviceScreenPreview() {
  SignalTheme {
    PigeonManualLinkDeviceScreen(
      uuid = "12345678-1234-5678-1234-567812345678",
      onUuidChange = {},
      pubKey = "pubkey1234567890abcdef",
      onPubKeyChange = {},
      onLinkClicked = {},
      isLinking = false,
      modifier = Modifier.padding(16.dp)
    )
  }
}