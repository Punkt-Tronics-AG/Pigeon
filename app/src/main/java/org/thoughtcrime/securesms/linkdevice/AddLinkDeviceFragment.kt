package org.thoughtcrime.securesms.linkdevice

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.signal.camera.CameraScreenEvents
import org.signal.camera.CameraScreenState
import org.signal.camera.CameraScreenViewModel
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.util.logging.Log
import org.signal.core.ui.permissions.Permissions
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.VibrateUtil
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.components.Mp02CustomDialog
import pigeon.compose.PigeonManualLinkDeviceScreen
import pigeon.extensions.isSignalVersion

/**
 * Fragment that allows users to scan a QR code from their camera to link a device
 */
class AddLinkDeviceFragment : ComposeFragment() {

  companion object {
    private const val VIBRATE_DURATION_MS = 50
  }

  private val viewModel: LinkDeviceViewModel by activityViewModels()

  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cameraViewModel: CameraScreenViewModel = viewModel { CameraScreenViewModel() }
    val cameraState by cameraViewModel.state
    val context = LocalContext.current
    val navController: NavController by remember { mutableStateOf(findNavController()) }
    val cameraPermissionState: PermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (isSignalVersion()) {
      if (!state.seenQrEducationSheet) {
        navController.safeNavigate(R.id.action_addLinkDeviceFragment_to_linkDeviceIntroBottomSheet)
        viewModel.markQrEducationSheetSeen()
      }

      if (state.qrCodeState != LinkDeviceSettingsState.QrCodeState.NONE && navController.currentDestination?.id == R.id.linkDeviceIntroBottomSheet) {
        navController.popBackStack()
      }
    }

    LaunchedEffect(cameraViewModel) {
      cameraViewModel.qrCodeDetected.collect { data ->
        if (VibrateUtil.isHapticFeedbackEnabled(requireContext())) {
          VibrateUtil.vibrate(requireContext(), VIBRATE_DURATION_MS)
        }
        viewModel.onQrCodeScanned(data)
      }
    }

    if (isSignalVersion()) {
    MainScreen(
      state = state,
      cameraState = cameraState,
      cameraEmitter = cameraViewModel::onEvent,
      navController = navController,
      hasPermissions = cameraPermissionState.status.isGranted,
      onRequestPermissions = { askPermissions() },
      onSwitchCamera = { cameraViewModel.onEvent(CameraScreenEvents.SwitchCamera(context)) },
      onQrCodeApproved = {
        navController.popBackStack()
        viewModel.addDevice(shouldSync = false)
      },
      onQrCodeDismissed = { viewModel.onQrCodeDismissed() },
      onLinkDeviceSuccess = {
        viewModel.onLinkDeviceResult(showSheet = true)
      },
      onLinkDeviceFailure = { viewModel.onLinkDeviceResult(showSheet = false) }
    )
    } else {
      val uuid by viewModel.uuid.collectAsStateWithLifecycle()
      val pubKey by viewModel.pubKey.collectAsStateWithLifecycle()
      val fullUrl by viewModel.fullUrl.collectAsStateWithLifecycle()
      val isLinking by viewModel.isLinking.collectAsStateWithLifecycle()

      val titleText = stringResource(id = R.string.DeviceProvisioningActivity_link_this_device)
      val introText = stringResource(id = R.string.DeviceProvisioningActivity_content_intro)
      val contentText = stringResource(id = R.string.DeviceProvisioningActivity_content_bullets)

      PigeonManualLinkDeviceScreen(
        navController = navController,
        uuid = uuid,
        pubKey = pubKey,
        fullUrl = fullUrl,
        onFullUrlChange = { viewModel.onFullUrlChanged(it) },
        onLinkClicked = {
          val dialog = Mp02CustomDialog(requireContext())
          dialog.setMessage("$titleText\n$introText\n$contentText")
          dialog.setNegativeListener(android.R.string.no, null)
          dialog.setPositiveListener(android.R.string.yes) {
            val uuid = uuid
            val pubKey = pubKey
            val qrLink = "sgnl://linkdevice?uuid=$uuid&pub_key=$pubKey"
            Log.d("AddLinkDeviceFragment", "Linking device with QR link: $qrLink")
            viewModel.linkDeviceManually(qrLink)
          }
          dialog.show()
        },
        isLinking = isLinking,
        qrCodeState = state.qrCodeState,
        onQrCodeDismissed = { viewModel.onQrCodeDismissed() },
        onLinkDeviceSuccess = {
          navController.popBackStack()
          viewModel.onLinkDeviceResult(showSheet = true)
        },
        onLinkDeviceFailure = { viewModel.onLinkDeviceResult(showSheet = false) },
        linkDeviceResult = state.linkDeviceResult,
        onQrCodeAccepted = {
          viewModel.addDevice(shouldSync = false)
        },
        onQrCodeRetry = { viewModel.addDevice(shouldSync = false) }
      )
    }
  }

  private fun askPermissions() {
    Permissions.with(this)
      .request(Manifest.permission.CAMERA)
      .ifNecessary()
      .withPermanentDenialDialog(getString(R.string.CameraXFragment_signal_needs_camera_access_scan_qr_code), null, R.string.CameraXFragment_allow_access_camera, R.string.CameraXFragment_to_scan_qr_codes, parentFragmentManager)
      .onAnyDenied { Toast.makeText(requireContext(), R.string.CameraXFragment_signal_needs_camera_access_scan_qr_code, Toast.LENGTH_LONG).show() }
      .execute()
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }
}

@Composable
private fun MainScreen(
  state: LinkDeviceSettingsState,
  cameraState: CameraScreenState = CameraScreenState(),
  cameraEmitter: (CameraScreenEvents) -> Unit = {},
  navController: NavController? = null,
  hasPermissions: Boolean = false,
  onRequestPermissions: () -> Unit = {},
  onSwitchCamera: () -> Unit = {},
  onQrCodeApproved: () -> Unit = {},
  onQrCodeDismissed: () -> Unit = {},
  onLinkDeviceSuccess: () -> Unit = {},
  onLinkDeviceFailure: () -> Unit = {}
) {
  Scaffolds.Settings(
    title = "",
    onNavigationClick = { navController?.popBackStack() },
    navigationIcon = ImageVector.vectorResource(id = R.drawable.ic_x),
    navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close),
    actions = {
      if (isSignalVersion()) {
        IconButton(onClick = onSwitchCamera) {
          Icon(painterResource(id = R.drawable.symbol_switch_24), contentDescription = null)
        }
      }
    }
  ) { contentPadding: PaddingValues ->
    LinkDeviceQrScanScreen(
      hasPermission = hasPermissions,
      onRequestPermissions = onRequestPermissions,
      cameraState = cameraState,
      cameraEmitter = cameraEmitter,
      qrCodeState = state.qrCodeState,
      onQrCodeAccepted = onQrCodeApproved,
      onQrCodeDismissed = onQrCodeDismissed,
      linkDeviceResult = state.linkDeviceResult,
      onLinkDeviceSuccess = onLinkDeviceSuccess,
      onLinkDeviceFailure = onLinkDeviceFailure,
      navController = navController,
      modifier = Modifier.padding(contentPadding)
    )
  }
}

@DayNightPreviews
@Composable
private fun LinkDeviceAddScreenPreview() {
  Previews.Preview {
    MainScreen(
      state = LinkDeviceSettingsState()
    )
  }
}
