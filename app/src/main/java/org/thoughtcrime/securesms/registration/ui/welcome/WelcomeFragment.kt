/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.ui.welcome


import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ViewBinderDelegate
import org.thoughtcrime.securesms.databinding.FragmentRegistrationWelcomeBinding
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView
import org.thoughtcrime.securesms.registration.fragments.WelcomePermissions
import org.thoughtcrime.securesms.registration.ui.RegistrationCheckpoint
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import org.thoughtcrime.securesms.registration.ui.grantpermissions.GrantPermissionsFragment
import org.thoughtcrime.securesms.restore.RestoreActivity
import org.thoughtcrime.securesms.util.BackupUtil
import org.thoughtcrime.securesms.util.CommunicationActions
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.focusOnLeft
import pigeon.extensions.isPigeonVersion
import pigeon.extensions.isSignalVersion


/**
 * First screen that is displayed on the very first app launch.
 */
class WelcomeFragment : LoggingFragment(R.layout.fragment_registration_welcome) {
  private val sharedViewModel by activityViewModels<RegistrationViewModel>()
  private val binding: FragmentRegistrationWelcomeBinding by ViewBinderDelegate(FragmentRegistrationWelcomeBinding::bind)

  //  PIGEON
  private var extraScreenIsShowed = false

  private val launchRestoreActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
    when (val resultCode = result.resultCode) {
      Activity.RESULT_OK -> {
        sharedViewModel.onBackupSuccessfullyRestored()
        findNavController().safeNavigate(WelcomeFragmentDirections.actionGoToRegistration())
      }

      Activity.RESULT_CANCELED -> {
        Log.w(TAG, "Backup restoration canceled.")
      }

      else -> Log.w(TAG, "Backup restoration activity ended with unknown result code: $resultCode")
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setDebugLogSubmitMultiTapView(binding.image)
    setDebugLogSubmitMultiTapView(binding.title)
    binding.welcomeContinueButton.setOnClickListener { onContinueClicked() }
    binding.welcomeTermsButton.setOnClickListener { onTermsClicked() }
    binding.welcomeTransferOrRestore.setOnClickListener { onTransferOrRestoreClicked() }

    if (isPigeonVersion()) {
      binding.welcomeTermsButton.focusOnLeft()
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
  }

  private fun onContinueClicked() {
    if (Permissions.isRuntimePermissionsRequired() && !hasAllPermissions()) {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToGrantPermissionsFragment(GrantPermissionsFragment.WelcomeAction.CONTINUE))
    } else {
      sharedViewModel.maybePrefillE164(requireContext())
      if (isSignalVersion()){
        findNavController().safeNavigate(WelcomeFragmentDirections.actionSkipRestore())
      } else {
        findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToCountryCodeFragment())
      }
    }
  }

  private fun hasAllPermissions(): Boolean {
    val isUserSelectionRequired = BackupUtil.isUserSelectionRequired(requireContext())
    return WelcomePermissions.getWelcomePermissions(isUserSelectionRequired).all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
  }

  private fun onTermsClicked() {
    if (isSignalVersion()) {
      CommunicationActions.openBrowserLink(requireContext(), TERMS_AND_CONDITIONS_URL)
    } else {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToTermsFragment())
    }
  }

  private fun onTransferOrRestoreClicked() {
    if (Permissions.isRuntimePermissionsRequired() && !hasAllPermissions()) {
      findNavController().safeNavigate(WelcomeFragmentDirections.actionWelcomeFragmentToGrantPermissionsFragment(GrantPermissionsFragment.WelcomeAction.RESTORE_BACKUP))
    } else {
      sharedViewModel.setRegistrationCheckpoint(RegistrationCheckpoint.PERMISSIONS_GRANTED)

      val restoreIntent = RestoreActivity.getRestoreIntent(requireActivity())
      launchRestoreActivity.launch(restoreIntent)
    }
  }

  //#PIGEON
  private fun onDisclaimerClicked() {
    findNavController().safeNavigate(WelcomeFragmentDirections.actionReadDisclaimer())
  }


  companion object {
    private val TAG = Log.tag(WelcomeFragment::class.java)
    private const val TERMS_AND_CONDITIONS_URL = "https://signal.org/legal"
  }
}
