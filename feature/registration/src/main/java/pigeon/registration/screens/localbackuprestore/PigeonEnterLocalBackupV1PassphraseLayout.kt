package pigeon.registration.screens.localbackuprestore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonRequestFocus
import pigeon.compose.PigeonTextField

private const val PASSPHRASE_LENGTH = 30
private const val CHUNK_SIZE = 5
private const val CHUNKS_PER_ROW = 3

/**
 * PIGEON: D-pad friendly V1 backup passphrase entry for the MP02. A single numeric field is typed on the keypad;
 * once all 30 digits are present focus jumps to "Next", which submits. The hardware Back key cancels, so there is
 * no separate "No passphrase?" button.
 */
@Composable
internal fun PigeonEnterLocalBackupV1PassphraseLayout(
  onSubmit: (String) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
  initialPassphrase: String = ""
) {
  val passphraseFocus = remember { FocusRequester() }
  val nextFocus = remember { FocusRequester() }
  var passphrase by rememberSaveable { mutableStateOf(initialPassphrase) }
  val isComplete = passphrase.length == PASSPHRASE_LENGTH

  BackHandler(onBack = onCancel)

  PigeonRequestFocus(passphraseFocus)

  // Once all digits are in, hand focus to "Next". The row only becomes a real focus target after this recomposition
  // settles, so retry briefly instead of relying on a single request.
  LaunchedEffect(isComplete) {
    if (isComplete) {
      repeat(10) {
        if (runCatching { nextFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        delay(50)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .testTag(TestTags.ENTER_LOCAL_BACKUP_PASSPHRASE_SCREEN)
  ) {
    Text(
      text = stringResource(R.string.LocalBackupRestoreScreen__enter_backup_passphrase),
      fontSize = 20.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 16.dp, end = 10.dp, bottom = 4.dp)
    )

    PigeonTextField(
      value = passphrase.chunkedForDisplay(),
      onValueChange = { newValue ->
        passphrase = newValue.filter { it.isDigit() }.take(PASSPHRASE_LENGTH)
      },
      placeholder = stringResource(R.string.LocalBackupRestoreScreen__passphrase),
      downFocus = nextFocus,
      singleLine = false,
      minLines = 2,
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next,
        autoCorrectEnabled = false
      ),
      keyboardActions = KeyboardActions(
        onNext = { if (isComplete) onSubmit(passphrase) }
      ),
      modifier = Modifier
        .focusRequester(passphraseFocus)
        .testTag(TestTags.ENTER_LOCAL_BACKUP_PASSPHRASE_INPUT)
    )

    Rows.TextRow(
      text = stringResource(R.string.LocalBackupRestoreScreen__next),
      onClick = { if (isComplete) onSubmit(passphrase) },
      modifier = Modifier
        .focusRequester(nextFocus)
        .testTag(TestTags.ENTER_LOCAL_BACKUP_PASSPHRASE_NEXT_BUTTON)
    )
  }
}

/**
 * Groups the digits in blocks of [CHUNK_SIZE] separated by spaces, [CHUNKS_PER_ROW] blocks per row – the full
 * 30-digit passphrase does not fit on one line of the 240 dp wide MP02 screen.
 */
private fun String.chunkedForDisplay(): String {
  return chunked(CHUNK_SIZE)
    .chunked(CHUNKS_PER_ROW)
    .joinToString("\n") { row -> row.joinToString(" ") }
}

@Preview(
  name = "MP02",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonEnterLocalBackupV1PassphraseLayoutPreview() {
  Previews.Preview {
    PigeonEnterLocalBackupV1PassphraseLayout(onSubmit = {}, onCancel = {})
  }
}

@Preview(
  name = "MP02 – complete",
  showBackground = true,
  device = "spec:width=240dp,height=320dp"
)
@Composable
private fun PigeonEnterLocalBackupV1PassphraseLayoutFilledPreview() {
  Previews.Preview {
    PigeonEnterLocalBackupV1PassphraseLayout(
      onSubmit = {},
      onCancel = {},
      initialPassphrase = "433967179743265042220573935883"
    )
  }
}

