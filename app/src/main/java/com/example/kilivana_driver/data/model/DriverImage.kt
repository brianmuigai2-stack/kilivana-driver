package com.example.kilivana_driver.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The envelope every Kilivana endpoint wraps its payload in.
 *
 * [data] is the endpoint-specific payload, so this stays generic and is used
 * as `ApiResponse<List<DriverImage>>` and friends. The `error` object is only
 * populated on failures, so both of its fields are nullable and default to null.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null,
    val timestamp: String = "",
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val code: String = "",
    val details: String = ""
)

/**
 * One uploaded image from GET /api/v1/profiles/drivers/{userId}/images.
 *
 * The backend hands back both `url` (what you display) and the storage
 * `publicId`/`assetId` pair, so all three are kept even though the UI only
 * needs [url] today.
 *
 * [assetId] is nullable because the backend genuinely returns `null` for it —
 * files are served from its own /uploads path, so there is no external asset
 * identifier to report. It has to be declared nullable rather than given a
 * default: kotlinx.serialization only applies a default when a key is *absent*,
 * and throws on an explicit `null` for a non-null field.
 */
@Serializable
data class DriverImage(
    val id: Long = 0L,
    val url: String = "",
    val publicId: String = "",
    val assetId: String? = null,
    val sortOrder: Int = 0,
    val isPrimary: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)
