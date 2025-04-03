package pigeon.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.fragment.app.ListFragment
import androidx.fragment.app.activityViewModels
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.navigation.fragment.NavHostFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.registration.ui.RegistrationViewModel
import pigeon.base.CountryListLoader

class CountryPickerFragment : ListFragment(), LoaderManager.LoaderCallbacks<ArrayList<Map<String, String>>> {
  private var countryFilter: EditText? = null
  private val sharedViewModel by activityViewModels<RegistrationViewModel>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
    return inflater.inflate(R.layout.pigeon_fragment_registration_country_picker, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    countryFilter = view.findViewById(R.id.country_search)

    countryFilter?.addTextChangedListener(FilterWatcher())
    LoaderManager.getInstance(this).initLoader(0, null, this).forceLoad()
  }


  override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
    val item = listAdapter!!.getItem(position) as Map<String, String>

    val countryCode = item["country_code"]!!.replace("+", "").toInt()
    val countryName = item["country_name"]

    sharedViewModel.setNewCountry(countryCode)

    NavHostFragment.findNavController(this).navigateUp()
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<Map<String, String>>> {
    return CountryListLoader(requireContext())
  }

  override fun onLoadFinished(
    loader: Loader<ArrayList<Map<String, String>>>,
    results: ArrayList<Map<String, String>>
  ) {
    (listView.emptyView as TextView).setText(
      R.string.country_selection_fragment__no_matching_countries
    )
    val from = arrayOf("country_name", "country_code")
    val to = intArrayOf(R.id.country_name, R.id.country_code)

    listAdapter = SimpleAdapter(activity, results, R.layout.country_list_item, from, to)

    countryFilter?.text?.let { applyFilter(it) }
  }

  private fun applyFilter(text: CharSequence) {
    val listAdapter: SimpleAdapter? = listAdapter as SimpleAdapter?

    listAdapter?.filter?.filter(text)
  }

  override fun onLoaderReset(loader: Loader<ArrayList<Map<String, String>>>) {
    listAdapter = null
  }

  private inner class FilterWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable) {
      applyFilter(s)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }
  }

  companion object {
    const val KEY_COUNTRY: String = "country"
    const val KEY_COUNTRY_CODE: String = "country_code"
  }
}
