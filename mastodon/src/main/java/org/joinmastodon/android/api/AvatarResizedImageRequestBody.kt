package org.joinmastodon.android.api

import android.graphics.Rect
import android.net.Uri
import kotlin.math.min
import kotlin.math.roundToInt

class AvatarResizedImageRequestBody(
  uri: Uri,
  progressListener: ProgressListener?
) : ResizedImageRequestBody(uri, 0, progressListener) {

  override fun getTargetSize(srcWidth: Int, srcHeight: Int): IntArray? {
    val factor = 400f / min(srcWidth, srcHeight)
    return intArrayOf((srcWidth * factor).roundToInt(), (srcHeight * factor).roundToInt())
  }

  override fun needResize(srcWidth: Int, srcHeight: Int) = srcHeight > 400 || srcWidth != srcHeight


  override fun needCrop(srcWidth: Int, srcHeight: Int) = srcWidth != srcHeight


  override fun getCropBounds(srcWidth: Int, srcHeight: Int): Rect {
    val halfSrcWidth = srcWidth / 2
    val halfSrcHeight = srcHeight / 2
    return if (srcWidth > srcHeight) {
      val differenceHalfWidthHeight = halfSrcWidth - halfSrcHeight
      Rect(differenceHalfWidthHeight, 0, differenceHalfWidthHeight + srcHeight, srcHeight)
    } else {
      val differenceHalfHeightWidth = halfSrcHeight - halfSrcWidth
      Rect(0, differenceHalfHeightWidth, srcWidth, differenceHalfHeightWidth + srcWidth)
    }
  }
}
