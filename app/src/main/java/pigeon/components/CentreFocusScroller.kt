package pigeon.components

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.widget.NestedScrollView

class CentreFocusScroller(private val scrollView: NestedScrollView) {
    @SuppressLint("SuspiciousIndentation")
    fun onFocusChange(v: View, focus: Boolean) {

        // change attributes of text based on focused or not
        if (v is TextView) {
          val mp = v.getLayoutParams() as MarginLayoutParams
            if (focus) {
              v.textSize = FONT_SIZE_FOCUSED
              v.setTextColor(v.textColors.withAlpha(FONT_ALPHA_FOCUSED))
              v.ellipsize = TextUtils.TruncateAt.MARQUEE
              v.isSelected = true
              mp.marginStart = MARGIN_START_FOCUSED
              v.setLayoutParams(mp)
            } else {
              v.textSize = FONT_SIZE_UNFOCUSED
              v.setTextColor(v.textColors.withAlpha(FONT_ALPHA_UNFOCUSED))
              v.ellipsize = TextUtils.TruncateAt.END
              mp.marginStart = MARGIN_START_UNFOCUSED
              v.setLayoutParams(mp)
              return
            }
        }

        // now scroll menu to move focused item to the centre
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        scrollView.smoothScrollBy(0, loc[1] - BASE_POS_Y)
    }

    companion object {
        private const val FONT_SIZE_FOCUSED = 40f
        private const val FONT_SIZE_UNFOCUSED = 24f
        private const val FONT_ALPHA_FOCUSED = 255
        private const val FONT_ALPHA_UNFOCUSED = 127
        private const val MARGIN_START_FOCUSED = 5
        private const val MARGIN_START_UNFOCUSED = 30
        private const val BASE_POS_Y = 76
    }
}