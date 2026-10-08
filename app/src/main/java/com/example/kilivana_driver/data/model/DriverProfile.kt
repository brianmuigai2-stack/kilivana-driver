package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

/**
 * The driver profile from GET/POST/PUT /api/v1/profiles/drivers/{userId}.
 *
 * This holds the licence and vehicle details. It is a separate resource from
 * the user ([AuthUser]) — the user is who you are, this is the driver record
 * describing the vehicle. [images] is nullable because the backend sends
 * `"images": null` on a freshly created profile, and an upload returns the
 * profile's images only on the image endpoints.
 */
@Serializable
data class DriverProfile(
    val id: Long = 0L,
    val userId: Long = 0L,
    val licenseNumber: String = "",
    val vehicleType: String = "",
    val vehicleNumber: String = "",
    val vehicleDetails: String = "",
    val availabilityStatus: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val images: List<DriverImage>? = null
) {
    /**
     * The avatar to show, taken from the profile itself so the profile screen
     * does not need a second request just to render a header photo.
     */
    val primaryImage: DriverImage?
        get() = images?.firstOrNull { it.isPrimary } ?: images?.minByOrNull { it.sortOrder }
}

/**
 * Body for creating or updating a driver profile.
 *
 * All fields are nullable with no defaults supplied: a PUT here is a partial
 * update, so sending an empty string for a field the driver did not touch
 * would silently wipe it. Null means "leave unchanged", so the repository
 * drops unset fields before sending.
 */
@Serializable
data class DriverProfileRequest(
    val licenseNumber: String? = null,
    val vehicleType: String? = null,
    val vehicleNumber: String? = null,
    val vehicleDetails: String? = null,
    val availabilityStatus: String? = null
)
