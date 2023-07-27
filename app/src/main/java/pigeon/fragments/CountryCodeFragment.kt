package pigeon.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.databinding.PigeonFragmentRegistrationCountryCodeBinding
import org.thoughtcrime.securesms.registration.fragments.CountryPickerFragment
import org.thoughtcrime.securesms.registration.fragments.CountryPickerFragmentArgs
import org.thoughtcrime.securesms.registration.fragments.RegistrationViewDelegate.setDebugLogSubmitMultiTapView
import org.thoughtcrime.securesms.registration.util.RegistrationNumberInputController
import org.thoughtcrime.securesms.registration.viewmodel.RegistrationViewModel
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import pigeon.extensions.focusOnRight
import pigeon.extensions.isSignalVersion

class CountryCodeFragment : LoggingFragment(), RegistrationNumberInputController.Callbacks {
  private var viewModel: RegistrationViewModel? = null
  private val disposables = LifecycleDisposable()

  private var _binding: PigeonFragmentRegistrationCountryCodeBinding? = null
  private val binding get() = _binding

  private fun createBinding(inflater: LayoutInflater, container: ViewGroup?): PigeonFragmentRegistrationCountryCodeBinding {
    return PigeonFragmentRegistrationCountryCodeBinding.inflate(inflater, container, false)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    _binding = createBinding(inflater, container)
    return binding!!.root
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding?.run {
      setDebugLogSubmitMultiTapView(verifyHeader)
      val controller = RegistrationNumberInputController(requireContext(), this@CountryCodeFragment, EditText(context), countryCode)

      nextButton.setOnClickListener { v: View -> handleRegister(v) }
      if (!isSignalVersion()) {
        countryCodeLayout.focusOnRight()
        countryCodeLayout.requestFocus()
        val arguments = CountryPickerFragmentArgs.Builder().setResultKey(NUMBER_COUNTRY_SELECT).build()
        countryCodeLayout.setOnClickListener { v: View? -> findNavController(v!!).safeNavigate(R.id.action_pickCountry, arguments.toBundle()) }
        parentFragmentManager.setFragmentResultListener(NUMBER_COUNTRY_SELECT, this@CountryCodeFragment) { requestKey: String?, result: Bundle ->
          val resultCode = result.getInt(CountryPickerFragment.KEY_COUNTRY_CODE)
          val resultCountryName = result.getString(CountryPickerFragment.KEY_COUNTRY)
          viewModel!!.onCountrySelected(resultCountryName, resultCode)
          controller.setPigeonNumberAndCountryCode(viewModel!!.number)
          nextButton.requestFocus()
        }
      }
      disposables.bindTo(viewLifecycleOwner.lifecycle)
      viewModel = ViewModelProvider(requireActivity())[RegistrationViewModel::class.java]
      controller.prepopulateCountryCode()
      controller.setPigeonNumberAndCountryCode(viewModel!!.number)
    }
  }

  private fun handleRegister(view: View) {
    if (TextUtils.isEmpty(binding?.countryCode!!.editText!!.text)) {
      showErrorDialog(getString(R.string.RegistrationActivity_you_must_specify_your_country_code))
      return
    }
    findNavController(view).safeNavigate(CountryCodeFragmentDirections.actionCountryCodeFragmentToEnterPhoneNumberFragment())
  }

  private fun showErrorDialog(msg: String?) {
    MaterialAlertDialogBuilder(requireContext()).setMessage(msg).setPositiveButton(R.string.ok, null).show()
  }

  override fun setCountry(countryCode: Int) {}
  override fun onNumberFocused() {}
  override fun onNumberInputDone(view: View) {}
  override fun setNationalNumber(number: String) {}

  companion object {
    private const val NUMBER_COUNTRY_SELECT = "number_country"
  }
}