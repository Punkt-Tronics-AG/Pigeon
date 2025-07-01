package pigeon.compose

import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.Rows.TextRow
import org.signal.core.ui.compose.theme.SignalTheme
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.linkdevice.LinkDeviceRepository.LinkDeviceResult
import org.thoughtcrime.securesms.linkdevice.LinkDeviceSettingsState
import org.thoughtcrime.securesms.linkdevice.makeToast
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.isPigeonVersion

@Composable
fun PigeonManualLinkDeviceScreen(
  uuid: String,
  onUuidChange: (String) -> Unit,
  pubKey: String,
  onPubKeyChange: (String) -> Unit,
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

  val lifecycleOwner = LocalLifecycleOwner.current
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


  val uuidRequester = remember { FocusRequester() }
  val pubKeyFocusRequester = remember { FocusRequester() }
  val sendFocusRequester = remember { FocusRequester() }

  SignalTheme {
    Column(
      modifier = modifier
        .fillMaxWidth()
    ) {
      Text(
        text = stringResource(R.string.Pigeon_uuid),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .focusable(false)
          .padding(
            top = 10.dp,
            start = 25.dp,
            end = 0.dp
          )
          .focusProperties { canFocus = false }
      )

      OutlinedTextField(
        value = uuid,
        onValueChange = onUuidChange,
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(uuidRequester)
          .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin), start = 10.dp, end = 10.dp)
          .onKeyEvent { keyEvent ->
            Log.d("PigeonManualLinkDeviceScreen", "Key event: ${keyEvent.key}, type: ${keyEvent.type}")
            if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_DOWN && keyEvent.type == KeyEventType.KeyUp
            ) {
              pubKeyFocusRequester.requestFocus()
              true
            } else {
              false
            }
          },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = colorResource(id = R.color.white_focus),
          unfocusedTextColor = colorResource(id = R.color.white_focus)
        ),
        keyboardOptions = KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Password,
        )
      )

      Text(
        text = stringResource(R.string.Pigeon_pubkey),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .focusable(false)
          .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin), start = 25.dp, end = 0.dp)
          .focusProperties { canFocus = false }
      )

      OutlinedTextField(
        value = pubKey,
        onValueChange = onPubKeyChange,
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(pubKeyFocusRequester)
          .padding(top = dimensionResource(R.dimen.pigeon_bottom_margin), start = 10.dp, end = 0.dp)
          .onKeyEvent { keyEvent ->
            Log.d("PigeonManualLinkDeviceScreen", "Key event: ${keyEvent.key}, type: ${keyEvent.type}")
            if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_DOWN && keyEvent.type == KeyEventType.KeyUp) {
              sendFocusRequester.requestFocus()
              true
            } else if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_UP && keyEvent.type == KeyEventType.KeyUp) {
              uuidRequester.requestFocus()
              true
            } else {
              false
            }
          },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = colorResource(id = R.color.white_focus),
          unfocusedTextColor = colorResource(id = R.color.white_focus)
        ),
        keyboardOptions = KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Password,
        ),
      )

      TextRow(
        onClick = onLinkClicked,
        enabled = true,
        modifier = Modifier
          .padding(horizontal = 20.dp)
          .focusProperties { canFocus = true }
          .focusable(true)
          .focusRequester(sendFocusRequester)
          .onKeyEvent { keyEvent ->
            Log.d("PigeonManualLinkDeviceScreen", "Key event: ${keyEvent.key}, type: ${keyEvent.type}")
            if (keyEvent.key.nativeKeyCode == KEYCODE_DPAD_UP && keyEvent.type == KeyEventType.KeyUp) {
              pubKeyFocusRequester.requestFocus()
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
    if (isPigeonVersion()) {
      LaunchedEffect(Unit) {
        uuidRequester.requestFocus()
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