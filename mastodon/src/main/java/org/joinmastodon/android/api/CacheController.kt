package org.joinmastodon.android.api

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.reflect.TypeToken
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import me.grishka.appkit.utils.WorkerThread
import org.joinmastodon.android.BuildConfig
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.api.requests.lists.GetLists
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV1
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV2
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV2.GroupedNotificationsResults
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.getID
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.CacheablePaginatedResponse
import org.joinmastodon.android.model.FilterContext
import org.joinmastodon.android.model.FollowList
import org.joinmastodon.android.model.Notification
import org.joinmastodon.android.model.NotificationGroup
import org.joinmastodon.android.model.NotificationType
import org.joinmastodon.android.model.PaginatedResponse
import org.joinmastodon.android.model.SearchResult
import org.joinmastodon.android.model.Status
import org.joinmastodon.android.model.viewmodel.NotificationViewModel
import java.io.IOException
import java.util.EnumSet
import java.util.function.Consumer


class CacheController(private val accountID: String) {

  companion object {
    private const val TIME = "time"
    private const val FLAGS = "flags"
    private const val HOME_TIMELINE = "home_timeline"
    private const val MENTIONS = "mentions"
    private const val ALL = "all"
    private const val ID = "id"
    private const val JSON = "json"
    private const val TYPE = "type"


    private const val TAG = "CacheController"
    private const val DB_VERSION = 5

    val databaseThread: WorkerThread = WorkerThread("databaseThread")
    val uiHandler: Handler = Handler(Looper.getMainLooper())

    private const val POST_FLAG_GAP_AFTER = 1

    init {
      databaseThread.start()
    }
  }


  private var db: DatabaseHelper? = null
  private val databaseCloseRunnable = Runnable { this.closeDatabase() }
  private var loadingNotifications = false
  private val pendingNotificationsCallbacks =
    ArrayList<Callback<PaginatedResponse<MutableList<NotificationViewModel>>>>()
  private var lists: MutableList<FollowList> = mutableListOf()

  fun getHomeTimeline(
    maxID: String?,
    count: Int,
    forceReload: Boolean,
    callback: Callback<CacheablePaginatedResponse<MutableList<Status>>>
  ) {
    cancelDelayedClose()
    databaseThread.postRunnable({
      try {
        if (!forceReload) {
          val base = getOrOpenDatabase()
          try {
            base.query(
              HOME_TIMELINE,
              arrayOf(JSON, FLAGS),
              maxID?.let { "`id`<?" },
              maxID?.let { arrayOf(maxID) },
              null,
              null,
              "`time` DESC",
              count.toString()
            ).use { cursor ->
              if (cursor.count == count) {
                val result = arrayListOf<Status>()
                cursor.moveToFirst()
                var newMaxID: String?
                do {
                  val status = MastodonAPIController.gson.fromJson(
                    cursor.getString(0),
                    Status::class.java
                  )
                  status.postprocess()
                  val flags = cursor.getInt(1)
                  status.hasGapAfter = ((flags and POST_FLAG_GAP_AFTER) != 0)
                  newMaxID = status.id
                  result.add(status)
                } while (cursor.moveToNext())
                getID(accountID).filterStatuses(result, FilterContext.HOME)
                uiHandler.post {
                  callback.onSuccess(
                    CacheablePaginatedResponse<MutableList<Status>>(
                      result,
                      newMaxID,
                      true
                    )
                  )
                }
                return@postRunnable
              }
            }
          } catch (exception: IOException) {
            Log.w(TAG, "getHomeTimeline: corrupted status object in database", exception)
          }
        }
        GetHomeTimeline(maxID, null, count, null)
          .setCallback(object : Callback<MutableList<Status>> {
            override fun onSuccess(result: MutableList<Status>) {
              val filtered = ArrayList<Status>(result)
              getID(accountID).filterStatuses(filtered, FilterContext.HOME)
              callback.onSuccess(
                CacheablePaginatedResponse<MutableList<Status>>(
                  filtered,
                  result.lastOrNull()?.id,
                  false
                )
              )
              putHomeTimeline(result, maxID == null)
            }

            override fun onError(error: ErrorResponse) {
              callback.onError(error)
            }
          })
          .exec(accountID)
      } catch (exception: SQLiteException) {
        Log.w(TAG, exception)
        uiHandler.post {
          callback.onError(
            MastodonErrorResponse(exception.localizedMessage, 500, exception)
          )
        }
      } finally {
        closeDelayed()
      }
    }, 0)
  }

