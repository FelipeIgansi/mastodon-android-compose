package org.joinmastodon.android

import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileNotFoundException

class TweakedFileProvider : FileProvider() {
  override fun getType(uri: Uri): String? {
    Log.d(TAG, "getType() called with: uri = [$uri]")
    if (uri.pathSegments[0] == "image_cache") {
      Log.i(TAG, "getType: HERE!")
      return "image/jpeg" // might as well be a png but image decoding APIs don't care, needs to be image/* though
    }
    return super.getType(uri)
  }

  override fun query(
    uri: Uri,
    projection: Array<String?>?,
    selection: String?,
    selectionArgs: Array<String?>?,
    sortOrder: String?
  ): Cursor? {
    Log.d(
      TAG,
      "query() called with: uri = [$uri], projection = [${projection.contentToString()}], selection = [$selection], selectionArgs = [${selectionArgs.contentToString()}], sortOrder = [$sortOrder]"
    )
    return super.query(uri, projection, selection, selectionArgs, sortOrder)
  }

  @Throws(FileNotFoundException::class)
  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
    Log.d(TAG, "openFile() called with: uri = [$uri], mode = [$mode]")
    return super.openFile(uri, mode)
  }

  companion object {
    private const val TAG = "TweakedFileProvider"
  }
}
