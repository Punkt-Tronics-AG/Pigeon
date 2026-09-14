package org.thoughtcrime.securesms.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.databinding.FragmentAboutBinding
import pigeon.base.PigeonBaseFragment

class AboutFragment : PigeonBaseFragment<FragmentAboutBinding>() {

  override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAboutBinding {
    return FragmentAboutBinding.inflate(inflater, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding?.version?.text = BuildConfig.VERSION_NAME
  }
}