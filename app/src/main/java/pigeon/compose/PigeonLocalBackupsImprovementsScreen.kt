/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package pigeon.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R

/**
 * Pigeon-only variant of [org.thoughtcrime.securesms.components.settings.app.backups.local.LocalBackupsImprovementsScreen]
 * adapted to MP02 hardware key navigation:
 *  - vertical scrollable column suitable for the small screen,
 *  - focusable "Continue" row at the bottom that auto-focuses on entry so the
 *    user can confirm with the hardware OK key,
 *  - no top app bar / close icon (MP02 back is handled by the hardware "<" key).
 */
@Composable
fun PigeonLocalBackupsImprovementsScreen(
  onContinueClick: () -> Unit
) {
  val continueFocusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    continueFocusRequester.requestFocus()
  }

  SignalTheme(incognitoKeyboardEnabled = false) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__improvements_to_on_device_backups),
        color = Color.White,
        fontSize = 22.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__your_on_device_backup_will_be_upgraded),
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(16.dp))

      PigeonFeatureText(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__backups_now_save_faster)
      )
      PigeonFeatureText(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__your_backup_will_be_saved_as_a_folder)
      )
      PigeonFeatureText(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__all_backups_remain_end_to_end_encrypted)
      )

      Spacer(modifier = Modifier.height(16.dp))

      PigeonContinueRow(
        text = stringResource(R.string.OnDeviceBackupsImprovementsScreen__continue),
        modifier = Modifier.focusRequester(continueFocusRequester),
        onClick = onContinueClick
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PigeonFeatureText(text: String) {
  Text(
    text = "• $text",
    color = Color.White.copy(alpha = 0.7f),
    fontSize = 14.sp,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  )
}

@Composable
private fun PigeonContinueRow(
  text: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val fontSize = if (isFocused) 28.sp else 20.sp
  val color = if (isFocused) Color.White else Color.White.copy(alpha = 0.5f)

  Text(
    text = text,
    color = color,
    fontSize = fontSize,
    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .focusable(interactionSource = interactionSource)
  )
}

@Preview
@Composable
private fun PigeonLocalBackupsImprovementsScreenPreview() {
  SignalTheme {
    PigeonLocalBackupsImprovementsScreen(onContinueClick = {})
  }
}
