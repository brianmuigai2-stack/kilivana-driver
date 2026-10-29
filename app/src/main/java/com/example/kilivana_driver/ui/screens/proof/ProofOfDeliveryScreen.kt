package com.example.kilivana_driver.ui.screens.proof

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProofOfDeliveryScreen(jobId: Long, vm: ProofOfDeliveryViewModel = ProofOfDeliveryViewModel()) {
    val s by vm.state.collectAsState()
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = s.recipientName, onValueChange = vm::updateRecipient, label = { Text("Recipient") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = s.photoUrl, onValueChange = vm::updatePhoto, label = { Text("Photo URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = s.signatureUrl, onValueChange = vm::updateSignature, label = { Text("Signature URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = s.otp, onValueChange = vm::updateOtp, label = { Text("OTP") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.submit(jobId) }, modifier = Modifier.fillMaxWidth()) { Text("File Proof") }
        s.message?.let { Text(it) }
    }
}
