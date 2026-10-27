package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionFindingRequest
import com.example.kilivana_driver.data.model.InspectionEvidenceImage
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Inspector endpoints use the existing host-root Retrofit client.
 * The /api/v1/ prefix is therefore included in each endpoint path.
 */
interface InspectorApi {

    @GET("api/v1/inspections/inspector/{inspectorId}")
    suspend fun inspections(
        @Path("inspectorId") inspectorId: Long
    ): Response<ApiResponse<List<InspectionDto>>>

    @GET("api/v1/inspections/{id}")
    suspend fun inspection(
        @Path("id") id: Long
    ): Response<ApiResponse<InspectionDto>>

    @PUT("api/v1/inspections/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): Response<ApiResponse<InspectionDto>>

    @PUT("api/v1/inspections/{id}/result")
    suspend fun updateResult(
        @Path("id") id: Long,
        @Query("result") result: String,
        @Body request: InspectionFindingRequest
    ): Response<ApiResponse<InspectionDto>>

    @GET("api/v1/inspections/{id}/evidence/images")
    suspend fun evidenceImages(@Path("id") id: Long): Response<ApiResponse<List<InspectionEvidenceImage>>>

    @Multipart
    @POST("api/v1/inspections/{id}/evidence/images")
    suspend fun uploadEvidence(
        @Path("id") id: Long,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<List<InspectionEvidenceImage>>>

    @DELETE("api/v1/inspections/{id}/evidence/images/{imageId}")
    suspend fun deleteEvidence(
        @Path("id") id: Long,
        @Path("imageId") imageId: Long
    ): Response<ApiResponse<Unit>>
}
