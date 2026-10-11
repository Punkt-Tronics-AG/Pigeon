/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package pigeon.base

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.thoughtcrime.securesms.registration.ui.countrycode.CountryUtils
import java.text.Collator
import java.util.Collections

class CountryListLoader(context: Context) : AsyncTaskLoader<ArrayList<Map<String, String>>>(context) {
  override fun loadInBackground(): ArrayList<Map<String, String>> {

    val countryList = CountryUtils.getCountries()
    val results = ArrayList<Map<String, String>>(countryList.size)

    for (country in countryList) {
      val data: MutableMap<String, String> = HashMap(2)
      data["country_name"] = country.name
      data["country_code"] = "+" + country.countryCode
      results.add(data)
    }

    Collections.sort(results, RegionComparator())

    return results
  }

  private class RegionComparator : Comparator<Map<String, String>> {
    private val collator: Collator = Collator.getInstance()

    init {
      collator.strength = Collator.PRIMARY
    }

    override fun compare(lhs: Map<String, String>, rhs: Map<String, String>): Int {
      val a = lhs["country_name"]
      val b = rhs["country_name"]
      return collator.compare(a, b)
    }
  }
}
