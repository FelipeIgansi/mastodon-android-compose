package org.joinmastodon.android.api

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import me.grishka.appkit.api.ErrorResponse
import org.joinmastodon.android.R
import java.net.SocketTimeoutException
import java.net.UnknownHostException

open class MastodonErrorResponse(
  @JvmField val error: String?,
  @JvmField val httpStatus: Int,
  underlyingException: Throwable?
) : ErrorResponse() {

  val messageResource = when {
    underlyingException is UnknownHostException -> R.string.could_not_reach_server
    underlyingException is SocketTimeoutException -> R.string.connection_timed_out

    underlyingException is JsonSyntaxException || underlyingException is JsonIOException ||
        httpStatus >= 500 -> R.string.server_error

    httpStatus == 404 -> R.string.not_found
    else -> 0
  }

  override fun bindErrorView(view: View) {
    val text = view.findViewById<TextView>(R.id.error_text)
    val message = getErrorMessage(view.context)

    text.text = message
  }

  override fun showToast(context: Context?) {
    context?.let {
      val message = getErrorMessage(it)
      Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
    }

  }

  private fun getErrorMessage(context: Context): String {
    return when {
      messageResource > 0 -> context.getString(messageResource, error.orEmpty())
      else -> error.orEmpty()
    }
  }
}
