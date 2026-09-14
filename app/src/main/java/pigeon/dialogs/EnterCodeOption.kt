package pigeon.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.registration.ActionCountDownButton
import org.thoughtcrime.securesms.databinding.EnterCodeOptionDialogBinding
import pigeon.extensions.focusOnLeft


class EnterCodeOption : DialogFragment() {
  private var buttons: ArrayList<ActionCountDownButton> = ArrayList()

  private var _binding: EnterCodeOptionDialogBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    _binding = EnterCodeOptionDialogBinding.inflate(layoutInflater, container, false)
    return binding.root
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setStyle(
      STYLE_NORMAL,
      android.R.style.Theme_Black_NoTitleBar
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupButtons()
    binding.run {
      closeButton.focusOnLeft()
      closeButton.setOnClickListener {
        dialog?.dismiss()
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun setupButtons() {
    binding.run {
      if (buttons.isEmpty()) {
        optionButton1.visibility = View.GONE
        optionButton2.visibility = View.GONE
      } else {
        optionButton1.visibility = View.VISIBLE
        optionButton1.focusOnLeft()
        optionButton1.text = buttons.first().text
        optionButton1.setOnClickListener {
          dialog?.dismiss()
          activity?.runOnUiThread {
            buttons.first().performClick()
          }
        }
        if (buttons.size == 2) {
          optionButton2.visibility = View.VISIBLE
          optionButton2.focusOnLeft()
          optionButton2.text = buttons[1].text
          optionButton2.setOnClickListener {
            dialog?.dismiss()
            activity?.runOnUiThread {
              buttons[1].performClick()
            }
          }
        }
      }
    }
  }

  fun showWithButtons(fragmentManager: FragmentManager, context: Context, buttons: ArrayList<ActionCountDownButton>) {
    this.buttons = buttons.filter { it.visibility == View.VISIBLE } as ArrayList<ActionCountDownButton>
    if (this.buttons.isEmpty()) {
      Toast.makeText(context, getString(R.string.no_options_available), Toast.LENGTH_SHORT).show()
    } else {
      show(fragmentManager, "dialog")
    }
  }
}