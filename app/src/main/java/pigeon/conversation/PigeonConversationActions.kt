package pigeon.conversation

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.orNull
import org.thoughtcrime.securesms.BindableConversationItem
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.conversation.mutiselect.MultiselectPart
import org.thoughtcrime.securesms.conversation.v2.items.InteractiveConversationElement
import org.thoughtcrime.securesms.crypto.SecurityEvent
import org.thoughtcrime.securesms.database.MediaTable
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.mediapreview.MediaIntentFactory
import org.thoughtcrime.securesms.messagerequests.MessageRequestState
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.sms.MessageSender
import pigeon.permissions.PigeonRationaleDialog

/**
 * Pigeon (MP02): conversation-screen behaviour that has no Signal counterpart, kept out of
 * [org.thoughtcrime.securesms.conversation.v2.ConversationFragment] so upstream merges stay clean.
 */
object PigeonConversationActions {

  /** "Take back" from the Pigeon message sub-menu – remote-delete every selected message. */
  fun deleteForEveryone(multiselectParts: Set<MultiselectPart>) {
    val messageRecords = multiselectParts.map(MultiselectPart::getMessageRecord).toSet()
    SignalExecutors.BOUNDED.execute {
      for (message in messageRecords) {
        MessageSender.sendRemoteDelete(message.id)
      }
    }
  }

  /** Asks for confirmation and drops all sessions with a 1:1 [recipient]. */
  fun resetSecureSession(context: Context, recipient: Recipient?) {
    val message = context.getString(R.string.ConversationActivity_reset_secure_session_question) +
      context.getString(R.string.ConversationActivity_this_may_help_if_youre_having_encryption_problems)

    PigeonRationaleDialog.createNonMsgDialog(
      context,
      message,
      R.string.ConversationActivity_reset,
      android.R.string.cancel,
      {
        if (recipient != null && !recipient.isGroup) {
          AppDependencies.protocolStore.aci().deleteAllSessions(recipient.requireServiceId().toString())
          SecurityEvent.broadcastSecurityUpdateEvent(context)
          Toast.makeText(context, R.string.conversation_secure_verified__menu_reset_secure_session, Toast.LENGTH_SHORT).show()
        }
      },
      null,
      null
    ).show()
  }

  /**
   * Builds media-preview args for the thumbnail of the currently focused list item, or null
   * when the focused item has no previewable media.
   */
  fun focusedItemMediaPreviewArgs(recyclerView: RecyclerView, threadRecipient: Recipient): MediaIntentFactory.MediaPreviewArgs? {
    val focusedChild = recyclerView.focusedChild ?: return null
    val viewHolder = recyclerView.getChildViewHolder(focusedChild) ?: return null

    val messageRecord =
      (viewHolder as? InteractiveConversationElement)?.conversationMessage?.messageRecord
        ?: (viewHolder.itemView as? BindableConversationItem)?.conversationMessage?.messageRecord
        ?: return null

    if (!messageRecord.isMms) return null
    val mmsRecord = messageRecord as MmsMessageRecord
    val slide = mmsRecord.slideDeck.thumbnailSlide ?: return null
    val mediaUri = slide.displayUri ?: return null

    return MediaIntentFactory.MediaPreviewArgs(
      threadId = messageRecord.threadId,
      date = messageRecord.timestamp,
      messageId = messageRecord.id,
      fromRecipientId = messageRecord.fromRecipient.id,
      threadRecipientId = threadRecipient.id,
      outgoing = messageRecord.isOutgoing,
      initialMediaUri = mediaUri,
      initialMediaDataUri = slide.uri,
      initialMediaType = slide.contentType,
      initialMediaSize = slide.asAttachment().size,
      initialCaption = slide.caption.orNull(),
      sorting = MediaTable.Sorting.Newest,
      isVideoGif = slide.isVideoGif,
      skipSharedElementTransition = true
    )
  }

  /**
   * True when focus sits on the last focusable position inside [recycler] – i.e. DPAD_DOWN
   * would otherwise go nowhere because the (GONE) input panel is excluded from focus search.
   */
  fun isFocusAtBottomOfList(recycler: RecyclerView): Boolean {
    val focusedChild = recycler.focusedChild ?: return false
    val nextFocus = recycler.focusSearch(focusedChild, View.FOCUS_DOWN)
    return nextFocus == null || nextFocus === focusedChild || !isDescendantOf(nextFocus, recycler)
  }

  /**
   * Message requests that Pigeon accepts automatically instead of showing the "review request" bar:
   * plain individual requests and being added to a group. Blocked / hidden chats, invites and legacy states
   * are left alone so nothing gets unblocked, un-hidden or joined behind the user's back.
   */
  fun shouldAutoAcceptMessageRequest(state: MessageRequestState): Boolean {
    return state.state == MessageRequestState.State.INDIVIDUAL || state.state == MessageRequestState.State.GROUP_V2_ADD
  }

  /** Input panel is visible when at the newest message or when anything inside it has focus. */
  fun shouldShowInputPanel(panel: View, isScrolledToBottom: Boolean): Boolean {
    return panel.findFocus() != null || isScrolledToBottom
  }

  private fun isDescendantOf(view: View, ancestor: View): Boolean {
    var current: View? = view
    while (current != null) {
      if (current === ancestor) return true
      current = current.parent as? View
    }
    return false
  }
}
