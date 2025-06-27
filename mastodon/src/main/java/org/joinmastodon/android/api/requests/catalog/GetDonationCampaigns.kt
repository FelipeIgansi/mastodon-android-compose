package org.joinmastodon.android.api.requests.catalog

import android.net.Uri
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.donations.DonationCampaign


class GetDonationCampaigns(
  private val locale: String,
  private val seed: String,
  private val source: String?
) : MastodonAPIRequest<DonationCampaign>(
  method = HttpMethod.GET,
  path = null,
  respClass = DonationCampaign::class.java
) {

  companion object {
    private const val HTTPS = "https"
    private const val DEFAULT_API = "api.joinmastodon.org"
    private const val API_ENDPOINT = "/v1/donations/campaigns/active"
    private const val PLATFORM = "platform"
    private const val ANDROID = "android"
    private const val LOCALE = "locale"
    private const val SEED = "seed"
    private const val ENVIRONMENT = "environment"
    private const val STAGING = "staging"
    private const val SOURCE = "source"
  }


  private var staging = false

  init {
    setCacheable()
  }

  fun setStaging(staging: Boolean) {
    this.staging = staging
  }

  override fun getURL(): Uri {

    return Uri.Builder().apply {

      scheme(HTTPS)
      authority(DEFAULT_API)
      path(API_ENDPOINT)

      appendQueryParameter(PLATFORM, ANDROID)
      appendQueryParameter(LOCALE, locale)
      appendQueryParameter(SEED, seed)

      if (staging) appendQueryParameter(ENVIRONMENT, STAGING)
      if (!source.isNullOrEmpty()) appendQueryParameter(SOURCE, source)

    }.build()

  }
}
