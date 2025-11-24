package com.bybit.rebalancer.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Bybit API v5 için authentication interceptor
 * Her istekte gerekli header'ları ekler:
 * - X-BAPI-API-KEY: API key
 * - X-BAPI-TIMESTAMP: Unix timestamp (ms)
 * - X-BAPI-SIGN: HMAC-SHA256 imza
 * - X-BAPI-RECV-WINDOW: Receive window (ms)
 */
class BybitAuthInterceptor(
    private val apiKeyProvider: () -> String,
    private val apiSecretProvider: () -> String
) : Interceptor {

    companion object {
        private const val RECV_WINDOW = "5000"
        private const val API_KEY_HEADER = "X-BAPI-API-KEY"
        private const val TIMESTAMP_HEADER = "X-BAPI-TIMESTAMP"
        private const val SIGN_HEADER = "X-BAPI-SIGN"
        private const val RECV_WINDOW_HEADER = "X-BAPI-RECV-WINDOW"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = apiKeyProvider()
        val apiSecret = apiSecretProvider()

        // API credentials yoksa direkt devam et
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val timestamp = System.currentTimeMillis().toString()
        val signPayload = buildSignPayload(originalRequest, timestamp)
        val signature = generateSignature(apiSecret, signPayload)

        val signedRequest = originalRequest.newBuilder()
            .header(API_KEY_HEADER, apiKey)
            .header(TIMESTAMP_HEADER, timestamp)
            .header(SIGN_HEADER, signature)
            .header(RECV_WINDOW_HEADER, RECV_WINDOW)
            .build()

        return chain.proceed(signedRequest)
    }

    /**
     * İmza için payload oluştur
     * GET: timestamp + apiKey + recvWindow + queryString
     * POST: timestamp + apiKey + recvWindow + body
     */
    private fun buildSignPayload(request: Request, timestamp: String): String {
        val apiKey = apiKeyProvider()

        return when (request.method) {
            "GET", "DELETE" -> {
                val queryString = request.url.query ?: ""
                "$timestamp$apiKey$RECV_WINDOW$queryString"
            }
            "POST", "PUT" -> {
                val body = request.body?.let { bodyToString(it) } ?: ""
                "$timestamp$apiKey$RECV_WINDOW$body"
            }
            else -> "$timestamp$apiKey$RECV_WINDOW"
        }
    }

    /**
     * RequestBody'yi string'e çevir
     */
    private fun bodyToString(body: RequestBody): String {
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * HMAC-SHA256 imza oluştur
     */
    private fun generateSignature(secret: String, payload: String): String {
        return try {
            val algorithm = "HmacSHA256"
            val mac = Mac.getInstance(algorithm)
            val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
            mac.init(secretKeySpec)
            val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("HMAC-SHA256 algorithm not available", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Invalid API secret key", e)
        }
    }
}
