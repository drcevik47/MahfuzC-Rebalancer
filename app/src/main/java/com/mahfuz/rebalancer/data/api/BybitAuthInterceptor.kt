package com.mahfuz.rebalancer.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Bybit API V5 Authentication Helper
 *
 * Signature generation rules:
 * - For GET requests: timestamp + apiKey + recvWindow + queryString
 * - For POST requests: timestamp + apiKey + recvWindow + jsonBody
 *
 * Use HMAC-SHA256 to sign the string
 */
object BybitSignatureHelper {

    private const val HMAC_SHA256 = "HmacSHA256"

    /**
     * Generate signature for API request
     *
     * @param timestamp Current timestamp in milliseconds
     * @param apiKey API key
     * @param recvWindow Receive window (default 5000)
     * @param queryOrBody Query string for GET or JSON body for POST
     * @param apiSecret API secret key
     * @return Hex encoded signature
     */
    fun generateSignature(
        timestamp: Long,
        apiKey: String,
        recvWindow: String,
        queryOrBody: String,
        apiSecret: String
    ): String {
        val paramStr = "$timestamp$apiKey$recvWindow$queryOrBody"
        return hmacSha256(paramStr, apiSecret)
    }

    /**
     * Generate signature for GET request
     */
    fun signGetRequest(
        apiKey: String,
        apiSecret: String,
        recvWindow: String = "5000",
        queryParams: Map<String, String?> = emptyMap()
    ): SignedRequest {
        val timestamp = System.currentTimeMillis()
        val queryString = queryParams
            .filterValues { it != null }
            .map { "${it.key}=${it.value}" }
            .sorted()
            .joinToString("&")

        val signature = generateSignature(timestamp, apiKey, recvWindow, queryString, apiSecret)

        return SignedRequest(
            timestamp = timestamp.toString(),
            signature = signature,
            recvWindow = recvWindow
        )
    }

    /**
     * Generate signature for POST request
     */
    fun signPostRequest(
        apiKey: String,
        apiSecret: String,
        recvWindow: String = "5000",
        jsonBody: String
    ): SignedRequest {
        val timestamp = System.currentTimeMillis()
        val signature = generateSignature(timestamp, apiKey, recvWindow, jsonBody, apiSecret)

        return SignedRequest(
            timestamp = timestamp.toString(),
            signature = signature,
            recvWindow = recvWindow
        )
    }

    private fun hmacSha256(data: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256)
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

data class SignedRequest(
    val timestamp: String,
    val signature: String,
    val recvWindow: String
)

/**
 * OkHttp Interceptor for logging API requests/responses
 */
class ApiLoggingInterceptor(
    private val onLog: (String, String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        onLog("API_REQUEST", "${request.method} ${request.url}")

        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = (System.nanoTime() - startTime) / 1_000_000

        onLog("API_RESPONSE", "${response.code} ${request.url} (${duration}ms)")

        return response
    }
}
