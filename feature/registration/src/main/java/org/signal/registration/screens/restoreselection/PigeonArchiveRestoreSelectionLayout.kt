package org.signal.registration.screens.restoreselection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import pigeon.compose.PigeonRequestFocus
import org.signal.registration.test.TestTags

/**
 * PIGEON: D-pad friendly restore method selection for the MP02 – one focusable row per available option.
 */
@Composable
internal fun PigeonArchiveRestoreSelectionLayout(
  state: ArchiveRestoreSelectionState,
  onEvent: (ArchiveRestoreSelectionScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

  PigeonRequestFocus(focusRequester)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.ARCHIVE_RESTORE_SELECTION_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.ArchiveRestoreSelectionScreen__restore_or_transfer_account),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    state.restoreOptions.forEachIndexed { index, option ->
      val (title, tag) = when (option) {
        ArchiveRestoreOption.SignalSecureBackup -> R.string.ArchiveRestoreSelectionScreen__from_signal_backups to TestTags.ARCHIVE_RESTORE_SELECTION_FROM_SIGNAL_BACKUPS
        ArchiveRestoreOption.DeviceTransfer -> R.string.ArchiveRestoreSelectionScreen__from_your_old_phone to TestTags.ARCHIVE_RESTORE_SELECTION_DEVICE_TRANSFER
        ArchiveRestoreOption.LocalBackup -> R.string.ArchiveRestoreSelectionScreen__local_backup_card_title to TestTags.ARCHIVE_RESTORE_SELECTION_FROM_BACKUP_FOLDER
        ArchiveRestoreOption.None -> R.string.ArchiveRestoreSelectionScreen__skip_restore_title to TestTags.ARCHIVE_RESTORE_SELECTION_NONE
      }

      Rows.TextRow(
        text = stringResource(title),
        onClick = { onEvent(ArchiveRestoreSelectionScreenEvents.RestoreOptionSelected(option)) },
        enabled = !state.isSkipping,
        modifier = Modifier
          .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
          .testTag(tag)
      )
    }
  }
}
