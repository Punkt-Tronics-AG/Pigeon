/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package pigeon.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.webrtc.v2.CallScreenControlsListener
import org.thoughtcrime.securesms.recipients.Recipient

/**
 * Simplified full-screen incoming call UI for the Pigeon / MP02 build.
 *
 * Displays:
 *  - caller name
 *  - call type label (Pigeon Call / Pigeon Video Call)
 *  - "Answer" button — receives auto-focus on entry
 *  - "Decline" button
 *
 * Focus loops back to "Answer" when navigating past the last item,
 * matching the HomePageScreen navigation paradigm.
 */
@Composable
fun PigeonIncomingCallScreen(
  callRecipient: Recipient,
  callStatus: String?,
  isVideoCall: Boolean,
  callScreenControlsListener: CallScreenControlsListener
) {
  val answerFocusRequester = remember { FocusRequester() }
  val scrollState = rememberScrollState()

  LaunchedEffect(Unit) {
    answerFocusRequester.requestFocus()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(top = 16.dp)
    ) {
      // Call type label
      Text(
        text = if (isVideoCall) {
          stringResource(R.string.Pigeon_WebRtcCallView__signal_video_call)
        } else {
          stringResource(R.string.Pigeon_WebRtcCallView__signal_call)
        },
        fontSize = 18.sp,
        color = Color.White.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
      )

      // Caller name
      Text(
        text = callRecipient.getDisplayName(LocalContext.current),
        fontSize = 24.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
      )

      // Status (e.g. "Incoming call…")
      if (callStatus != null) {
        Text(
          text = callStatus,
          fontSize = 18.sp,
          color = Color.White.copy(alpha = 0.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(start = 30.dp, bottom = 20.dp)
        )
      } else {
        Spacer(modifier = Modifier.height(20.dp))
      }

      // Answer — auto-focused, focus loops back here from Decline
      PigeonCallTextButton(
        text = stringResource(R.string.WebRtcCallScreen__answer),
        onClick = callScreenControlsListener::onAcceptCallPressed,
        modifier = Modifier
          .focusRequester(answerFocusRequester)
          .focusProperties { up = answerFocusRequester }
      )

      // Decline
      PigeonCallTextButton(
        text = stringResource(R.string.WebRtcCallScreen__decline),
        onClick = callScreenControlsListener::onDenyCallPressed
      )
    }
  }
}

