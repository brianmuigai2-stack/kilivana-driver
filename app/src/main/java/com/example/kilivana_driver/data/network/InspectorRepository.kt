package com.example.kilivana_driver.data.network

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.InspectionDto
import com.example.kilivana_driver.data.model.InspectionFinding
import com.example.kilivana_driver.data.model.InspectionFindingRequest
import com.example.kilivana_driver.data.model.InspectionStatus
import retrofit2.Response

class InspectorRepository(
    private val api: InspectorApi = ApiClient.create()
) {

    private fun requireInspectorId(): Long {
        val user = SessionStore.currentUser.value
            ?: throw IllegalStateException("Your session has expired. Please sign in again.")

        if (!user.role.equals("INSPECTOR", ignoreCase = true)) {
            throw IllegalStateException("This screen is only available to inspector accounts.")
        }

        return user.id.takeIf { it > 0L }
            ?: throw IllegalStateException("The inspector account has an invalid user ID.")
    }

    suspend fun getAssignments(): List<InspectionDto> {
        val inspectorId = requireInspectorId()
        val items = read(api.inspections(inspectorId))

        // Do not display records returned for another inspector.
        if (items.any { it.inspectorId != inspectorId || it.id == null || it.id <= 0L }) {
            throw IllegalStateException(
                "The server returned an inspection that could not be verified."
            )
        }

        if (items.mapNotNull { it.id }.distinct().size != items.size) {
            throw IllegalStateException("The server returned duplicate inspection records.")
        }

        return items.sortedByDescending { it.createdAt.orEmpty() }
    }

    suspend fun getInspection(id: Long): InspectionDto {
        requireInspectorId()
        require(id > 0L) { "Invalid inspection selected." }

        val inspectorId = requireInspectorId()
        val item = read(api.inspection(id))

        if (item.id != id || item.inspectorId != inspectorId) {
            throw IllegalStateException(
                "This inspection could not be verified for your account."
            )
        }

        return item
    }

    suspend fun updateStatus(
        id: Long,
        status: InspectionStatus
    ): InspectionDto {
        val current = getInspection(id)
        if (current.status == status.name) {
            throw IllegalStateException("That status is already recorded.")
        }

        val updated = read(api.updateStatus(id, status.name))
        if (updated.id != id || updated.inspectorId != current.inspectorId) {
            throw IllegalStateException("The updated inspection could not be verified.")
        }
        return updated
    }

    suspend fun submitFinding(
        id: Long,
        finding: InspectionFinding,
        notes: String
    ): InspectionDto {
        val cleanNotes = notes.trim()
        require(cleanNotes.isNotEmpty()) {
            "Enter inspection findings before submitting."
        }
        require(cleanNotes.length <= 4000) {
            "Findings must be no longer than 4000 characters."
        }

        val current = getInspection(id)
        val inspectorId = requireInspectorId()

        val request = InspectionFindingRequest(
            inspectorId = inspectorId,
            targetType = current.targetType
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The inspection has no target type."),
            targetId = current.targetId
                ?.takeIf { it > 0L }
                ?: throw IllegalStateException("The inspection has an invalid target ID."),
            status = current.status
                ?.takeIf { InspectionStatus.from(it) != null }
                ?: throw IllegalStateException("The inspection has an unsupported status."),
            result = finding.name,
            notes = cleanNotes
        )

        val updated = read(api.updateResult(id, finding.name, request))
        if (updated.id != id || updated.inspectorId != inspectorId) {
            throw IllegalStateException("The submitted findings could not be verified.")
        }
        return updated
    }

    private fun <T> read(response: Response<ApiResponse<T>>): T {
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw IllegalStateException(
                when (response.code()) {
                    400, 422 -> "The server rejected the submitted values. Check them and try again."
                    401 -> "Your session has expired. Please sign in again."
                    403 -> "You are not authorised to access this inspection."
                    404 -> "This inspection was not found. Refresh your assignments."
                    409 -> "This inspection changed on the server. Refresh and try again."
                    429 -> "Too many requests. Please wait before trying again."
                    else -> "The inspection service is unavailable. Please try again."
                }
            )
        }

        val envelope = response.body()
            ?: throw IllegalStateException("The server returned an empty response.")

        if (envelope.success != true) {
            throw IllegalStateException(
                envelope.message.takeIf { it.isNotBlank() }
                    ?: "The server could not complete the request."
            )
        }

        return envelope.data
            ?: throw IllegalStateException("The server response did not contain inspection data.")
    }
}
