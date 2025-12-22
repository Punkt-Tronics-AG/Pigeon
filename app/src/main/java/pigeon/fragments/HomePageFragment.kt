package pigeon.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import org.signal.core.util.concurrent.SignalExecutors
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.MainNavigator.REQUEST_CONFIG_CHANGES
import org.thoughtcrime.securesms.components.settings.app.AppSettingsActivity.Companion.home
import org.thoughtcrime.securesms.conversation.NewConversationActivity
import org.thoughtcrime.securesms.database.SignalDatabase.Companion.threads
import org.thoughtcrime.securesms.databinding.PigeonFragmentHomePageBinding
import org.thoughtcrime.securesms.dependencies.AppDependencies.messageNotifier
import org.thoughtcrime.securesms.groups.ui.creategroup.CreateGroupActivity
import org.thoughtcrime.securesms.notifications.MarkReadReceiver
import org.thoughtcrime.securesms.permissions.Permissions
import pigeon.base.PigeonBaseFragment
import pigeon.components.CentreFocusScroller
import pigeon.extensions.cancelNotifications


class HomePageFragment : PigeonBaseFragment<PigeonFragmentHomePageBinding>() {

  private lateinit var mainActivity: MainActivity
  private lateinit var scroller: CentreFocusScroller

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }

  override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): PigeonFragmentHomePageBinding {
    return PigeonFragmentHomePageBinding.inflate(inflater, container, false)
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

    scroller = view.findParentNestedScrollView()?.let { nestedScrollView ->
      CentreFocusScroller(nestedScrollView)
    } ?: throw IllegalStateException("NestedScrollView not found")
    binding?.run {


      newMessageButton.setOnClickListener { handleNewMessage() }
      newMessageButton.setOnFocusChangeListener { v, b ->
        scroller.onFocusChange(v, b)
      }

      markAllReadButton.setOnClickListener {
        handleMarkAllRead()
      }
      markAllReadButton.setOnFocusChangeListener { v, b ->
        scroller.onFocusChange(v, b)
      }

      settingsButton.setOnClickListener { handleAppSettings() }
      settingsButton.setOnFocusChangeListener { v, b ->
        scroller.onFocusChange(v, b)
      }

      newGroupButton.setOnClickListener { goToGroupCreation() }
      newGroupButton.setOnFocusChangeListener { v, b ->
        scroller.onFocusChange(v, b)
      }

      searchButton.setOnClickListener {
        mainActivity.collapseHomePage()
      }
      searchButton.setOnFocusChangeListener { v, b ->
        scroller.onFocusChange(v, b)
      }
    }

      binding?.newMessageButton?.post {
        binding?.newMessageButton?.requestFocus()
      }
  }

  private fun handleNewMessage() {
    startActivity(Intent(requireActivity(), NewConversationActivity::class.java))
  }

  private fun handleAppSettings() {
    requireActivity().startActivityForResult(home(requireContext()), REQUEST_CONFIG_CHANGES)
  }

  private fun goToGroupCreation() {
    requireActivity().startActivity(CreateGroupActivity.createIntent(requireContext()));
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
    binding?.searchButton?.let { searchButton ->
      if (searchButtonVisibility) {
        searchButton.visibility = View.VISIBLE
        searchButton.requestFocus()
      } else {
        searchButton.visibility = View.GONE
      }
    }
  }
}