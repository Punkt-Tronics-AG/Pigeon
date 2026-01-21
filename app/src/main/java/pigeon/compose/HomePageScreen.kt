package pigeon.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.widget.NestedScrollView
import org.thoughtcrime.securesms.R

@Composable
fun HomePageScreen(
    nestedScrollView: NestedScrollView?,
    onNewMessage: () -> Unit,
    onNewGroup: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    isSearchVisible: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(76.dp))

        HomePageButton(
            text = stringResource(R.string.Pigeon_HomePage_new_message),
            nestedScrollView = nestedScrollView,
            onClick = onNewMessage,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusProperties { up = focusRequester }
        )

        HomePageButton(
            text = stringResource(R.string.Pigeon_HomePage_new_group),
            nestedScrollView = nestedScrollView,
            onClick = onNewGroup
        )

        HomePageButton(
            text = stringResource(R.string.Pigeon_HomePage_mark_all_read),
            nestedScrollView = nestedScrollView,
            onClick = onMarkAllRead
        )

        HomePageButton(
            text = stringResource(R.string.Pigeon_HomePage_settings),
            nestedScrollView = nestedScrollView,
            onClick = onSettings
        )

        if (isSearchVisible) {
            HomePageButton(
                text = stringResource(R.string.Pigeon_HomePage_search),
                nestedScrollView = nestedScrollView,
                onClick = onSearch
            )
        }

        // Add padding at the bottom to allow scrolling the last item to the target position
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePageButton(
    text: String,
    nestedScrollView: NestedScrollView?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val density = LocalDensity.current
    var hasCentered by remember { mutableStateOf(false) }

    val fontSize = if (isFocused) 40.sp else 24.sp
    val color = if (isFocused) Color.White else Color.White.copy(alpha = 0.5f)
    val startPadding = if (isFocused) 5.dp else 30.dp

    // Reset loop breaker when focus is lost
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            hasCentered = false
        }
    }

    Text(
        text = text,
        fontSize = fontSize,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable ripple if original didn't have it (likely didn't or was text color change)
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .let {
                if (isFocused) it.basicMarquee() else it
            }
            .onGloballyPositioned { coordinates ->
                if (isFocused && !hasCentered && nestedScrollView != null) {
                    val y = coordinates.positionInWindow().y
                    val basePosY = with(density) { 76.dp.toPx() }

                    // We use post to avoid interfering with the current layout pass
                    nestedScrollView.post {
                        nestedScrollView.smoothScrollBy(0, (y - basePosY).toInt())
                    }
                }
            }
    )
}
