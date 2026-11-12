package pigeon.fragments

import androidx.compose.runtime.Composable
import org.signal.core.ui.compose.ComposeFragment
import pigeon.compose.PigeonScrollableText
import pigeon.registration.PigeonTexts

/**
 * PIGEON: Pigeon disclaimer shown in-app as scrollable text.
 */
class DisclaimerFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    PigeonScrollableText(parts = PigeonTexts.DISCLAIMER)
  }
}
