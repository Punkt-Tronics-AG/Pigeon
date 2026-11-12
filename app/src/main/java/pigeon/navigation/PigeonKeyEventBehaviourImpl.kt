package pigeon.navigation

import android.app.Activity
import android.view.KeyEvent
import androidx.fragment.app.FragmentManager
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.conversation.v2.ConversationFragment
import org.thoughtcrime.securesms.ratelimit.RecaptchaProofActivity

class PigeonKeyEventBehaviourImpl : KeyEventBehaviour {
  override fun dispatchKeyEvent(event: KeyEvent, fragmentManager: FragmentManager, activity: Activity) {
    Log.w(org.thoughtcrime.securesms.longmessage.TAG, "$event")
    Log.w(org.thoughtcrime.securesms.longmessage.TAG, "$activity")

    when (event.keyCode) {
      KeyEvent.KEYCODE_2,
      KeyEvent.KEYCODE_4,
      KeyEvent.KEYCODE_6,
      KeyEvent.KEYCODE_8,
      KeyEvent.KEYCODE_5,
      KeyEvent.KEYCODE_0,
      KeyEvent.KEYCODE_DPAD_UP,
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_LEFT,
      KeyEvent.KEYCODE_DPAD_RIGHT,
      KeyEvent.KEYCODE_DPAD_CENTER,
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_STAR,
      KeyEvent.KEYCODE_POUND,
      KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> {
        if (activity is RecaptchaProofActivity) {
          activity.onKeyDown(event.keyCode, event.action)
          return
        }
      }
    }
  }

  override fun dispatchConversationKeyEvent(event: KeyEvent, fragmentManager: FragmentManager) {
    Log.w("PIGEON", "dispatchConversationKeyEvent CALLED: keyCode=${event.keyCode}, action=${event.action}, event=$event")
    when (event.keyCode) {
      KeyEvent.KEYCODE_CALL -> {
        val conversationFragment = fragmentManager.fragments.find { it is ConversationFragment }
        if (conversationFragment != null && conversationFragment is ConversationFragment && event.action == KeyEvent.ACTION_UP) {
          conversationFragment.onKeycodeCallPressed()
          return
        }
      }
      KeyEvent.KEYCODE_DPAD_CENTER -> {
        if (event.action == KeyEvent.ACTION_UP) {
          val conversationFragment = fragmentManager.fragments.find { it is ConversationFragment }
          if (conversationFragment != null && conversationFragment is ConversationFragment) {
            conversationFragment.pigeonOpenFocusedItemPhotoIfPresent()
          }
        }
      }
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        // Intercept on ACTION_DOWN — by ACTION_UP the framework's default focus search
        // has already moved focus, which would defeat the purpose of this handler.
        val conversationFragment = fragmentManager.fragments.find { it is ConversationFragment } as? ConversationFragment
        conversationFragment?.pigeonFocusComposeFromList()
      }
    }
    return
  }
}