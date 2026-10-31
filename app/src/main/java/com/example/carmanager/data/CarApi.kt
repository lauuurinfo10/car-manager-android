package com.example.carmanager.data

import com.example.carmanager.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface CarApi {
    @GET("cars") suspend fun listCars(): List<Car>
    @GET("cars/{id}") suspend fun getCar(@Path("id") id: Int): Car
    @POST("cars") suspend fun createCar(@Body body: CarInput): Car
    @PATCH("cars/{id}") suspend fun updateCar(@Path("id") id: Int, @Body body: CarInput): Car
    @PATCH("cars/{id}") suspend fun updateMileage(@Path("id") id: Int, @Body body: MileageUpdate): Car
    @DELETE("cars/{id}") suspend fun deleteCar(@Path("id") id: Int)
    @GET("cars/{id}/costs") suspend fun getCosts(@Path("id") id: Int): CostSummary

    @GET("cars/{id}/documents") suspend fun listDocuments(@Path("id") carId: Int): List<CarDocument>
    @POST("cars/{id}/documents") suspend fun createDocument(@Path("id") carId: Int, @Body body: DocumentInput): CarDocument
    @PUT("documents/{id}") suspend fun updateDocument(@Path("id") id: Int, @Body body: DocumentInput): CarDocument
    @DELETE("documents/{id}") suspend fun deleteDocument(@Path("id") id: Int)
    @GET("documents/expiring") suspend fun expiringDocuments(@Query("days") days: Int = 30): List<ExpiringDocument>

    @GET("cars/{id}/maintenance") suspend fun listMaintenance(@Path("id") carId: Int): List<Maintenance>
    @POST("cars/{id}/maintenance") suspend fun createMaintenance(@Path("id") carId: Int, @Body body: MaintenanceInput): Maintenance
    @PUT("maintenance/{id}") suspend fun updateMaintenance(@Path("id") id: Int, @Body body: MaintenanceInput): Maintenance
    @DELETE("maintenance/{id}") suspend fun deleteMaintenance(@Path("id") id: Int)
}

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val http = OkHttpClient.Builder()
        // Render free „adoarme” serviciul; prima cerere poate dura ~1 minut.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
            if (BuildConfig.API_KEY.isNotEmpty()) req.header("X-API-Key", BuildConfig.API_KEY)
            chain.proceed(req.build())
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val api: CarApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CarApi::class.java)

    /** Transformă o excepție de rețea/HTTP într-un mesaj pe înțelesul utilizatorului. */
    fun errorMessage(t: Throwable): String = when (t) {
        is HttpException -> {
            val detail = runCatching {
                val body = t.response()?.errorBody()?.string().orEmpty()
                when (val d = json.parseToJsonElement(body).jsonObject["detail"]) {
                    is JsonPrimitive -> d.contentOrNull
                    // erorile de validare FastAPI (422) vin ca listă de obiecte cu "msg"
                    is JsonArray -> d.joinToString("\n") {
                        (it as? JsonObject)?.get("msg")?.jsonPrimitive?.contentOrNull.orEmpty()
                    }
                    else -> null
                }
            }.getOrNull()
            when {
                t.code() == 401 -> "Cheie API invalidă sau lipsă (verifică carApi.key în local.properties)."
                !detail.isNullOrBlank() -> detail
                else -> "Eroare server (${t.code()})."
            }
        }
        is IOException -> "Nu mă pot conecta la server. Verifică internetul și încearcă din nou."
        else -> t.message ?: "Eroare necunoscută."
    }
}