  fun putHomeTimeline(posts: List<Status>, clear: Boolean) {
    runOnDbThread { db ->
      if (clear) db.delete(HOME_TIMELINE, null, null)
      val values = ContentValues(4)
      posts.forEach { status ->
        values.run {
          put(ID, status.id)
          put(JSON, MastodonAPIController.gson.toJson(status))
          var flags = 0
          if (status.hasGapAfter) flags = flags or POST_FLAG_GAP_AFTER
          put(FLAGS, flags)
          put(TIME, status.createdAt.epochSecond)
          db.insertWithOnConflict(HOME_TIMELINE, null, this, CONFLICT_REPLACE)
        }
      }
    }
  }

  private fun makeNotificationViewModels(
    notifications: List<NotificationGroup>,
    accounts: Map<String, Account>,
    statuses: Map<String, Status>
  ): MutableList<NotificationViewModel> {

    return notifications.mapNotNull { ng ->
      if (ng.type == null) return@mapNotNull null

      val accountList = ng.sampleAccountIds.mapNotNull { accounts[it] }
      if (accountList.size != ng.sampleAccountIds.size) return@mapNotNull null

      val status = ng.statusId?.let { statuses[it] }
      if (ng.statusId != null && status == null) return@mapNotNull null

      NotificationViewModel().apply {
        this.notification = ng
        this.accounts = accountList
        this.status = status
      }
    }.toMutableList()
  }

