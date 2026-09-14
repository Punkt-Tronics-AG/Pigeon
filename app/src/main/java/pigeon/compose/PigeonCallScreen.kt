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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.thoughtcrime.securesms.components.webrtc.WebRtcAudioOutput
import org.thoughtcrime.securesms.components.webrtc.v2.CallControlsState
import org.thoughtcrime.securesms.components.webrtc.v2.CallScreenControlsListener
import org.thoughtcrime.securesms.recipients.Recipient

/**
 * Simplified full-screen call UI for the Pigeon / MP02 build.
 *
 * Displays only:
 *  - recipient name
 *  - call status
 *  - focusable text-based call controls (identical focus behaviour to HomePageScreen)
 *
 * Everything else (video grid, PiP, bottom sheet, reactions, overflow…) is intentionally omitted.
 */
@Composable
fun PigeonCallScreen(
  callRecipient: Recipient,
  callStatus: String?,
  callControlsState: CallControlsState,
  callScreenControlsListener: CallScreenControlsListener
) {
  val scrollState = rememberScrollState()

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
      // Recipient name
      Text(
        text = callRecipient.getDisplayName(LocalContext.current),
        fontSize = 24.sp,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
      )

      // Call status (Connecting…, Connected, etc.)
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

      // Controls — same focus style as HomePageButton, focus lands on End Call
      PigeonCallControls(
        callControlsState = callControlsState,
        isSpeakerOn = callControlsState.audioOutput == WebRtcAudioOutput.SPEAKER,
        callScreenControlsListener = callScreenControlsListener,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
