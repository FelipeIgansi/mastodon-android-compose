package org.joinmastodon.android.api.session

import android.content.SharedPreferences
import androidx.core.content.edit

class AccountLocalPreferences(private val prefs: SharedPreferences) {
  @JvmField //Used only to communicate with Java code, if it is calling this object.
  var serverSideFiltersSupported: Boolean = prefs.getBoolean("serverSideFilters", false)

  var notificationsPauseEndTime: Long
    get() = prefs.getLong("notificationsPauseTime", 0L)
    set(time) { prefs.edit { putLong("notificationsPauseTime", time) } }

  fun save() {
    prefs.edit { putBoolean("serverSideFilters", serverSideFiltersSupported) }
  }
}
