package pigeon.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.databinding.PigeonFragmentRegistrationTermsBinding
import org.signal.core.ui.permissions.Permissions
import pigeon.base.PigeonBaseFragment

class TermsFragment: PigeonBaseFragment<PigeonFragmentRegistrationTermsBinding>() {
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

  override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): PigeonFragmentRegistrationTermsBinding {
    return PigeonFragmentRegistrationTermsBinding.inflate(inflater, container, false)
  }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = StringBuilder()
        builder.append(getString(R.string.terms_and_policy_1))
        builder.append(getString(R.string.terms_and_policy_2))
        builder.append(getString(R.string.terms_and_policy_3))
        builder.append(getString(R.string.terms_and_policy_4))
        builder.append(getString(R.string.terms_and_policy_5))
        builder.append(getString(R.string.terms_and_policy_6))
        builder.append(getString(R.string.terms_and_policy_7))
        builder.append(getString(R.string.terms_and_policy_8))
        builder.append(getString(R.string.terms_and_policy_9))
        builder.append(getString(R.string.terms_and_policy_10))
        builder.append(getString(R.string.terms_and_policy_11))
        builder.append(getString(R.string.terms_and_policy_12))
        builder.append(getString(R.string.terms_and_policy_13))
        builder.append(getString(R.string.terms_and_policy_14))
        binding?.termsTv?.text = builder
    }
}