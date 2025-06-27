package org.joinmastodon.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.webkit.WebView
import me.grishka.appkit.imageloader.ImageCache
import me.grishka.appkit.utils.NetworkUtils
import me.grishka.appkit.utils.V
import org.joinmastodon.android.api.PushSubscriptionManager

class MastodonApp : Application() {
  override fun onCreate() {

    super.onCreate()
    context = applicationContext
    V.setApplicationContext(context)
    val params = ImageCache.Parameters()

    params.diskCacheSize = 100 * 1024 * 1024
    params.maxMemoryCacheSize = Int.Companion.MAX_VALUE

    ImageCache.setParams(params)
    NetworkUtils.setUserAgent("MastodonAndroid/" + BuildConfig.VERSION_NAME)

    PushSubscriptionManager.tryRegisterFCM()
    GlobalUserPreferences.load()

    if (BuildConfig.DEBUG) {
      WebView.setWebContentsDebuggingEnabled(true)
    }
  }

  companion object {
    @JvmField
    @SuppressLint("StaticFieldLeak") // it's not a leak
    var context: Context? = null
  }
}
