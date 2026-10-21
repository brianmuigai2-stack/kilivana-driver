package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

@Serializable
data class InspectionDto(
    val id: Long? = null,
    val inspectorId: Long? = null,
    val targetType: String? = null,
    val targetId: Long? = null,
    val status: String? = null,
    val result: String? = null,
    val notes: String? = null,
    val evidenceUrls: String? = null,
    val inspectedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class InspectionFindingRequest(
    val inspectorId: Long,
    val targetType: String,
    val targetId: Long,
    val status: String,
    val result: String,
    val notes: String
)

enum class InspectionStatus(val label: String) {
    ASSIGNED("Assigned"),
    PENDING("Pending"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun from(raw: String?): InspectionStatus? =
            entries.firstOrNull { it.name == raw }
    }
}

enum class InspectionFinding(val label: String) {
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CHANGES_REQUIRED("Changes required")
}
