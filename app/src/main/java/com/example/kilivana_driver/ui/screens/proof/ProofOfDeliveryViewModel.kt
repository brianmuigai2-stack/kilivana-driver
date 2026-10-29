package com.example.kilivana_driver.ui.screens.proof

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.ProofOfDelivery
import com.example.kilivana_driver.data.network.ProofOfDeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProofUiState(
    val recipientName: String = "",
    val photoUrl: String = "",
    val signatureUrl: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

class ProofOfDeliveryViewModel(
    private val repo: ProofOfDeliveryRepository = ProofOfDeliveryRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ProofUiState())
    val state: StateFlow<ProofUiState> = _state.asStateFlow()

    fun updateRecipient(v: String) { _state.value = _state.value.copy(recipientName = v) }
    fun updatePhoto(v: String) { _state.value = _state.value.copy(photoUrl = v) }
    fun updateSignature(v: String) { _state.value = _state.value.copy(signatureUrl = v) }
    fun updateOtp(v: String) { _state.value = _state.value.copy(otp = v) }

    fun submit(jobId: Long) {
        val s = _state.value
        if (s.otp.isBlank()) {
            _state.value = s.copy(message = "Enter the buyer's delivery code.")
            return
        }
        if (s.photoUrl.isBlank() && s.signatureUrl.isBlank()) {
            _state.value = s.copy(message = "Add a photo or signature as delivery evidence.")
            return
        }
        _state.value = s.copy(isLoading = true, message = null)
        viewModelScope.launch {
            val proof = ProofOfDelivery(logisticsJobId = jobId, recipientName = s.recipientName, photoUrl = s.photoUrl, signatureUrl = s.signatureUrl)
            val res = repo.file(proof, s.otp.trim())
            _state.value = _state.value.copy(isLoading = false, message = res.fold({ "Filed" }, { it.message }))
        }
    }
}
