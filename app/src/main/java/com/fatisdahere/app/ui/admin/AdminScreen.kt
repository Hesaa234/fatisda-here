package com.fatisdahere.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.FatisdaTheme
import com.fatisdahere.app.ui.theme.PageBackground

/**
 * Tempat sementara untuk halaman admin (belum dibuat).
 * Buat halaman admin di folder ini dengan komponen dari ui/components supaya gayanya sama.
 */
@Composable
fun AdminScreen(user: AppUser, onLogout: () -> Unit) {
    Scaffold(
        topBar = { FatisdaTopBar(user.name) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Halaman Admin") {
                HelperText("Halaman admin belum tersedia. Nanti di sini admin mengelola akun, ruangan, dan mata kuliah.")
            }
            DangerButton("Log Out", onLogout)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminScreenPreview() {
    FatisdaTheme { AdminScreen(AppUser("adm1", "Admin Fatisda", "admin@fatisda.demo", Role.ADMIN), {}) }
}
