package pigeon.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import org.signal.core.util.concurrent.SignalExecutors
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.MainNavigator.REQUEST_CONFIG_CHANGES
import org.thoughtcrime.securesms.components.settings.app.AppSettingsActivity.Companion.home
import org.thoughtcrime.securesms.conversation.NewConversationActivity
import org.thoughtcrime.securesms.database.SignalDatabase.Companion.threads
import org.thoughtcrime.securesms.dependencies.AppDependencies.messageNotifier
import org.thoughtcrime.securesms.groups.ui.creategroup.CreateGroupActivity
import org.thoughtcrime.securesms.notifications.MarkReadReceiver
import org.thoughtcrime.securesms.permissions.Permissions
import pigeon.compose.HomePageScreen
import pigeon.extensions.cancelNotifications


class HomePageFragment : Fragment() {

  private lateinit var mainActivity: MainActivity
  private val isSearchVisible = mutableStateOf(true)

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      return ComposeView(requireContext()).apply {
          setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    mainActivity = context as MainActivity
  }

  private fun View.findParentNestedScrollView(): NestedScrollView? {
    var parent = this.parent
    while (parent != null) {
      if (parent is NestedScrollView) {
        return parent
      }
      parent = parent.parent
    }
    return null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val nestedScrollView = view.findParentNestedScrollView()
        ?: throw IllegalStateException("NestedScrollView not found")

    (view as ComposeView).setContent {
        HomePageScreen(
            nestedScrollView = nestedScrollView,
            onNewMessage = { handleNewMessage() },
            onNewGroup = { goToGroupCreation() },
            onMarkAllRead = { handleMarkAllRead() },
            onSettings = { handleAppSettings() },
            onSearch = { mainActivity.collapseHomePage() },
            isSearchVisible = isSearchVisible.value
        )
    }
  }

  private fun handleNewMessage() {
    startActivity(Intent(requireActivity(), NewConversationActivity::class.java))
  }

  private fun handleAppSettings() {
    requireActivity().startActivityForResult(home(requireContext()), REQUEST_CONFIG_CHANGES)
  }

  private fun goToGroupCreation() {
    requireActivity().startActivity(CreateGroupActivity.createIntent(requireContext()))
  }

  private fun handleMarkAllRead() {
    val context = requireContext()
    context.cancelNotifications()
    SignalExecutors.BOUNDED.execute {
      val messageIds = threads.setAllThreadsRead()
      messageNotifier.updateNotification(context)
      MarkReadReceiver.process(messageIds)
    }
  }

  fun setupSearchButtonState(searchButtonVisibility: Boolean) {
      isSearchVisible.value = searchButtonVisibility
  }
}