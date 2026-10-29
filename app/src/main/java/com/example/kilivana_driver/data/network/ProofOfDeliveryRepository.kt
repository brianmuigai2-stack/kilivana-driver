package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.ProofOfDelivery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ProofOfDeliveryRepository(
    private val api: ProofOfDeliveryApi = ApiClient.create()
) {
    private val errorJson = Json { ignoreUnknownKeys = true }

    suspend fun getById(id: Long): Result<ProofOfDelivery> =
        call { api.getProofOfDelivery(id) }

    suspend fun getByJob(jobId: Long): Result<ProofOfDelivery?> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProofOfDeliveryByJob(jobId)
            if (response.success == false) {
                Result.failure(Exception(describe(response, "Unable to get proof of delivery")))
            } else {
                Result.success(response.data)
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.success(null)
            } else {
                Result.failure(
                    Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}", e)
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getByDriver(driverId: Long): Result<List<ProofOfDelivery>> =
        call { api.listProofsOfDeliveryByDriver(driverId) }.map { it.orEmpty() }

    suspend fun file(
        proof: ProofOfDelivery,
        otp: String? = null
    ): Result<ProofOfDelivery> = call { api.fileProofOfDelivery(proof, otp) }

    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteProofOfDelivery(id)
            if (response.success == false) {
                Result.failure(Exception(describe(response, "Unable to delete proof of delivery")))
            } else {
                Result.success(Unit)
            }
        } catch (e: HttpException) {
            Result.failure(
                Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}", e)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> call(
        request: suspend () -> ApiResponse<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = request()
            val data = response.data
            if (response.success != false && data != null) {
                Result.success(data)
            } else {
                Result.failure(
                    Exception(describe(response, "The server returned no proof of delivery"))
                )
            }
        } catch (e: HttpException) {
            Result.failure(
                Exception(readErrorBody(e) ?: "Server responded with HTTP ${e.code()}", e)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun describe(response: ApiResponse<*>, fallback: String): String {
        val code = response.error?.code?.takeIf { it.isNotBlank() }
        val details = response.error?.details?.takeIf { it.isNotBlank() }
        return listOfNotNull(code, details).joinToString(": ").ifBlank {
            response.message.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    private fun readErrorBody(e: HttpException): String? {
        val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val response = runCatching {
            errorJson.decodeFromString<ApiResponse<Unit>>(raw)
        }.getOrNull() ?: return null
        return describe(response, "").takeIf { it.isNotBlank() }
    }
}
