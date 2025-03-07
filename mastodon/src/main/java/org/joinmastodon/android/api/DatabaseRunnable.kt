package org.joinmastodon.android.api

import android.database.sqlite.SQLiteDatabase
import java.io.IOException

fun interface DatabaseRunnable {
    @Throws(IOException::class)
    fun run(db: SQLiteDatabase)
}
