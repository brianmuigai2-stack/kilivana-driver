package com.example.kilivana_driver.ui.screens.jobs

import androidx.lifecycle.ViewModel
import com.example.kilivana_driver.data.model.Job
import com.example.kilivana_driver.data.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JobsUiState(
    val selectedStatus: JobStatus = JobStatus.AVAILABLE,
    val jobs: List<Job> = emptyList()
) {
    val visibleJobs: List<Job>
        get() = jobs.filter { it.status == selectedStatus }

    fun countFor(status: JobStatus): Int = jobs.count { it.status == status }
}

class JobsViewModel : ViewModel() {

    // TODO: replace sample data with jobs from the API
    private val _uiState = MutableStateFlow(JobsUiState(jobs = sampleJobs()))
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    fun onStatusSelected(status: JobStatus) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun acceptJob(jobId: String) {
        // TODO: call the API to accept the job, then update on success
        _uiState.update { state ->
            state.copy(
                jobs = state.jobs.map { job ->
                    if (job.id == jobId) job.copy(status = JobStatus.ACCEPTED, isNew = false) else job
                },
                selectedStatus = JobStatus.ACCEPTED
            )
        }
    }
}

internal fun sampleJobs(): List<Job> = listOf(
    Job(
        id = "AG-4589",
        pickupLocation = "Thika, Kiambu", pickupPlace = "Greenfield Farm", pickupTime = "Today 10:00 AM",
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Wakulima Market", dropoffTime = "Today 12:30 PM",
        cargo = "Vegetables (Mixed)", quantity = "1,000 kg",
        distanceKm = 45, estimatedTime = "1h 20m", payoutKsh = 2500,
        customer = "Greenfield Farm Ltd",
        status = JobStatus.AVAILABLE, isNew = true
    ),
    Job(
        id = "AG-4590",
        pickupLocation = "Mwea, Kirinyaga", pickupPlace = "Mwea Dairy Co-op", pickupTime = "Today 1:00 PM",
        dropoffLocation = "Nakuru, Nakuru", dropoffPlace = "Nakuru Dairy Hub", dropoffTime = "Today 4:40 PM",
        cargo = "Fresh Milk", quantity = "500 L",
        distanceKm = 205, estimatedTime = "3h 40m", payoutKsh = 3200,
        customer = "Mwea Dairy Co-op",
        status = JobStatus.AVAILABLE, isNew = true
    ),
    Job(
        id = "AG-4591",
        pickupLocation = "Machakos, Machakos", pickupPlace = "Machakos Grain Store", pickupTime = "Tomorrow 6:00 AM",
        dropoffLocation = "Kisumu, Kisumu", dropoffPlace = "Kisumu Grain Depot", dropoffTime = "Tomorrow 1:45 PM",
        cargo = "Dry Maize", quantity = "2,000 kg",
        distanceKm = 390, estimatedTime = "7h 45m", payoutKsh = 8500,
        customer = "Machakos Growers Ltd",
        status = JobStatus.AVAILABLE
    ),
    Job(
        id = "AG-4581",
        pickupLocation = "Kiambu, Kiambu", pickupPlace = "Kiambu Fresh Farm", pickupTime = "Yesterday 9:00 AM",
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "City Market", dropoffTime = "Yesterday 10:00 AM",
        cargo = "Tomatoes", quantity = "800 kg",
        distanceKm = 32, estimatedTime = "55m", payoutKsh = 1900,
        customer = "Kiambu Fresh Farms",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4577",
        pickupLocation = "Nyeri, Nyeri", pickupPlace = "Highlands Farm", pickupTime = "Mon 7:30 AM",
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Wakulima Market", dropoffTime = "Mon 11:00 AM",
        cargo = "Potatoes", quantity = "1,500 kg",
        distanceKm = 155, estimatedTime = "3h 10m", payoutKsh = 4200,
        customer = "Highlands Growers",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4572",
        pickupLocation = "Naivasha, Nakuru", pickupPlace = "Naivasha Blooms", pickupTime = "Sun 5:00 AM",
        dropoffLocation = "Mombasa, Mombasa", dropoffPlace = "Mombasa Cargo Terminal", dropoffTime = "Sun 3:30 PM",
        cargo = "Cut Flowers", quantity = "300 kg",
        distanceKm = 480, estimatedTime = "9h 30m", payoutKsh = 9800,
        customer = "Naivasha Blooms Ltd",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4566",
        pickupLocation = "Embu, Embu", pickupPlace = "Embu Farm Collective", pickupTime = "Sat 8:00 AM",
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Marikiti Market", dropoffTime = "Sat 11:15 AM",
        cargo = "Bananas", quantity = "1,200 kg",
        distanceKm = 125, estimatedTime = "3h 15m", payoutKsh = 3600,
        customer = "Embu Farmers Co-op",
        status = JobStatus.COMPLETED
    ),
    Job(
        id = "AG-4561",
        pickupLocation = "Limuru, Kiambu", pickupPlace = "Limuru Dairy", pickupTime = "Fri 6:30 AM",
        dropoffLocation = "Nairobi, Nairobi", dropoffPlace = "Nairobi Milk Depot", dropoffTime = "Fri 7:45 AM",
        cargo = "Fresh Milk", quantity = "400 L",
        distanceKm = 28, estimatedTime = "1h 15m", payoutKsh = 1500,
        customer = "Limuru Dairy Ltd",
        status = JobStatus.COMPLETED
    )
)
