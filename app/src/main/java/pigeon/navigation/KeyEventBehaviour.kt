package pigeon.navigation

import android.app.Activity
import android.view.KeyEvent
import androidx.fragment.app.FragmentManager

open interface KeyEventBehaviour {
  fun dispatchKeyEvent(event: KeyEvent, fragmentManager: FragmentManager, activity: Activity)
  fun dispatchConversationKeyEvent(event: KeyEvent, fragmentManager: FragmentManager)
}