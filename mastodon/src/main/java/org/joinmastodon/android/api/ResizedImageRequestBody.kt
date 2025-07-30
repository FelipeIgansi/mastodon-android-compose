package org.joinmastodon.android.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.Source
import okio.source
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.ui.utils.UiUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class ResizedImageRequestBody(
  private val uri: Uri,
  private val maxSize: Int,
  progressListener: ProgressListener?
) : CountingRequestBody(progressListener) {
  private var tempFile: File? = null
  private var contentType: MediaType? = null

  init {
    var opts = BitmapFactory.Options()
    opts.inJustDecodeBounds = true
    val context =
      checkNotNull(MastodonApp.context) { "ResizedImageRequestBody File: Context não pode ser nulo!" }
    val isFileScheme = "file" == uri.scheme
    if (isFileScheme) {
      BitmapFactory.decodeFile(uri.path, opts)
      contentType = UiUtils.getFileMediaType(File(uri.path ?: ""))
    } else {
      context.contentResolver.openInputStream(uri).use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, opts)
      }
      val mime = context.contentResolver.getType(uri)
      contentType = if (TextUtils.isEmpty(mime)) null else mime?.toMediaTypeOrNull()
    }
    if (contentType == null) contentType = "image/jpeg".toMediaType()
    if (needResize(opts.outWidth, opts.outHeight) || needCrop(opts.outWidth, opts.outHeight)) {
      var bitmap: Bitmap
      if (Build.VERSION.SDK_INT >= 28) {
        val source = if (isFileScheme) {
          ImageDecoder.createSource(File(uri.path ?: ""))
        } else {
          ImageDecoder.createSource(
            context.contentResolver,
            uri
          )
        }
        bitmap = ImageDecoder.decodeBitmap(
          source
        ) { decoder, info, source ->
          val size = getTargetSize(info.size.width, info.size.height)
          decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
          decoder.setTargetSize(size[0], size[1])
        }
        if (needCrop(bitmap.width, bitmap.height)) {
          val crop = getCropBounds(bitmap.width, bitmap.height) ?: Rect()
          bitmap = Bitmap.createBitmap(bitmap, crop.left, crop.top, crop.width(), crop.height())
        }
      } else {
        val size = getTargetSize(opts.outWidth, opts.outHeight)
        val targetWidth = size[0]
        val targetHeight = size[1]
        val factor = opts.outWidth / targetWidth.toFloat()
        opts = BitmapFactory.Options()
        opts.inSampleSize = factor.toInt()
        if (isFileScheme) {
          bitmap = BitmapFactory.decodeFile(uri.path, opts)
        } else {
          context.contentResolver.openInputStream(uri).use { inputStream ->
            bitmap = BitmapFactory.decodeStream(inputStream, null, opts) ?: createBitmap(0, 0)
          }
        }
        val needCrop = needCrop(targetWidth, targetHeight)
        if (factor % 1f != 0f || needCrop) {
          var srcBounds: Rect? = null
          val dstBounds: Rect?
          if (needCrop) {
            val crop = getCropBounds(targetWidth, targetHeight) ?: Rect()
            dstBounds = Rect(0, 0, crop.width(), crop.height())
            srcBounds =
              Rect(
                (crop.left / targetWidth.toFloat() * bitmap.width).roundToInt(),
                (crop.top / targetHeight.toFloat() * bitmap.height).roundToInt(),
                (crop.right / targetWidth.toFloat() * bitmap.width).roundToInt(),
                (crop.bottom / targetHeight.toFloat() * bitmap.height).roundToInt()
              )
          } else {
            dstBounds = Rect(0, 0, targetWidth, targetHeight)
          }
          val scaled = createBitmap(dstBounds.width(), dstBounds.height())
          Canvas(scaled).drawBitmap(bitmap, srcBounds, dstBounds, Paint(Paint.FILTER_BITMAP_FLAG))
          bitmap = scaled
        }
        var orientation = 0
        if (isFileScheme) {
          val exif = ExifInterface(uri.path ?: "")
          orientation =
            exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          context.contentResolver.openInputStream(uri).use { inputStream ->
            val exif = inputStream?.let { ExifInterface(it) } ?: ExifInterface("")
            orientation = exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)
          }
        }
        val rotation = when (orientation) {
          ORIENTATION_ROTATE_90 -> 90
          ORIENTATION_ROTATE_180 -> 180
          ORIENTATION_ROTATE_270 -> 270
          else -> 0
        }
        if (rotation != 0) {
          val matrix = Matrix()
          matrix.setRotate(rotation.toFloat())
          bitmap =
            Bitmap.createBitmap(
              bitmap,
              0,
              0,
              bitmap.width,
              bitmap.height,
              matrix,
              false
            )
        }
      }

      val isPNG = "image/png".equals(contentType)
      tempFile = File.createTempFile("mastodon_tmp_resized", null)
      FileOutputStream(tempFile).use { out ->
        if (isPNG) {
          bitmap.compress(Bitmap.CompressFormat.PNG, 0, out)
        } else {
          bitmap.compress(Bitmap.CompressFormat.JPEG, 97, out)
          contentType = "image/jpeg".toMediaType()
        }
      }
      length = tempFile?.length() ?: 0
    } else {
      if (isFileScheme) {
        length = File(uri.path ?: "").length()
      } else {
        context.contentResolver
          .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null).use { cursor ->
            cursor?.moveToFirst()
            length = cursor?.getInt(0)?.toLong() ?: 0
          }
      }
    }
  }

  @Throws(IOException::class)
  override fun openSource(): Source {
    return if (tempFile == null) {
      val context =
        checkNotNull(MastodonApp.context) { "ResizedImageRequestBody File: Context não pode ser nulo!" }
      val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("Não foi possível abrir InputStream para URI: $uri")
      inputStream.source()
    } else {
      tempFile!!.source()
    }
  }

  override fun contentType() = contentType


  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    try {
      super.writeTo(sink)
    } finally {
      tempFile?.delete()
    }
  }

  protected open fun getTargetSize(srcWidth: Int, srcHeight: Int): IntArray {
    val targetWidth = sqrt(maxSize.toDouble() * (srcWidth / srcHeight)).roundToInt()
    val targetHeight = sqrt(maxSize.toDouble() * (srcHeight / srcWidth)).roundToInt()
    return intArrayOf(targetWidth, targetHeight)
  }

  protected open fun needResize(srcWidth: Int, srcHeight: Int) = srcWidth * srcHeight > maxSize

  protected open fun needCrop(srcWidth: Int, srcHeight: Int) = false

  protected open fun getCropBounds(srcWidth: Int, srcHeight: Int): Rect? = null

}
