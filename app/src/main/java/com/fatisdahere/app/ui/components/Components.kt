package com.fatisdahere.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*

@Composable
fun FatisdaTopBar(
    userName: String? = null,
    onUserClick: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    HeaderGradien(
        judul = "FATISDA Here",
        onBack = onBack,
        userName = userName,
        onUserClick = onUserClick
    )
}

@Composable
fun HeaderHalaman(
    judul: String,
    subjudul: String?,
    username: String,
    onProfileClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    isi: (@Composable ColumnScope.() -> Unit)? = null
) {
    HeaderGradien(
        judul = judul,
        subjudul = subjudul,
        onBack = onBack,
        userName = username,
        onUserClick = onProfileClick,
        isi = isi
    )
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().muncul().animateContentSize(),
        shape = BentukKartu,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(
                title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = UnsNavy
            )
            content()
        }
    }
}

@Composable
fun ItemCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    inisial: String? = null,
    warnaInisial: Color = UnsBlue,
    content: @Composable RowScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.fillMaxWidth().muncul(),
        shape = BentukItem,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().then(clickModifier).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inisial != null) {
                LingkaranIkon(44.dp, warnaInisial) {
                    Text(
                        inisial.trim().take(2).uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = warnaInisial
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            content()
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = 2.dp, top = 8.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = UnsNavy
    )
}

@Composable
fun ItemTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 15.sp)
}

@Composable
fun ItemSubtitle(text: String) {
    Text(text, fontSize = 13.sp, color = TextMuted)
}

@Composable
fun HelperText(text: String, color: Color = TextMuted) {
    Text(text, fontSize = 13.sp, color = color)
}

@Composable
fun ErrorText(text: String) {
    Text(text, fontSize = 13.sp, color = StatusDanger)
}

fun readableColor(color: Color): Color = when (color) {
    StatusWarning -> Color(0xFF9A6B00)
    StatusGray -> Color(0xFF4A6075)
    else -> color
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = BentukPil, color = color.copy(alpha = 0.15f)) {
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
    Surface(modifier, shape = BentukPil, color = UnsTeal.copy(alpha = 0.14f)) {
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(72.dp), fontSize = 13.sp, color = TextMuted)
        Text(value, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
fun SessionInfo(session: SessionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(session.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
        Spacer(Modifier.height(2.dp))
        InfoRow("Waktu", session.time)
        InfoRow("Ruang", session.room)
        InfoRow("Dosen", session.lecturer)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val latar = if (enabled) GradienUtama else Brush.linearGradient(listOf(TrackBlue, TrackBlue))
    val sumber = remember { MutableInteractionSource() }
    val ditekan by sumber.collectIsPressedAsState()
    val skala by animateFloatAsState(if (ditekan && enabled) 0.96f else 1f, tween(120), label = "tekan")
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { scaleX = skala; scaleY = skala }
            .clip(BentukPil)
            .background(latar)
            .clickable(
                interactionSource = sumber,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else TextMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun TombolMini(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    warna: Color = UnsBlue,
    terisi: Boolean = true,
    enabled: Boolean = true
) {
    if (terisi) {
        Button(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            enabled = enabled,
            shape = BentukPil,
            contentPadding = PaddingValues(horizontal = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = warna)
        ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            enabled = enabled,
            shape = BentukPil,
            contentPadding = PaddingValues(horizontal = 18.dp),
            border = BorderStroke(1.5.dp, warna),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = warna)
        ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

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
        modifier = modifier.fillMaxWidth().height(50.dp),
        enabled = enabled,
        shape = BentukPil,
        border = BorderStroke(1.5.dp, if (enabled) color else TextPrimary.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = BentukPil,
        colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextMuted)
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
        shape = BentukField,
        colors = warnaField()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun warnaField() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = UnsBlue,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.4f),
    cursorColor = UnsBlue,
    containerColor = SurfaceWhite
)

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
            .clip(BentukField)
            .clickable(enabled = enabled, onClick = onClick),
        shape = BentukField,
        color = if (enabled) SurfaceWhite else PageBackground,
        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.45f))
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
                Icon(trailingIcon, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

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
        containerColor = SurfaceWhite,
        shape = BentukKartu,
        title = { Text(title, fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 18.sp) },
        text = { Text(text, color = TextPrimary, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (danger) StatusDanger else UnsBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = TextMuted, fontWeight = FontWeight.SemiBold) }
        }
    )
}

@Composable
fun LoadingContent(padding: PaddingValues = PaddingValues(0.dp)) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Memuat data...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = UnsBlue)
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, padding: PaddingValues = PaddingValues(0.dp), onLogout: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        SectionCard("Gagal memuat data") {
            HelperText(message)
            PrimaryButton("Coba Lagi", onRetry)
            if (onLogout != null) SecondaryButton("Log Out", onLogout)
        }
    }
}

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = BentukKartu,
        title = { Text("Terjadi kesalahan", fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 18.sp) },
        text = { Text(message, color = TextPrimary, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = UnsBlue, fontWeight = FontWeight.Bold) } }
    )
}

