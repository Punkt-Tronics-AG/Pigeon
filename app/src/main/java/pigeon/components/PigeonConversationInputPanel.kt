package pigeon.components

import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.doAfterTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import org.signal.core.models.recipients.RecipientId
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ComposeText
import org.thoughtcrime.securesms.components.InputPanel
import org.thoughtcrime.securesms.components.SendButton
import org.thoughtcrime.securesms.conversation.ui.mentions.MentionsPickerRepositoryV2
import org.thoughtcrime.securesms.database.MentionUtil
import org.thoughtcrime.securesms.recipients.Recipient
import pigeon.compose.PigeonConversationInputActions
import pigeon.compose.PigeonConversationInputActionsListener
import pigeon.compose.PigeonConversationInputActionsState
import pigeon.compose.PigeonMentionCandidate

/**
 * Pigeon (MP02): glue between the conversation input panel (`layout-small/conversation_input_panel.xml`)
 * and the Compose-rendered Send / actions sub-menu ([PigeonConversationInputActions]).
 *
 * All views are looked up in [root], which must be the already-inflated conversation content
 * (NOT the fragment's ComposeView root – the content is attached to it asynchronously).
 */
class PigeonConversationInputPanel(
  root: View,
  private val inputPanel: InputPanel,
  private val composeText: ComposeText,
  private val sendButton: SendButton,
  private val callbacks: Callbacks
) {

  interface Callbacks {
    fun onDial()
    fun onVideoCall()
    fun onConversationSettings()
    fun onResetSecureSession()
  }

  private val composeContainer: View? = root.findViewById(R.id.pigeon_compose_container)
  private val recordTime: TextView? = root.findViewById(R.id.record_time)
  private val mentionsRepository = MentionsPickerRepositoryV2()
  private var mentionDisposable: Disposable? = null
  private var participantIds: List<RecipientId> = emptyList()

  private var state by mutableStateOf(PigeonConversationInputActionsState())

  val isCallVisible: Boolean
    get() = state.showCall

  val isGroupCallVisible: Boolean
    get() = state.showGroupCall

  private val listener = object : PigeonConversationInputActionsListener {
    override fun onExpand() {
      composeContainer?.visibility = View.GONE
      state = state.copy(isExpanded = true)
    }

    override fun onCollapse() {
      collapse()
      composeText.requestFocus()
    }

    override fun onSend() {
      sendButton.performClick()
      collapse()
      composeText.requestFocus()
    }

    override fun onVoiceMessage() {
      if (state.isRecordingVoice) {
        inputPanel.onRecordReleased()
        state = state.copy(isRecordingVoice = false)
      } else {
        inputPanel.onRecordPressed()
        state = state.copy(isRecordingVoice = true, recordingTime = recordTime?.text?.toString() ?: "00:00")
      }
    }

    override fun onMentionCancel() {
      mentionDisposable?.dispose()
      state = state.copy(mentionCandidates = null)
    }

    override fun onCall() = callbacks.onDial()

    override fun onMention() {
      // Replace the actions with the member list; selecting a member inserts the mention token.
      mentionDisposable?.dispose()
      mentionDisposable = mentionsRepository
        .search("", participantIds)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { members ->
          val candidates = members.map { PigeonMentionCandidate(it.id, it.getDisplayName(composeText.context)) }
          if (candidates.isNotEmpty()) {
            state = state.copy(mentionCandidates = candidates)
          }
        }
    }

    override fun onMentionSelected(candidate: PigeonMentionCandidate) {
      // ComposeText.replaceTextWithMention() replaces the text from the last '@' before the cursor, so insert one
      // first – that is also what the user would have typed on a phone with a full keyboard.
      composeText.text?.let { text ->
        val cursor = composeText.selectionEnd.coerceAtLeast(0)
        text.insert(cursor, MentionUtil.MENTION_STARTER.toString())
        composeText.setSelection(cursor + 1)
        composeText.replaceTextWithMention(candidate.displayName, candidate.recipientId)
      }
      collapse()
      composeText.requestFocus()
    }
    override fun onGroupCall() = callbacks.onVideoCall()
    override fun onConversationSettings() = callbacks.onConversationSettings()
    override fun onResetSecureSession() = callbacks.onResetSecureSession()
  }

  init {
    // `record_time` lives in the hidden Signal chrome of `layout-small/conversation_input_panel.xml`, so it is never
    // visible on the MP02. InputPanel's RecordTime still updates it every second (and enforces the length limit),
    // so mirror its text into the Compose action row instead of rendering the TextView itself.
    recordTime?.doAfterTextChanged { text ->
      if (state.isRecordingVoice) {
        state = state.copy(recordingTime = text?.toString() ?: "00:00")
      }
    }

    root.findViewById<ComposeView>(R.id.pigeon_input_actions)?.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        SignalTheme {
          PigeonConversationInputActions(state = state, listener = listener)
        }
      }

      // Collapse once focus leaves the panel for good. Checked on the View level – the ComposeView keeps
      // View focus while Compose moves focus between rows, so there is no transient "unfocused" state there.
      // Swapping the row set (actions ↔ mention list) does drop View focus for one frame, though, until the
      // LaunchedEffect in PigeonConversationInputActions re-focuses a row – so re-check on the next frame.
      val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
        if (state.isExpanded && !hasFocus()) {
          post {
            if (state.isExpanded && !hasFocus()) {
              collapse()
            }
          }
        }
      }
      addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = v.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
        override fun onViewDetachedFromWindow(v: View) = v.viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
      })
      if (isAttachedToWindow) {
        viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
      }
    }
  }

  fun bindRecipient(recipient: Recipient) {
    val isGroup = recipient.isGroup
    participantIds = if (isGroup) recipient.participantIds else emptyList()
    state = state.copy(
      showCall = !isGroup,
      showGroupCall = isGroup,
      showSettings = isGroup,
      showResetSecureSession = !isGroup,
      showMention = recipient.isPushV2Group
    )
  }

  /**
   * Called when InputPanel reports that recording stopped for any reason (sent, canceled, limit hit, fragment
   * paused) so the "Send voice message" row does not stay in its recording state.
   */
  fun onRecordingStopped() {
    if (state.isRecordingVoice) {
      state = state.copy(isRecordingVoice = false)
    }
  }

  private fun collapse() {
    mentionDisposable?.dispose()
    composeContainer?.visibility = View.VISIBLE
    state = state.copy(isExpanded = false, mentionCandidates = null)
  }
}
