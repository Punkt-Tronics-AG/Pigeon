package pigeon.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIGEON: inline "working…" indicator for the MP02 text-menu screens. Signal shows a spinner inside the button
 * that was pressed; the Pigeon rows only get disabled, which looks like a hang during slow network calls
 * (PIN creation / SVR restore / code verification). Renders nothing while [visible] is false.
 */
@Composable
fun PigeonProgressRow(
  visible: Boolean,
  modifier: Modifier = Modifier,
  label: String? = null
) {
  if (!visible) return

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

    if (label != null) {
      Text(
        text = label,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
      )
    }
  }
}
