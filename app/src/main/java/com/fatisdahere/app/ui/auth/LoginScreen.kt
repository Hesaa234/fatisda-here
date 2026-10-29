package com.fatisdahere.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*
import com.fatisdahere.app.ui.components.*

@Composable
fun LoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    infoMessage: String? = null
) {
    Scaffold(modifier, topBar = { FatisdaTopBar() }, containerColor = PageBackground) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SectionCard("Masuk") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("nama@student.uns.ac.id") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Kata Sandi")
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                if (errorMessage != null) ErrorText(errorMessage)
                // Tanpa animasi berputar: lihat catatan di LoadingContent (Components.kt)
                PrimaryButton(if (isLoading) "Memproses..." else "Masuk", onLoginClick, enabled = !isLoading)
                Text(
                    "Akun dibuat oleh admin. Hubungi admin jika belum memiliki akun.",
                    fontSize = 13.sp,
                    color = TextPrimary.copy(alpha = 0.65f)
                )
                if (infoMessage != null) HelperText(infoMessage, color = UnsBlue)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    FatisdaTheme { LoginScreen("", "", {}, {}, {}, infoMessage = "Mode demo") }
}
