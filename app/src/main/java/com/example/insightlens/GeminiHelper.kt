package com.example.insightlens

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiHelper {
    // ⚠️ Ensure your API Key is correct
    private val apiKey = "PLACE_YOUR_API_KEY_HERE"

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // FIXED: Using "gemini-2.5-flash-lite"
                // This is the specific 2026 model with high free-tier limits (1,000/day).
                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash-lite",
                    apiKey = apiKey
                )

                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                response.text ?: "No response from AI."
            } catch (e: Exception) {
                if (e.message?.contains("Quota") == true) {
                    "⚠️ Limit reached (1000/day). Try again tomorrow."
                } else if (e.message?.contains("404") == true) {
                    "⚠️ Model not found. Check API Key or spelling."
                } else {
                    "Error: ${e.message}"
                }
            }
        }
    }
}