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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.fonts.MonoTypeface

/**
 * Pigeon-only variant of the "Record your backup key" screen content adapted to
 * MP02 hardware key navigation. Displays the key and a list of focusable
 * action rows (copy / save / next) styled the same way as the rest of the
 * Pigeon UI.
 */
@Composable
fun PigeonMessageBackupsKeyRecordContent(
  backupKey: String,
  notifyKeyIsSameAsOnDeviceBackupKey: Boolean,
  canSaveToPasswordManager: Boolean,
  onCopyToClipboardClick: () -> Unit,
  onSaveToPasswordManagerClick: () -> Unit,
  onNextClick: () -> Unit
) {
  val firstActionFocusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    firstActionFocusRequester.requestFocus()
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
        text = stringResource(R.string.MessageBackupsKeyRecordScreen__record_your_backup_key),
        color = Color.White,
        fontSize = 22.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      val description = if (notifyKeyIsSameAsOnDeviceBackupKey) {
        stringResource(R.string.MessageBackupsKeyRecordScreen__this_key_is_the_same_as_your_on_device_recovery_key)
      } else {
        stringResource(R.string.MessageBackupsKeyRecordScreen__this_key_is_required_to_recover)
      }

      Text(
        text = description,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = backupKey,
        color = Color.White,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight(400),
        fontFamily = MonoTypeface.fontFamily(),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      PigeonRecordActionRow(
        text = stringResource(R.string.MessageBackupsKeyRecordScreen__next),
        modifier = Modifier.focusRequester(firstActionFocusRequester),
        onClick = onNextClick
      )

      PigeonRecordActionRow(
        text = stringResource(R.string.MessageBackupsKeyRecordScreen__copy_to_clipboard),
        onClick = onCopyToClipboardClick
      )

      if (canSaveToPasswordManager) {
        PigeonRecordActionRow(
          text = stringResource(R.string.MessageBackupsKeyRecordScreen__save_to_password_manager),
          onClick = onSaveToPasswordManagerClick
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PigeonRecordActionRow(
  text: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val fontSize = if (isFocused) 26.sp else 20.sp
  val color = if (isFocused) Color.White else Color.White.copy(alpha = 0.5f)

  Text(
    text = text,
    color = color,
    fontSize = fontSize,
    textAlign = TextAlign.Center,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
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
private fun PigeonMessageBackupsKeyRecordContentPreview() {
  SignalTheme {
    PigeonMessageBackupsKeyRecordContent(
      backupKey = "ABCD  EFGH  IJKL  MNOP  QRST  UVWX  YZ12  3456",
      notifyKeyIsSameAsOnDeviceBackupKey = false,
      canSaveToPasswordManager = true,
      onCopyToClipboardClick = {},
      onSaveToPasswordManagerClick = {},
      onNextClick = {}
    )
  }
}
