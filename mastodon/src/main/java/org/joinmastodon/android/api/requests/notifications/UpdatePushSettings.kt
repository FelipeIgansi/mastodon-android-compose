package org.joinmastodon.android.api.requests.notifications

import okhttp3.Response
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.PushSubscription
import org.joinmastodon.android.model.PushSubscription.*
import java.io.IOException

class UpdatePushSettings(alerts: Alerts, policy: Policy) :
  MastodonAPIRequest<PushSubscription>(
    method = HttpMethod.PUT,
    path = "/push/subscription",
    respClass = PushSubscription::class.java
  ) {
  private val policy: Policy

  init {
    setRequestBody(Request(alerts, policy))
    this.policy = policy
  }

  @Throws(IOException::class)
  override fun validateAndPostprocessResponse(
    respObj: PushSubscription,
    httpResponse: Response
  ) {
    super.validateAndPostprocessResponse(respObj, httpResponse)
    respObj.policy = policy
  }

  private class Request(
    alerts: Alerts,
    policy: Policy
  ) {
    var data: Data = Data()
    var policy: Policy

    init {
      this.data.alerts = alerts
      this.policy = policy
    }

    private class Data {
      var alerts: Alerts? = null
    }
  }
}
