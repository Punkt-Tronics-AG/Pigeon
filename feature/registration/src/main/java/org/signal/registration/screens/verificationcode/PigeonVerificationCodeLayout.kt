package org.signal.registration.screens.verificationcode

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.signal.core.ui.compose.Rows
import org.signal.registration.R
import org.signal.registration.test.TestTags
import pigeon.compose.PigeonTextField
import pigeon.compose.PigeonRequestFocus
import kotlin.time.Duration

/**
 * PIGEON: D-pad friendly verification code entry for the MP02. A single numeric field is typed on the keypad; once all
 * digits are present focus jumps to "Next" which submits. Resend / call-me / wrong-number are plain rows below.
 * Incoming SMS are watched directly (no Play Services on the device) and auto-fill the code.
 */
@Composable
internal fun PigeonVerificationCodeLayout(
  state: VerificationCodeState,
  onEvent: (VerificationCodeScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val codeFocus = remember { FocusRequester() }
  val nextFocus = remember { FocusRequester() }
  var code by remember { mutableStateOf(state.code) }
  val isComplete = code.length == VerificationCodeState.CODE_LENGTH

  PigeonSmsCodeObserver(onCode = { onEvent(VerificationCodeScreenEvents.CodeAutoFilled(it)) })

  PigeonRequestFocus(codeFocus)

  // Mirror auto-filled / reset digits coming from the view model into the local field.
  LaunchedEffect(state.digits) {
    val fromState = state.code
    if (fromState != code && (state.isComplete || fromState.isEmpty())) {
      code = fromState
    }
  }

  LaunchedEffect(isComplete) {
    if (isComplete) {
      runCatching { nextFocus.requestFocus() }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = stringResource(R.string.VerificationCodeScreen__enter_the_code_we_sent_to_s, state.e164),
      fontSize = 20.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 10.dp, top = 16.dp, end = 10.dp, bottom = 4.dp)
    )

    PigeonTextField(
      value = code,
      onValueChange = { newValue ->
        val digits = newValue.filter { it.isDigit() }.take(VerificationCodeState.CODE_LENGTH)
        code = digits
      },
      placeholder = stringResource(R.string.VerificationCodeScreen__verification_code),
      enabled = !state.isSubmittingCode,
      downFocus = nextFocus,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier
        .focusRequester(codeFocus)
        .testTag(TestTags.VERIFICATION_CODE_INPUT)
    )

    Rows.TextRow(
      text = stringResource(R.string.RegistrationActivity_next),
      onClick = { if (isComplete && !state.isSubmittingCode) onEvent(VerificationCodeScreenEvents.CodeEntered(code)) },
      modifier = Modifier.focusRequester(nextFocus)
    )

    Rows.TextRow(
      text = countdownLabel(
        countdown = state.smsResendCountdown(),
        withCountdown = R.string.VerificationCodeScreen__resend_code_available_in,
        ready = R.string.VerificationCodeScreen__resend_code
      ),
      onClick = { onEvent(VerificationCodeScreenEvents.ResendSms) },
      enabled = state.canResendSms(),
      modifier = Modifier.testTag(TestTags.VERIFICATION_CODE_RESEND_SMS_BUTTON)
    )

    Rows.TextRow(
      text = countdownLabel(
        countdown = state.callRequestCountdown(),
        withCountdown = R.string.VerificationCodeScreen__call_me_available_in,
        ready = R.string.VerificationCodeScreen__call_me_instead
      ),
      onClick = { onEvent(VerificationCodeScreenEvents.CallMe) },
      enabled = state.canRequestCall(),
      modifier = Modifier.testTag(TestTags.VERIFICATION_CODE_CALL_ME_BUTTON)
    )

    Rows.TextRow(
      text = stringResource(R.string.VerificationCodeScreen__wrong_number),
      onClick = { onEvent(VerificationCodeScreenEvents.WrongNumber) },
      enabled = !state.isSubmittingCode,
      modifier = Modifier.testTag(TestTags.VERIFICATION_CODE_WRONG_NUMBER_BUTTON)
    )
  }
}

@Composable
private fun countdownLabel(countdown: Duration?, withCountdown: Int, ready: Int): String {
  if (countdown == null) return stringResource(ready)
  val totalSeconds = countdown.inWholeSeconds.toInt()
  return stringResource(withCountdown, totalSeconds / 60, totalSeconds % 60)
}

/**
 * Watches the SMS inbox for a Signal verification message ("SIGNAL … 123-456") and reports the code.
 * Replaces the Play Services SMS retriever, which is unavailable on the MP02.
 */
@Composable
private fun PigeonSmsCodeObserver(onCode: (String) -> Unit) {
  val context = LocalContext.current
  val currentOnCode by rememberUpdatedState(onCode)

  DisposableEffect(context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
      return@DisposableEffect onDispose {}
    }

    val pattern = Regex("SIGNAL.+?(\\d{3}-?\\d{3})")
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (uri?.toString() == "content://sms/raw") return
        context.contentResolver.query(Uri.parse("content://sms/inbox"), arrayOf("body"), null, null, "date desc")?.use { cursor ->
          if (!cursor.moveToFirst()) return
          val body = cursor.getString(0) ?: return
          pattern.find(body)?.groupValues?.get(1)?.replace("-", "")?.let(currentOnCode)
        }
      }
    }

    context.contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
    onDispose { context.contentResolver.unregisterContentObserver(observer) }
  }
}
