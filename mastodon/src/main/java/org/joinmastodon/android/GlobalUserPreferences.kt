package org.joinmastodon.android

import android.content.Context
import android.content.SharedPreferences
import org.joinmastodon.android.api.session.AccountSessionManager
import java.util.Locale
import androidx.core.content.edit
import org.joinmastodon.android.model.Account

object GlobalUserPreferences {
  @JvmField var playGifs: Boolean = false
  @JvmField var useCustomTabs: Boolean = false
  @JvmField var altTextReminders: Boolean = false
  @JvmField var confirmUnfollow: Boolean = false
  @JvmField var confirmBoost: Boolean = false
  @JvmField var confirmDeletePost: Boolean = false
  @JvmField var theme: ThemePreference = ThemePreference.AUTO
  @JvmField var useDynamicColors: Boolean = false
  @JvmField var showInteractionCounts: Boolean = false
  @JvmField var customEmojiInNames: Boolean = false
  @JvmField var showCWs: Boolean = false
  @JvmField var hideSensitiveMedia: Boolean = false

  private val prefs: SharedPreferences
    get() = MastodonApp.context!!.getSharedPreferences("global", Context.MODE_PRIVATE)

  private val preReplyPrefs: SharedPreferences
    get() = MastodonApp.context!!.getSharedPreferences(
      "pre_reply_sheets",
      Context.MODE_PRIVATE
    )

  fun load() {
    val prefs: SharedPreferences = prefs
    playGifs = prefs.getBoolean("playGifs", true)
    useCustomTabs = prefs.getBoolean("useCustomTabs", true)
    altTextReminders = prefs.getBoolean("altTextReminders", false)
    confirmUnfollow = prefs.getBoolean("confirmUnfollow", false)
    confirmBoost = prefs.getBoolean("confirmBoost", false)
    confirmDeletePost = prefs.getBoolean("confirmDeletePost", true)
    theme = ThemePreference.entries[prefs.getInt("theme", 0)]
    useDynamicColors = prefs.getBoolean("useDynamicColors", true)
    showInteractionCounts = prefs.getBoolean("interactionCounts", true)
    customEmojiInNames = prefs.getBoolean("emojiInNames", true)
    showCWs = prefs.getBoolean("showCWs", true)
    hideSensitiveMedia = prefs.getBoolean("hideSensitive", true)
    if (!prefs.getBoolean("perAccountMigrationDone", false)) {
      val account = AccountSessionManager.getInstance().getLastActiveAccount()
      if (account != null) {
        val accPrefs = account.getRawLocalPreferences()
        showInteractionCounts = accPrefs.getBoolean("interactionCounts", true)
        customEmojiInNames = accPrefs.getBoolean("emojiInNames", true)
        showCWs = accPrefs.getBoolean("showCWs", true)
        hideSensitiveMedia = accPrefs.getBoolean("hideSensitive", true)
        save()
      }
      // Also applies to new app installs
      prefs.edit { putBoolean("perAccountMigrationDone", true) }
    }
  }

  @JvmStatic
	fun save() {
    prefs.edit {
      putBoolean("playGifs", playGifs)
        .putBoolean("useCustomTabs", useCustomTabs)
        .putInt("theme", theme.ordinal)
        .putBoolean("altTextReminders", altTextReminders)
        .putBoolean("confirmUnfollow", confirmUnfollow)
        .putBoolean("confirmBoost", confirmBoost)
        .putBoolean("confirmDeletePost", confirmDeletePost)
        .putBoolean("useDynamicColors", useDynamicColors)
        .putBoolean("interactionCounts", showInteractionCounts)
        .putBoolean("emojiInNames", customEmojiInNames)
        .putBoolean("showCWs", showCWs)
        .putBoolean("hideSensitive", hideSensitiveMedia)
    }
  }

  @JvmStatic
	fun isOptedOutOfPreReplySheet(
    type: PreReplySheetType?,
    account: Account?,
    accountID: String?
  ): Boolean {
    if (preReplyPrefs.getBoolean("opt_out_$type", false)) return true
    if (account == null) return false
    var accountKey = account.acct ?: ""
    if (!accountKey.contains("@")) accountKey += "@${AccountSessionManager.get(accountID).domain}"
    return preReplyPrefs.getBoolean(
      "opt_out_${type}_${accountKey.lowercase(Locale.getDefault())}",
      false
    )
  }

  @JvmStatic
	fun optOutOfPreReplySheet(type: PreReplySheetType?, account: Account?, accountID: String?) {
    val key: String?
    if (account == null) {
      key = "opt_out_$type"
    } else {
      var accountKey = account.acct ?: ""
      if (!accountKey.contains("@")) accountKey += "@${AccountSessionManager.get(accountID).domain}"
      key = "opt_out_${type}_${accountKey.lowercase(Locale.getDefault())}"
    }
    preReplyPrefs.edit { putBoolean(key, true) }
  }

  @JvmStatic
	fun resetPreReplySheets() {
    preReplyPrefs.edit { clear() }
  }

  enum class ThemePreference {
    AUTO,
    LIGHT,
    DARK
  }

  enum class PreReplySheetType {
    OLD_POST,
    NON_MUTUAL
  }
}
