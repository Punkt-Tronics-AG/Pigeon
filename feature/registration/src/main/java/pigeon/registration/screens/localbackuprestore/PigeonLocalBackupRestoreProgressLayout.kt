package pigeon.registration.screens.localbackuprestore

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import org.signal.registration.screens.localbackuprestore.LocalBackupRestoreState
import org.signal.registration.screens.shared.RestoreProgress
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus

/**
 * PIGEON: restore-in-progress step for the MP02. Signal shows this inside the registration scaffold with a
 * "Cancel" button; on the 240×320 screen that layout leaves no visible feedback, so render a plain title,
 * progress bar and percentage instead. The restore writes straight into the database, so the hardware Back key
 * is swallowed while it runs – the user has to wait for it to finish.
 */
@Composable
internal fun PigeonLocalBackupRestoreProgressLayout(
  state: LocalBackupRestoreState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isPreparing = state.restorePhase == LocalBackupRestoreState.RestorePhase.Preparing
  val fraction = state.progressFraction.coerceIn(0f, 1f)

  BackHandler { }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.LOCAL_BACKUP_RESTORE_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__restoring_backup),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    if (isPreparing) {
      LinearProgressIndicator(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp)
          .testTag(TestTags.LOCAL_BACKUP_RESTORE_PROGRESS_BAR)
      )
    } else {
      LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp)
          .testTag(TestTags.LOCAL_BACKUP_RESTORE_PROGRESS_BAR)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    val progressLabel = if (isPreparing) {
      stringResource(R.string.LocalBackupRestoreScreen__preparing_restore)
    } else {
      val bytes = state.restoreProgress
      if (bytes != null && bytes.totalBytes > 0) {
        "${(fraction * 100).toInt()}% · ${Formatter.formatShortFileSize(context, bytes.bytesCompleted)} / ${Formatter.formatShortFileSize(context, bytes.totalBytes)}"
      } else {
        "${(fraction * 100).toInt()}%"
      }
    }

    Text(
      text = progressLabel,
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__restoring_description),
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
    )
  }
}

/**
 * PIGEON: "looking for backups" step – shown while the fixed backup directory is scanned.
 */
@Composable
internal fun PigeonLocalBackupScanningLayout(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag(TestTags.LOCAL_BACKUP_RESTORE_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__scanning_folder),
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    LinearProgressIndicator(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp)
    )
  }
}

/**
 * PIGEON: result step shown when the restore stops – wrong passphrase/recovery key or an error. One focusable
 * "Try again" row; the hardware Back key cancels the restore flow.
 */
@Composable
internal fun PigeonLocalBackupRestoreFailedLayout(
  headline: String,
  body: String,
  onTryAgain: () -> Unit,
  onEvent: (LocalBackupRestoreEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

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
      text = headline,
      fontSize = 24.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 10.dp)
    )

    Text(
      text = body,
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
    )

    Rows.TextRow(
      text = stringResource(R.string.LocalBackupRestoreScreen__try_again),
      onClick = onTryAgain,
      modifier = Modifier.focusRequester(focusRequester)
    )
  }
}

@Composable
internal fun PigeonLocalBackupIncorrectCredentialLayout(
  backupType: LocalBackupInfo.BackupType?,
  onEvent: (LocalBackupRestoreEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val headline = if (backupType == LocalBackupInfo.BackupType.V1) {
    stringResource(R.string.LocalBackupRestoreScreen__incorrect_passphrase)
  } else {
    stringResource(R.string.LocalBackupRestoreScreen__incorrect_recovery_key)
  }

  PigeonLocalBackupRestoreFailedLayout(
    headline = headline,
    body = stringResource(R.string.LocalBackupRestoreScreen__incorrect_credential_description),
    onTryAgain = { onEvent(LocalBackupRestoreEvents.RestoreBackup) },
    onEvent = onEvent,
    modifier = modifier
  )
}

@Composable
internal fun PigeonLocalBackupRestoreErrorLayout(
  errorMessage: String?,
  onEvent: (LocalBackupRestoreEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  PigeonLocalBackupRestoreFailedLayout(
    headline = stringResource(R.string.LocalBackupRestoreScreen__restore_failed),
    body = errorMessage ?: stringResource(R.string.LocalBackupRestoreScreen__restore_failed_description),
    onTryAgain = { onEvent(LocalBackupRestoreEvents.PickBackupFolder) },
    onEvent = onEvent,
    modifier = modifier
  )
}

@Preview(
  name = "MP02 – preparing",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonLocalBackupRestoreProgressLayoutPreparingPreview() {
  Previews.Preview {
    PigeonLocalBackupRestoreProgressLayout(
      state = LocalBackupRestoreState(restorePhase = LocalBackupRestoreState.RestorePhase.Preparing)
    )
  }
}

@Preview(
  name = "MP02 – in progress",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonLocalBackupRestoreProgressLayoutInProgressPreview() {
  Previews.Preview {
    PigeonLocalBackupRestoreProgressLayout(
      state = LocalBackupRestoreState(
        restorePhase = LocalBackupRestoreState.RestorePhase.InProgress,
        progressFraction = 0.65f,
        restoreProgress = RestoreProgress(
          phase = RestoreProgress.Phase.Restoring,
          bytesCompleted = 5_900_000,
          totalBytes = 9_087_966
        )
      )
    )
  }
}

@Preview(
  name = "MP02 – incorrect passphrase",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonLocalBackupIncorrectCredentialLayoutPreview() {
  Previews.Preview {
    PigeonLocalBackupIncorrectCredentialLayout(backupType = LocalBackupInfo.BackupType.V1, onEvent = {})
  }
}
