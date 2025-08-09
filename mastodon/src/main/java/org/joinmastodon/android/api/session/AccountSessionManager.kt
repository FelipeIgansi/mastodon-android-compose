package org.joinmastodon.android.api.session

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import me.grishka.appkit.api.APIRequest
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import org.joinmastodon.android.BuildConfig
import org.joinmastodon.android.E.post
import org.joinmastodon.android.MainActivity
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.R
import org.joinmastodon.android.api.CacheController
import org.joinmastodon.android.api.DatabaseRunnable
import org.joinmastodon.android.api.MastodonAPIController
import org.joinmastodon.android.api.MastodonAPIController.Companion.runInBackground
import org.joinmastodon.android.api.MastodonErrorResponse
import org.joinmastodon.android.api.PushSubscriptionManager.Companion.arePushNotificationsAvailable
import org.joinmastodon.android.api.WrapperRequest
import org.joinmastodon.android.api.gson.JsonObjectBuilder
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount
import org.joinmastodon.android.api.requests.filters.GetLegacyFilters
import org.joinmastodon.android.api.requests.instance.GetCustomEmojis
import org.joinmastodon.android.api.requests.instance.GetInstanceV1
import org.joinmastodon.android.api.requests.instance.GetInstanceV2
import org.joinmastodon.android.api.requests.oauth.CreateOAuthApp
import org.joinmastodon.android.events.EmojiUpdatedEvent
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.Application
import org.joinmastodon.android.model.Emoji
import org.joinmastodon.android.model.EmojiCategory
import org.joinmastodon.android.model.Instance
import org.joinmastodon.android.model.InstanceV1
import org.joinmastodon.android.model.InstanceV2
import org.joinmastodon.android.model.LegacyFilter
import org.joinmastodon.android.model.Preferences
import org.joinmastodon.android.model.Token
import org.joinmastodon.android.ui.utils.UiUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AccountSessionManager private constructor() {


  companion object {
    private const val TAG = "AccountSessionManager"
    const val SCOPE = "read write follow push"
    const val REDIRECT_URI = "mastodon-android-auth://callback"
    private const val DB_VERSION = 3

    @JvmStatic
    private fun findAnySessionForDomain(domain: String): AccountSession? {
      instance.sessions.values.forEach { session ->
        if (domain.equals(session.domain, ignoreCase = true)) return session
      }
      return null
    }


    @SuppressLint("StaticFieldLeak")
    @JvmField
    val instance: AccountSessionManager = AccountSessionManager()

    @JvmStatic
    fun getID(id: String) = instance.getAccount(id)

    private fun insertInstanceIntoDatabase(
      db: SQLiteDatabase,
      domain: String,
      instance: Instance,
      emojis: MutableList<Emoji>?,
      lastUpdated: Long
    ) {
      val values = ContentValues()
      values.put("domain", domain)
      values.put("instance_obj", MastodonAPIController.gson.toJson(instance))

      if (emojis != null) values.put("emojis", MastodonAPIController.gson.toJson(emojis))
      values.put("last_updated", lastUpdated)
      values.put("version", instance.getVersion())

      db.insertWithOnConflict("instances", null, values, CONFLICT_REPLACE)
    }

    @JvmStatic
    fun loadInstanceInfo(
      domain: String,
      callback: Callback<Instance>
    ): APIRequest<Instance> {
      val wrapper = WrapperRequest<Instance>()
      val session = findAnySessionForDomain(domain)
      val req = GetInstanceV2().setCallback(object : Callback<InstanceV2> {

        override fun onSuccess(result: InstanceV2) {
          wrapper.wrappedRequest = null
          callback.onSuccess(result)
        }

        override fun onError(error: ErrorResponse) {
          if (error is MastodonErrorResponse && error.httpStatus == 404) {
            // Mastodon pre-4.0 or a non-Mastodon server altogether. Let's try /api/v1/instance
            val fallbackReq = GetInstanceV1().setCallback(object : Callback<InstanceV1> {

              override fun onSuccess(result: InstanceV1) {
                wrapper.wrappedRequest = null
                callback.onSuccess(result)
              }

              override fun onError(error: ErrorResponse) {
                wrapper.wrappedRequest = null
                callback.onError(error)
              }
            })
            wrapper.wrappedRequest = fallbackReq
            if (session != null) fallbackReq.exec(session.getID())
            else fallbackReq.execNoAuth(domain)

          } else {
            wrapper.wrappedRequest = null
            callback.onError(error)
          }
        }
      })

      wrapper.wrappedRequest = req
      if (session != null) req.exec(session.getID())
      else req.execNoAuth(domain)
      
      return wrapper
    }
  }


  private val sessions = HashMap<String, AccountSession>()
  private val customEmojis = HashMap<String, MutableList<EmojiCategory>>()
  private val instancesLastUpdated = HashMap<String, Long>()
  private val instances = HashMap<String, Instance>()
  val unauthenticatedApiController = MastodonAPIController(null)
  var authenticatingInstance: Instance? = null
    private set
  var authenticatingApp: Application? = null
    private set
  private var lastActiveAccountID: String?
  val mastodonContext = checkNotNull(MastodonApp.context) {
    "AccountSessionManager File: The Mastodon Context could not be null"
  }
  private val prefs: SharedPreferences =
    mastodonContext.getSharedPreferences("account_manager", Context.MODE_PRIVATE)

  private var loadedInstances = false
  private var db: DatabaseHelper? = null
  private val databaseCloseRunnable = Runnable { this.closeDatabase() }
  private val databaseLock = Any()

  init {
    runWithDatabase { db ->
      val domains = mutableSetOf<String>()
      db.query("accounts", null, null, null, null, null, null).use { cursor ->
        val values = ContentValues()
        while (cursor.moveToNext()) {
          DatabaseUtils.cursorRowToContentValues(cursor, values)
          val session = AccountSession(values)
          session.domain?.let { domains.add(it.lowercase()) }
          sessions[session.getID()] = session
        }
      }
      readInstanceInfo(db, domains)
    }
    lastActiveAccountID = prefs.getString("lastActiveAccount", null)
    maybeUpdateShortcuts()
  }

  fun addAccount(
    instance: Instance,
    token: Token,
    self: Account,
    app: Application,
    activationInfo: AccountActivationInfo?
  ) {
    instances[instance.getDomain()] = instance
    runOnDbThread { db: SQLiteDatabase ->
      insertInstanceIntoDatabase(db, instance.getDomain(), instance, null, 0)
    }
    val session = AccountSession(
      token, self, app, instance.getDomain(), activationInfo == null, activationInfo
    )
    sessions[session.getID()] = session
    lastActiveAccountID = session.getID()
    prefs.edit { putString("lastActiveAccount", lastActiveAccountID) }
    runOnDbThread { db ->
      val values = ContentValues()
      session.toContentValues(values)
      db.insertWithOnConflict("accounts", null, values, CONFLICT_REPLACE)
    }
    updateInstanceEmojis(instance, instance.getDomain())
    if (arePushNotificationsAvailable()) {
      session.pushSubscriptionManager.registerAccountForPush(null)
    }
    maybeUpdateShortcuts()
  }

  val loggedInAccounts: MutableList<AccountSession>
    get() = sessions.values.toMutableList()

  fun getAccount(id: String): AccountSession {
    val session: AccountSession? = sessions[id]
    checkNotNull(session) { "Account session $id not found" }
    return session
  }

  fun tryGetAccount(id: String) = sessions[id]

  fun getLastActiveAccount(): AccountSession? {

    if (sessions.isEmpty() || lastActiveAccountID == null) return null
    if (!sessions.containsKey(lastActiveAccountID)) {
      // TODO figure out why this happens. It should not be possible.
      lastActiveAccountID = this.loggedInAccounts[0].getID()
      prefs.edit { putString("lastActiveAccount", lastActiveAccountID) }
    }
    return lastActiveAccountID?.let { getAccount(it) }

  }

  fun getLastActiveAccountID() = lastActiveAccountID

  fun setLastActiveAccountID(id: String) {
    check(sessions.containsKey(id)) { "Account session $id not found" }
    lastActiveAccountID = id
    prefs.edit { putString("lastActiveAccount", id) }
  }

  fun removeAccount(id: String) {
    val session = getAccount(id)
    session.cacheController.closeDatabase()
    mastodonContext.deleteDatabase("$id.db")
    mastodonContext.getSharedPreferences(id, 0).edit { clear() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      mastodonContext.deleteSharedPreferences(id)
    } else {
      mastodonContext.applicationInfo.dataDir?.let { dataDir ->
        val prefsDir = File(dataDir, "shared_prefs")
        File(prefsDir, "$id.xml").delete()
      }
    }
    sessions.remove(id)

    if (lastActiveAccountID == id) {
      lastActiveAccountID = if (sessions.isEmpty()) null
      else loggedInAccounts[0].getID()
      prefs.edit { putString("lastActiveAccount", lastActiveAccountID) }
    }

    runOnDbThread { db ->
      db.delete("accounts", "`id`=?", arrayOf(id))
      db.delete(
        "instances",
        "`domain` NOT IN (SELECT DISTINCT `domain` FROM `accounts`)",
        arrayOf()
      )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = mastodonContext.getSystemService(NotificationManager::class.java)
      try {
        nm.deleteNotificationChannelGroup(id)
      } catch (_: Exception) {
      }
    }
    maybeUpdateShortcuts()
  }

  fun authenticate(activity: Activity, instance: Instance) {
    authenticatingInstance = instance
    CreateOAuthApp().setCallback(object : Callback<Application> {
      override fun onSuccess(result: Application) {
        authenticatingApp = result
        val uri = Uri.Builder().apply {
          scheme("https")
          authority(instance.getDomain())
          path("/oauth/authorize")
          appendQueryParameter("response_type", "code")
          appendQueryParameter("client_id", result.clientId)
          appendQueryParameter("redirect_uri", REDIRECT_URI)
          appendQueryParameter("scope", SCOPE)
        }.build()

        CustomTabsIntent.Builder()
          .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
          .setShowTitle(true)
          .build()
          .launchUrl(activity, uri)
      }

      override fun onError(error: ErrorResponse) {
        error.showToast(activity)
      }
    }).wrapProgress(activity, R.string.preparing_auth, false)
      .execNoAuth(instance.getDomain())
  }

  fun isSelf(id: String, other: Account) = getAccount(id).self?.id == other.id

  fun maybeUpdateLocalInfo() {
    val now = System.currentTimeMillis()
    val domains = mutableSetOf<String>()
    for (session in sessions.values) {
      session.domain?.let { domains.add(it.lowercase()) }
      if (now - session.infoLastUpdated > 24L * 3600000L) {
        updateSessionLocalInfo(session)
      }
      if (!session.localPreferences.serverSideFiltersSupported && now - session.filtersLastUpdated > 3600000L) {
        updateSessionWordFilters(session)
      }
    }
    if (loadedInstances) maybeUpdateInstanceInfo(domains)
  }

  private fun maybeUpdateInstanceInfo(domains: MutableSet<String>) {
    val now = System.currentTimeMillis()
    for (domain in domains) {
      val lastUpdated = instancesLastUpdated.get(domain)
      if (lastUpdated == null || now - lastUpdated > 24L * 3600000L) {
        updateInstanceInfo(domain)
      }
    }
  }

  /*package*/
  fun updateSessionLocalInfo(session: AccountSession) {
    GetOwnAccount().setCallback(object : Callback<Account> {
      override fun onSuccess(result: Account) {
        session.self = result
        session.infoLastUpdated = System.currentTimeMillis()
        runOnDbThread { db ->
          val values = ContentValues().apply {
            put("account_obj", MastodonAPIController.gson.toJson(result))
            put("info_last_updated", session.infoLastUpdated)
          }
          db.update("accounts", values, "`id`=?", arrayOf(session.getID()))
        }
      }

      override fun onError(error: ErrorResponse) {
      }
    }).exec(session.getID())
  }

  private fun updateSessionWordFilters(session: AccountSession) {
    GetLegacyFilters().setCallback(object : Callback<MutableList<LegacyFilter>> {
      override fun onSuccess(result: MutableList<LegacyFilter>) {
        session.wordFilters = result
        session.filtersLastUpdated = System.currentTimeMillis()
        runOnDbThread { db ->
          val values = ContentValues().apply {
            put(
              "legacy_filters",
              JsonObjectBuilder().add(
                "filters",
                MastodonAPIController.gson.toJsonTree(session.wordFilters)
              ).add("updated", session.filtersLastUpdated).build().toString()
            )
          }
          db.update("accounts", values, "`id`=?", arrayOf(session.getID()))
        }
      }

      override fun onError(error: ErrorResponse) {}
    }).exec(session.getID())
  }

  fun updateInstanceInfo(domain: String) {
    loadInstanceInfo(domain, object : Callback<Instance> {
      override fun onSuccess(instance: Instance) {
        instances.put(domain, instance)
        runOnDbThread { db ->
          insertInstanceIntoDatabase(
            db, domain, instance, null, 0
          )
        }
        updateInstanceEmojis(instance, domain)
      }

      override fun onError(error: ErrorResponse) {}
    })
  }

  private fun updateInstanceEmojis(instance: Instance, domain: String) {
    val getCustomEmojisRequest =
      GetCustomEmojis().setCallback(object : Callback<MutableList<Emoji>> {
        override fun onSuccess(result: MutableList<Emoji>) {
          val lastUpdated = System.currentTimeMillis()
          customEmojis[domain] = groupCustomEmojis(result)
          instancesLastUpdated[domain] = lastUpdated
          runOnDbThread { db ->
            insertInstanceIntoDatabase(
              db, domain, instance, result, lastUpdated
            )
          }
          post(EmojiUpdatedEvent(domain))
        }

        override fun onError(error: ErrorResponse) {}
      })

    val session = sessions.values.firstOrNull { it.domain == domain }

    if (session?.token != null) {
      getCustomEmojisRequest.exec(domain, session.token!!)
    } else {
      getCustomEmojisRequest.execNoAuth(domain)
    }
  }

  private fun readInstanceInfo(db: SQLiteDatabase, domains: MutableSet<String>) {
    domains.forEach { domain ->
      val maxEmojiLength = 500000
      try {
        db.rawQuery(
          """SELECT domain, instance_obj, substr(emojis,1,?) 
            |    AS emojis, length(emojis) 
            |    AS emoji_length, last_updated, version 
            |FROM instances 
            |WHERE `domain` = ?""".trimMargin(),
          arrayOf(maxEmojiLength.toString(), domain)
        ).use { cursor ->
          val values = ContentValues()
          while (cursor.moveToNext()) {
            DatabaseUtils.cursorRowToContentValues(cursor, values)
            val version = values.getAsInteger("version") ?: 0
            val instance = MastodonAPIController.gson.fromJson(
              values.getAsString("instance_obj"),
              when (version) {
                1 -> InstanceV1::class.java
                2 -> InstanceV2::class.java
                else -> throw IllegalStateException("Unexpected value: $version")
              }
            )
            instances[domain] = instance
            val emojiSB = StringBuilder()
            val emojiPart = values.getAsString("emojis")
            if (emojiPart.isNullOrEmpty()) {
              // not putting anything into instancesLastUpdated to force a reload
              return@forEach
            }
            emojiSB.append(emojiPart)
            //get emoji in chunks of 1MB if it didn't fit in the first query
            val emojiStringLength = values.getAsInteger("emoji_length") ?: 0
            if (emojiStringLength > maxEmojiLength) {
              val pagesize = 1000000
              var start = maxEmojiLength + 1
              while (start <= emojiStringLength) {
                db.rawQuery(
                  """SELECT substr(emojis,?, ?) 
                    |FROM instances 
                    |WHERE `domain` = ?""".trimMargin(),
                  arrayOf(start.toString(), pagesize.toString(), domain)
                ).use { emojiCursor ->
                  emojiCursor.moveToNext()
                  emojiSB.append(emojiCursor.getString(0))
                }
                start += pagesize
              }
            }
            val emojis = MastodonAPIController.gson.fromJson<MutableList<Emoji>>(
              emojiSB.toString(), object : TypeToken<MutableList<Emoji>>() {}.type
            )
            customEmojis[domain] = groupCustomEmojis(emojis)
            instancesLastUpdated[domain] = values.getAsLong("last_updated") ?: 0L
          }
        }
      } catch (ex: Exception) {
        Log.d(TAG, "readInstanceInfo failed", ex)
        // instancesLastUpdated will not contain that domain, so instance data will be forced to be reloaded
      }
    }
    if (!loadedInstances) {
      loadedInstances = true
      runInBackground { maybeUpdateInstanceInfo(domains) }
    }
  }

  private fun groupCustomEmojis(emojis: MutableList<Emoji>): MutableList<EmojiCategory> {
    return emojis
      .filter { it.visibleInPicker }
      .groupBy { it.category ?: "" }
      .map { (category, emojis) -> EmojiCategory(category, emojis) }
      .sortedBy { it.title }
      .toMutableList()
  }

  fun getCustomEmojis(domain: String) = customEmojis[domain.lowercase()] ?: mutableListOf()


  fun getInstanceInfo(domain: String): Instance {
    instances[domain]?.let { return it }

    Log.e(
      TAG,
      "Instance info for $domain was not found. " +
          "This should normally never happen. " +
          "Returning fake instance object"
    )
    check(!BuildConfig.DEBUG) { "Instance info for $domain missing" }
    val fake = InstanceV1().apply {
      title = domain
      uri = domain
      email = ""
      version = ""
      description = ""
    }
    updateInstanceInfo(domain)
    return fake
  }

  fun updateAccountInfo(id: String, account: Account) {
    val session = getAccount(id).apply {
      self = account
      infoLastUpdated = System.currentTimeMillis()
    }

    runOnDbThread { db ->
      val values = ContentValues().apply {
        put("account_obj", MastodonAPIController.gson.toJson(account))
        put("info_last_updated", session.infoLastUpdated)
      }
      db.update("accounts", values, "`id`=?", arrayOf(session.getID()))
    }
  }

  fun updateAccountPreferences(id: String, prefs: Preferences) {
    val session = getAccount(id).apply { preferences = prefs }
    runOnDbThread { db ->
      val values = ContentValues().apply {
        put("preferences", MastodonAPIController.gson.toJson(prefs))
      }
      db.update("accounts", values, "`id`=?", arrayOf(session.getID()))
    }
  }

  fun writeAccountPushSettings(id: String) {
    val session = getAccount(id)
    runWithDatabase { db ->  // Called from a background thread anyway
      val values = ContentValues()
      val privateKey = session.pushPrivateKey
      val publicKey = session.pushPublicKey
      val authKey = session.pushAuthKey
      if (authKey != null &&
        privateKey != null &&
        publicKey != null
      ) {
        values.put(
          "push_keys",
          JsonObjectBuilder()
            .add("auth", authKey)
            .add("private", privateKey)
            .add("public", publicKey)
            .build()
            .toString()
        )
      }
      values.put("push_subscription", MastodonAPIController.gson.toJson(session.pushSubscription))
      values.put("flags", session.getFlagsForDatabase())
      values.put("push_id", session.pushAccountID)
      db.update("accounts", values, "`id`=?", arrayOf(id))
    }
  }

  fun writeAccountActivationInfo(id: String) {
    val session = getAccount(id)
    runOnDbThread { db ->
      val values = ContentValues().apply {
        put("activation_info", MastodonAPIController.gson.toJson(session.activationInfo))
        put("flags", session.getFlagsForDatabase())
      }
      db.update("accounts", values, "`id`=?", arrayOf(id))
    }
  }

  private fun maybeUpdateShortcuts() {
    if (Build.VERSION.SDK_INT < 26) return
    val sm = mastodonContext.getSystemService(ShortcutManager::class.java)
    if ((sm.dynamicShortcuts.isEmpty() || BuildConfig.DEBUG) && sessions.isNotEmpty()) {
      // There are no shortcuts, but there are accounts. Add a compose shortcut.
      val compose = ShortcutInfo.Builder(mastodonContext, "compose")
        .setActivity(
          ComponentName.createRelative(
            mastodonContext, MainActivity::class.java.name
          )
        )
        .setShortLabel(mastodonContext.getString(R.string.new_post))
        .setIcon(Icon.createWithResource(mastodonContext, R.mipmap.ic_shortcut_compose))
        .setIntent(
          Intent(mastodonContext, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra("compose", true)
        )
        .build()
      val explore = ShortcutInfo
        .Builder(mastodonContext, "explore")
        .setActivity(
          ComponentName.createRelative(
            mastodonContext, MainActivity::class.java.name
          )
        )
        .setShortLabel(mastodonContext.getString(R.string.tab_search))
        .setIcon(Icon.createWithResource(mastodonContext, R.mipmap.ic_shortcut_explore))
        .setIntent(
          Intent(mastodonContext, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra("explore", true)
        ).build()
      sm.dynamicShortcuts = listOf(compose, explore)
    } else if (sessions.isEmpty()) {
      // There are shortcuts, but no accounts. Disable existing shortcuts.
      sm.disableShortcuts(
        listOf("compose", "explore"), mastodonContext.getString(R.string.err_not_logged_in)
      )
    } else {
      sm.enableShortcuts(listOf("compose", "explore"))
    }
  }

  private fun closeDelayed() {
    CacheController.databaseThread.postRunnable(databaseCloseRunnable, 10000)
  }

  fun closeDatabase() {
    db?.let {
      if (BuildConfig.DEBUG) Log.d(TAG, "closeDatabase")
      db?.close()
      db = null
    }
  }

  private fun cancelDelayedClose() {
    db?.let {
      CacheController.databaseThread.handler
        .removeCallbacks(databaseCloseRunnable)
    }
  }

  private val orOpenDatabase: SQLiteDatabase
    get() {
      if (db == null) db = DatabaseHelper()
      return checkNotNull(db).writableDatabase
    }

  private fun runOnDbThread(runnable: DatabaseRunnable) {
    CacheController.databaseThread.postRunnable({
      synchronized(databaseLock) {
        cancelDelayedClose()
        try {
          val db = this.orOpenDatabase
          runnable.run(db)
        } catch (exception: Exception) {
          when (exception) {
            is SQLiteException,
            is IOException -> Log.w(TAG, exception)
          }
        } finally {
          closeDelayed()
        }
      }
    }, 0)
  }

  private fun runWithDatabase(runnable: DatabaseRunnable) {
    synchronized(databaseLock) {
      cancelDelayedClose()
      try {
        val db = this.orOpenDatabase
        runnable.run(db)
      } catch (exception: Exception) {
        when (exception) {
          is SQLiteException,
          is IOException -> Log.w(TAG, exception)
        }
      } finally {
        closeDelayed()
      }
    }
  }

  fun runIfDonationCampaignNotDismissed(id: String, action: Runnable) {
    runOnDbThread { db ->
      db.query(
        "dismissed_donation_campaigns", null, "id=?", arrayOf(id), null, null, null
      ).use { cursor ->
        if (!cursor.moveToFirst()) {
          UiUtils.runOnUiThread(action)
        }
      }
    }
  }

  fun markDonationCampaignAsDismissed(id: String) {
    runOnDbThread { db ->
      val values = ContentValues().apply {
        put("id", id)
        put("dismissed_at", System.currentTimeMillis())
      }
      db.insert("dismissed_donation_campaigns", null, values)
    }
  }

  fun clearDismissedDonationCampaigns() {
    runOnDbThread { db ->
      db.delete(
        "dismissed_donation_campaigns", null, null
      )
    }
  }

  fun clearInstanceInfo() {
    val db = this.orOpenDatabase
    db.delete("instances", null, null)
    db.close()
  }

  private class DatabaseHelper : SQLiteOpenHelper(MastodonApp.context, "accounts.db", null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
      db.execSQL(
        """
						CREATE TABLE `dismissed_donation_campaigns` (
							`id` text PRIMARY KEY,
							`dismissed_at` bigint
						)
						""".trimIndent()
      )
      createAccountsTable(db)
      db.execSQL(
        """
						CREATE TABLE `instances` (
							`domain` text PRIMARY KEY,
							`instance_obj` text,
							`emojis` text,
							`last_updated` bigint,
							`version` integer NOT NULL DEFAULT 1
						)
						""".trimIndent()
      )
      maybeMigrateAccounts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      if (oldVersion < 2) {
        createAccountsTable(db)
        db.execSQL(
          """
						CREATE TABLE `instances` (
							`domain` text PRIMARY KEY,
							`instance_obj` text,
							`emojis` text,
							`last_updated` bigint
						)
						""".trimIndent()
        )
        maybeMigrateAccounts(db)
      }
      if (oldVersion < 3) {
        db.execSQL("ALTER TABLE `instances` ADD `version` integer NOT NULL DEFAULT 1")
      }
    }

    private fun createAccountsTable(db: SQLiteDatabase) {
      db.execSQL(
        """
						CREATE TABLE `accounts` (
							`id` text PRIMARY KEY,
							`domain` text,
							`account_obj` text,
							`token` text,
							`application` text,
							`info_last_updated` bigint,
							`flags` bigint,
							`push_keys` text,
							`push_subscription` text,
							`legacy_filters` text DEFAULT NULL,
							`push_id` text,
							`activation_info` text,
							`preferences` text
						)
						""".trimIndent()
      )
    }

    private fun maybeMigrateAccounts(db: SQLiteDatabase) {
      val context = MastodonApp.context ?: return
      val accountsFile = File(context.filesDir, "accounts.json")
      if (accountsFile.exists()) {
        val domains = mutableSetOf<String>()
        try {
          FileInputStream(accountsFile).use { inputStream ->
            val jobj = JsonParser
              .parseReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
              .getAsJsonObject()
            val values = ContentValues()
            val accounts =
              if (jobj.has("a")) jobj.getAsJsonArray("a")
              else jobj.getAsJsonArray("accounts")
            for (jacc in accounts) {
              val session = MastodonAPIController.gson.fromJson(
                jacc, AccountSession::class.java
              )
              session.domain?.let { domains.add(it.lowercase()) }
              session.toContentValues(values)
              db.insertWithOnConflict("accounts", null, values, CONFLICT_REPLACE)
            }
          }
        } catch (x: Exception) {
          Log.e(TAG, "Error migrating accounts", x)
          return
        }
        accountsFile.delete()
        for (domain in domains) {
          val file = File(
            context.filesDir, "instance_${domain.replace('.', '_')}.json"
          )
          try {
            FileInputStream(file).use { inputStream ->
              val jobj =
                JsonParser.parseReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                  .getAsJsonObject()
              insertInstanceIntoDatabase(
                db, domain,
                MastodonAPIController.gson.fromJson(
                  jobj.get(if (jobj.has("instance")) "instance" else "a"), Instance::class.java
                ),
                MastodonAPIController.gson.fromJson(
                  jobj.get("emojis"), object : TypeToken<List<Emoji>>() {}.type
                ),
                jobj.get("last_updated").asLong
              )
            }
          } catch (x: Exception) {
            Log.w(TAG, "Error reading instance info file for $domain", x)
          }
          file.delete()
        }
      }
    }
  }
}