  fun getNotifications(
    maxID: String?,
    count: Int,
    onlyMentions: Boolean,
    forceReload: Boolean,
    callback: Callback<PaginatedResponse<MutableList<NotificationViewModel>>>
  ) {
    cancelDelayedClose()
    databaseThread.postRunnable({
      try {
        if (!forceReload) {
          val db = getOrOpenDatabase()
          val suffix = if (onlyMentions) MENTIONS else ALL
          val table = "notifications_$suffix"
          val accountsTable = "notifications_accounts_$suffix"
          val statusesTable = "notifications_statuses_$suffix"
          try {
            db.query(
              table,
              arrayOf(JSON),
              maxID?.let { "`max_id`<?" },
              maxID?.let { arrayOf(maxID) },
              null,
              null,
              "`time` DESC",
              count.toString()
            ).use { cursor ->
              if (cursor.count == count) {
                val result = arrayListOf<NotificationGroup>()
                cursor.moveToFirst()
                var newMaxID: String?
                val needAccounts = mutableSetOf<String>()
                val needStatuses = mutableSetOf<String>()
                do {
                  val ntf = MastodonAPIController.gson.fromJson(
                    cursor.getString(0),
                    NotificationGroup::class.java
                  ) ?: NotificationGroup()

                  ntf.postprocess()
                  newMaxID = ntf.pageMinId
                  needAccounts.addAll(ntf.sampleAccountIds)
                  if (ntf.statusId != null) needStatuses.add(ntf.statusId)
                  result.add(ntf)
                } while (cursor.moveToNext())
                val currentMaxID = newMaxID
                val accounts = HashMap<String, Account>()
                val statuses = HashMap<String, Status>()
                if (needAccounts.isNotEmpty()) {
                  db.query(
                    accountsTable,
                    arrayOf(JSON),
                    "`id` IN (${listOf(needAccounts.size, "?").joinToString(", ")})",
                    needAccounts.toTypedArray(),
                    null,
                    null,
                    null
                  ).use { cursor2 ->
                    while (cursor2.moveToNext()) {
                      val account = MastodonAPIController.gson.fromJson(
                        cursor2.getString(0),
                        Account::class.java
                      ) ?: Account()

                      account.postprocess()
                      accounts.put(account.id, account)
                    }
                  }
                }
                if (needStatuses.isNotEmpty()) {
                  db.query(
                    statusesTable,
                    arrayOf(JSON),
                    "`id` IN (${listOf(needStatuses.size, "?").joinToString(", ")})",
                    needStatuses.toTypedArray(),
                    null,
                    null,
                    null
                  ).use { cursor2 ->
                    while (cursor2.moveToNext()) {
                      val s = MastodonAPIController.gson.fromJson(
                        cursor2.getString(0),
                        Status::class.java
                      )
                      s.postprocess()
                      statuses.put(s.id, s)
                    }
                  }
                }
                uiHandler.post {
                  callback.onSuccess(
                    PaginatedResponse<MutableList<NotificationViewModel>>(
                      makeNotificationViewModels(result, accounts, statuses),
                      currentMaxID
                    )
                  )
                }
                return@postRunnable
              }
            }
          } catch (exception: IOException) {
            Log.w(TAG, "getNotifications: corrupted notification object in database", exception)
          }
        }

        if (!onlyMentions && loadingNotifications) {
          synchronized(pendingNotificationsCallbacks) {
            pendingNotificationsCallbacks.add(callback)
          }
          return@postRunnable
        }
        if (!onlyMentions) loadingNotifications = true
        val instanceInfo = getID(accountID).instanceInfo
        if ((instanceInfo?.apiVersion ?: 0) >= 2) {
          GetNotificationsV2(
            maxID,
            count,
            when (onlyMentions) {
              true -> EnumSet.of(NotificationType.MENTION)
              else -> EnumSet.allOf(NotificationType::class.java)
            }, NotificationType.getGroupableTypes()
          ).setCallback(object : Callback<GroupedNotificationsResults> {
            override fun onSuccess(result: GroupedNotificationsResults) {
              val accounts = result.accounts.associateBy { it.id }
              val statuses = result.statuses.associateBy { it.id }
              val notifications = makeNotificationViewModels(
                result.notificationGroups, accounts, statuses
              )
              databaseThread.postRunnable({
                putNotifications(
                  result.notificationGroups,
                  result.accounts,
                  result.statuses,
                  onlyMentions,
                  maxID == null
                )
              }, 0)
              val res = PaginatedResponse(
                notifications,
                result.notificationGroups.lastOrNull()?.pageMinId
              )
              callback.onSuccess(res)
              if (!onlyMentions) {
                loadingNotifications = false
                synchronized(pendingNotificationsCallbacks) {
                  pendingNotificationsCallbacks.forEach { it.onSuccess(res) }
                  pendingNotificationsCallbacks.clear()
                }
              }
            }

            override fun onError(error: ErrorResponse) {
              callback.onError(error)
              if (!onlyMentions) {
                loadingNotifications = false
                synchronized(pendingNotificationsCallbacks) {
                  pendingNotificationsCallbacks.forEach { it.onError(error) }
                  pendingNotificationsCallbacks.clear()
                }
              }
            }
          }).exec(accountID)
        } else {
          GetNotificationsV1(
            maxID,
            count,
            when (onlyMentions) {
              true -> EnumSet.of(NotificationType.MENTION)
              else -> EnumSet.allOf(NotificationType::class.java)
            }
          ).setCallback(object : Callback<MutableList<Notification>> {
            override fun onSuccess(result: MutableList<Notification>) {
              val filtered = ArrayList<Notification>(result)
              getID(accountID).filterStatusContainingObjects(
                filtered, { notification -> notification.status }, FilterContext.NOTIFICATIONS
              )

              val statuses = result.mapNotNull { it.status }
              val accounts = result.mapNotNull { it.account }

              val converted = filtered.map { notification ->
                val group = NotificationGroup().apply {
                  groupKey = "converted-${notification.id}"
                  notificationsCount = 1
                  type = notification.type
                  pageMinId = notification.id
                  pageMaxId = notification.id
                  mostRecentNotificationId = notification.id
                  latestPageNotificationAt = notification.createdAt
                  sampleAccountIds = listOf(notification.account?.id)
                  event = notification.event
                  moderationWarning = notification.moderationWarning
                  statusId = notification.status?.id
                }

                NotificationViewModel().apply {
                  this.notification = group
                  this.status = notification.status
                  this.accounts = listOf(notification.account)
                }
              }.toMutableList()
              val res = PaginatedResponse(
                converted,
                result.lastOrNull()?.id
              )
              callback.onSuccess(res)
              if (!onlyMentions) {
                loadingNotifications = false
                synchronized(pendingNotificationsCallbacks) {
                  pendingNotificationsCallbacks.forEach { it.onSuccess(res) }
                  pendingNotificationsCallbacks.clear()
                }
              }
              databaseThread.postRunnable({
                putNotifications(
                  converted.mapNotNull { it.notification },
                  accounts,
                  statuses,
                  onlyMentions,
                  maxID == null
                )
              }, 0)
            }

            override fun onError(error: ErrorResponse?) {
              callback.onError(error)
              if (!onlyMentions) {
                loadingNotifications = false
                synchronized(pendingNotificationsCallbacks) {
                  pendingNotificationsCallbacks.forEach { it.onError(error) }
                  pendingNotificationsCallbacks.clear()
                }
              }
            }
          }).exec(accountID)
        }
      } catch (e: SQLiteException) {
        Log.w(TAG, e)
        uiHandler.post {
          callback.onError(
            MastodonErrorResponse(e.localizedMessage, 500, e)
          )
        }
      } finally {
        closeDelayed()
      }
    }, 0)
  }

