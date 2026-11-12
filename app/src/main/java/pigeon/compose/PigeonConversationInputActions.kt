package pigeon.compose

import android.view.KeyEvent.KEYCODE_BACK
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import org.signal.core.models.recipients.RecipientId
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
  val firstMentionFocusRequester = remember { FocusRequester() }
  val mentionRowFocusRequester = remember { FocusRequester() }
  var wasShowingMentionList by remember { mutableStateOf(false) }

  // The "Mention" row that had focus is replaced by the member list – move focus onto its first entry; when the
  // list is dismissed without a selection (Back), return focus to the "Mention" row.
  LaunchedEffect(state.mentionCandidates) {
    val showingList = !state.mentionCandidates.isNullOrEmpty()
    val target = when {
      showingList -> firstMentionFocusRequester
      wasShowingMentionList -> mentionRowFocusRequester
      else -> null
    }
    wasShowingMentionList = showingList
    if (target != null) {
      // The target row is attached by the recomposition that triggered this effect; retry briefly in case the
      // focus system has not picked it up yet.
      repeat(10) {
        if (runCatching { target.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        delay(16)
      }
    }
  }

  // Back while the member list is open closes the list instead of leaving the conversation.
  val mentionListBackHandler = Modifier.onPreviewKeyEvent { event ->
    if (event.key.nativeKeyCode == KEYCODE_BACK) {
      if (event.type == KeyEventType.KeyUp) listener.onMentionCancel()
      true
    } else {
      false
    }
  }

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
      if (state.mentionCandidates != null) {
        // PIGEON: '@' is hard to type on the T9 keypad, so members are picked from this list instead.
        state.mentionCandidates.forEachIndexed { index, candidate ->
          PigeonInputActionRow(
            text = "@${candidate.displayName}",
            onClick = { listener.onMentionSelected(candidate) },
            modifier = mentionListBackHandler.then(if (index == 0) Modifier.focusRequester(firstMentionFocusRequester) else Modifier)
          )
        }
        return@Column
      }

      PigeonInputActionRow(
        text = if (state.isRecordingVoice) {
          // Keep the elapsed time visible while recording – the MP02 has no other recording indicator.
          "${stringResource(R.string.conversation__menu_voice_message_send)}  ${state.recordingTime}"
        } else {
          stringResource(R.string.conversation__menu_voice_message)
        },
        onClick = listener::onVoiceMessage
      )
      if (state.showMention) {
        PigeonInputActionRow(
          text = stringResource(R.string.Pigeon_conversation__menu_mention),
          onClick = listener::onMention,
          modifier = Modifier.focusRequester(mentionRowFocusRequester)
        )
      }
      if (state.showCall) {
        PigeonInputActionRow(
          text = stringResource(R.string.ConversationSettingsFragment__call),
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

/** PIGEON: one selectable group member in the mention list. */
data class PigeonMentionCandidate(
  val recipientId: RecipientId,
  val displayName: String
)

data class PigeonConversationInputActionsState(
  val isExpanded: Boolean = false,
  val isRecordingVoice: Boolean = false,
  /** Elapsed recording time as formatted by [org.thoughtcrime.securesms.components.InputPanel] ("mm:ss"). */
  val recordingTime: String = "00:00",
  /** Group chats only: shows the "Mention" action. */
  val showMention: Boolean = false,
  /** Non-null while the member list replaces the regular actions (after "Mention" was chosen). */
  val mentionCandidates: List<PigeonMentionCandidate>? = null,
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
  fun onMention()
  fun onMentionSelected(candidate: PigeonMentionCandidate)

  /** Back on the member list: close it without inserting a mention. */
  fun onMentionCancel()
  fun onCall()
  fun onGroupCall()
  fun onConversationSettings()
  fun onResetSecureSession()

  object Empty : PigeonConversationInputActionsListener {
    override fun onExpand() = Unit
    override fun onCollapse() = Unit
    override fun onSend() = Unit
    override fun onVoiceMessage() = Unit
    override fun onMention() = Unit
    override fun onMentionSelected(candidate: PigeonMentionCandidate) = Unit
    override fun onMentionCancel() = Unit
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

@NightPreview
@Composable
private fun PigeonConversationInputActionsRecordingPreview() {
  Previews.Preview {
    PigeonConversationInputActions(
      state = PigeonConversationInputActionsState(isExpanded = true, isRecordingVoice = true, recordingTime = "00:42", showCall = true),
      listener = PigeonConversationInputActionsListener.Empty
    )
  }
}

@NightPreview
@Composable
private fun PigeonConversationInputActionsGroupExpandedPreview() {
  Previews.Preview {
    PigeonConversationInputActions(
      state = PigeonConversationInputActionsState(isExpanded = true, showMention = true, showGroupCall = true, showSettings = true),
      listener = PigeonConversationInputActionsListener.Empty
    )
  }
}

@NightPreview
@Composable
private fun PigeonConversationInputActionsMentionListPreview() {
  Previews.Preview {
    PigeonConversationInputActions(
      state = PigeonConversationInputActionsState(
        isExpanded = true,
        showMention = true,
        mentionCandidates = listOf(
          PigeonMentionCandidate(RecipientId.from(1), "Alice Johnson"),
          PigeonMentionCandidate(RecipientId.from(2), "Bob"),
          PigeonMentionCandidate(RecipientId.from(3), "Charlie Brown")
        )
      ),
      listener = PigeonConversationInputActionsListener.Empty
    )
  }
}
