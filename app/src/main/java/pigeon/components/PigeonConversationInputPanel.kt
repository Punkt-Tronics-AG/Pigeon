package pigeon.components

import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ComposeText
import org.thoughtcrime.securesms.components.InputPanel
import org.thoughtcrime.securesms.components.SendButton
import org.thoughtcrime.securesms.recipients.Recipient
import pigeon.compose.PigeonConversationInputActions
import pigeon.compose.PigeonConversationInputActionsListener
import pigeon.compose.PigeonConversationInputActionsState

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
      } else {
        inputPanel.onRecordPressed()
        recordTime?.visibility = View.VISIBLE
      }
      state = state.copy(isRecordingVoice = !state.isRecordingVoice)
    }

    override fun onCall() = callbacks.onDial()
    override fun onGroupCall() = callbacks.onVideoCall()
    override fun onConversationSettings() = callbacks.onConversationSettings()
    override fun onResetSecureSession() = callbacks.onResetSecureSession()
  }

  init {
    root.findViewById<ComposeView>(R.id.pigeon_input_actions)?.apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        SignalTheme {
          PigeonConversationInputActions(state = state, listener = listener)
        }
      }

      // Collapse once focus leaves the panel for good. Checked on the View level – the ComposeView keeps
      // View focus while Compose moves focus between rows, so there is no transient "unfocused" state here.
      val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
        if (state.isExpanded && !hasFocus()) {
          collapse()
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
    state = state.copy(
      showCall = !isGroup,
      showGroupCall = isGroup,
      showSettings = isGroup,
      showResetSecureSession = !isGroup
    )
  }

  private fun collapse() {
    composeContainer?.visibility = View.VISIBLE
    state = state.copy(isExpanded = false)
  }
}
