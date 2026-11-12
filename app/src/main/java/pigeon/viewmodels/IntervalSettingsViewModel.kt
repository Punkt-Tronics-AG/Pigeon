package pigeon.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.thoughtcrime.securesms.util.livedata.Store

class IntervalSettingsViewModel(
  private val sharedPreferences: SharedPreferences,
) : ViewModel() {

  companion object {
    const val KEEP_ALIVE_TIME_PREF = "pref_keep_alive_time"
    const val KEEP_SLEEP_TIME_PREF = "pref_keep_sleep_time"
    const val INCOMING_MESSAGE_TIME_PREF = "pref_incoming_message_time"
  }


  private val store = Store(getState())

  val state: LiveData<IntervalSettingsState> = store.stateLiveData

  fun setKeepAliveTime(resultSet: Int) {
    sharedPreferences.edit().putInt(KEEP_ALIVE_TIME_PREF, resultSet).commit()
    getStateAndCopy()
  }

  fun setKeepSleepTime(resultSet: Int) {
    sharedPreferences.edit().putInt(KEEP_SLEEP_TIME_PREF, resultSet).commit()
    getStateAndCopy()
  }

  fun setIncomingMessageTime(resultSet: Int) {
    sharedPreferences.edit().putInt(INCOMING_MESSAGE_TIME_PREF, resultSet).commit()
    getStateAndCopy()
  }

  private fun getStateAndCopy() {
    store.update { getState().copy(keepAliveTime = it.keepAliveTime, keepSleepTime = it.keepSleepTime) }
  }

  private fun getState() = IntervalSettingsState(
    keepAliveTime = sharedPreferences.getInt(KEEP_ALIVE_TIME_PREF, 30),
    keepSleepTime = sharedPreferences.getInt(KEEP_SLEEP_TIME_PREF, 120),
    incomingMessageIntervalTime = sharedPreferences.getInt(INCOMING_MESSAGE_TIME_PREF, 60)
  )

  class Factory(
    private val sharedPreferences: SharedPreferences
  ) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(IntervalSettingsViewModel(sharedPreferences)))
    }
  }
}
