package org.joinmastodon.android.api.session

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import org.joinmastodon.android.E.post
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.R
import org.joinmastodon.android.api.CacheController
import org.joinmastodon.android.api.MastodonAPIController
import org.joinmastodon.android.api.PushSubscriptionManager
import org.joinmastodon.android.api.StatusInteractionController
import org.joinmastodon.android.api.gson.JsonObjectBuilder
import org.joinmastodon.android.api.requests.accounts.GetPreferences
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentialsPreferences
import org.joinmastodon.android.api.requests.markers.GetMarkers
import org.joinmastodon.android.api.requests.markers.SaveMarkers
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.Application
import org.joinmastodon.android.model.FilterAction
import org.joinmastodon.android.model.FilterContext
import org.joinmastodon.android.model.Instance
import org.joinmastodon.android.model.LegacyFilter
import org.joinmastodon.android.model.Preferences
import org.joinmastodon.android.model.PushSubscription
import org.joinmastodon.android.model.Status
import org.joinmastodon.android.model.TimelineMarkers
import org.joinmastodon.android.model.Token
import org.joinmastodon.android.utils.ObjectIdComparator
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer
import java.util.function.Function
import kotlin.math.abs

class AccountSession {


  companion object {
    private const val TAG = "AccountSession"
    private const val MIN_DAYS_ACCOUNT_AGE_FOR_DONATIONS = 28

    const val FLAG_ACTIVATED: Int = 1
    const val FLAG_NEED_UPDATE_PUSH_SETTINGS: Int = 1 shl 1
  }


  @JvmField
  @SerializedName(value = "token", alternate = ["a"])
  var token: Token? = null

  @JvmField
  @SerializedName(value = "self", alternate = ["b"])
  var self: Account? = null

  @JvmField
  @SerializedName(value = "domain", alternate = ["c"])
  var domain: String? = null

  @JvmField
  @SerializedName(value = "app", alternate = ["d"])
  var app: Application? = null

  @JvmField
  @SerializedName(value = "info_last_updated", alternate = ["e"])
  var infoLastUpdated: Long = 0

  @JvmField
  @SerializedName(value = "activated", alternate = ["f"])
  var activated: Boolean = true

  @JvmField
  @SerializedName(value = "push_private_key", alternate = ["g"])
  var pushPrivateKey: String? = null

  @JvmField
  @SerializedName(value = "push_public_key", alternate = ["h"])
  var pushPublicKey: String? = null

  @JvmField
  @SerializedName(value = "push_auth_key", alternate = ["i"])
  var pushAuthKey: String? = null

  @JvmField
  @SerializedName(value = "push_subscription", alternate = ["j"])
  var pushSubscription: PushSubscription? = null

  @SerializedName(value = "need_update_push_settings", alternate = ["k"])
  var needUpdatePushSettings: Boolean = false

  @JvmField
  @SerializedName(value = "filters_last_updated", alternate = ["l"])
  var filtersLastUpdated: Long = 0

  @JvmField
  @SerializedName(value = "word_filters", alternate = ["m"])
  var wordFilters: MutableList<LegacyFilter> = mutableListOf()

  @JvmField
  @SerializedName(value = "push_account_i_d", alternate = ["n"])
  var pushAccountID: String? = null

  @JvmField
  @SerializedName(value = "activation_info", alternate = ["o"])
  var activationInfo: AccountActivationInfo? = null

  @JvmField
  @SerializedName(value = "preferences", alternate = ["p"])
  var preferences: Preferences? = null

  @delegate:Transient
  val apiController by lazy { MastodonAPIController(this) }

  @delegate:Transient
  val statusInteractionController by lazy { StatusInteractionController(getID()) }

  @delegate:Transient
  val cacheController by lazy { CacheController(getID()) }

  @delegate:Transient
  val pushSubscriptionManager by lazy { PushSubscriptionManager(getID()) }

  @Transient
  private var prefs: SharedPreferences? = null

  @Transient
  private var preferencesNeedSaving = false

  @delegate:Transient
  val localPreferences by lazy { AccountLocalPreferences(getRawLocalPreferences()) }

  fun getRawLocalPreferences(): SharedPreferences {
    if (prefs == null) {
      val currentContext = MastodonApp.context
        ?: throw IllegalStateException("AccountSession File: Application context is null")
      val newPrefs = currentContext.getSharedPreferences(getID(), Context.MODE_PRIVATE)
      prefs = newPrefs
    }
    return prefs!!
  }

  fun getID() = self?.let { "${domain}_${it.id}" } ?: throw IllegalStateException("AccountSession File: LocalID is null")

  val fullUsername: String
    get() = self?.let { "@${it.username}@$domain" }
      ?: throw IllegalStateException("AccountSession File: FullUserName is null")

  fun getLastKnownNotificationsMarker(): String? {
    return getRawLocalPreferences().getString("notificationsMarker", null)
  }


