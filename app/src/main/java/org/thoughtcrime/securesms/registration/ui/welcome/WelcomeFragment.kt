/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.ui.welcome


import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.signal.core.util.getSerializableCompat
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ViewBinderDelegate
import org.thoughtcrime.securesms.databinding.FragmentRegistrationWelcomeV3Binding
import org.thoughtcrime.securesms.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView
import org.thoughtcrime.securesms.registration.fragments.WelcomePermissions
import org.thoughtcrime.securesms.registration.ui.RegistrationCheckpoint
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import org.thoughtcrime.securesms.registration.ui.permissions.GrantPermissionsFragment
import org.thoughtcrime.securesms.registration.ui.phonenumber.EnterPhoneNumberMode
import org.thoughtcrime.securesms.util.BackupUtil
import org.thoughtcrime.securesms.util.CommunicationActions
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.thoughtcrime.securesms.util.visible
import org.thoughtcrime.securesms.util.visible
import pigeon.extensions.focusOnLeft
import pigeon.extensions.isPigeonVersion
import pigeon.extensions.isSignalVersion


/**
 * First screen that is displayed on the very first app launch.
 */
class WelcomeFragment : LoggingFragment(R.layout.fragment_registration_welcome_v3) {
  companion object {
    private val TAG = Log.tag(WelcomeFragment::class.java)
    private const val TERMS_AND_CONDITIONS_URL = "https://signal.org/legal"
  }

  private val sharedViewModel by activityViewModels<RegistrationViewModel>()
  private val binding: FragmentRegistrationWelcomeV3Binding by ViewBinderDelegate(FragmentRegistrationWelcomeV3Binding::bind)

  //  PIGEON
  private var extraScreenIsShowed = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setDebugLogSubmitMultiTapView(binding.image)
    setDebugLogSubmitMultiTapView(binding.title)

    binding.welcomeContinueButton.setOnClickListener { onContinueClicked() }
    binding.welcomeTermsButton.setOnClickListener { onTermsClicked() }
    binding.welcomeTransferOrRestore.setOnClickListener { onRestoreOrTransferClicked() }
    binding.welcomeTransferOrRestore.visible = !sharedViewModel.isReregister

