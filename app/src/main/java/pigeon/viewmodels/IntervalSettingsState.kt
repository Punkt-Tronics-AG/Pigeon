package pigeon.viewmodels

data class IntervalSettingsState(
  val keepAliveTime: Int,
  val keepSleepTime: Int,
  val incomingMessageIntervalTime: Int,
)
