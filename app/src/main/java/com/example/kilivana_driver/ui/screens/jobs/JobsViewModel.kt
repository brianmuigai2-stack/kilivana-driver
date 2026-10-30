package com.example.kilivana_driver.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.JobStatus
import com.example.kilivana_driver.data.model.JobsErrorType
import com.example.kilivana_driver.data.network.DRIVER_ROLE
import com.example.kilivana_driver.data.network.JobsRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobsUiState(
    val selectedStatus: JobStatus = JobStatus.AVAILABLE,
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorType: JobsErrorType? = null
) {
    val visibleJobs: List<Job>
        get() = jobs.filter { it.status == selectedStatus }

    fun countFor(status: JobStatus): Int = jobs.count { it.status == status }
}

/**
 * ViewModel for the Jobs tab.
 *
 * Loads delivery jobs from the backend via [JobsRepository] once a driver is
 * signed in, following the same watch-session pattern as [ProfileViewModel]:
 * when SessionStore.currentUser changes, jobs are reloaded. This keeps the job
 * list fresh across login/logout without a manual refresh call.
 *
 * The backend returns [DeliveryJob] objects (serializable, API field names);
 * [DeliveryJob.toDomain] maps each one to the UI-friendly [Job] model the
 * composables already consume.
 */
class JobsViewModel(
    private val jobsRepository: JobsRepository = JobsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobsUiState())
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    /** Tracks the user id whose jobs are currently loaded, so a switch to a
     * different driver clears the old list before fetching the new one. */
    private var currentUserId: Long? = null

    init {
        // Created at activity launch, before the driver may have logged in,
        // so watch the session and load once it's populated. When the user
        // signs out — or a different driver signs in — clear the previous
        // driver's jobs so they don't leak across accounts.
        //
        // Inspectors also pass through this collector because the ViewModel
        // is created at activity launch, but their role must not trigger
        // driver-only logistics calls.
        viewModelScope.launch {
            SessionStore.currentUser.collect { user ->
                if (user != null && user.id != currentUserId) {
                    if (!user.role.equals(DRIVER_ROLE, ignoreCase = true)) {
                        _uiState.update {
                            it.copy(
                                jobs = emptyList(),
                                isLoading = false,
                                errorMessage = null,
                                errorType = null
                            )
                        }
                        currentUserId = user.id
                        return@collect
                    }
                    // Different driver: wipe the stale list immediately so
                    // the old jobs are never shown to the new owner.
                    // Note: we do NOT set isLoading=true here — refresh()
                    // reads its own guard and sets it internally.
                    _uiState.update {
                        it.copy(
                            jobs = emptyList(),
                            errorMessage = null
                        )
                    }
                    currentUserId = user.id
                    refresh()
                } else if (user == null) {
                    _uiState.update { it.copy(isLoading = false, jobs = emptyList()) }
                    currentUserId = null
                }
            }
        }
    }

    /** Fetches jobs from the backend and updates the UI state. */
    fun refresh() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val driverId = currentUserId ?: SessionStore.userId
            if (driverId == null) {
                _uiState.update { it.copy(isLoading = false, jobs = emptyList()) }
                return@launch
            }
            val result = jobsRepository.getJobsByDriver(driverId)
            val signedInDriver = SessionStore.currentUser.value
                ?.takeIf { it.role.equals(DRIVER_ROLE, ignoreCase = true) }
            if (signedInDriver?.id != driverId) {
                currentUserId = signedInDriver?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        jobs = emptyList(),
                        errorMessage = null,
                        errorType = null
                    )
                }
                if (signedInDriver != null) refresh()
                return@launch
            }

            if (result.isSuccess) {
                val jobs = result.getOrNull().orEmpty()
                    // Admin assignment is required before a driver can accept or reject a job.
                    .filterNot { it.status.equals("pending_assignment", ignoreCase = true) ||
                        it.status.equals("unassigned", ignoreCase = true) }
                    .map { it.toDomain() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        jobs = jobs,
                        errorMessage = null,
                        errorType = null
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                val errorType = (exception as? JobsRepository.JobsException)?.errorType
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception?.message,
                        errorType = errorType
                    )
                }
            }
        }
    }

    /**
     * Fetches a single delivery job by its backend ID.
     *
     * Used when a job is opened directly (e.g. from a notification deep link)
     * rather than from the jobs list, so the detail screen always shows fresh
     * data instead of a stale list entry.
     */
    suspend fun getJob(id: Long): Result<Job> = runCatching {
        jobsRepository.getJob(id).getOrThrow().toDomain()
    }

    fun onStatusSelected(status: JobStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun acceptJob(jobId: String) {
        val job = _uiState.value.jobs.firstOrNull { it.id == jobId } ?: return
        val driverId = SessionStore.userId ?: return
        val numericId = jobId.toLongOrNull() ?: run {
            // Sample/non-numeric ID: fall back to local-only update
            _uiState.update { state ->
                state.copy(
                    jobs = state.jobs.map { job ->
                        if (job.id == jobId) job.copy(status = JobStatus.ACCEPTED, isNew = false) else job
                    },
                    selectedStatus = JobStatus.ACCEPTED
                )
            }
            return
        }
        viewModelScope.launch {
            val result = jobsRepository.acceptJob(numericId, driverId)
            if (result.isSuccess) {
                val updated = result.getOrNull()!!.toDomain()
                _uiState.update { state ->
                    state.copy(
                        jobs = state.jobs.map { if (it.id == jobId) updated else it },
                        selectedStatus = JobStatus.ACCEPTED
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun declineJob(jobId: String, reason: String = "Driver declined") {
        val numericId = jobId.toLongOrNull() ?: run {
            _uiState.update { state ->
                state.copy(jobs = state.jobs.filter { it.id != jobId })
            }
            return
        }
        viewModelScope.launch {
            val result = jobsRepository.cancelJob(numericId, reason)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(jobs = state.jobs.filter { it.id != jobId })
                }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun completeJob(jobId: String) {
        val numericId = jobId.toLongOrNull() ?: run {
            _uiState.update { state ->
                state.copy(
                    jobs = state.jobs.map { if (it.id == jobId) it.copy(status = JobStatus.COMPLETED) else it },
                    selectedStatus = JobStatus.COMPLETED
                )
            }
            return
        }
        viewModelScope.launch {
            val result = jobsRepository.updateJobStatus(numericId, "delivered")
            if (result.isSuccess) {
                val updated = result.getOrNull()!!.toDomain()
                _uiState.update { state ->
                    state.copy(
                        jobs = state.jobs.map { if (it.id == jobId) updated else it },
                        selectedStatus = JobStatus.COMPLETED
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }
}

// TODO: deliveryOtp is a made-up sample here. In production this code is
// generated by the backend and sent to the BUYER by SMS once the driver
// marks the order as picked up — never shown to the driver in the UI.
internal fun sampleJobs(): List<Job> = listOf(
    Job(
        id = "AG-4589",
        pickupLocation = "Thika, Kiambu", pickupPlace = "Greenfield Farm", pickupTime = "Today 10:00 AM",
        pickupLat = -1.0332, pickupLon = 37.0693,
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Wakulima Market", dropoffTime = "Today 12:30 PM",
        dropoffLat = -1.2921, dropoffLon = 36.8219,
        cargo = "Vegetables (Mixed)", quantity = "1,000 kg",
        distanceKm = 45.0, estimatedTime = "1h 20m", payoutKsh = 2500,
        customer = "Greenfield Farm Ltd", deliveryOtp = "4821",
        status = JobStatus.AVAILABLE, isNew = true
    ),
    Job(
        id = "AG-4590",
        pickupLocation = "Mwea, Kirinyaga", pickupPlace = "Mwea Dairy Co-op", pickupTime = "Today 1:00 PM",
        pickupLat = -0.6839, pickupLon = 37.3556,
        dropoffLocation = "Nakuru, Nakuru", dropoffPlace = "Nakuru Dairy Hub", dropoffTime = "Today 4:40 PM",
        dropoffLat = -0.3031, dropoffLon = 36.0800,
        cargo = "Fresh Milk", quantity = "500 L",
        distanceKm = 205.0, estimatedTime = "3h 40m", payoutKsh = 3200,
        customer = "Mwea Dairy Co-op", deliveryOtp = "7734",
        status = JobStatus.AVAILABLE, isNew = true
    ),
    Job(
        id = "AG-4591",
        pickupLocation = "Machakos, Machakos", pickupPlace = "Machakos Grain Store", pickupTime = "Tomorrow 6:00 AM",
        pickupLat = -1.5177, pickupLon = 37.2634,
        dropoffLocation = "Kisumu, Kisumu", dropoffPlace = "Kisumu Grain Depot", dropoffTime = "Tomorrow 1:45 PM",
        dropoffLat = -0.0917, dropoffLon = 34.7680,
        cargo = "Dry Maize", quantity = "2,000 kg",
        distanceKm = 390.0, estimatedTime = "7h 45m", payoutKsh = 8500,
        customer = "Machakos Growers Ltd", deliveryOtp = "1029",
        status = JobStatus.AVAILABLE
    ),
    Job(
        id = "AG-4581",
        pickupLocation = "Kiambu, Kiambu", pickupPlace = "Kiambu Fresh Farm", pickupTime = "Yesterday 9:00 AM",
        pickupLat = -1.1714, pickupLon = 36.8356,
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "City Market", dropoffTime = "Yesterday 10:00 AM",
        dropoffLat = -1.2921, dropoffLon = 36.8219,
        cargo = "Tomatoes", quantity = "800 kg",
        distanceKm = 32.0, estimatedTime = "55m", payoutKsh = 1900,
        customer = "Kiambu Fresh Farms", deliveryOtp = "3390",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4577",
        pickupLocation = "Nyeri, Nyeri", pickupPlace = "Highlands Farm", pickupTime = "Mon 7:30 AM",
        pickupLat = -0.4201, pickupLon = 36.9476,
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Wakulima Market", dropoffTime = "Mon 11:00 AM",
        dropoffLat = -1.2921, dropoffLon = 36.8219,
        cargo = "Potatoes", quantity = "1,500 kg",
        distanceKm = 155.0, estimatedTime = "3h 10m", payoutKsh = 4200,
        customer = "Highlands Growers", deliveryOtp = "5567",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4572",
        pickupLocation = "Naivasha, Nakuru", pickupPlace = "Naivasha Blooms", pickupTime = "Sun 5:00 AM",
        pickupLat = -0.7167, pickupLon = 36.4333,
        dropoffLocation = "Mombasa, Mombasa", dropoffPlace = "Mombasa Cargo Terminal", dropoffTime = "Sun 3:30 PM",
        dropoffLat = -4.0435, dropoffLon = 39.6682,
        cargo = "Cut Flowers", quantity = "300 kg",
        distanceKm = 480.0, estimatedTime = "9h 30m", payoutKsh = 9800,
        customer = "Naivasha Blooms Ltd", deliveryOtp = "8843",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4566",
        pickupLocation = "Embu, Embu", pickupPlace = "Embu Farm Collective", pickupTime = "Sat 8:00 AM",
        pickupLat = -0.5310, pickupLon = 37.4500,
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Marikiti Market", dropoffTime = "Sat 11:15 AM",
        dropoffLat = -1.2921, dropoffLon = 36.8219,
        cargo = "Bananas", quantity = "1,200 kg",
        distanceKm = 125.0, estimatedTime = "3h 15m", payoutKsh = 3600,
        customer = "Embu Farmers Co-op", deliveryOtp = "2214",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4561",
        pickupLocation = "Limuru, Kiambu", pickupPlace = "Limuru Dairy", pickupTime = "Fri 6:30 AM",
        pickupLat = -1.1122, pickupLon = 36.6438,
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Nairobi Milk Depot", dropoffTime = "Fri 7:45 AM",
        dropoffLat = -1.2921, dropoffLon = 36.8219,
        cargo = "Fresh Milk", quantity = "400 L",
        distanceKm = 28.0, estimatedTime = "1h 15m", payoutKsh = 1500,
        customer = "Limuru Dairy Ltd", deliveryOtp = "9081",
        status = JobStatus.COMPLETED
    )
)