  private fun putNotifications(
    notifications: List<NotificationGroup>,
    accounts: List<Account>,
    statuses: List<Status>,
    onlyMentions: Boolean,
    clear: Boolean
  ) {
    runOnDbThread { base ->
      val suffix = if (onlyMentions) MENTIONS else ALL
      val table = "notifications_$suffix"
      val accountsTable = "notifications_accounts_$suffix"
      val statusesTable = "notifications_statuses_$suffix"
      if (clear) {
        base.delete(table, null, null)
        base.delete(accountsTable, null, null)
        base.delete(statusesTable, null, null)
      }
      ContentValues(4).run {
        notifications.forEach { ng ->
          if (ng.type == null) {
            return@forEach
          }
          put(ID, ng.groupKey)
          put(JSON, MastodonAPIController.gson.toJson(ng))
          put(TYPE, ng.type.ordinal)
          put(TIME, ng.latestPageNotificationAt.epochSecond)
          put("max_id", ng.pageMaxId)
          base.insertWithOnConflict(table, null, this, CONFLICT_REPLACE)
        }
        clear()
        accounts.forEach { account ->
          put(ID, account.id)
          put(JSON, MastodonAPIController.gson.toJson(account))
          base.insertWithOnConflict(accountsTable, null, this, CONFLICT_REPLACE)
        }
        statuses.forEach { status ->
          put(ID, status.id)
          put(JSON, MastodonAPIController.gson.toJson(status))
          base.insertWithOnConflict(statusesTable, null, this, CONFLICT_REPLACE)
        }
      }

    }
  }

