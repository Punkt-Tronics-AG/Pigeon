package pigeon.navigation

import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.conversation.v2.ConversationFragment
import org.thoughtcrime.securesms.registration.fragments.CaptchaFragment

class PigeonKeyEventBehaviourImpl : KeyEventBehaviour {
  override fun dispatchKeyEvent(event: KeyEvent, fragmentManager: FragmentManager) {
    val navFragment: Fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment) ?: return
    Log.w(org.thoughtcrime.securesms.longmessage.TAG, "$event")

    when (event.keyCode) {
      KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_0 -> {
        val fragment = navFragment.childFragmentManager.primaryNavigationFragment
        if (fragment is CaptchaFragment) {
          fragment.onKeyDown(event.keyCode, event.action)
          return
        }
      }
    }
  }

  override fun dispatchConversationKeyEvent(event: KeyEvent, fragmentManager: FragmentManager) {
    when (event.keyCode) {
      KeyEvent.KEYCODE_CALL -> {
        val conversationFragment = fragmentManager.fragments.find { it is ConversationFragment }
        if (conversationFragment != null && conversationFragment is ConversationFragment && event.action == KeyEvent.ACTION_UP) {
          conversationFragment.onKeycodeCallPressed()
          return
        }
      }
    }
  }
}