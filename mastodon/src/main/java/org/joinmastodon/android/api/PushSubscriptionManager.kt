package org.joinmastodon.android.api

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import me.grishka.appkit.api.Callback
import me.grishka.appkit.api.ErrorResponse
import org.joinmastodon.android.BuildConfig
import org.joinmastodon.android.MastodonApp
import org.joinmastodon.android.api.requests.notifications.RegisterForPushNotifications
import org.joinmastodon.android.api.requests.notifications.UpdatePushSettings
import org.joinmastodon.android.api.session.AccountSessionManager
import org.joinmastodon.android.model.PushNotification
import org.joinmastodon.android.model.PushSubscription
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


class PushSubscriptionManager(private val accountID: String) {

  companion object {

    private const val HMAC_SHA_256 = "HmacSHA256"


    private const val FCM_SENDER_ID = "449535203550"
    private const val EC_CURVE_NAME = "prime256v1"

    private val P256_HEAD = byteArrayOf(
      0x30.toByte(), 0x59.toByte(), 0x30.toByte(), 0x13.toByte(), 0x06.toByte(),
      0x07.toByte(), 0x2a.toByte(), 0x86.toByte(), 0x48.toByte(), 0xce.toByte(),
      0x3d.toByte(), 0x02.toByte(), 0x01.toByte(), 0x06.toByte(), 0x08.toByte(),
      0x2a.toByte(), 0x86.toByte(), 0x48.toByte(), 0xce.toByte(), 0x3d.toByte(),
      0x03.toByte(), 0x01.toByte(), 0x07.toByte(), 0x03.toByte(), 0x42.toByte(),
      0x00.toByte()
    )

    private val BASE85_DECODE_TABLE = intArrayOf(
      0xff, 0x44, 0xff, 0x54, 0x53, 0x52, 0x48, 0xff,
      0x4b, 0x4c, 0x46, 0x41, 0xff, 0x3f, 0x3e, 0x45,
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x40, 0xff, 0x49, 0x42, 0x4a, 0x47,
      0x51, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a,
      0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32,
      0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a,
      0x3b, 0x3c, 0x3d, 0x4d, 0xff, 0x4e, 0x43, 0xff,
      0xff, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
      0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
      0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20,
      0x21, 0x22, 0x23, 0x4f, 0xff, 0x50, 0xff, 0xff
    )

    private const val TAG = "PushSubscriptionManager"
    const val EXTRA_APPLICATION_PENDING_INTENT = "app"
    const val GSF_PACKAGE = "com.google.android.gms"

    /** Internal parameter used to indicate a 'subtype'. Will not be stored in DB for Nacho. */
    private const val EXTRA_SUBTYPE = "subtype"

    /** Extra used to indicate which senders (Google API project IDs) can send messages to the app */
    private const val EXTRA_SENDER = "sender"
    private const val EXTRA_SCOPE = "scope"
    private const val KID_VALUE = "|ID|1|"
    private const val TOKEN_REFRESH_INTERVAL = 30 * 24 * 60 * 60 * 1000L

    private var deviceToken: String? = null
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null
    private var authKey: ByteArray? = null

    @JvmStatic
    fun resetLocalPreferences() {
      getPrefs()?.edit { clear() }
    }

    @JvmStatic
    fun tryRegisterFCM() {
      deviceToken = getPrefs()?.getString("deviceToken", null)
      val tokenVersion = getPrefs()!!.getInt("version", 0)
      val tokenLastRefreshed = getPrefs()!!.getLong("lastRefresh", 0)

      if (!deviceToken.isNullOrEmpty() &&
        tokenVersion == BuildConfig.VERSION_CODE &&
        System.currentTimeMillis() - tokenLastRefreshed < TOKEN_REFRESH_INTERVAL
      ) {
        registerAllAccountsForPush(false)
        return
      }

      Log.i(
        TAG,
        "tryRegisterFCM: no token found, token due for refresh, or app was updated. Trying to get push token..."
      )

      val intent = Intent("com.google.iid.TOKEN_REQUEST").apply {
        setPackage(GSF_PACKAGE)
        putExtra(
          EXTRA_APPLICATION_PENDING_INTENT,
          PendingIntent.getBroadcast(MastodonApp.context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        )
        putExtra(EXTRA_SENDER, FCM_SENDER_ID)
        putExtra(EXTRA_SUBTYPE, FCM_SENDER_ID)
        putExtra(EXTRA_SCOPE, "*")
        putExtra("kid", KID_VALUE)
      }

      MastodonApp.context?.sendBroadcast(intent)
    }

    private fun getPrefs() = MastodonApp.context?.getSharedPreferences("push", Context.MODE_PRIVATE)

    @JvmStatic
    fun arePushNotificationsAvailable() = !deviceToken.isNullOrEmpty()

    private fun decode85(input: String): ByteArray {
      val data = ByteArrayOutputStream()
      var block = 0
      var n = 0

      for (char in input.toCharArray()) {
        val code = char.code
        if (code >= 32 && code < 128 && BASE85_DECODE_TABLE[code - 32] != 0xff) {
          val value = BASE85_DECODE_TABLE[code - 32]
          block = block * 85 + value
          n++
          if (n == 5) {
            data.write(block ushr 24)
            data.write(block ushr 16)
            data.write(block ushr 8)
            data.write(block)
            block = 0
            n = 0
          }
        }
      }

      if (n >= 4) data.write(block ushr 16)
      if (n >= 3) data.write(block ushr 8)
      if (n >= 2) data.write(block)

      return data.toByteArray()
    }

    private fun registerAllAccountsForPush(forceReRegister: Boolean) {
      if (!arePushNotificationsAvailable()) return

      AccountSessionManager.getInstance().loggedInAccounts.forEach { session ->
        when {
          session.pushSubscription == null || forceReRegister -> {
            session.pushSubscriptionManager.registerAccountForPush(session.pushSubscription)
          }

          session.needUpdatePushSettings -> {
            session.pushSubscriptionManager.updatePushSettings(session.pushSubscription ?: PushSubscription())
          }
        }
      }
    }
  }


  fun registerAccountForPush(subscription: PushSubscription?) {
    require(!deviceToken.isNullOrEmpty()) { "No device push token available" }

    MastodonAPIController.runInBackground {
      Log.d(TAG, "registerAccountForPush: started for $accountID")

      val encodedKeys = try {
        val generator = KeyPairGenerator.getInstance("EC")
        val spec = ECGenParameterSpec(EC_CURVE_NAME)
        generator.initialize(spec)

        val keyPair = generator.generateKeyPair()
        publicKey = keyPair.public
        privateKey = keyPair.private

        val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        val encodedPublicKey = publicKey?.let { key ->
          Base64.encodeToString(
            serializeRawPublicKey(key),
            flags
          )
        } ?: ""

        authKey = ByteArray(16)
        SecureRandom().nextBytes(authKey)

        val randomAccountID = ByteArray(16)
        SecureRandom().nextBytes(randomAccountID)

        val encodedPrivateKey = privateKey?.let { key ->
          Base64.encodeToString(
            key.encoded,
            flags
          )
        } ?: ""

        val encodedAuthKey = authKey?.let { key ->
          Base64.encodeToString(
            key,
            flags
          )
        } ?: ""

        val pushAccountID = Base64.encodeToString(
          randomAccountID,
          flags
        ) ?: ""

        EncodedKeys(encodedPublicKey, encodedPrivateKey, encodedAuthKey, pushAccountID)

      } catch (exception: Exception) {
        when (exception) {
          is NoSuchAlgorithmException, is InvalidAlgorithmParameterException -> {
            Log.e(TAG, "registerAccountForPush: error generating encryption key", exception)
          }
        }
        return@runInBackground
      }

      val session = AccountSessionManager.getInstance().tryGetAccount(accountID)
        ?: return@runInBackground

      with(session) {
        pushPrivateKey = encodedKeys.privateKey
        pushPublicKey = encodedKeys.publicKey
        pushAuthKey = encodedKeys.authKey
        pushAccountID = encodedKeys.accountID
      }

      AccountSessionManager.getInstance().writeAccountPushSettings(accountID)

      RegisterForPushNotifications(
        deviceToken!!,
        encodedKeys.publicKey,
        encodedKeys.authKey,
        subscription?.alerts ?: PushSubscription.Alerts.ofAll(),
        subscription?.policy ?: PushSubscription.Policy.ALL,
        encodedKeys.accountID
      ).setCallback(object : Callback<PushSubscription> {
        override fun onSuccess(result: PushSubscription) {
          MastodonAPIController.runInBackground {
            AccountSessionManager.getInstance().tryGetAccount(accountID)?.let { session ->
              session.pushSubscription = result
              AccountSessionManager.getInstance().writeAccountPushSettings(accountID)
              Log.d(TAG, "Successfully registered $accountID for push notifications")
            }
          }
        }

        override fun onError(error: ErrorResponse) {
          Log.w(TAG, "Failed to register account for push: $error")
        }
      }).exec(accountID)
    }
  }

  private data class EncodedKeys(
    val publicKey: String,
    val privateKey: String,
    val authKey: String,
    val accountID: String
  )


  fun updatePushSettings(subscription: PushSubscription) {
    UpdatePushSettings(subscription.alerts, subscription.policy)
      .setCallback(object : Callback<PushSubscription> {
        override fun onSuccess(result: PushSubscription) {
          AccountSessionManager.getInstance().tryGetAccount(accountID)?.let { session ->
            if (result.policy != subscription.policy) {
              result.policy = subscription.policy
            }
            session.pushSubscription = result
            session.needUpdatePushSettings = false
            AccountSessionManager.getInstance().writeAccountPushSettings(accountID)
          }
        }

        override fun onError(error: ErrorResponse) {
          val mastodonError = error as? MastodonErrorResponse
          if (mastodonError?.httpStatus == 404) {
            registerAccountForPush(subscription)
          } else {
            AccountSessionManager.getInstance().tryGetAccount(accountID)?.let { session ->
              session.needUpdatePushSettings = true
              session.pushSubscription = subscription
              AccountSessionManager.getInstance().writeAccountPushSettings(accountID)
            }
          }
        }
      }).exec(accountID)
  }

  private fun deserializeRawPublicKey(rawBytes: ByteArray): PublicKey? {
    if (rawBytes.size !in 64..65) return null

    return try {
      val kf = KeyFactory.getInstance("EC")
      val os = ByteArrayOutputStream().apply {
        write(P256_HEAD)
        if (rawBytes.size == 64) write(4)
        write(rawBytes)
      }
      kf.generatePublic(X509EncodedKeySpec(os.toByteArray()))
    } catch (exception: Exception) {
      when (exception) {
        is NoSuchAlgorithmException,
        is InvalidKeySpecException,
        is IOException -> {
          Log.e(TAG, "deserializeRawPublicKey", exception)
        }
      }
      null
    }
  }

  private fun serializeRawPublicKey(key: PublicKey): ByteArray {
    val point = (key as ECPublicKey).w
    var x = point.affineX.toByteArray()
    var y = point.affineY.toByteArray()

    if (x.size > 32) x = x.copyOfRange(x.size - 32, x.size)
    if (y.size > 32) y = y.copyOfRange(y.size - 32, y.size)

    return ByteArray(65).apply {
      this[0] = 4
      System.arraycopy(x, 0, this, 1 + (32 - x.size), x.size)
      System.arraycopy(y, 0, this, this.size - y.size, y.size)
    }
  }

  fun decryptNotification(k: String, p: String, s: String): PushNotification? {
    val serverKeyBytes = decode85(k)
    val payload = decode85(p)
    val salt = decode85(s)
    val serverKey = deserializeRawPublicKey(serverKeyBytes) ?: return null

    if (privateKey == null) {
      try {
        val kf = KeyFactory.getInstance("EC")
        val account = AccountSessionManager.getInstance().getAccount(accountID)
        val flag = Base64.URL_SAFE

        privateKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(account.pushPrivateKey, flag)))
        publicKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(account.pushPublicKey, flag)))
        authKey = Base64.decode(account.pushAuthKey, flag)
      } catch (exception: Exception) {
        when (exception) {
          is NoSuchAlgorithmException,
          is InvalidKeySpecException -> {
            Log.e(TAG, "decryptNotification: error loading private key", exception)
          }
        }
        return null
      }
    }

    val sharedSecret = try {
      KeyAgreement.getInstance("ECDH").run {
        this.init(privateKey)
        this.doPhase(serverKey, true)
        this.generateSecret()
      } ?: byteArrayOf()
    } catch (exception: Exception) {
      when (exception) {
        is NoSuchAlgorithmException,
        is InvalidKeyException -> {
          Log.e(
            TAG,
            "decryptNotification: error doing key exchange",
            exception
          )
        }
      }
      return null
    }

    val (key, nonce) = try {
      val secondSaltInfo = "Content-Encoding: auth\u0000".toByteArray()

      val secondSalt = authKey?.let { key ->
        deriveKey(key, sharedSecret, secondSaltInfo, 32)
      } ?: byteArrayOf()

      val keyInfo = publicKey?.let { key ->
        info("aesgcm", key, serverKey)
      } ?: byteArrayOf()

      val key = deriveKey(salt, secondSalt, keyInfo, 16)

      val nonceInfo = publicKey?.let { key ->
        info("nonce", key, serverKey)
      } ?: byteArrayOf()

      val nonce = deriveKey(salt, secondSalt, nonceInfo, 12)

      KeyAndNonce(key, nonce)

    } catch (exception: Exception) {
      when (exception) {
        is NoSuchAlgorithmException,
        is InvalidKeyException -> {
          Log.e(TAG, "decryptNotification: error deriving key", exception)
        }
      }
      return null
    }

    val decryptedStr = try {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val aesKey = SecretKeySpec(key, "AES")
      val iv = GCMParameterSpec(128, nonce)
      cipher.init(Cipher.DECRYPT_MODE, aesKey, iv)

      val decrypted = cipher.doFinal(payload)
      val decryptedStr = String(decrypted, 2, decrypted.size - 2, StandardCharsets.UTF_8)

      if (BuildConfig.DEBUG) {
        Log.i(TAG, "decryptNotification: notification json $decryptedStr")
      }

      decryptedStr

    } catch (exception: Exception) {
      when (exception) {
        is NoSuchAlgorithmException,
        is NoSuchPaddingException,
        is InvalidAlgorithmParameterException,
        is InvalidKeyException,
        is BadPaddingException,
        is IllegalBlockSizeException -> {
          Log.e(
            TAG,
            "decryptNotification: error decrypting payload",
            exception
          )
        }

      }
      return null
    }

    return try {
      val notification =
        MastodonAPIController.gson.fromJson(decryptedStr, PushNotification::class.java)
      notification.postprocess()
      notification
    } catch (e: IOException) {
      Log.e(TAG, "decryptNotification: error verifying notification object", e)
      null
    }
  }

  private data class KeyAndNonce(val key: ByteArray, val nonce: ByteArray) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as KeyAndNonce

      if (!key.contentEquals(other.key)) return false
      if (!nonce.contentEquals(other.nonce)) return false

      return true
    }

    override fun hashCode(): Int {
      var result = key.contentHashCode()
      result = 31 * result + nonce.contentHashCode()
      return result
    }
  }


  @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
  private fun deriveKey(
    firstSalt: ByteArray,
    secondSalt: ByteArray,
    info: ByteArray,
    length: Int
  ): ByteArray {
    val hmacContext = Mac.getInstance(HMAC_SHA_256)
    hmacContext.init(SecretKeySpec(firstSalt, HMAC_SHA_256))
    val hmac = hmacContext.doFinal(secondSalt)

    hmacContext.init(SecretKeySpec(hmac, HMAC_SHA_256))
    hmacContext.update(info)
    val result = hmacContext.doFinal(byteArrayOf(1))

    return if (result.size <= length) result else result.copyOfRange(0, length)
  }

  private fun info(
    type: String,
    clientPublicKey: PublicKey,
    serverPublicKey: PublicKey
  ): ByteArray {
    return ByteArrayOutputStream().apply {
      write("Content-Encoding: ".toByteArray())
      write(type.toByteArray())
      write(0)
      write("P-256".toByteArray())
      write(0)
      write(0)
      write(65)
      write(serializeRawPublicKey(clientPublicKey))
      write(0)
      write(65)
      write(serializeRawPublicKey(serverPublicKey))
    }.toByteArray()
  }

  class RegistrationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action != "com.google.android.c2dm.intent.REGISTRATION") return

      if (intent.hasExtra("registration_id")) {
        var token = intent.getStringExtra("registration_id") ?: return

        if (token.startsWith(KID_VALUE)) {
          token = token.substring(KID_VALUE.length + 1)
        }

        deviceToken = token

        getPrefs()?.edit {
          putString("deviceToken", token)
          putInt("version", BuildConfig.VERSION_CODE)
          putLong("lastRefresh", System.currentTimeMillis())
        }

        Log.i(TAG, "Successfully registered for FCM")
        registerAllAccountsForPush(true)
      } else {
        Log.e(TAG, "FCM registration intent did not contain registration_id: $intent")
        intent.extras?.keySet()?.forEach { key ->
          Log.i(TAG, "$key -> ${intent.extras?.getString(key)}")
        }
      }
    }
  }
}