  fun getRecentSearches(callback: Consumer<MutableList<SearchResult>>) {
    runOnDbThread { base ->
      base.query(
        "recent_searches",
        arrayOf(JSON),
        null,
        null,
        null,
        null,
        "time DESC"
      ).use { cursor ->
        val results: MutableList<SearchResult> = ArrayList()
        while (cursor.moveToNext()) {
          val result = MastodonAPIController.gson.fromJson(
            cursor.getString(0),
            SearchResult::class.java
          )
          result.postprocess()
          results.add(result)
        }
        uiHandler.post { callback.accept(results) }
      }
    }
  }

  fun putRecentSearch(result: SearchResult) {
    runOnDbThread { base ->
      ContentValues(4).run {
        put(ID, result.getID())
        put(JSON, MastodonAPIController.gson.toJson(result))
        put(TIME, (System.currentTimeMillis() / 1000).toInt())
        base.insertWithOnConflict("recent_searches", null, this, CONFLICT_REPLACE)
      }
    }
  }

  fun deleteStatus(id: String) {
    runOnDbThread { it.delete(HOME_TIMELINE, "`id`=?", arrayOf(id)) }
  }

  fun clearRecentSearches() {
    runOnDbThread { it.delete("recent_searches", null, null) }
  }

  private fun closeDelayed() {
    databaseThread.postRunnable(databaseCloseRunnable, 10000)
  }

  fun closeDatabase() {
    if (db != null) {
      if (BuildConfig.DEBUG) Log.d(TAG, "closeDatabase")
      db?.close()
      db = null
    }
  }

  private fun cancelDelayedClose() {
    if (db != null) {
      databaseThread.handler.removeCallbacks(databaseCloseRunnable)
    }
  }

  private fun getOrOpenDatabase(): SQLiteDatabase {
    return db?.writableDatabase ?: DatabaseHelper().writableDatabase
  }

  fun runOnDbThread(runnable: DatabaseRunnable) {
    runOnDbThread(runnable, null)
  }

  private fun runOnDbThread(runnable: DatabaseRunnable, onError: Consumer<Exception>?) {
    cancelDelayedClose()
    databaseThread.postRunnable({
      try {
        val base = getOrOpenDatabase()
        runnable.run(base)
      } catch (exception: Exception) {
        when (exception) {
          is SQLiteException,
          is IOException -> Log.w(TAG, exception)
        }
        onError?.accept(exception)
      } finally {
        closeDelayed()
      }
    }, 0)
  }

  fun reloadLists(callback: Callback<MutableList<FollowList>>) {
    GetLists()
      .setCallback(object : Callback<MutableList<FollowList>> {
        override fun onSuccess(result: MutableList<FollowList>) {
          result.sortedBy { it.title }
          lists = result
          callback.onSuccess(result)
          writeLists()
        }

        override fun onError(error: ErrorResponse) {
          callback.onError(error)
        }
      })
      .exec(accountID)
  }

  private fun loadLists(): MutableList<FollowList>? {
    val base = getOrOpenDatabase()
    base.query(
      "misc",
      arrayOf("value"),
      "`key`=?",
      arrayOf("lists"),
      null,
      null,
      null
    ).use { cursor ->
      if (!cursor.moveToFirst()) return null
      return MastodonAPIController.gson.fromJson(
        cursor.getString(0),
        object : TypeToken<MutableList<FollowList>>() {}.type
      )
    }
  }

  private fun writeLists() {
    runOnDbThread { base ->
      ContentValues().run {
        put("key", "lists")
        put("value", MastodonAPIController.gson.toJson(lists))
        base.insertWithOnConflict("misc", null, this, CONFLICT_REPLACE)
      }

    }
  }

  fun getLists(callback: Callback<MutableList<FollowList>>) {
    callback.onSuccess(lists)
    return databaseThread.postRunnable({
      val lists = loadLists() ?: mutableListOf()
      this.lists = lists
      uiHandler.post { callback.onSuccess(lists) }
      return@postRunnable
    }, 0)
  }

