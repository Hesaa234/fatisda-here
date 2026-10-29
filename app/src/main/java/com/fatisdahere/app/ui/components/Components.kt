package com.fatisdahere.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*

/*
 * Komponen bersama untuk SEMUA halaman (mahasiswa, dosen, admin).
 * Kalau butuh tampilan baru, tambahkan di sini dulu supaya gaya tetap seragam,
 * jangan membuat warna atau bentuk sendiri di file layar.
 */

// ---------------------------------------------------------------- Header

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

// ---------------------------------------------------------------- Kartu & teks

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

/** Kartu satu baris untuk isi daftar (mata kuliah, pertemuan, peserta, dll). */
@Composable
fun ItemCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.fillMaxWidth().then(clickModifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Judul di atas daftar, contoh "Mata Kuliah Saya". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
}

@Composable
fun ItemTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 15.sp)
}

@Composable
fun ItemSubtitle(text: String) {
    Text(text, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.7f))
}

@Composable
fun HelperText(text: String, color: Color = TextPrimary.copy(alpha = 0.65f)) {
    Text(text, fontSize = 13.sp, color = color)
}

@Composable
fun ErrorText(text: String) {
    Text(text, fontSize = 13.sp, color = StatusDanger)
}

/** Warna teks untuk label berwarna (kuning dibuat lebih gelap supaya terbaca). */
fun readableColor(color: Color): Color = if (color == StatusWarning) Color(0xFF8A6D00) else color

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = readableColor(color),
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

// ---------------------------------------------------------------- Tombol

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

/** Tombol garis tepi. Pakai color = StatusDanger untuk aksi seperti "Tutup Sesi". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = UnsBlue
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) color else TextPrimary.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

// ---------------------------------------------------------------- Form

@Composable
fun FieldLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/** Kotak yang bisa diklik, bentuknya sama dengan kolom isian (dipakai untuk pilihan dan jam). */
@Composable
fun ClickableField(
    text: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.Filled.ArrowDropDown
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) SurfaceWhite else PageBackground,
        border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text ?: placeholder,
                Modifier.weight(1f),
                fontSize = 16.sp,
                color = if (text == null) TextPrimary.copy(alpha = 0.5f) else TextPrimary
            )
            if (enabled && trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, tint = TextPrimary.copy(alpha = 0.6f))
            }
        }
    }
}

/** Pilihan dari daftar (dropdown). onSelect menerima index pilihan. */
@Composable
fun SelectField(
    value: String?,
    placeholder: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyText: String = "Tidak ada pilihan"
) {
    var expanded by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier.fillMaxWidth().onSizeChanged { widthPx = it.width }) {
        ClickableField(value, placeholder, onClick = { expanded = true }, enabled = enabled)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { widthPx.toDp() })
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text(emptyText) }, onClick = { expanded = false }, enabled = false)
            }
            options.forEachIndexed { i, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(i)
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Lain-lain

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) StatusDanger else UnsBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/*
 * CATATAN: jangan pakai CircularProgressIndicator() / LinearProgressIndicator() tanpa "progress"
 * (versi berputar terus). Di proyek ini material3 (BOM 2023.08) dipakai bersama
 * material-icons-extended 1.6.0, dan kombinasi itu membuat animasinya crash
 * (NoSuchMethodError ... KeyframesSpec). Versi dengan progress = angka aman.
 */
@Composable
fun LoadingContent(padding: PaddingValues = PaddingValues(0.dp)) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Memuat data...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UnsBlue)
    }
}

/** Isi halaman profil, sama untuk mahasiswa, dosen, dan admin. fields = (label, nilai). */
@Composable
fun ProfileContent(fields: List<Pair<String, String>>, onLogoutClick: () -> Unit, padding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard("Profil") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    Modifier.size(96.dp).background(UnsBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, Modifier.size(56.dp), tint = UnsBlue)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    fields.forEach { (label, value) ->
                        Column {
                            Text(label, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.6f))
                            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                        }
                    }
                }
            }
        }
        DangerButton("Log Out", onLogoutClick)
    }
}
