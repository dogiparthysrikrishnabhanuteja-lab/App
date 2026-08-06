package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun generatePersonalizedWish(
        clientName: String,
        occasion: String, // "Birthday" or "Wedding Anniversary"
        customPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not configured in Secrets panel."))
        }

        try {
            val promptText = if (customPrompt.isNotBlank()) {
                "Generate a warm, professional $occasion wish for $clientName. Extra context: $customPrompt. Keep it suitable for sending via WhatsApp from their financial advisor."
            } else {
                "Generate a warm, respectful, and cheerful $occasion greeting message for client '$clientName' from their financial & insurance advisor. Include subtle best wishes for prosperity and health. Keep it under 60 words and ready to send via WhatsApp."
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val jsonObject = JSONObject(responseBody)
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCand = candidates.getJSONObject(0)
                    val content = firstCand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        return@withContext Result.success(text.trim())
                    }
                }
            }
            Result.failure(Exception("Failed to parse response: ${response.code} $responseBody"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateRenewalFollowUpMessage(
        clientName: String,
        productType: String,
        policyNumber: String,
        amount: Double,
        dueDate: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key is not configured."))
        }

        try {
            val promptText = """
                Draft a polite, highly professional WhatsApp renewal reminder message for:
                Client: $clientName
                Policy/Product: $productType
                Policy No: $policyNumber
                Premium Amount: ₹$amount
                Due Date: $dueDate

                The tone should be courteous, clear, and reassuring. Mention that timely payment ensures continuous cover/benefits. Add placeholders like [Payment Link]. Keep it within 70 words.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val jsonObject = JSONObject(responseBody)
                val candidates = jsonObject.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.getJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext Result.success(text.trim())
                    }
                }
            }
            Result.failure(Exception("Error generating message"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
