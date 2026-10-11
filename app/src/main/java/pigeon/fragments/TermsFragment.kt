package pigeon.fragments

import androidx.compose.runtime.Composable
import org.signal.core.ui.compose.ComposeFragment
import pigeon.compose.PigeonScrollableText
import pigeon.registration.PigeonTexts

/**
 * PIGEON: Signal Terms & Privacy Policy shown in-app (Help settings).
 */
class TermsFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    PigeonScrollableText(parts = PigeonTexts.TERMS)
  }
}