  var isNotificationsMentionsOnly: Boolean
    get() = getRawLocalPreferences().getBoolean("notificationsMentionsOnly", false)
    set(mentionsOnly) {
      getRawLocalPreferences().edit {
        putBoolean("notificationsMentionsOnly", mentionsOnly)
      }
    }

  val isEligibleForDonations: Boolean
    get() {
      val currentDomain = domain ?: return false
      val currentSelf = self ?: return false

      return (currentDomain.equals("mastodon.social", ignoreCase = true) ||
          (currentDomain.equals("mastodon.online", ignoreCase = true))) &&
          currentSelf.createdAt.isBefore(
            Instant.now().minus(
              MIN_DAYS_ACCOUNT_AGE_FOR_DONATIONS.toLong(),
              ChronoUnit.DAYS
            )
          )
    }

  val donationSeed: Int
    get() = abs(this.fullUsername.hashCode()) % 100

  val instanceInfo: Instance?
    get() = AccountSessionManager.getInstance().getInstanceInfo(domain)

  fun getFlagsForDatabase(): Long {
    var flags: Long = 0
    if (activated) flags = flags or FLAG_ACTIVATED.toLong()
    if (needUpdatePushSettings) flags = flags or FLAG_NEED_UPDATE_PUSH_SETTINGS.toLong()
    return flags
  }


  constructor()


  constructor(
    token: Token,
    self: Account,
    app: Application,
    domain: String,
    activated: Boolean,
    activationInfo: AccountActivationInfo?
  ) {
    this.token = token
    this.self = self
    this.domain = domain
    this.app = app
    this.activated = activated
    this.activationInfo = activationInfo
    infoLastUpdated = System.currentTimeMillis()
  }


  constructor(values: ContentValues) {
    domain = values.getAsString("domain")

    val gson = MastodonAPIController.gson
    self = gson.fromJson(values.getAsString("account_obj"), Account::class.java)
    token = gson.fromJson(values.getAsString("token"), Token::class.java)
    app = gson.fromJson(values.getAsString("application"), Application::class.java)
    infoLastUpdated = values.getAsLong("info_last_updated")

    val flags = values.getAsLong("flags") ?: 0L
    activated = (flags and FLAG_ACTIVATED.toLong()) == FLAG_ACTIVATED.toLong()
    needUpdatePushSettings =
      (flags and FLAG_NEED_UPDATE_PUSH_SETTINGS.toLong()) == FLAG_NEED_UPDATE_PUSH_SETTINGS.toLong()

    val pushKeys = JsonParser.parseString(values.getAsString("push_keys")).getAsJsonObject()
    if (!pushKeys.get("auth").isJsonNull && !pushKeys.get("private")
        .isJsonNull && !pushKeys.get("public").isJsonNull
    ) {
      pushAuthKey = pushKeys.get("auth").asString
      pushPrivateKey = pushKeys.get("private").asString
      pushPublicKey = pushKeys.get("public").asString
    }

    pushSubscription = gson.fromJson(
      values.getAsString("push_subscription"),
      PushSubscription::class.java
    )

    val legacyFilters =
      JsonParser.parseString(values.getAsString("legacy_filters")).getAsJsonObject()
    wordFilters = gson.fromJson(
      legacyFilters.getAsJsonArray("filters"),
      object : TypeToken<MutableList<LegacyFilter>>() {}.type
    )
    filtersLastUpdated = legacyFilters.get("updated").asLong
    pushAccountID = values.getAsString("push_id")
    activationInfo = gson.fromJson(
      values.getAsString("activation_info"),
      AccountActivationInfo::class.java
    )
    preferences =
      gson.fromJson(values.getAsString("preferences"), Preferences::class.java)
  }

  fun toContentValues(values: ContentValues) {
    values.put("id", getID())
    values.put(
      "domain",
      domain?.lowercase()
        ?: throw IllegalStateException("AccountSession File: Domain cannot be null for ContentValues")
    )
    values.put("account_obj", MastodonAPIController.gson.toJson(self))
    values.put("token", MastodonAPIController.gson.toJson(token))
    values.put("application", MastodonAPIController.gson.toJson(app))
    values.put("info_last_updated", infoLastUpdated)
    values.put("flags", this.getFlagsForDatabase())

    if (pushAuthKey != null && pushPrivateKey != null && pushPublicKey != null) {
      values.put(
        "push_keys", JsonObjectBuilder()
          .add("auth", pushAuthKey!!)
          .add("private", pushPrivateKey!!)
          .add("public", pushPublicKey!!)
          .build()
          .toString()
      )
    }

    values.put("push_subscription", MastodonAPIController.gson.toJson(pushSubscription))
    values.put(
      "legacy_filters", JsonObjectBuilder()
        .add("filters", MastodonAPIController.gson.toJsonTree(wordFilters))
        .add("updated", filtersLastUpdated)
        .build()
        .toString()
    )
    values.put("push_id", pushAccountID)
    values.put("activation_info", MastodonAPIController.gson.toJson(activationInfo))
    values.put("preferences", MastodonAPIController.gson.toJson(preferences))
  }


