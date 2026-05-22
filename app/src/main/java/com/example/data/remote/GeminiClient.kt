package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun extractTextFromCaptcha(base64Image: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "ERROR_NO_API_KEY"
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "You are an OCR API. Extract the alphanumeric characters from this CAPTCHA image. Reply ONLY with the extracted characters as a single word, with no spaces, punctuation, or explanations. If you cannot read it, reply with UNKNOWN."),
                        Part(inlineData = InlineData(mimeType = "image/png", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.1f)
        )

        val response = service.generateContent(apiKey, request)
        val extracted = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "UNKNOWN"
        return extracted.filter { it.isLetterOrDigit() }.uppercase()
    }

    suspend fun generateConversationalReply(
        systemInstruction: String,
        history: List<Content>
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Note: Gemini API Key is not configured. Please add your key to the Secrets panel in AI Studio.\n\nHere is a local math calculation for your command shortcut."
        }

        try {
            val request = GeminiRequest(
                contents = history,
                generationConfig = GenerationConfig(temperature = 0.7f),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            val response = service.generateContent(apiKey, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Sorry, I couldn't formulate a reply."
        } catch (e: Exception) {
            return "Error calling AI chatbot: ${e.message ?: "Unknown issue"}. Let's discuss offline or retry."
        }
    }
}
