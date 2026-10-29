package com.fatisdahere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.di.AppContainer
import com.fatisdahere.app.ui.admin.AdminApp
import com.fatisdahere.app.ui.auth.LoginScreen
import com.fatisdahere.app.ui.debug.DebugPositioningScreen
import com.fatisdahere.app.ui.dosen.DosenApp
import com.fatisdahere.app.ui.mahasiswa.MahasiswaApp
import com.fatisdahere.app.ui.theme.FatisdaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FatisdaTheme {
                // true = buka layar debug positioning (X-01), melewati login
                val modeDebug = false

                if (modeDebug) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        DebugPositioningScreen(
                            fingerprint = container.fingerprint,
                            engine = container.positioningEngine,
                            params = container.positioningParams,
                            scanner = container.wifiScanner,
                        )
                    }
                } else {
                    FatisdaApp(container)
                }
            }
        }
    }
}

/** Login, lalu arahkan ke halaman sesuai role akun. */
@Composable
private fun FatisdaApp(container: AppContainer) {
    var user by remember { mutableStateOf<AppUser?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val logout: () -> Unit = {
        scope.launch { container.authRepository.signOut() }
        user = null
        password = ""
    }

    val u = user
    if (u == null) {
        LoginScreen(
            email = email,
            password = password,
            onEmailChange = {
                email = it
                error = null
            },
            onPasswordChange = {
                password = it
                error = null
            },
            onLoginClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    container.authRepository.signIn(email.trim(), password)
                        .onSuccess { user = it }
                        .onFailure { error = it.message ?: "Gagal masuk." }
                    isLoading = false
                }
            },
            isLoading = isLoading,
            errorMessage = error,
            infoMessage = if (AppContainer.PAKAI_FIREBASE) null
            else "Mode demo: masuk dengan mahasiswa1, dosen1, atau admin (kata sandi bebas)."
        )
    } else {
        when (u.role) {
            Role.MAHASISWA -> MahasiswaApp(u, container, logout)
            Role.DOSEN -> DosenApp(u, container, logout)
            Role.ADMIN -> AdminApp(u, container, logout)
        }
    }
}
