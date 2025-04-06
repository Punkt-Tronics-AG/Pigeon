/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package pigeon.extensions

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.focusOnLeft(
  enabled: Boolean,
  interactionSource: MutableInteractionSource,
  onFocusChanged: (Boolean, Dp, Color) -> Unit
): Modifier = composed {
  val isFocused by interactionSource.collectIsFocusedAsState()
  var textSize by remember { mutableStateOf(24.dp) }
  var textColor by remember { mutableStateOf(Color(0x80FFFFFF)) }

  textSize = if (isFocused) 36.dp else 24.dp
  textColor = if (isFocused) Color(0xFFFFFFFF) else Color(0x80FFFFFF)

  onFocusChanged(isFocused, textSize, textColor)

  this
    .alpha(if (!enabled) 0.5f else 1.0f)
    .padding(start = if (isFocused) 5.dp else 30.dp)
}