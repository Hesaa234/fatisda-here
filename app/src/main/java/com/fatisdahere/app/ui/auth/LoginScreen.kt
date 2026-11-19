package com.fatisdahere.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

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
    var tampilSandi by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    /** Enter di keyboard (kolom sandi) atau tombol Masuk. */
    fun masuk() {
        if (isLoading) return
        focusManager.clearFocus()
        onLoginClick()
    }

    Scaffold(modifier, topBar = { FatisdaTopBar() }, containerColor = PageBackground) { padding ->
        // Scroll + imePadding: tombol "Masuk" tetap terjangkau saat keyboard muncul.
        // heightIn(min = maxHeight) menjaga form tetap di tengah saat keyboard tertutup.
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // ---------- Identitas aplikasi ----------
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(72.dp).background(UnsBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, Modifier.size(40.dp), tint = UnsBlue)
                    }
                    Text("FATISDA Here", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                    Text(
                        "Absensi kelas berbasis lokasi Wi-Fi",
                        fontSize = 14.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
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
                            visualTransformation = if (tampilSandi) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { tampilSandi = !tampilSandi }) {
                                    Icon(
                                        if (tampilSandi) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (tampilSandi) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                                        tint = TextPrimary.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { masuk() })
                        )
                    }
                    if (errorMessage != null) ErrorText(errorMessage)
                    // Tanpa animasi berputar: lihat catatan di LoadingContent (Components.kt)
                    PrimaryButton(if (isLoading) "Memproses..." else "Masuk", { masuk() }, enabled = !isLoading)
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
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    FatisdaTheme { LoginScreen("", "", {}, {}, {}, infoMessage = "Mode demo") }
}
