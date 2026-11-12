package pigeon.dialogs

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.signal.core.ui.compose.theme.SignalTheme
import pigeon.dialogs.PigeonBackupTimePickerTestTags as Tags

/**
 * PIGEON: UI coverage for the MP02 backup time picker.
 *
 * The MP02 has no touch screen and no left/right keys, so the picker is a digit-by-digit
 * input driven purely by the keypad: typing a digit overwrites the focused box and moves
 * on, DPAD_UP always goes back one box, DPAD_DOWN goes forward, and typing the last digit
 * lands on OK.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PigeonBackupTimePickerDialogTest {

  @get:Rule
  val composeRule = createComposeRule()

  private class CallbackRecorder {
    var confirmed: Pair<Int, Int>? = null
    var cancelled: Boolean = false
  }

  // region Initial state

  @Test
  fun initialTime_isSplitIntoPaddedDigits_andFirstHourDigitHasFocus() {
    setContent(hour = 5, minute = 7)

    assertDigits("0", "5", "0", "7")
    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()
  }

  @Test
  fun outOfRangeInitialValues_areClampedToValidTime() {
    setContent(hour = 99, minute = 99)

    assertDigits("2", "3", "5", "9")
  }

  // endregion

  // region Digit entry

  @Test
  fun typingDigit_overwritesFocusedBox_andAdvancesToNextBox() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("1")

    assertDigits("1", "5", "0", "7")
    composeRule.onNodeWithTag(Tags.HOUR_ONES).assertIsFocused()
  }

  @Test
  fun typingSameDigitAsCurrentValue_stillAdvances() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("0")

    assertDigits("0", "5", "0", "7")
    composeRule.onNodeWithTag(Tags.HOUR_ONES).assertIsFocused()
  }

  @Test
  fun typingNonDigit_isIgnored_andFocusStays() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("a")

    assertDigits("0", "5", "0", "7")
    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()
  }

  @Test
  fun typingLastMinuteDigit_movesFocusToOk() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_ONES).requestFocus()
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).performTextInput("9")

    assertDigits("0", "5", "0", "9")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).assertIsFocused()
  }

  @Test
  fun typingFullTime_thenOk_confirmsEnteredTime() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("2")
    composeRule.onNodeWithTag(Tags.HOUR_ONES).performTextInput("3")
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).performTextInput("5")
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).performTextInput("9")

    assertDigits("2", "3", "5", "9")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).assertIsFocused()

    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()
    composeRule.waitForIdle()

    assertThat(recorder.confirmed).isEqualTo(23 to 59)
    assertThat(recorder.cancelled).isFalse()
  }

  // endregion

  // region DPAD navigation

  @Test
  fun dpadUp_movesToPreviousBox_andNeverForwardToOk() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_ONES).requestFocus()
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.HOUR_ONES).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.CANCEL_BUTTON).assertIsFocused()
  }

  @Test
  fun dpadDown_movesThroughDigits_andEndsOnOk() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()

    press(Key.DirectionDown)
    composeRule.onNodeWithTag(Tags.HOUR_ONES).assertIsFocused()

    press(Key.DirectionDown)
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).assertIsFocused()

    press(Key.DirectionDown)
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).assertIsFocused()

    press(Key.DirectionDown)
    composeRule.onNodeWithTag(Tags.OK_BUTTON).assertIsFocused()
  }

  @Test
  fun dpadUpFromOk_goesToCancel_andFromCancelBackToLastDigit() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.OK_BUTTON).requestFocus()
    composeRule.onNodeWithTag(Tags.OK_BUTTON).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.CANCEL_BUTTON).assertIsFocused()

    press(Key.DirectionUp)
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).assertIsFocused()
  }

  @Test
  fun dpadDownFromCancel_goesToOk() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.CANCEL_BUTTON).requestFocus()
    press(Key.DirectionDown)

    composeRule.onNodeWithTag(Tags.OK_BUTTON).assertIsFocused()
  }

  @Test
  fun dpadNavigation_doesNotChangeAnyDigit() {
    setContent(hour = 5, minute = 7)

    repeat(4) { press(Key.DirectionDown) }
    repeat(4) { press(Key.DirectionUp) }

    assertDigits("0", "5", "0", "7")
  }

  // endregion

  // region Backspace

  @Test
  fun backspace_clearsFocusedDigit_andKeepsFocus() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_ONES).requestFocus()
    press(Key.Backspace)

    assertDigits("0", "5", "0", "")
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).assertIsFocused()
  }

  @Test
  fun backspaceOnEmptyLastDigit_movesToPreviousDigit_notToOk() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_ONES).requestFocus()
    press(Key.Backspace)
    press(Key.Backspace)

    assertDigits("0", "5", "0", "")
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).assertIsFocused()
  }

  @Test
  fun backspaceOnEmptyFirstDigit_movesToCancel() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()
    press(Key.Backspace)
    press(Key.Backspace)

    composeRule.onNodeWithTag(Tags.CANCEL_BUTTON).assertIsFocused()
  }

  @Test
  fun typingAfterBackspace_fillsClearedBox_andAdvances() {
    setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_ONES).requestFocus()
    press(Key.Backspace)
    assertDigits("0", "", "0", "7")

    composeRule.onNodeWithTag(Tags.HOUR_ONES).performTextInput("8")

    assertDigits("0", "8", "0", "7")
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).assertIsFocused()
  }

  // endregion

  // region Validation and callbacks

  @Test
  fun ok_withValidTime_confirmsInitialValue() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()
    composeRule.waitForIdle()

    assertThat(recorder.confirmed).isEqualTo(5 to 7)
  }

  @Test
  fun ok_withHourAbove23_showsHourError_andDoesNotConfirm() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("2")
    composeRule.onNodeWithTag(Tags.HOUR_ONES).performTextInput("9")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()

    composeRule.onNodeWithTag(Tags.HOUR_ERROR).assertIsDisplayed()
    composeRule.onNodeWithTag(Tags.HOUR_TENS).assertIsFocused()
    assertThat(recorder.confirmed).isNull()
  }

  @Test
  fun ok_withMinuteAbove59_showsMinuteError_andDoesNotConfirm() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_TENS).performTextInput("6")
    composeRule.onNodeWithTag(Tags.MINUTE_ONES).performTextInput("0")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()

    composeRule.onNodeWithTag(Tags.MINUTE_ERROR).assertIsDisplayed()
    composeRule.onNodeWithTag(Tags.MINUTE_TENS).assertIsFocused()
    assertThat(recorder.confirmed).isNull()
  }

  @Test
  fun ok_withEmptyDigit_showsError_andDoesNotConfirm() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.MINUTE_ONES).requestFocus()
    press(Key.Backspace)
    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()

    composeRule.onNodeWithTag(Tags.MINUTE_ERROR).assertIsDisplayed()
    assertThat(recorder.confirmed).isNull()
  }

  @Test
  fun errorDisappears_afterFixingTheDigit() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.HOUR_TENS).performTextInput("2")
    composeRule.onNodeWithTag(Tags.HOUR_ONES).performTextInput("9")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()
    composeRule.onNodeWithTag(Tags.HOUR_ERROR).assertIsDisplayed()

    composeRule.onNodeWithTag(Tags.HOUR_ONES).performTextInput("3")
    composeRule.onNodeWithTag(Tags.OK_BUTTON).performClick()
    composeRule.waitForIdle()

    assertThat(recorder.confirmed).isEqualTo(23 to 7)
  }

  @Test
  fun cancel_invokesOnCancel_andDoesNotConfirm() {
    val recorder = setContent(hour = 5, minute = 7)

    composeRule.onNodeWithTag(Tags.CANCEL_BUTTON).performClick()
    composeRule.waitForIdle()

    assertThat(recorder.cancelled).isTrue()
    assertThat(recorder.confirmed).isNull()
  }

  // endregion

  // region Helpers

  private fun setContent(hour: Int, minute: Int): CallbackRecorder {
    val recorder = CallbackRecorder()
    composeRule.setContent {
      SignalTheme {
        PigeonBackupTimePickerContent(
          initialHour = hour,
          initialMinute = minute,
          onConfirm = { h, m -> recorder.confirmed = h to m },
          onCancel = { recorder.cancelled = true }
        )
      }
    }
    composeRule.waitForIdle()
    return recorder
  }

  private fun press(key: Key) {
    composeRule.onRoot().performKeyInput { pressKey(key) }
    composeRule.waitForIdle()
  }

  private fun digit(tag: String): String {
    return composeRule.onNodeWithTag(tag)
      .fetchSemanticsNode()
      .config[SemanticsProperties.EditableText]
      .text
  }

  private fun assertDigits(hourTens: String, hourOnes: String, minuteTens: String, minuteOnes: String) {
    composeRule.waitForIdle()
    assertThat(digit(Tags.HOUR_TENS)).isEqualTo(hourTens)
    assertThat(digit(Tags.HOUR_ONES)).isEqualTo(hourOnes)
    assertThat(digit(Tags.MINUTE_TENS)).isEqualTo(minuteTens)
    assertThat(digit(Tags.MINUTE_ONES)).isEqualTo(minuteOnes)
  }

  // endregion
}

