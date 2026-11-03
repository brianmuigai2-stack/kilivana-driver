package com.example.kilivana_driver

import com.example.kilivana_driver.data.model.ApiResponse
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.data.model.LocationUpdate
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.model.ReportLocationResponseData
import com.example.kilivana_driver.data.model.TripLocationRequest
import com.example.kilivana_driver.data.model.TripStage
import com.example.kilivana_driver.data.model.TripStatus
import com.example.kilivana_driver.data.network.LocationRepository
import com.example.kilivana_driver.data.network.TripApi
import com.example.kilivana_driver.data.network.TripRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryLifecycleTest {

    @Test
    fun deliveryJobMapsServerAddressesCoordinatesAndStatus() {
        val job = DeliveryJob(
            id = 42,
            pickupAddress = "Farm, Thika",
            destinationAddress = "Buyer, Nairobi",
            pickupLatitude = -1.02,
            pickupLongitude = 37.08,
            destinationLatitude = -1.29,
            destinationLongitude = 36.82,
            status = "in_transit",
            tripId = "trip-42"
        ).toDomain()

        assertEquals("42", job.id)
        assertEquals("Farm, Thika", job.pickupLocation)
        assertEquals("Buyer, Nairobi", job.dropoffLocation)
        assertEquals(-1.02, job.pickupLat, 0.0)
        assertEquals(36.82, job.dropoffLon, 0.0)
        assertEquals(JobStatus.ACCEPTED, job.status)
        assertEquals("trip-42", job.tripId)
        assertEquals(
            JobStatus.UNKNOWN,
            DeliveryJob(id = 43, status = "new_backend_status").toDomain().status
        )
    }

    @Test
    fun mapsKnownServerStatusesWithoutTreatingUnknownAsAnActiveStage() {
        assertEquals(TripStage.HEADING_TO_PICKUP, TripRepository.tripStageForStatus("accepted"))
        assertEquals(TripStage.IN_TRANSIT, TripRepository.tripStageForStatus("picked_up"))
        assertEquals(TripStage.DELIVERED, TripRepository.tripStageForStatus("delivered"))
        assertNull(TripRepository.tripStageForStatus("backend_added_new_state"))
    }

    @Test
    fun gpsUploadRequiresMatchingActiveTripAndValidCoordinates() {
        val fix = LocationUpdate(
            tripId = "trip-8",
            latitude = -1.2,
            longitude = 36.8
        )

        assertTrue(fix.isEligibleForUpload("trip-8", true))
        assertFalse(fix.isEligibleForUpload("job-8", true))
        assertFalse(fix.isEligibleForUpload("trip-8", false))
        assertFalse(
            fix.copy(latitude = 91.0).isEligibleForUpload("trip-8", true)
        )
    }

    @Test
    fun failedStartDoesNotEnableLocationTracking() = runBlocking {
        val api = FakeTripApi().apply {
            startResponse = ApiResponse(success = false, message = "Trip cannot start")
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()

        assertFalse(repository.startTracking("trip-8"))
        assertFalse(repository.trackingActive.value)
        assertNull(repository.activeTripId.value)
        assertEquals("Trip cannot start", repository.lastError.value)
    }

    @Test
    fun acceptJobFailsIfTripIdIsBlank() = runBlocking {
        val api = FakeTripApi().apply {
            acceptResponse = ApiResponse(success = true, data = trip("accepted").copy(tripId = ""))
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()

        val result = repository.acceptJob(42L)

        assertTrue(result.isFailure)
        assertNull(repository.activeTripId.value)
        assertFalse(repository.trackingActive.value)
    }

    @Test
    fun failedPickupDoesNotAdvanceTheLocalTripStage() = runBlocking {
        val api = FakeTripApi().apply {
            updateStatusResponse = ApiResponse(success = false, message = "Pickup is not allowed")
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()
        assertTrue(repository.startTracking("trip-8"))

        val result = repository.confirmPickup("trip-8")

        assertTrue(result.isFailure)
        assertEquals(TripStage.HEADING_TO_PICKUP, repository.tripStage.value)
    }

    @Test
    fun failedProofDoesNotMarkTripCompleteOrClearActiveTrip() = runBlocking {
        val api = FakeTripApi().apply {
            proofResponse = ApiResponse(success = false, message = "OTP is invalid")
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()
        assertTrue(repository.startTracking("trip-8"))

        val result = repository.fileProof(
            "trip-8",
            ProofOfDelivery(logisticsJobId = 42, photoUrl = "https://evidence.test/image.jpg"),
            "000000"
        )

        assertTrue(result.isFailure)
        assertEquals("trip-8", repository.activeTripId.value)
        assertEquals(TripStage.HEADING_TO_PICKUP, repository.tripStage.value)
        assertTrue(repository.trackingActive.value)
    }

    @Test
    fun proofIsNotReportedCompleteUntilTripStatusIsTerminal() = runBlocking {
        val api = FakeTripApi().apply {
            proofResponse = ApiResponse(
                success = true,
                data = ProofOfDelivery(logisticsJobId = 42)
            )
            tripResponse = ApiResponse(success = true, data = trip("in_transit"))
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()
        assertTrue(repository.startTracking("trip-8"))

        val result = repository.fileProof(
            "trip-8",
            ProofOfDelivery(logisticsJobId = 42, photoUrl = "https://evidence.test/image.jpg"),
            "123456"
        )

        assertTrue(result.isFailure)
        assertEquals("trip-8", repository.activeTripId.value)
        assertTrue(repository.trackingActive.value)
    }

    @Test
    fun successfulProofRequiresServerConfirmedTerminalStatus() = runBlocking {
        val api = FakeTripApi().apply {
            tripResponse = ApiResponse(success = true, data = trip("delivered"))
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()
        assertTrue(repository.startTracking("trip-8"))

        val result = repository.fileProof(
            "trip-8",
            ProofOfDelivery(logisticsJobId = 42, photoUrl = "https://evidence.test/image.jpg"),
            "123456"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun activeTripRecoveryConfirmsServerTrackingBeforeEnablingService() = runBlocking {
        val api = FakeTripApi().apply {
            myTripsResponse = ApiResponse(
                success = true,
                data = listOf(trip(status = "in_transit"))
            )
        }
        val repository = TripRepository(api, LocationRepository())
        repository.clear()

        val restored = repository.checkForActiveTrip()

        assertEquals("trip-8", restored?.tripId)
        assertEquals("trip-8", repository.activeTripId.value)
        assertEquals(TripStage.IN_TRANSIT, repository.tripStage.value)
        assertTrue(repository.trackingActive.value)
    }

    @Test
    fun verifyOtpReturnsSuccessOnValidCode() = runBlocking {
        val api = FakeTripApi().apply {
            otpResponse = ApiResponse(success = true, data = trip("in_transit"))
        }
        val repository = TripRepository(api, LocationRepository())

        val result = repository.verifyOtp("trip-8", "123456")

        assertTrue(result.isSuccess)
        assertEquals("trip-8", result.getOrNull()?.tripId)
    }

    private class FakeTripApi : TripApi {
        var startResponse: ApiResponse<Unit> = ApiResponse(success = true)
        var acceptResponse: ApiResponse<TripStatus> = ApiResponse(success = true, data = trip("accepted"))
        var updateStatusResponse: ApiResponse<TripStatus> =
            ApiResponse(success = true, data = trip("picked_up"))
        var proofResponse: ApiResponse<ProofOfDelivery> =
            ApiResponse(success = true, data = ProofOfDelivery(logisticsJobId = 42))
        var tripResponse: ApiResponse<TripStatus> =
            ApiResponse(success = true, data = trip("accepted"))
        var myTripsResponse: ApiResponse<List<TripStatus>> =
            ApiResponse(success = true, data = emptyList())
        var otpResponse: ApiResponse<TripStatus> = ApiResponse(success = true, data = trip("accepted"))

        override suspend fun getMyTrips() = myTripsResponse
        override suspend fun acceptJob(jobId: Long) = acceptResponse
        override suspend fun getTrip(tripId: String) = tripResponse
        override suspend fun updateStatus(tripId: String, status: String) = updateStatusResponse
        override suspend fun reportLocation(tripId: String, body: TripLocationRequest) =
            ApiResponse(success = true, data = ReportLocationResponseData(tripId = 8))
        override suspend fun verifyOtp(tripId: String, otp: String) = otpResponse
        override suspend fun fileProof(tripId: String, otp: String, body: ProofOfDelivery) =
            proofResponse
        override suspend fun getActiveTrips(): ApiResponse<List<DeliveryJob>> =
            ApiResponse(success = true, data = emptyList())
        override suspend fun getCurrentTrip() =
            ApiResponse<DeliveryJob>(success = true, data = null)
        override suspend fun startTracking(tripId: String) = startResponse
    }

    companion object {
        private fun trip(status: String) = TripStatus(
            tripId = "trip-8",
            status = status,
            driverId = 3,
            updatedAt = "2026-11-02T00:00:00Z"
        )
    }
}
