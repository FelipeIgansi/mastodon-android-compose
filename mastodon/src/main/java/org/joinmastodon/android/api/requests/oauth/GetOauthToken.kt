package org.joinmastodon.android.api.requests.oauth

import com.google.gson.annotations.SerializedName
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.REDIRECT_URI
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.SCOPE
import org.joinmastodon.android.model.Token

class GetOauthToken(
  clientID: String,
  clientSecret: String,
  code: String?,
  grantType: GrantType
) : MastodonAPIRequest<Token>(
  method = HttpMethod.POST,
  path = "/oauth/token",
  respClass = Token::class.java
) {
  init {
    val isClientCredentialsGrant = GrantType.CLIENT_CREDENTIALS == grantType
    setRequestBody(
      Request(
        clientId = clientID,
        clientSecret = clientSecret,
        code = code,
        grantType = grantType,
        scope = if (isClientCredentialsGrant) SCOPE else null,
        redirectUri = if (isClientCredentialsGrant) null else REDIRECT_URI
      )
    )
  }

  override fun getPathPrefix() = ""

  private data class Request(
    val clientId: String,
    val clientSecret: String,
    val code: String?,
    val grantType: GrantType,
    val redirectUri: String?,
    val scope: String?
  )

  enum class GrantType {
    @SerializedName("authorization_code")
    AUTHORIZATION_CODE,

    @SerializedName("client_credentials")
    CLIENT_CREDENTIALS
  }
}
