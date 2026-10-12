package pigeon.registration.screens.localbackuprestore

import android.text.format.Formatter
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.screens.localbackuprestore.LocalBackupInfo
import org.signal.registration.screens.localbackuprestore.LocalBackupRestoreEvents
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * PIGEON: D-pad friendly "backup found" step for the MP02 – a short title followed by one focusable row per
 * on-device backup (newest first). Selecting a row restores that backup straight away; the hardware Back key
 * cancels. There are deliberately no footer buttons: the 240×320 screen has no room for them and they would
 * only duplicate "select" and "back".
 */
@Composable
internal fun PigeonLocalBackupFoundLayout(
  backups: List<LocalBackupInfo>,
  selectedBackup: LocalBackupInfo,
  onEvent: (LocalBackupRestoreEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val focusRequester = remember { FocusRequester() }
  val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm") }
  var requestInitialFocus by remember(backups) { mutableStateOf(true) }

  BackHandler { onEvent(LocalBackupRestoreEvents.Cancel) }

  // Focus always starts on the first row (newest backup); re-request it if the list is re-scanned. The row is
  // mounted right after the Scanning→BackupFound transition, so also request focus once it is actually laid out.
  PigeonRequestFocus(focusRequester, key = backups)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.LOCAL_BACKUP_RESTORE_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__choose_a_backup_to_restore),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    backups.forEachIndexed { index, backup ->
      val isSelected = backup == selectedBackup
      val sizeLabel = backup.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }

      Rows.TextRow(
        text = backup.date.format(dateFormatter),
        label = sizeLabel,
        onClick = {
          onEvent(LocalBackupRestoreEvents.BackupSelected(backup))
          onEvent(LocalBackupRestoreEvents.RestoreBackup)
        },
        modifier = Modifier
          .then(
            if (index == 0) {
              Modifier
                .focusRequester(focusRequester)
                .onGloballyPositioned {
                  if (requestInitialFocus) {
                    requestInitialFocus = false
                    runCatching { focusRequester.requestFocus() }
                  }
                }
            } else {
              Modifier
            }
          )
          .then(if (isSelected) Modifier.testTag(TestTags.LOCAL_BACKUP_RESTORE_RESTORE_BUTTON) else Modifier)
      )
    }
  }
}

/**
 * PIGEON: "no backup found" step for the MP02. The backup directory is fixed, so there is no folder to change –
 * the only action is to scan it again (e.g. after copying a backup file onto the device). Back cancels.
 */
@Composable
internal fun PigeonLocalBackupNotFoundLayout(
  onEvent: (LocalBackupRestoreEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }
  var requestInitialFocus by remember { mutableStateOf(true) }

  BackHandler { onEvent(LocalBackupRestoreEvents.Cancel) }

  PigeonRequestFocus(focusRequester)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.LOCAL_BACKUP_RESTORE_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__no_backup_found),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    Rows.TextRow(
      text = stringResource(R.string.LocalBackupRestoreScreen__try_again),
      onClick = { onEvent(LocalBackupRestoreEvents.PickBackupFolder) },
      modifier = Modifier
        .focusRequester(focusRequester)
        .onGloballyPositioned {
          if (requestInitialFocus) {
            requestInitialFocus = false
            runCatching { focusRequester.requestFocus() }
          }
        }
    )
  }
}

@Preview(
  name = "MP02",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonLocalBackupFoundLayoutPreview() {
  val backups = listOf(
    LocalBackupInfo(
      type = LocalBackupInfo.BackupType.V1,
      date = LocalDateTime.of(2026, 10, 12, 20, 18, 48),
      name = "signal-2026-10-12-20-18-48.backup",
      uri = Uri.EMPTY,
      sizeBytes = 9_087_966
    ),
    LocalBackupInfo(
      type = LocalBackupInfo.BackupType.V1,
      date = LocalDateTime.of(2026, 10, 12, 14, 22, 28),
      name = "signal-2026-10-12-14-22-28.backup",
      uri = Uri.EMPTY,
      sizeBytes = 9_087_966
    )
  )

  Previews.Preview {
    PigeonLocalBackupFoundLayout(
      backups = backups,
      selectedBackup = backups.first(),
      onEvent = {}
    )
  }
}

@Preview(
  name = "MP02 – no backup",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonLocalBackupNotFoundLayoutPreview() {
  Previews.Preview {
    PigeonLocalBackupNotFoundLayout(onEvent = {})
  }
}

