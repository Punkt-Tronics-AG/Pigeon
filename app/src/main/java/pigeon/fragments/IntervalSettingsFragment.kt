package pigeon.fragments

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.components.settings.configure
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import pigeon.viewmodels.IntervalSettingsState
import pigeon.viewmodels.IntervalSettingsViewModel
import kotlin.system.exitProcess


class IntervalSettingsFragment : DSLSettingsFragment(R.string.Pigeon_Interval) {

  private lateinit var viewModel: IntervalSettingsViewModel
  override fun bindAdapter(adapter: MappingAdapter) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    val factory = IntervalSettingsViewModel.Factory(preferences)
    viewModel = ViewModelProvider(this, factory)[IntervalSettingsViewModel::class.java]

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  fun getConfiguration(state: IntervalSettingsState): DSLConfiguration {
    return configure {

      sectionHeaderPref(R.string.Pigeon_Setup_Interval)

      pigeonEditTextPref(
        title = DSLSettingsText.from(R.string.Pigeon_Setup_Alive_Interval),
        summary = DSLSettingsText.from(state.keepAliveTime.toString()),
        onSelected = {
          viewModel.setKeepAliveTime(it)
          restartApp()
        }
      )

      pigeonEditTextPref(
        title = DSLSettingsText.from(R.string.Pigeon_Setup_Sleep_Interval),
        summary = DSLSettingsText.from(state.keepSleepTime.toString()),
        onSelected = {
          viewModel.setKeepSleepTime(it)
          restartApp()
        }
      )

      pigeonEditTextPref(
        title = DSLSettingsText.from(R.string.Pigeon_Setup_Incoming_Message_Interval),
        summary = DSLSettingsText.from(state.incomingMessageIntervalTime.toString()),
        onSelected = {
          viewModel.setIncomingMessageTime(it)
          restartApp()
        }
      )
    }
  }

  private fun restartApp() {
    val startActivity = Intent(context, MainActivity::class.java)
    val pendingIntentId = 123456
    val pendingIntent = PendingIntent.getActivity(context, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager[AlarmManager.RTC, System.currentTimeMillis() + 100] = pendingIntent
    exitProcess(0)
  }
}
