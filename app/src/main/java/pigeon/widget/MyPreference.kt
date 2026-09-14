package pigeon.widget

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import pigeon.extensions.focusOnLeft

class MyPreference(context: Context) : Preference(context) {

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    holder.itemView.focusOnLeft()
    super.onBindViewHolder(holder)
  }
}