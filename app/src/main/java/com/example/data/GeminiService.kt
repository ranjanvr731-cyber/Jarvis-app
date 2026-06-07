package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // Using gemini-3.5-flash as the default for robust and fast performance
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured.")
            return@withContext "Error: Gemini API Key is missing. Please configure it in the Secrets panel in AI Studio, sir."
        }

        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

        try {
            // Build contents array including conversation history if any
            val contentsArray = JSONArray()

            // 1. Add older history entries
            history.forEach { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                val textPart = JSONObject().put("text", msg.message)
                val parts = JSONArray().put(textPart)
                val contentObj = JSONObject()
                    .put("role", role)
                    .put("parts", parts)
                contentsArray.put(contentObj)
            }

            // 2. Add current prompt
            val currentUserPart = JSONObject().put("text", prompt)
            val currentParts = JSONArray().put(currentUserPart)
            val currentContentObj = JSONObject()
                .put("role", "user")
                .put("parts", currentParts)
            contentsArray.put(currentContentObj)

            // Build request JSON
            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                
                // Add system instructions if present
                if (systemInstruction.isNotEmpty()) {
                    val systemPart = JSONObject().put("text", systemInstruction)
                    val systemParts = JSONArray().put(systemPart)
                    val systemInstructionObj = JSONObject().put("parts", systemParts)
                    put("systemInstruction", systemInstructionObj)
                }

                // Add temperature configs inside generationConfig
                val config = JSONObject()
                    .put("temperature", 0.7)
                put("generationConfig", config)
            }

            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed code ${response.code}: $errorBody")
                    
                    // User friendly parsing
                    if (response.code == 400 && errorBody.contains("API key not valid")) {
                        return@withContext "Apologies, sir. The Gemini API key provided appears to be invalid. Please verify it in your AI Studio settings."
                    }
                    return@withContext "I encountered a communication error with the core systems, sir. Status code: ${response.code}."
                }

                val responseBody = response.body?.string() ?: return@withContext "I received an empty telemetry package from the online core, sir."
                
                // Parse JSON response
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Telemetry parsing fetched empty text data.")
                        }
                    }
                }
                
                return@withContext "My neural link parsed the response, but found no descriptive command outputs, sir."
            }

        } catch (e: IOException) {
            Log.e(TAG, "Network exception during generation", e)
            "A standard network failure occurred while attempting to contact the servers, sir. Details: ${e.localizedMessage ?: "Unknown connection error"}."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during generation", e)
            "I suffered a visual syntax buffer overflow inside my parsing routine, sir. Error: ${e.localizedMessage ?: "Unknown parser error"}."
        }
    }
}
