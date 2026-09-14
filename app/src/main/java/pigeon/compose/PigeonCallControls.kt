/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package pigeon.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import org.signal.core.ui.compose.NightPreview
import org.signal.core.ui.compose.Previews
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.webrtc.WebRtcAudioOutput
import org.thoughtcrime.securesms.components.webrtc.v2.CallControlsState
import org.thoughtcrime.securesms.components.webrtc.v2.CallScreenControlsListener

/**
 * Pigeon text-based call controls — replaces icon buttons with focusable
 * text items matching the MP02 physical-key navigation paradigm.
 *
 * Maps 1-to-1 with the XML views:
 *   pigeon_decline          → onDenyCallPressed
 *   pigeon_answer           → onAcceptCallWithVoiceOnlyPressed
 *   pigeon_start_call       → onStartCall
 *   call_screen_error_cancel→ onCancelStartCall
 *   pigeon_hangup           → onEndCallPressed
 *   call_screen_volume_toggle    → onVolumePressed
 *   call_screen_speaker_toggle   → onAudioOutputChanged (speaker on/off)
 *   call_screen_audio_mic_toggle → onMicChanged
 */
@Composable
fun PigeonCallControls(
  callControlsState: CallControlsState,
  isSpeakerOn: Boolean,
  callScreenControlsListener: CallScreenControlsListener,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(callControlsState.displayEndCallButton) {
    if (callControlsState.displayEndCallButton) {
      focusRequester.requestFocus()
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {

    // Start call — visible in pre-join state
    if (callControlsState.displayStartCallButton) {
      PigeonCallTextButton(
        text = stringResource(callControlsState.startCallButtonText),
        onClick = { callScreenControlsListener.onStartCall(callControlsState.isVideoEnabled) }
      )
    }

    // Hang up — always receives initial focus when visible
    if (callControlsState.displayEndCallButton) {
      PigeonCallTextButton(
        text = stringResource(R.string.WebRtcCallView__end_call),
        onClick = callScreenControlsListener::onEndCallPressed,
        modifier = Modifier
          .focusRequester(focusRequester)
          .focusProperties { up = focusRequester }
      )
    }

    // Volume
    if (callControlsState.displayAudioOutputToggle) {
      PigeonCallTextButton(
        text = stringResource(R.string.WebRtcCallControls_volume_button_description),
        onClick = callScreenControlsListener::onVolumePressed
      )
    }

    // Speaker toggle
    if (callControlsState.displayAudioOutputToggle) {
      PigeonCallTextButton(
        text = if (isSpeakerOn) {
          stringResource(R.string.turn_speaker_off)
        } else {
          stringResource(R.string.turn_speaker_on)
        },
        onClick = {
          callScreenControlsListener.onAudioOutputChanged(
            if (isSpeakerOn) WebRtcAudioOutput.HANDSET else WebRtcAudioOutput.SPEAKER
          )
        }
      )
    }

    // Mic toggle
    if (callControlsState.displayMicToggle) {
      PigeonCallTextButton(
        text = if (callControlsState.isMicEnabled) {
          stringResource(R.string.mute)
        } else {
          stringResource(R.string.unmute)
        },
        onClick = { callScreenControlsListener.onMicChanged(!callControlsState.isMicEnabled) }
      )
    }
  }
}

/**
 * Single focusable text button matching the MP02 style.
 * Focus behaviour mirrors HomePageButton — focused item grows and brightens.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PigeonCallTextButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val fontSize = if (isFocused) 40.sp else 24.sp
  val color = if (isFocused) Color.White else Color.White.copy(alpha = 0.5f)
  val startPadding = if (isFocused) 5.dp else 30.dp

  Text(
    text = text,
    fontSize = fontSize,
    color = color,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .fillMaxWidth()
      .padding(start = startPadding, top = 6.dp, bottom = 6.dp)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .focusable(interactionSource = interactionSource)
      .let { if (isFocused) it.basicMarquee() else it }
  )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@NightPreview
@Composable
fun PigeonCallControlsPreview() {
  Previews.Preview {
    PigeonCallControls(
      callControlsState = CallControlsState(
        displayAudioOutputToggle = true,
        displayMicToggle = true,
        isMicEnabled = true,
        displayEndCallButton = true,
        displayStartCallButton = false
      ),
      isSpeakerOn = false,
      callScreenControlsListener = CallScreenControlsListener.Empty
    )
  }
}

@NightPreview
@Composable
fun PigeonCallControlsIncomingPreview() {
  Previews.Preview {
    PigeonCallControls(
      callControlsState = CallControlsState(),
      isSpeakerOn = false,
      callScreenControlsListener = CallScreenControlsListener.Empty
    )
  }
}
