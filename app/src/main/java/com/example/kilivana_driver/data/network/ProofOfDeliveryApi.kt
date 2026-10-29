package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.ProofOfDelivery
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProofOfDeliveryApi {

    @GET("api/v1/logistics/proof-of-delivery/{id}")
    suspend fun getProofOfDelivery(
        @Path("id") id: Long
    ): ApiResponse<ProofOfDelivery>

    @GET("api/v1/logistics/proof-of-delivery/job/{jobId}")
    suspend fun getProofOfDeliveryByJob(
        @Path("jobId") jobId: Long
    ): ApiResponse<ProofOfDelivery>

    @GET("api/v1/logistics/proof-of-delivery/driver/{driverId}")
    suspend fun listProofsOfDeliveryByDriver(
        @Path("driverId") driverId: Long
    ): ApiResponse<List<ProofOfDelivery>>

    @POST("api/v1/logistics/proof-of-delivery")
    suspend fun fileProofOfDelivery(
        @Body body: ProofOfDelivery,
        @Query("otp") otp: String? = null
    ): ApiResponse<ProofOfDelivery>

    @DELETE("api/v1/logistics/proof-of-delivery/{id}")
    suspend fun deleteProofOfDelivery(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}