    if (isPigeonVersion()) {
      binding.welcomeTermsButton.focusOnLeft()
      binding.welcomeTransferOrRestore.focusOnLeft()
      binding.welcomeTransferOrRestore.visible = true
      val disclaimerButton: TextView = view.findViewById(R.id.disclaimer_button)
      disclaimerButton.setOnClickListener { v: View? -> onDisclaimerClicked() }

      val welcomeLayout: ConstraintLayout = view.findViewById(R.id.welcome_layout)
      val extraLayout: ConstraintLayout = view.findViewById(R.id.extra_buttons)

      disclaimerButton.focusOnLeft()

      val titleTextView: TextView = view.findViewById(R.id.tv_welcome_title)
      titleTextView.requestFocus()

      titleTextView.setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent? ->
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
          welcomeLayout.visibility = View.GONE
          extraLayout.visibility = View.VISIBLE
          binding.welcomeContinueButton.requestFocus()
          disclaimerButton.requestFocus()
          extraScreenIsShowed = false
          return@setOnKeyListener true
        }
        false
      }

      disclaimerButton.setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent ->
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_UP) {
          if (!extraScreenIsShowed) {
            extraScreenIsShowed = true
            return@setOnKeyListener false
          }
          welcomeLayout.visibility = View.VISIBLE
          extraLayout.visibility = View.GONE
          titleTextView.requestFocus()
          return@setOnKeyListener true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
          extraScreenIsShowed = false
        }
        false
      }


    }

    if (BuildConfig.LINK_DEVICE_UX_ENABLED) {
      binding.image.setOnLongClickListener {
        MaterialAlertDialogBuilder(requireContext())
          .setMessage("Link device?")
          .setPositiveButton("Link", { _, _ -> onLinkDeviceClicked() })
          .setNegativeButton(android.R.string.cancel, null)
          .show()
        true
      }
    }

    childFragmentManager.setFragmentResultListener(RestoreWelcomeBottomSheet.REQUEST_KEY, viewLifecycleOwner) { requestKey, bundle ->
      if (requestKey == RestoreWelcomeBottomSheet.REQUEST_KEY) {
        when (val userSelection = bundle.getSerializableCompat(RestoreWelcomeBottomSheet.REQUEST_KEY, WelcomeUserSelection::class.java)) {
          WelcomeUserSelection.RESTORE_WITH_OLD_PHONE,
          WelcomeUserSelection.RESTORE_WITH_NO_PHONE -> afterRestoreOrTransferClicked(userSelection)
          else -> Unit
        }
      }
    }

    parentFragmentManager.setFragmentResultListener(GrantPermissionsFragment.REQUEST_KEY, viewLifecycleOwner) { requestKey, bundle ->
      if (requestKey == GrantPermissionsFragment.REQUEST_KEY) {
        when (val userSelection = bundle.getSerializableCompat(GrantPermissionsFragment.REQUEST_KEY, WelcomeUserSelection::class.java)) {
          WelcomeUserSelection.RESTORE_WITH_OLD_PHONE,
          WelcomeUserSelection.RESTORE_WITH_NO_PHONE -> navigateToNextScreenViaRestore(userSelection)
          WelcomeUserSelection.CONTINUE -> navigateToNextScreenViaContinue()
          WelcomeUserSelection.LINK -> navigateToLinkDevice()
          null -> Unit
        }
      }
    }
  }

  private fun onLinkDeviceClicked() {
    if (!hasAllPermissions()) {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToGrantPermissionsFragment(WelcomeUserSelection.LINK))
    } else {
      navigateToLinkDevice()
    }
  }

  private fun navigateToLinkDevice() {
    findNavController().safeNavigate(WelcomeFragmentDirections.goToLinkViaQr())
  }

  override fun onResume() {
    super.onResume()
    sharedViewModel.resetRestoreDecision()
  }

  private fun onContinueClicked() {
    if (!hasAllPermissions()) {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToGrantPermissionsFragment(WelcomeUserSelection.CONTINUE))
    } else {
      if (isSignalVersion()) {
        navigateToNextScreenViaContinue()
      } else {
        findNavController().safeNavigate(WelcomeFragmentDirections.pigeonActionWelcomeFragmentToCountryCodeFragment())
      }
    }
  }

  private fun navigateToNextScreenViaContinue() {
    sharedViewModel.maybePrefillE164(requireContext())
    findNavController().safeNavigate(WelcomeFragmentDirections.goToEnterPhoneNumber(EnterPhoneNumberMode.NORMAL))
  }

  private fun onTermsClicked() {
    if (isSignalVersion()) {
      CommunicationActions.openBrowserLink(requireContext(), TERMS_AND_CONDITIONS_URL)
    } else {
      findNavController().safeNavigate(WelcomeFragmentDirections.pigeonActionWelcomeFragmentToTermsFragment())
    }
  }

  private fun onRestoreOrTransferClicked() {
    RestoreWelcomeBottomSheet().show(childFragmentManager, null)
  }

  private fun afterRestoreOrTransferClicked(userSelection: WelcomeUserSelection) {
    if (!hasAllPermissions()) {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToGrantPermissionsFragment(userSelection))
    } else {
      navigateToNextScreenViaRestore(userSelection)
    }
  }

  private fun navigateToNextScreenViaRestore(userSelection: WelcomeUserSelection) {
    sharedViewModel.maybePrefillE164(requireContext())
    sharedViewModel.setRegistrationCheckpoint(RegistrationCheckpoint.PERMISSIONS_GRANTED)

      // PIGEON
      var backupFileUri: Uri? = null
      try {
        backupFileUri = BackupUtil.getLatestBackup()?.uri
      } catch (e: Exception) {
        Log.e(TAG, "Error getting latest backup", e)
      }
      if (backupFileUri == null) {
        Log.w(TAG, "No backups available at the moment.")
        Toast.makeText(requireContext(), R.string.registration_no_backups_available, Toast.LENGTH_LONG).show()
        return
      }
      // End PIGEON

      sharedViewModel.setRegistrationCheckpoint(RegistrationCheckpoint.PERMISSIONS_GRANTED)

    when (userSelection) {
      WelcomeUserSelection.LINK,
      WelcomeUserSelection.CONTINUE -> throw IllegalArgumentException()
      WelcomeUserSelection.RESTORE_WITH_OLD_PHONE -> {
        sharedViewModel.intendToRestore(hasOldDevice = true, fromRemote = true)
        findNavController().safeNavigate(WelcomeFragmentDirections.goToRestoreViaQr())
      }
      WelcomeUserSelection.RESTORE_WITH_NO_PHONE -> {
        sharedViewModel.intendToRestore(hasOldDevice = false, fromRemote = true)
        findNavController().safeNavigate(WelcomeFragmentDirections.goToSelectRestoreMethod(userSelection))
      }
    }
  }

  private fun hasAllPermissions(): Boolean {
    val isUserSelectionRequired = BackupUtil.isUserSelectionRequired(requireContext())
    return WelcomePermissions.getWelcomePermissions(isUserSelectionRequired).all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
  }

  //#PIGEON
  private fun onDisclaimerClicked() {
    findNavController().safeNavigate(WelcomeFragmentDirections.pigeonActionWelcomeFragmentToDisclaimerFragment())
  }
}
