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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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

    fun masuk() {
        if (isLoading) return
        focusManager.clearFocus()
        onLoginClick()
    }

    Scaffold(modifier, containerColor = PageBackground) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState())
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(GradienUtama)
                        .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(76.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, Modifier.size(42.dp), tint = UnsBlue)
                    }
                    Text("FATISDA Here", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Absensi kelas berbasis lokasi Wi-Fi",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                Column(Modifier.padding(horizontal = 20.dp).offset(y = (-36).dp)) {
                    SectionCard("Masuk") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FieldLabel("Email")
                            OutlinedTextField(
                                value = email,
                                onValueChange = onEmailChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("nama@student.uns.ac.id") },
                                singleLine = true,
                                shape = BentukField,
                                colors = warnaField(),
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
                                shape = BentukField,
                                colors = warnaField(),
                                visualTransformation = if (tampilSandi) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { tampilSandi = !tampilSandi }) {
                                        Icon(
                                            if (tampilSandi) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (tampilSandi) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                                            tint = TextMuted
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { masuk() })
                            )
                        }
                        if (errorMessage != null) ErrorText(errorMessage)
                        PrimaryButton(if (isLoading) "Memproses..." else "Masuk", { masuk() }, enabled = !isLoading)
                        Text(
                            "Akun dibuat oleh admin. Hubungi admin jika belum memiliki akun.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                        if (infoMessage != null) HelperText(infoMessage, color = UnsBlue)
                    }
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
