package org.joinmastodon.android.api

import android.app.Activity
import android.app.ProgressDialog
import android.net.Uri
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import com.google.gson.reflect.TypeToken
import me.grishka.appkit.api.APIRequest
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import okhttp3.Call
import okhttp3.RequestBody
import okhttp3.Response
import org.joinmastodon.android.BuildConfig
import org.joinmastodon.android.api.session.AccountSession
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.model.BaseModel
import org.joinmastodon.android.model.Token
import java.io.IOException

abstract class MastodonAPIRequest<T> : APIRequest<T> {
  companion object {
    private const val TAG = "MastodonAPIRequest"
  }


  private var domain: String? = null
  private var account: AccountSession? = null
  private var path: String? = null
  private var method: String? = null
  private var requestBody: Any? = null
  private var queryParams: MutableList<Pair<String, String>>? = null

  @JvmField
  var respClass: Class<T>? = null

  @JvmField
  var respTypeToken: TypeToken<T>? = null

  @JvmField
  var okhttpCall: Call? = null

  @JvmField
  var token: Token? = null

  @JvmField
  var canceled = false

  @JvmField
  var headers: MutableMap<String, String>? = null

  @JvmField
  var timeout: Long = 0

  @JvmField
  var cacheable = false
  private var progressDialog: ProgressDialog? = null

  @JvmField
  protected var removeUnsupportedItems = false


  constructor(method: HttpMethod?, path: String?, respClass: Class<T>?) {
    this.path = path
    this.method = method.toString()
    this.respClass = respClass
  }

  constructor(method: HttpMethod?, path: String?, respTypeToken: TypeToken<T>?) {
    this.path = path
    this.method = method.toString()
    this.respTypeToken = respTypeToken
  }


  @Synchronized
  override fun cancel() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "canceling request $this")
    }
    canceled = true
    okhttpCall?.cancel()
  }

  override fun exec(): APIRequest<T> {
    throw UnsupportedOperationException("Use exec(accountID) or execNoAuth(domain)")
  }


  fun exec(accountID: String): MastodonAPIRequest<T> {
    try {
      account = AccountSessionManager.getInstance().getAccount(accountID)
      domain = account?.domain
      account?.apiController?.submitRequest(this)
    } catch (x: Exception) {
      Log.e(TAG, "exec: this shouldn't happen, but it still did", x)
      invokeErrorCallback(MastodonErrorResponse(x.localizedMessage ?: "", -1, x))
    }
    return this
  }


  fun execNoAuth(domain: String): MastodonAPIRequest<T> {
    this.domain = domain
    AccountSessionManager.getInstance().unauthenticatedApiController.submitRequest(this)
    return this
  }

  fun exec(domain: String, token: Token): MastodonAPIRequest<T> {
    this.domain = domain
    this.token = token
    AccountSessionManager.getInstance().unauthenticatedApiController.submitRequest(this)
    return this
  }


  fun wrapProgress(
    activity: Activity, @StringRes message: Int, cancelable: Boolean
  ): MastodonAPIRequest<T> {
    progressDialog = ProgressDialog(activity).apply {
      setMessage(activity.getString(message))
      setCancelable(cancelable)
      if (cancelable) {
        setOnCancelListener { cancel() }
      }
      show()
    }
    return this
  }

  protected fun setRequestBody(body: Any?) {
    this.requestBody = body
  }

  protected fun addQueryParameter(key: String, value: String) {
    if (queryParams == null) {
      queryParams = ArrayList()
    }
    queryParams?.add(Pair(key, value))
  }


  protected fun addHeader(key: String, value: String) {
    if (headers == null) {
      headers = HashMap()
    }
    headers?.put(key, value)
  }


  protected fun setTimeout(timeout: Long) {
    this.timeout = timeout
  }

  protected fun setCacheable() {
    this.cacheable = true
  }


  protected open fun getPathPrefix() = "/api/v1"


  open fun getURL(): Uri {
    val builder = Uri.Builder().scheme("https").authority(domain).path(getPathPrefix() + path)

    queryParams?.forEach { param ->
      builder.appendQueryParameter(param.first, param.second)
    }

    return builder.build()
  }


  fun getMethod() = method

  @Throws(IOException::class)
  open fun getRequestBody(): RequestBody? {
    return when (requestBody) {
      is RequestBody -> requestBody as RequestBody
      null -> null
      else -> JsonObjectRequestBody(requestBody!!)
    }
  }

  override fun setCallback(callback: Callback<T>): MastodonAPIRequest<T> {
    super.setCallback(callback)
    return this
  }


  @CallSuper
  @Throws(IOException::class)
  open fun validateAndPostprocessResponse(respObj: T, httpResponse: Response) {
    when (respObj) {
      is BaseModel -> {
        respObj.postprocess()
      }

      is List<*> -> {
        if (removeUnsupportedItems) {
          val iterator = (respObj as MutableList<*>).iterator()
          while (iterator.hasNext()) {
            val item = iterator.next()
            if (item is BaseModel) {
              try {
                item.postprocess()
              } catch (x: ObjectValidationException) {
                Log.w(TAG, "Removing invalid object from list", x)
                iterator.remove()
              }
            }
          }
          respObj.forEach { item ->
            if (item is BaseModel) {
              item.postprocess()
            }
          }
        } else {
          respObj.forEach { item ->
            if (item is BaseModel) {
              item.postprocess()
            }
          }
        }
      }
    }
  }


  fun onError(err: ErrorResponse) {
    if (!canceled) {
      invokeErrorCallback(err)
    }
  }

  fun onError(msg: String, httpStatus: Int, exception: Throwable?) {
    if (!canceled) {
      invokeErrorCallback(MastodonErrorResponse(msg, httpStatus, exception))
    }
  }

  fun onSuccess(resp: T) {
    if (!canceled) {
      invokeSuccessCallback(resp)
    }
  }

  override fun onRequestDone() {
    progressDialog?.dismiss()
  }

  enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
  }

}