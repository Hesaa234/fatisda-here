package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/** Profil mahasiswa. Memakai ProfileContent yang sama dengan dosen dan admin (termasuk konfirmasi Log Out). */
@Composable
fun StudentProfileScreen(
    name: String,
    nim: String,
    onLogoutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    email: String = ""
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(name, onBack = onBack) },
        containerColor = PageBackground
    ) { padding ->
        ProfileContent(
            fields = listOf(
                "Nama" to name,
                "NIM" to nim,
                "Email" to email.ifBlank { "-" },
                "Peran" to "Mahasiswa"
            ),
            onLogoutClick = onLogoutClick,
            padding = padding
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentProfilePreview() {
    FatisdaTheme { StudentProfileScreen("Andi Prasetyo", "M0521001", {}, {}, email = "andi@student.uns.ac.id") }
}
