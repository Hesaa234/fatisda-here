package com.fatisdahere.app.ui.dosen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fatisdahere.app.ui.components.FatisdaTopBar
import com.fatisdahere.app.ui.components.ProfileContent
import com.fatisdahere.app.ui.theme.FatisdaTheme
import com.fatisdahere.app.ui.theme.PageBackground

@Composable
fun ProfilDosen(
    nama: String,
    nip: String,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(nama, onBack = onBack) },
        containerColor = PageBackground
    ) { padding ->
        ProfileContent(
            fields = listOf("Nama" to nama, "NIP" to nip, "Peran" to "Dosen"),
            onLogoutClick = onLogout,
            padding = padding
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilDosenPreview() {
    FatisdaTheme { ProfilDosen("Dr. Ahmad Fauzi, M.Kom.", "198001012005011001", {}, {}) }
}
