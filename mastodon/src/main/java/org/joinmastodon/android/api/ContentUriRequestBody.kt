package org.joinmastodon.android.api

import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Source
import okio.source
import org.joinmastodon.android.MastodonApp
import java.io.IOException

internal class ContentUriRequestBody(
  private val uri: Uri,
  progressListener: ProgressListener?
) : CountingRequestBody(progressListener) {
  init {
    MastodonApp.context!!.contentResolver
      .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
      .use { cursor ->
        cursor?.use {
          if (it.moveToFirst()) {
            length = it.getLong(0)
          }
        }
      }
  }

  override fun contentType(): MediaType? {
    val mimeType = MastodonApp.context!!.contentResolver.getType(uri) ?: return null
    return mimeType.toMediaTypeOrNull()
  }

  @Throws(IOException::class)
  override fun openSource(): Source {
    return MastodonApp.context!!.contentResolver.openInputStream(uri)
      ?.use { it.source() }
      ?: throw IOException("Unable to open InputStream for URI: $uri")
  }
}