  fun addList(list: FollowList) {
    lists.run {
      add(list)
      sortedBy { it.title }
      writeLists()
    }
  }

  fun deleteList(id: String) {
    lists.removeIf { it.id == id }
    writeLists()
  }

  fun updateList(list: FollowList) {
    for (i in lists.indices) {
      if (lists[i].id == list.id) {
        lists[i] = list
        lists.sortedBy { it.title }
        writeLists()
        break
      }
    }
  }

  private inner class DatabaseHelper :
    SQLiteOpenHelper(MastodonApp.context, "$accountID.db", null, DB_VERSION) {
    override fun onCreate(base: SQLiteDatabase) {
      base.execSQL(
        """
						CREATE TABLE `home_timeline` (
							`id` VARCHAR(25) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`flags` INTEGER NOT NULL DEFAULT 0,
							`time` INTEGER NOT NULL
						)
						""".trimIndent()
      )
      createNotificationsTables(base, ALL)
      createNotificationsTables(base, MENTIONS)
      createRecentSearchesTable(base)
      createMiscTable(base)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      when {
        oldVersion < 2 -> createRecentSearchesTable(db)
        oldVersion < 3 -> addTimeColumns(db)
        oldVersion < 4 -> createMiscTable(db)

        oldVersion < 5 -> {
          db.execSQL("DROP TABLE `notifications_all`")
          db.execSQL("DROP TABLE `notifications_mentions`")
          createNotificationsTables(db, ALL)
          createNotificationsTables(db, MENTIONS)
        }
      }
    }

    fun createRecentSearchesTable(db: SQLiteDatabase) {
      db.execSQL(
        """
						CREATE TABLE `recent_searches` (
							`id` VARCHAR(50) NOT NULL PRIMARY KEY,
							`json` TEXT NOT NULL,
							`time` INTEGER NOT NULL
						)
						""".trimIndent()
      )
    }

    fun addTimeColumns(base: SQLiteDatabase) {
      base.run {
        execSQL("DELETE FROM `home_timeline`")
        execSQL("DELETE FROM `notifications_all`")
        execSQL("DELETE FROM `notifications_mentions`")
        execSQL("ALTER TABLE `home_timeline` ADD `time` INTEGER NOT NULL DEFAULT 0")
        execSQL("ALTER TABLE `notifications_all` ADD `time` INTEGER NOT NULL DEFAULT 0")
        execSQL("ALTER TABLE `notifications_mentions` ADD `time` INTEGER NOT NULL DEFAULT 0")
      }
    }

    fun createMiscTable(db: SQLiteDatabase) {
      db.execSQL( """
        CREATE TABLE `misc` (
							`key` TEXT NOT NULL PRIMARY KEY,
							`value` TEXT )	""".trimIndent()
      )
    }

    fun createNotificationsTables(base: SQLiteDatabase, suffix: String?) {
      base.run {
        execSQL("""
          CREATE TABLE `notifications_${suffix}` (
                `id` VARCHAR(100) NOT NULL PRIMARY KEY,
                `json` TEXT NOT NULL,
                `flags` INTEGER NOT NULL DEFAULT 0,
                `type` INTEGER NOT NULL,
                `time` INTEGER NOT NULL,
                `max_id` VARCHAR(25) NOT NULL ) """.trimIndent()
        )
        execSQL("CREATE INDEX `notifications_${suffix}_max_id` ON `notifications_$suffix`(`max_id`)")
        execSQL("""  
          CREATE TABLE `notifications_accounts_${suffix}` (
                `id` VARCHAR(25) NOT NULL PRIMARY KEY,
                `json` TEXT NOT NULL )""".trimIndent()
        )
        execSQL("""
          CREATE TABLE `notifications_statuses_${suffix}` (
                `id` VARCHAR(25) NOT NULL PRIMARY KEY,
                `json` TEXT NOT NULL ) """.trimIndent()
        )
      }
    }
  }
}
