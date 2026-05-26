/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.help

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Rows.TextAndLabel
import org.signal.core.ui.compose.Rows.defaultPadding
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.CommunicationActions
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.isPigeonVersion
import pigeon.extensions.isSignalVersion


class HelpSettingsFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    val navController: NavController = remember { findNavController() }

    val context = LocalContext.current

    val pigeonFocusRequester = remember { FocusRequester() }

    LocalFocusManager.current.clearFocus(true)

    Scaffolds.Settings(
      title = stringResource(R.string.preferences__help),
      // Pigeon code
      modifier = Modifier
        .focusable(false),
      onNavigationClick = { navController.popBackStack() },
      navigationIcon = SignalIcons.ArrowStart.imageVector,
      navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close)
    ) { contentPadding ->
      LazyColumn(
        modifier = Modifier
          .padding(contentPadding)
          .focusable(false)
      ) {
        if (isSignalVersion()) {
          item {
            Rows.LinkRow(
              text = stringResource(R.string.HelpSettingsFragment__support_center),
              icon = ImageVector.vectorResource(R.drawable.symbol_open_20),
              onClick = {
                CommunicationActions.openBrowserLink(context, getString(R.string.support_center_url))
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Rows.TextRow(
              text = stringResource(id = R.string.HelpSettingsFragment__contact_us),
              onClick = {
                navController.safeNavigate(R.id.action_helpSettingsFragment_to_helpFragment)
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Dividers.Default()
          }
        }

        if (isSignalVersion()) {
          item {
            Rows.TextRow(
              text = stringResource(R.string.HelpSettingsFragment__version),
              label = BuildConfig.VERSION_NAME
            )
          }
        } else {
          item {
            Rows.TextRow(
              text = "${stringResource(R.string.HelpSettingsFragment__version)} ${BuildConfig.VERSION_NAME}",
              onClick = {
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Rows.TextRow(
              text = stringResource(id = R.string.HelpSettingsFragment__debug_log),
              onClick = {
                navController.safeNavigate(R.id.action_helpSettingsFragment_to_submitDebugLogActivity)
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Rows.TextRow(
              text = stringResource(id = R.string.HelpSettingsFragment__licenses),
              onClick = {
                navController.safeNavigate(R.id.action_helpSettingsFragment_to_licenseFragment)
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Rows.LinkRow(
              text = stringResource(R.string.HelpSettingsFragment__terms_amp_privacy_policy),
              icon = ImageVector.vectorResource(R.drawable.symbol_open_20),
              onClick = {
                CommunicationActions.openBrowserLink(context, getString(R.string.terms_and_privacy_policy_url))
              }
            )
          }
        } else {
          item {
            Rows.TextRow(
              text = stringResource(R.string.HelpSettingsFragment__terms_amp_privacy_policy),
              onClick = {
                navController.safeNavigate(R.id.action_helpSettingsFragment_to_pigeonCodeFragment)
              }
            )
          }
        }

        if (isSignalVersion()) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding()),
              verticalAlignment = CenterVertically
            ) {
              TextAndLabel(
                label = StringBuilder().apply {
                  append(getString(R.string.HelpFragment__copyright_signal_messenger))
                  append("\n")
                  append(getString(R.string.HelpFragment__licenced_under_the_agplv3))
                  append("\n")
                  append(getString(R.string.HelpSettingsFragment__signal_is_a_501c3))
                }.toString()
              )
            }
          }
        }
      }
    }
    if (isPigeonVersion()) {
      LaunchedEffect(Unit) {
        try {
          pigeonFocusRequester.requestFocus()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }
}
