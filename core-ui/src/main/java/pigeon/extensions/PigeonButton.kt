package pigeon.extensions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.theme.SignalTheme

object PigeonButtons {

  private val largeButtonContentPadding = PaddingValues(
    start = 24.dp,
    top = 16.dp,
    end = 24.dp,
    bottom = 16.dp
  )

  /**
   * A large primary button with default content padding.
   */

  @Composable
  fun LargePrimary(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = largeButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
  ) {
    Button(
      onClick = onClick,
      modifier = modifier,
      enabled = enabled,
      shape = shape,
      colors = colors,
      elevation = elevation,
      border = border,
      contentPadding = contentPadding,
      interactionSource = interactionSource,
      content = content
    )
  }
}

@Preview(name = "Buttons.LargePrimaryButton")
@Composable
private fun LargePrimaryButtonPreview() {
  Column {
    Row {
      LargePrimaryButtonSample(darkMode = false, enabled = true)
      LargePrimaryButtonSample(darkMode = true, enabled = true)
    }

    Row {
      LargePrimaryButtonSample(darkMode = false, enabled = false)
      LargePrimaryButtonSample(darkMode = true, enabled = false)
    }
  }
}

@Composable
private fun LargePrimaryButtonSample(
  darkMode: Boolean,
  enabled: Boolean
) {
  SampleBox(darkMode) {
    Buttons.LargePrimary(
      onClick = {},
      enabled = enabled
    ) {
      Text("Button")
    }
  }
}

@Composable
private fun SampleBox(
  darkMode: Boolean,
  content: @Composable BoxScope.() -> Unit
) {
  SignalTheme(isDarkMode = darkMode) {
    Surface {
      Box(modifier = Modifier.padding(8.dp)) {
        content()
      }
    }
  }
}