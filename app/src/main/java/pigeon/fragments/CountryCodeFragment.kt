package pigeon.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.signal.core.util.concurrent.LifecycleDisposable
import org.signal.core.util.getParcelableCompat
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.databinding.PigeonFragmentRegistrationCountryCodeBinding
import org.thoughtcrime.securesms.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import org.thoughtcrime.securesms.registration.ui.countrycode.Country
import org.thoughtcrime.securesms.registration.ui.countrycode.CountryCodeFragment.Companion.REQUEST_COUNTRY
import org.thoughtcrime.securesms.registration.ui.countrycode.CountryUtils
import org.thoughtcrime.securesms.registration.ui.phonenumber.EnterPhoneNumberMode
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.focusOnRight
import pigeon.extensions.isSignalVersion

class CountryCodeFragment : LoggingFragment() {
  private val sharedViewModel by activityViewModels<RegistrationViewModel>()
  private val disposables = LifecycleDisposable()

  private var _binding: PigeonFragmentRegistrationCountryCodeBinding? = null
  private val binding get() = _binding

  private fun createBinding(inflater: LayoutInflater, container: ViewGroup?): PigeonFragmentRegistrationCountryCodeBinding {
    return PigeonFragmentRegistrationCountryCodeBinding.inflate(inflater, container, false)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = createBinding(inflater, container)
    return binding!!.root
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  @SuppressLint("SetTextI18n")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding?.run {
      setDebugLogSubmitMultiTapView(verifyHeader)

      nextButton.setOnClickListener { _: View -> handleRegister() }
      if (!isSignalVersion()) {
        countryCodeLayout.focusOnRight()
        if (sharedViewModel.phoneNumber?.countryCode == null) {
          countryCodeLayout.requestFocus()
        } else {
          nextButton.requestFocus()
        }
        countryCodeLayout.setOnClickListener {
          val country = CountryUtils.getCountries().firstOrNull { it.countryCode == sharedViewModel.uiState.value?.pigeonCountryCode } ?: CountryUtils.getCountries().first()
            findNavController().safeNavigate(CountryCodeFragmentDirections.actionPickCountry(country))
        }
      }
      disposables.bindTo(viewLifecycleOwner.lifecycle)
    }

    val initialCountry = arguments?.getParcelableCompat(REQUEST_COUNTRY, Country::class.java)
    if (initialCountry != null) {
      sharedViewModel.setNewCountry(countryCode = initialCountry.countryCode)
      binding?.countryCode?.editText?.setText("+${initialCountry.countryCode}")
    }

  }

  override fun onResume() {
    super.onResume()
    sharedViewModel.uiState.observe(viewLifecycleOwner) {
      val countryCode = it.pigeonCountryCode?.toString()
      println("countryCode: $countryCode")
      binding?.countryCode?.editText?.setText(if (countryCode != null) "+$countryCode" else "")
    }
  }

  private fun handleRegister() {
    if (TextUtils.isEmpty(binding?.countryCode?.editText?.text)) {
      showErrorDialog(getString(R.string.RegistrationActivity_you_must_specify_your_country_code))
      return
    }
    findNavController().safeNavigate(CountryCodeFragmentDirections.enterPhoneNumberFragment(EnterPhoneNumberMode.NORMAL))
  }

  private fun showErrorDialog(msg: String?) {
    MaterialAlertDialogBuilder(requireContext()).setMessage(msg).setPositiveButton(R.string.Pigeon_ok, null).show()
  }

}