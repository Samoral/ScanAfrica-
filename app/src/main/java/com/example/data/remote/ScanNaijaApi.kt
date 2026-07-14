package com.example.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// DTO Models
data class HealthResponse(
    val service: String,
    val version: String
)

data class CompanyRequest(
    val slug: String,
    val nameOfCompany: String,
    val regNo: String,
    val email: String,
    val mfrId: String
)

data class ProductDto(
    val barcode: String,
    val companyName: String,
    val companySlug: String,
    val mfrId: String,
    val productName: String,
    val category: String,
    val size: String,
    val price: String,
    val mfd: String,
    val expiry: String,
    val registeredAt: String
)

data class BulkProductsRequest(
    val products: List<ProductDto>
)

data class VerifyBarcodeResponse(
    val found: Boolean,
    val product: ProductDto? = null
)

data class VerifyCompanyResponse(
    val found: Boolean,
    val company: CompanyRequest? = null
)

data class PiracyFlagRequest(
    val barcode: String,
    val claimedCompany: String,
    val realCompany: String,
    val ts: String
)

data class PiracyFlagDto(
    val id: Int,
    val barcode: String,
    val claimedCompany: String,
    val realCompany: String,
    val ts: String
)

data class PiracyFlagsResponse(
    val flags: List<PiracyFlagDto>
)

interface ScanNaijaService {
    @GET("api/health")
    suspend fun getHealth(
        @Header("X-API-Key") apiKey: String?
    ): HealthResponse

    @POST("api/company")
    suspend fun registerCompany(
        @Header("X-API-Key") apiKey: String?,
        @Body company: CompanyRequest
    ): Response<Unit>

    @POST("api/products/bulk")
    suspend fun syncBulkProducts(
        @Header("X-API-Key") apiKey: String?,
        @Body request: BulkProductsRequest
    ): Response<Unit>

    @GET("api/verify/barcode/{barcode}")
    suspend fun verifyBarcode(
        @Path("barcode") barcode: String
    ): VerifyBarcodeResponse

    @GET("api/verify/company/{name}")
    suspend fun verifyCompany(
        @Path("name") name: String
    ): VerifyCompanyResponse

    @POST("api/piracy/flag")
    suspend fun flagPiracy(
        @Body flag: PiracyFlagRequest
    ): Response<Unit>

    @GET("api/piracy/flags/{companyName}")
    suspend fun getPiracyFlags(
        @Header("X-API-Key") apiKey: String?,
        @Path("companyName") companyName: String
    ): PiracyFlagsResponse
}

class ScanNaijaApiClient {
    private var cachedBaseUrl: String? = null
    private var cachedService: ScanNaijaService? = null

    fun getService(baseUrl: String): ScanNaijaService {
        // Sanitize base URL
        var url = baseUrl.trim()
        if (url.isEmpty()) {
            url = "http://localhost:3000"
        }
        if (!url.endsWith("/")) {
            url += "/"
        }

        if (url == cachedBaseUrl && cachedService != null) {
            return cachedService!!
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(ScanNaijaService::class.java)
        cachedBaseUrl = url
        cachedService = service
        return service
    }
}
