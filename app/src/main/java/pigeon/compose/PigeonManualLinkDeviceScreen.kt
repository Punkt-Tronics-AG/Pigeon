package pigeon.compose

import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.Rows.TextRow
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.linkdevice.LinkDeviceRepository.LinkDeviceResult
import org.thoughtcrime.securesms.linkdevice.LinkDeviceSettingsState
import org.thoughtcrime.securesms.linkdevice.makeToast
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.isPigeonVersion

@Composable
fun PigeonManualLinkDeviceScreen(
  uuid: String,
  pubKey: String,
  fullUrl: String = "",
  onFullUrlChange: (String) -> Unit = {},
  onLinkClicked: () -> Unit,
  isLinking: Boolean = false,
  qrCodeState: LinkDeviceSettingsState.QrCodeState,
  onQrCodeAccepted: () -> Unit,
  onQrCodeDismissed: () -> Unit,
  onQrCodeRetry: () -> Unit,
  linkDeviceResult: LinkDeviceResult,
  onLinkDeviceSuccess: () -> Unit,
  onLinkDeviceFailure: () -> Unit,
  navController: NavController?,
  modifier: Modifier = Modifier
) {

  val context = LocalContext.current

  when (qrCodeState) {
    LinkDeviceSettingsState.QrCodeState.NONE -> {
      Unit
    }

    LinkDeviceSettingsState.QrCodeState.VALID_WITH_SYNC -> {
      navController?.safeNavigate(R.id.action_addLinkDeviceFragment_to_linkDeviceSyncBottomSheet)
    }

    LinkDeviceSettingsState.QrCodeState.VALID_WITHOUT_SYNC -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.DeviceProvisioningActivity_link_this_device),
        body = stringResource(id = R.string.AddLinkDeviceFragment__this_device_will_see_your_groups_contacts),
        confirm = stringResource(id = R.string.device_list_fragment__link_new_device),
        onConfirm = onQrCodeAccepted,
        dismiss = stringResource(id = android.R.string.cancel),
        onDismiss = onQrCodeDismissed
      )
    }

    LinkDeviceSettingsState.QrCodeState.INVALID -> {
      Dialogs.SimpleAlertDialog(
        title = stringResource(id = R.string.AddLinkDeviceFragment__linking_device_failed),
        body = stringResource(id = R.string.AddLinkDeviceFragment__this_qr_code_not_valid),
        confirm = stringResource(id = R.string.AddLinkDeviceFragment__retry),
        onConfirm = onQrCodeRetry,
        dismiss = stringResource(id = android.R.string.cancel),
        onDismiss = onQrCodeDismissed
      )
    }
  }

  LaunchedEffect(linkDeviceResult) {
    when (linkDeviceResult) {
      is LinkDeviceResult.Success -> onLinkDeviceSuccess()
      is LinkDeviceResult.NoDevice -> makeToast(context, R.string.DeviceProvisioningActivity_content_progress_no_device, onLinkDeviceFailure)
      is LinkDeviceResult.NetworkError -> makeToast(context, R.string.DeviceProvisioningActivity_content_progress_network_error, onLinkDeviceFailure)
      is LinkDeviceResult.KeyError -> makeToast(context, R.string.DeviceProvisioningActivity_content_progress_key_error, onLinkDeviceFailure)
      is LinkDeviceResult.LimitExceeded -> makeToast(context, R.string.DeviceProvisioningActivity_sorry_you_have_too_many_devices_linked_already, onLinkDeviceFailure)
      is LinkDeviceResult.BadCode -> makeToast(context, R.string.DeviceActivity_sorry_this_is_not_a_valid_device_link_qr_code, onLinkDeviceFailure)
      is LinkDeviceResult.None -> Unit
    }
  }


  val fullUrlFocusRequester = remember { FocusRequester() }
  val sendFocusRequester = remember { FocusRequester() }

  SignalTheme(incognitoKeyboardEnabled = false) {
    Column(
      modifier = modifier
        .fillMaxWidth()
    ) {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = stringResource(R.string.Pigeon_link_full_url),
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
          modifier = Modifier
            .fillMaxWidth()
            .focusable(false)
            .padding(top = 10.dp, start = 25.dp, end = 0.dp)
            .focusProperties { canFocus = false }
        )

        OutlinedTextField(
          value = fullUrl,
          onValueChange = onFullUrlChange,
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(fullUrlFocusRequester)
            .padding(start = 10.dp, end = 10.dp)
            .onKeyEvent { keyEvent ->
              if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_DOWN && keyEvent.type == KeyEventType.KeyUp) {
                sendFocusRequester.requestFocus()
                true
              } else {
                false
              }
            },
          placeholder = {
            Text(
              text = "sgnl://linkdevice?uuid=...&pub_key=...",
              color = Color.Gray,
              fontSize = 11.sp
            )
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = colorResource(id = R.color.white_focus),
            unfocusedTextColor = colorResource(id = R.color.white_focus)
          ),
          keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
          )
        )

        if (uuid.isNotEmpty()) {
          Text(
            text = stringResource(R.string.Pigeon_uuid) + ": $uuid",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier
              .fillMaxWidth()
              .focusable(false)
              .padding(top = 6.dp, start = 25.dp, end = 10.dp)
          )
        }

        if (pubKey.isNotEmpty()) {
          Text(
            text = stringResource(R.string.Pigeon_pubkey) + ": $pubKey",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier
              .fillMaxWidth()
              .focusable(false)
              .padding(top = 4.dp, start = 25.dp, end = 10.dp)
          )
        }

        TextRow(
          onClick = onLinkClicked,
          enabled = uuid.isNotEmpty() && pubKey.isNotEmpty(),
          modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .focusProperties { canFocus = true }
            .focusable(true)
            .focusRequester(sendFocusRequester)
            .onKeyEvent { keyEvent ->
              if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_UP && keyEvent.type == KeyEventType.KeyUp) {
                fullUrlFocusRequester.requestFocus()
                true
              } else {
                false
              }
            },
          text = { dp, color ->
            Text(
              text = stringResource(id = R.string.DeviceProvisioningActivity_link_this_device),
              style = MaterialTheme.typography.bodyLarge,
              color = color,
              fontSize = dp.value.sp,
            )
          },
        )
      }
    }
    if (isPigeonVersion()) {
      LaunchedEffect(Unit) {
        fullUrlFocusRequester.requestFocus()
      }
    }
  }
}

@Preview
@Composable
fun PigeonManualLinkDeviceScreenPreview() {
  SignalTheme(incognitoKeyboardEnabled = false) {
    PigeonManualLinkDeviceScreen(
      uuid = "12345678-1234-5678-1234-567812345678",
      pubKey = "pubkey1234567890abcdef",
      onLinkClicked = {},
      isLinking = false,
      qrCodeState = LinkDeviceSettingsState.QrCodeState.NONE,
      onQrCodeAccepted = {},
      onQrCodeDismissed = {},
      onQrCodeRetry = {},
      linkDeviceResult = LinkDeviceResult.None,
      onLinkDeviceSuccess = {},
      onLinkDeviceFailure = {},
      navController = null,
      modifier = Modifier.padding(16.dp)
    )
  }
}