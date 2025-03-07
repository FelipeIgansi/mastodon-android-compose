package org.joinmastodon.android.api

interface ProgressListener {
    fun onProgress(transferred: Long, total: Long)
}
