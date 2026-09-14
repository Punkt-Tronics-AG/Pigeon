package pigeon.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class PigeonBaseFragment<VB : ViewBinding> : Fragment() {

  private var _binding: VB? = null
  protected val binding get() = _binding

  abstract fun createBinding(
    inflater: LayoutInflater,
    container: ViewGroup?,
  ): VB

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    _binding = createBinding(inflater, container)
    return binding!!.root
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

}