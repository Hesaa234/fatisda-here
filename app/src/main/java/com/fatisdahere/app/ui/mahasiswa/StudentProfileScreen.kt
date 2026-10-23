package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*
import com.fatisdahere.app.ui.components.*

@Composable
fun StudentProfileScreen(
    name: String,
    nim: String,
    onLogoutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(name, onBack = onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Profil") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        Modifier.size(96.dp).background(UnsBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, Modifier.size(56.dp), tint = UnsBlue)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileField("Nama", name)
                        ProfileField("NIM", nim)
                    }
                }
            }
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
            ) { Text("Log Out", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.6f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentProfilePreview() {
    FatisdaTheme { StudentProfileScreen("Andi Prasetyo", "M0521001", {}, {}) }
}
