@file:JvmName("MastodonAPIController")

package org.joinmastodon.android.api

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import me.grishka.appkit.utils.WorkerThread
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.joinmastodon.android.BuildConfig
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.api.gson.IsoInstantTypeAdapter
import org.joinmastodon.android.api.gson.IsoLocalDateTypeAdapter
import org.joinmastodon.android.api.session.AccountSession
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit


class MastodonAPIController(private val session: AccountSession?) {

  companion object {
    private const val TAG = "MastodonAPIController"

    @JvmField
    var gson: Gson = GsonBuilder()
      .disableHtmlEscaping()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(Instant::class.java, IsoInstantTypeAdapter())
      .registerTypeAdapter(LocalDate::class.java, IsoLocalDateTypeAdapter())
      .create()

    private val thread = WorkerThread("MastodonAPIController").apply { start() }

    private val httpClient = OkHttpClient.Builder()
      .connectTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .cache(Cache(File(MastodonApp.context!!.cacheDir, "http"), 10 * 1024 * 1024))
      .build()

    private val NO_CACHE_WHATSOEVER = CacheControl.Builder().noCache().noStore().build()

    @JvmStatic
    fun runInBackground(action: Runnable?) {
      thread.postRunnable(action, 0)
    }

    @JvmStatic
    fun getHttpClient(): OkHttpClient = this.httpClient

    private fun logTag(session: AccountSession?) = "[${session?.id ?: "no-auth"}] "

  }


  fun <T> submitRequest(req: MastodonAPIRequest<T>) {
    thread.postRunnable({
      try {
        if (req.canceled) return@postRunnable

        val builder = Request.Builder()
          .url(url = req.getURL().toString())
          .method(
            method = req.getMethod()!!,
            body = req.getRequestBody()
          )
          .header(
            name = "User-Agent",
            value = "MastodonAndroid/${BuildConfig.VERSION_NAME}"
          )

        val token = when {
          session != null -> session.token.accessToken
          req.token != null -> req.token?.accessToken
          else -> null
        }

        token?.let {
          builder.header(name = "Authorization", value = "Bearer $it")
        }

        if (!req.cacheable) {
          builder.cacheControl(cacheControl = NO_CACHE_WHATSOEVER)
        }

        req.headers?.forEach { (key, value) ->
          builder.header(key, value)
        }

        val hreq = builder.build()
        val call = httpClient.newCall(hreq)

        synchronized(req) {
          req.okhttpCall = call
        }

        if (req.timeout > 0) {
          call.timeout().timeout(req.timeout, TimeUnit.MILLISECONDS)
        }

        if (BuildConfig.DEBUG) {
          Log.d(TAG, "${logTag(session)}Sending request: $hreq")
        }

        call.enqueue(object : Callback {
          override fun onFailure(call: Call, e: IOException) {
            if (req.canceled) return

            if (BuildConfig.DEBUG) {
              Log.w(TAG, "${logTag(session)}$hreq failed", e)
            }

            synchronized(req) {
              req.okhttpCall = null
            }

            req.onError(e.localizedMessage ?: "Unknown error", 0, e)
          }

          override fun onResponse(call: Call, response: Response) {
            if (req.canceled) {
              response.close()
              return
            }

            if (BuildConfig.DEBUG) {
              Log.d(TAG, "${logTag(session)}$hreq received response: $response")
            }

            synchronized(req) {
              req.okhttpCall = null
            }

            try {
              response.body?.use { body ->
                val reader = body.charStream()

                if (response.isSuccessful) {
                  val respObj: T? = try {
                    if (BuildConfig.DEBUG) {
                      val respJson = JsonParser.parseReader(reader)
                      Log.d(TAG, "${logTag(session)}response body: $respJson")

                      when {
                        req.respTypeToken != null -> gson.fromJson(
                          respJson,
                          req.respTypeToken?.type
                        )

                        req.respClass != null -> gson.fromJson(
                          respJson,
                          req.respClass
                        )

                        else -> null
                      }
                    } else {
                      when {
                        req.respTypeToken != null -> gson.fromJson(
                          reader,
                          req.respTypeToken?.type
                        )

                        req.respClass != null -> gson.fromJson(
                          reader,
                          req.respClass
                        )

                        else -> null
                      }
                    }
                  } catch (exception: Exception) {
                    when (exception) {
                      is JsonIOException, is JsonSyntaxException -> {
                        if (BuildConfig.DEBUG) {
                          Log.w(
                            TAG,
                            "${logTag(session)}$response error parsing or reading body",
                            exception
                          )
                        }
                        req.onError(
                          exception.localizedMessage as String,
                          response.code,
                          exception
                        )
                        return
                      }

                      else -> throw exception
                    }
                  }

                  try {
                    req.validateAndPostprocessResponse(
                      respObj = respObj
                        ?: throw NullPointerException("Empty response body"),
                      httpResponse = response
                    )
                  } catch (ioException: IOException) {
                    if (BuildConfig.DEBUG) {
                      Log.w(
                        TAG,
                        "${logTag(session)}$response error post-processing or validating response",
                        ioException
                      )
                    }
                    req.onError(
                      ioException.localizedMessage ?: "Unknown error",
                      response.code,
                      ioException
                    )
                    return
                  }

                  if (BuildConfig.DEBUG) {
                    Log.d(
                      TAG,
                      "${logTag(session)}$response parsed successfully: $respObj"
                    )
                  }

                  req.onSuccess(respObj)
                } else {
                  try {
                    val errorJson = JsonParser.parseReader(reader).asJsonObject
                    Log.w(
                      TAG,
                      "${logTag(session)}$response received error: $errorJson"
                    )

                    if (errorJson.has("details")) {
                      val err = MastodonDetailedErrorResponse(
                        errorJson.get("error").asString,
                        response.code,
                        null
                      )

                      val details =
                        hashMapOf<String, List<MastodonDetailedErrorResponse.FieldError>>()
                      val errorDetails = errorJson.getAsJsonObject("details")

                      errorDetails.keySet().forEach { key ->
                        val fieldErrors =
                          arrayListOf<MastodonDetailedErrorResponse.FieldError>()
                        errorDetails.getAsJsonArray(key).forEach { el ->
                          val eobj = el.asJsonObject
                          val fe =
                            MastodonDetailedErrorResponse.FieldError()
                              .apply {
                                description =
                                  eobj.get("description").asString
                                error = eobj.get("error").asString
                              }
                          fieldErrors.add(fe)
                        }
                        details[key] = fieldErrors
                      }

                      err.detailedErrors = details
                      req.onError(err)
                    } else {
                      req.onError(
                        errorJson.get("error").asString,
                        response.code,
                        null
                      )
                    }
                  } catch (exception: Exception) {
                    when (exception) {
                      is JsonIOException, is JsonSyntaxException -> {
                        req.onError(
                          "${response.code} ${response.message}",
                          response.code,
                          exception
                        )
                      }
                    }
                  } catch (exception: Exception) {
                    req.onError(
                      "Error parsing an API error",
                      response.code,
                      exception
                    )
                  }
                }
              }
            } catch (exception: Exception) {
              Log.w(TAG, "onResponse: error processing response", exception)
              onFailure(
                call,
                IOException(exception).fillInStackTrace() as IOException
              )
            }
          }
        })

      } catch (exception: Exception) {
        if (BuildConfig.DEBUG) {
          Log.w(
            TAG,
            "${logTag(session)}error creating and sending http request",
            exception
          )
        }
        req.onError(exception.localizedMessage ?: "Unknown error", 0, exception)
      }
    }, 0)
  }

}