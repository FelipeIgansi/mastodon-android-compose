package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.PushSubscription
import org.joinmastodon.android.model.PushSubscription.Alerts

class RegisterForPushNotifications(
  deviceToken: String,
  encryptionKey: String,
  authKey: String,
  alerts: Alerts,
  policy: PushSubscription.Policy?,
  accountID: String
) : MastodonAPIRequest<PushSubscription>(
  method = HttpMethod.POST,
  path = "/push/subscription",
  respClass = PushSubscription::class.java
) {
  init {
    setRequestBody(Request().apply {
      this.subscription.endpoint = "https://app.joinmastodon.org/relay-to/fcm/$deviceToken/$accountID"
      this.data.alerts = alerts
      this.policy = policy
      this.subscription.keys.p256dh = encryptionKey
      this.subscription.keys.auth = authKey
    })
  }

  private class Request {
    var subscription: Subscription = Subscription()
    var data: Data = Data()
    var policy: PushSubscription.Policy? = null

    class Keys {
      var p256dh: String = ""
      var auth: String = ""
    }

    class Subscription {
      var endpoint: String = ""
      var keys: Keys = Keys()
    }

    class Data {
      var alerts: Alerts? = null
    }
  }
}