  fun reloadPreferences(callback: Consumer<Preferences>?) {
    GetPreferences()
      .setCallback(object : Callback<Preferences> {
        override fun onSuccess(result: Preferences) {
          preferences = result
          callback?.accept(result)
          AccountSessionManager.getInstance().updateAccountPreferences(getID(), result)
        }

        override fun onError(error: ErrorResponse?) {
          Log.w(TAG, "Failed to load preferences for account ${getID()}: $error")
        }
      })
      .exec(getID())
  }


  fun reloadNotificationsMarker(callback: Consumer<String?>) {
    GetMarkers()
      .setCallback(object : Callback<TimelineMarkers> {
        override fun onSuccess(result: TimelineMarkers) {
          val notifications = result.notifications ?: return
          var markerId = notifications.lastReadId ?: return
          val lastKnown = getLastKnownNotificationsMarker()
          if (lastKnown != null &&
            ObjectIdComparator.INSTANCE.compare(markerId, lastKnown) < 0
          ) {
            // Marker moved back -- previous marker update must have failed.
            // Pretend it didn't happen and repeat the request.
            markerId = lastKnown
            SaveMarkers(null, markerId).exec(getID())
          }
          callback.accept(markerId)
          setNotificationsMarker(markerId, false)
        }


        override fun onError(error: ErrorResponse?) {}
      })
      .exec(getID())
  }


  fun setNotificationsMarker(id: String?, clearUnread: Boolean) {
    getRawLocalPreferences().edit { putString("notificationsMarker", id) }
    post(NotificationsMarkerUpdatedEvent(getID(), id, clearUnread))
  }

  fun logOut(activity: Activity, onDone: Runnable) {

    val cleanup = {
      AccountSessionManager.getInstance().removeAccount(getID())
      onDone.run()
    }

    app?.let { currentApp ->
      token?.let { currentToken ->
        RevokeOauthToken(
          currentApp.clientId,
          currentApp.clientSecret,
          currentToken.accessToken
        ).setCallback(object : Callback<Any> {
          override fun onSuccess(result: Any?) = cleanup()
          override fun onError(error: ErrorResponse?) = cleanup()
        })
          .wrapProgress(activity, R.string.loading, false)
          .exec(getID())
      }
    }

    cleanup()
  }

  fun savePreferencesLater() {
    preferencesNeedSaving = true
  }

  fun savePreferencesIfPending() {
    if (preferencesNeedSaving) {
      preferences?.let { pref ->
        UpdateAccountCredentialsPreferences(
          pref,
          null,
          self?.discoverable,
          self?.source?.indexable
        ).setCallback(object : Callback<Account> {
          override fun onSuccess(result: Account) {
            preferencesNeedSaving = false
            self = result
            AccountSessionManager.getInstance().updateAccountInfo(getID(), self)
          }

          override fun onError(error: ErrorResponse?) {
            Log.e(TAG, "failed to save preferences: $error")
          }
        }).exec(getID())
      } ?: Log.w(TAG, "AccountSession File: Preferences are null, cannot save pending preferences.")
    }
  }

  fun filterStatuses(statuses: MutableList<Status?>, context: FilterContext?) {
    filterStatusContainingObjects<Status?>(statuses, Function.identity<Status?>(), context)
  }

  fun <T> filterStatusContainingObjects(
    objects: MutableList<T?>,
    extractor: Function<T?, Status?>,
    context: FilterContext?
  ) {
    if (this.localPreferences.serverSideFiltersSupported) {
      // Even with server-side filters, clients are expected to remove statuses that match a filter that hides them
      objects.removeIf { o: T? ->
        val s = extractor.apply(o)
        if (s == null) return@removeIf false
        if (s.filtered == null) return@removeIf false
        for (filter in s.filtered) {
          if (filter.filter.isActive && filter.filter.filterAction == FilterAction.HIDE) return@removeIf true
        }
        false
      }
      return
    }
    for (obj in objects) {
      val s = extractor.apply(obj)
      if (s != null && s.filtered != null) {
        this.localPreferences.serverSideFiltersSupported = true
        this.localPreferences.save()
        return
      }
    }
    objects.removeIf { o: T? ->
      val s = extractor.apply(o)
      if (s == null) return@removeIf false
      for (filter in wordFilters) {
        if (filter.context.contains(context) && filter.matches(s) && filter.isActive) return@removeIf true
      }
      false
    }
  }

  fun updateAccountInfo() {
    AccountSessionManager.getInstance().updateSessionLocalInfo(this)
  }

}
