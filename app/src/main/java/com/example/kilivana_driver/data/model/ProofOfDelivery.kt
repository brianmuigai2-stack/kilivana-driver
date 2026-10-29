package com.example.kilivana_driver.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProofOfDelivery(
    val id: Long = 0L,
    val logisticsJobId: Long = 0L,
    val recipientName: String? = null,
    val signatureUrl: String? = null,
    val photoUrl: String? = null,
    val deliveredAt: String? = null
)