fun pesanGalat(e: Throwable): String {
    val m = e.message.orEmpty()
    return when {
        m.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Akses ditolak oleh Firebase (cek rules Firestore dan role akun)."
        m.contains("offline", ignoreCase = true) || m.contains("UNAVAILABLE") ->
            "Tidak bisa terhubung ke server. Cek koneksi internet."
        m.isNotBlank() -> m
        else -> "Terjadi kesalahan."
    }
}

@Composable
fun ProfileContent(fields: List<Pair<String, String>>, onLogoutClick: () -> Unit, padding: PaddingValues) {
    var konfirmasiKeluar by remember { mutableStateOf(false) }
    if (konfirmasiKeluar) {
        ConfirmDialog(
            title = "Keluar dari akun?",
            text = "Kamu perlu login lagi untuk masuk ke aplikasi.",
            confirmText = "Log Out",
            onConfirm = {
                konfirmasiKeluar = false
                onLogoutClick()
            },
            onDismiss = { konfirmasiKeluar = false }
        )
    }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(88.dp).background(GradienUtama, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, Modifier.size(52.dp), tint = Color.White)
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    fields.forEach { (label, value) ->
                        Column {
                            Text(label, fontSize = 12.sp, color = TextMuted)
                            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                        }
                    }
                }
            }
        }
        DangerButton("Log Out", { konfirmasiKeluar = true })
    }
}

@Composable
fun KartuKosong(teks: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().muncul(),
        shape = BentukItem,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LingkaranIkon(40.dp, TextMuted) {
                Icon(Icons.Filled.Info, contentDescription = null, Modifier.size(22.dp), tint = TextMuted)
            }
            Text(teks, Modifier.weight(1f), fontSize = 14.sp, color = TextMuted)
        }
    }
}

data class ItemStat(val angka: String, val label: String, val warna: Color)

@Composable
fun StatistikBaris(items: List<ItemStat>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.warna.copy(alpha = 0.12f))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.angka, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = readableColor(item.warna))
                Text(item.label, fontSize = 11.sp, color = TextMuted, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TabPil(opsi: List<String>, terpilih: Int, onPilih: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(BentukPil)
            .background(TrackBlue)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opsi.forEachIndexed { i, teks ->
            val aktif = i == terpilih
            val warnaLatar by animateColorAsState(if (aktif) UnsBlue else Color.Transparent, tween(250), label = "tabLatar")
            val warnaTeks by animateColorAsState(if (aktif) Color.White else TextMuted, tween(250), label = "tabTeks")
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(BentukPil)
                    .background(warnaLatar)
                    .clickable(role = Role.Tab) { onPilih(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    teks,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = warnaTeks,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BatangProgres(persen: Int, warna: Color, modifier: Modifier = Modifier) {
    var mulai by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mulai = true }
    val isi by animateFloatAsState(
        if (mulai) persen.coerceIn(0, 100) / 100f else 0f,
        tween(800),
        label = "batang"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(BentukPil)
            .background(warna.copy(alpha = 0.15f))
    ) {
        if (isi > 0f) Box(Modifier.fillMaxWidth(isi).fillMaxHeight().clip(BentukPil).background(warna))
    }
}
