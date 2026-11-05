package com.fatisdahere.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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

/** AppUser bukan Parcelable, jadi disimpan sebagai daftar string agar selamat dari recreate Activity. */
private val AppUserSaver = listSaver<AppUser?, String>(
    save = { u ->
        if (u == null) emptyList()
        else listOf(u.uid, u.name, u.email, u.role.name, u.nim.orEmpty(), u.nip.orEmpty())
    },
    restore = { l ->
        if (l.isEmpty()) null
        else AppUser(
            uid = l[0], name = l[1], email = l[2], role = Role.valueOf(l[3]),
            nim = l[4].ifEmpty { null }, nip = l[5].ifEmpty { null }
        )
    }
)

/** Login, lalu arahkan ke halaman sesuai role akun. */
@Composable
private fun FatisdaApp(container: AppContainer) {
    var user by rememberSaveable(stateSaver = AppUserSaver) { mutableStateOf<AppUser?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
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

                    // Normalisasi input: Hapus spasi di awal/akhir dan ubah email/username ke huruf kecil
                    val cleanEmail = email.trim().lowercase()
                    val cleanPassword = password.trim()

                    Log.d("FatisdaAuth", "Mencoba login dengan: '$cleanEmail'")

                    container.authRepository.signIn(cleanEmail, cleanPassword)
                        .onSuccess { user = it }
                        .onFailure { err ->
                            Log.e("FatisdaAuth", "Login gagal: ${err.message}", err)
                            error = err.message ?: "Gagal masuk. Periksa email/username dan kata sandi."
                        }
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