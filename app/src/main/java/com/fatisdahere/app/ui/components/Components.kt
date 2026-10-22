package com.fatisdahere.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatisdaTopBar(
    userName: String? = null,
    onUserClick: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text("FATISDA Here", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
        },
        actions = {
            if (userName != null) {
                Row(
                    Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onUserClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.AccountCircle, null, Modifier.size(28.dp))
                    Text(userName, fontSize = 14.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = UnsBlue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(
                title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UnsNavy
            )
            content()
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (color == StatusWarning) Color(0xFF8A6D00) else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = UnsTeal.copy(alpha = 0.12f)) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).background(StatusSuccess, CircleShape))
            Text("Sedang Berlangsung", color = UnsBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text(label, Modifier.width(64.dp), fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.7f))
        Text(": $value", fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
fun SessionInfo(session: SessionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(session.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
        Spacer(Modifier.height(4.dp))
        InfoRow("Waktu", session.time)
        InfoRow("Ruang", session.room)
        InfoRow("Dosen", session.lecturer)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = UnsBlue)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}
