package com.medialert.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.medialert.utils.GeminiPrompts
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Named

// --- Response models ---

@JsonClass(generateAdapter = true)
data class GeminiPrescriptionResponse(
    @Json(name = "medicamentos") val medicamentos: List<GeminiMedication>
)

@JsonClass(generateAdapter = true)
data class GeminiMedication(
    @Json(name = "nombre") val nombre: String,
    @Json(name = "principioActivo") val principioActivo: String?,
    @Json(name = "dosis") val dosis: String,
    @Json(name = "formaFarmaceutica") val formaFarmaceutica: String,
    @Json(name = "frecuencia") val frecuencia: String,
    @Json(name = "horariosSugeridos") val horariosSugeridos: List<String>,
    @Json(name = "duracionDias") val duracionDias: Int?,
    @Json(name = "instruccionesEspeciales") val instruccionesEspeciales: String?,
    @Json(name = "confianza") val confianza: Double
)

@JsonClass(generateAdapter = true)
data class GeminiBoxResponse(
    @Json(name = "nombreComercial") val nombreComercial: String,
    @Json(name = "principioActivo") val principioActivo: String,
    @Json(name = "concentracion") val concentracion: String,
    @Json(name = "formaFarmaceutica") val formaFarmaceutica: String,
    @Json(name = "unidadesCaja") val unidadesCaja: Int?,
    @Json(name = "laboratorio") val laboratorio: String?,
    @Json(name = "confianza") val confianza: Double
)

@JsonClass(generateAdapter = true)
data class GeminiDoseResponse(
    @Json(name = "medicamentosDetectados") val medicamentosDetectados: List<String>,
    @Json(name = "coincideConEsperado") val coincideConEsperado: Boolean,
    @Json(name = "observaciones") val observaciones: String?,
    @Json(name = "confianza") val confianza: Double
)

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val code: Int? = null) : GeminiResult<Nothing>()
}

// --- Service ---

class GeminiApiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("gemini_api_key") private val apiKey: String,
    private val moshi: Moshi
) {
    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        private const val MAX_IMAGE_SIZE = 768 // px (compressed for API efficiency)
    }

    suspend fun analizarInforme(imagePath: String): GeminiResult<GeminiPrescriptionResponse> {
        return callGemini(
            imagePath = imagePath,
            prompt = GeminiPrompts.SCAN_PRESCRIPTION
        ) { json ->
            moshi.adapter(GeminiPrescriptionResponse::class.java).fromJson(json)
                ?: throw IllegalStateException("Empty response")
        }
    }

    suspend fun verificarCaja(imagePath: String): GeminiResult<GeminiBoxResponse> {
        return callGemini(
            imagePath = imagePath,
            prompt = GeminiPrompts.SCAN_MEDICATION_BOX
        ) { json ->
            moshi.adapter(GeminiBoxResponse::class.java).fromJson(json)
                ?: throw IllegalStateException("Empty response")
        }
    }

    suspend fun registrarToma(
        imagePath: String,
        medicationName: String,
        dose: String
    ): GeminiResult<GeminiDoseResponse> {
        return callGemini(
            imagePath = imagePath,
            prompt = GeminiPrompts.buildRegisterDosePrompt(medicationName, dose)
        ) { json ->
            moshi.adapter(GeminiDoseResponse::class.java).fromJson(json)
                ?: throw IllegalStateException("Empty response")
        }
    }

    private suspend fun <T> callGemini(
        imagePath: String,
        prompt: String,
        parser: (String) -> T
    ): GeminiResult<T> = withContext(Dispatchers.IO) {
        try {
            val base64Image = encodeImageToBase64(imagePath)
            val requestBody = buildRequestBody(prompt, base64Image)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return@withContext GeminiResult.Error("Empty body", response.code)

            if (!response.isSuccessful) {
                return@withContext GeminiResult.Error("HTTP ${response.code}: $bodyStr", response.code)
            }

            val extractedJson = extractJsonFromGeminiResponse(bodyStr)
            GeminiResult.Success(parser(extractedJson))

        } catch (e: Exception) {
            GeminiResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun encodeImageToBase64(imagePath: String): String {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)

        val scale = maxOf(
            options.outWidth / MAX_IMAGE_SIZE,
            options.outHeight / MAX_IMAGE_SIZE,
            1
        )

        val scaledOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        val bitmap = BitmapFactory.decodeFile(imagePath, scaledOptions)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        bitmap.recycle()

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestBody(prompt: String, base64Image: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("topK", 32)
                put("topP", 1)
                put("maxOutputTokens", 2048)
            })
        }.toString()
    }

    private fun extractJsonFromGeminiResponse(responseBody: String): String {
        val responseJson = JSONObject(responseBody)
        val text = responseJson
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()

        // Strip markdown code fences if present
        return text
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
