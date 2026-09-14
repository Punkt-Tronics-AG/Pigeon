package pigeon.compose

import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.NightPreview
import org.signal.core.ui.compose.Previews
import org.thoughtcrime.securesms.R

/**
 * Pigeon (MP02): Send button + DPAD sub-menu shown under the compose text in the
 * conversation input panel (replaces the former `send_text` / `extra_buttons` XML views).
 *
 * Collapsed: a single "Send" row. DPAD_DOWN on it expands the action list
 * (Send / Voice / Call / Group call / Settings / Reset session). DPAD_UP on the first
 * action collapses the list and hands focus back to the compose text via [onCollapsed].
 */
@Composable
fun PigeonConversationInputActions(
  state: PigeonConversationInputActionsState,
  listener: PigeonConversationInputActionsListener,
  modifier: Modifier = Modifier
) {
  // Collapsing when focus leaves the panel is handled by the host view (PigeonConversationInputPanel):
  // Compose reports a transient "no focus" while moving between rows, which would collapse the list mid-navigation.
  Column(modifier = modifier.fillMaxWidth()) {
    // The "Send" row is the same node in both states so focus never has to be moved around.
    // Focusing it expands the panel; DPAD_UP on it hands focus back to the compose text
    // (which is hidden while expanded, so default traversal would find nothing above).
    PigeonInputActionRow(
      text = stringResource(R.string.conversation_activity__send),
      onClick = listener::onSend,
      modifier = Modifier
        .onFocusChanged { if (it.isFocused && !state.isExpanded) listener.onExpand() }
        .onPreviewKeyEvent { event ->
          if (event.key.nativeKeyCode == KEYCODE_DPAD_UP) {
            if (event.type == KeyEventType.KeyDown) listener.onCollapse()
            true
          } else {
            false
          }
        }
    )

    if (state.isExpanded) {
      PigeonInputActionRow(
        text = stringResource(
          if (state.isRecordingVoice) R.string.conversation__menu_voice_message_send else R.string.conversation__menu_voice_message
        ),
        onClick = listener::onVoiceMessage
      )
      if (state.showCall) {
        PigeonInputActionRow(
          text = stringResource(R.string.conversation_callable_insecure__menu_call),
          onClick = listener::onCall
        )
      }
      if (state.showGroupCall) {
        PigeonInputActionRow(
          text = stringResource(R.string.Pigeon_group_call),
          onClick = listener::onGroupCall
        )
      }
      if (state.showSettings) {
        PigeonInputActionRow(
          text = stringResource(R.string.conversation__menu_conversation_settings),
          onClick = listener::onConversationSettings
        )
      }
      if (state.showResetSecureSession) {
        PigeonInputActionRow(
          text = stringResource(R.string.ThreadRecord_secure_session_reset),
          onClick = listener::onResetSecureSession
        )
      }
    }
  }
}

data class PigeonConversationInputActionsState(
  val isExpanded: Boolean = false,
  val isRecordingVoice: Boolean = false,
  val showCall: Boolean = false,
  val showGroupCall: Boolean = false,
  val showSettings: Boolean = false,
  val showResetSecureSession: Boolean = false
)

interface PigeonConversationInputActionsListener {
  fun onExpand()

  /** DPAD_UP on "Send": collapse and move focus back to the compose text. */
  fun onCollapse()
  fun onSend()
  fun onVoiceMessage()
  fun onCall()
  fun onGroupCall()
  fun onConversationSettings()
  fun onResetSecureSession()

  object Empty : PigeonConversationInputActionsListener {
    override fun onExpand() = Unit
    override fun onCollapse() = Unit
    override fun onSend() = Unit
    override fun onVoiceMessage() = Unit
    override fun onCall() = Unit
    override fun onGroupCall() = Unit
    override fun onConversationSettings() = Unit
    override fun onResetSecureSession() = Unit
  }
}

/**
 * Focusable text row in the MP02 style (mirrors `Mp02.Signal.Text.MaterialTextButtonFocusable`).
 */
@Composable
private fun PigeonInputActionRow(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  Text(
    text = text,
    fontSize = 24.sp,
    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.5f),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 25.dp, top = 6.dp, bottom = 6.dp)
      .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
      .focusable(interactionSource = interactionSource)
  )
}

@NightPreview
@Composable
private fun PigeonConversationInputActionsCollapsedPreview() {
  Previews.Preview {
    PigeonConversationInputActions(
      state = PigeonConversationInputActionsState(),
      listener = PigeonConversationInputActionsListener.Empty
    )
  }
}

@NightPreview
@Composable
private fun PigeonConversationInputActionsExpandedPreview() {
  Previews.Preview {
    PigeonConversationInputActions(
      state = PigeonConversationInputActionsState(isExpanded = true, showCall = true, showResetSecureSession = true),
      listener = PigeonConversationInputActionsListener.Empty
    )
  }
}
