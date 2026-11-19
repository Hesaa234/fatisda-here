package com.fatisdahere.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*

private val BentukHeader = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

@Composable
fun HeaderGradien(
    judul: String,
    modifier: Modifier = Modifier,
    subjudul: String? = null,
    onBack: (() -> Unit)? = null,
    userName: String? = null,
    onUserClick: () -> Unit = {},
    isi: (@Composable ColumnScope.() -> Unit)? = null
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(BentukHeader)
            .background(GradienUtama)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 6.dp else 20.dp, end = 16.dp, top = 8.dp, bottom = if (isi == null) 14.dp else 22.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                }
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(
                        judul,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subjudul != null) Text(
                        subjudul,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (userName != null) ChipPengguna(userName, onUserClick)
            }
            if (isi != null) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.padding(start = if (onBack != null) 14.dp else 0.dp), content = isi)
            }
        }
    }
}

@Composable
private fun ChipPengguna(nama: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(BentukPil)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(28.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nama.trim().firstOrNull()?.uppercase() ?: "?",
                color = UnsBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            nama,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp)
        )
    }
}

@Composable
fun CincinProgres(
    persen: Int,
    ukuran: Dp,
    modifier: Modifier = Modifier,
    warna: Color = StatusSuccess,
    track: Color = TrackBlue,
    tebal: Dp = 10.dp,
    teks: String? = null,
    warnaTeks: Color = UnsNavy,
    label: String? = null
) {
    var mulai by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mulai = true }
    val p by animateFloatAsState(
        if (mulai) persen.coerceIn(0, 100).toFloat() else 0f,
        tween(1000),
        label = "cincin"
    )
    Box(modifier.size(ukuran), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = tebal.toPx()
            val sisi = size.minDimension - t
            val pojok = Offset(t / 2, t / 2)
            val ukuranBusur = Size(sisi, sisi)
            drawArc(track, 0f, 360f, false, pojok, ukuranBusur, style = Stroke(t))
            if (p > 0f) drawArc(
                warna, -90f, 360f * p / 100f, false, pojok, ukuranBusur,
                style = Stroke(t, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(teks ?: "${p.toInt()}%", color = warnaTeks, fontWeight = FontWeight.Bold, fontSize = (ukuran.value * 0.24f).sp)
            if (label != null) Text(label, color = warnaTeks.copy(alpha = 0.8f), fontSize = (ukuran.value * 0.12f).sp)
        }
    }
}

@Composable
fun BarisTimeline(
    warna: Color,
    modifier: Modifier = Modifier,
    aktif: Boolean = false,
    terisi: Boolean = true,
    pertama: Boolean = false,
    terakhir: Boolean = false,
    isi: @Composable () -> Unit
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(28.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val cy = 28.dp.toPx()
                    val awal = if (pertama) cy else 0f
                    val akhir = if (terakhir) cy else size.height
                    drawLine(TrackBlue, Offset(x, awal), Offset(x, akhir), strokeWidth = 2.dp.toPx())
                    if (aktif) drawCircle(warna.copy(alpha = 0.22f), radius = 12.dp.toPx(), center = Offset(x, cy))
                    if (terisi) {
                        drawCircle(warna, radius = 7.dp.toPx(), center = Offset(x, cy))
                    } else {
                        drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(x, cy))
                        drawCircle(warna, radius = 6.dp.toPx(), center = Offset(x, cy), style = Stroke(3.dp.toPx()))
                    }
                }
        )
        Box(Modifier.weight(1f).padding(bottom = 10.dp)) { isi() }
    }
}

@Composable
fun KartuSesiAktif(
    judul: String,
    keterangan: String,
    tombol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tombolAktif: Boolean = true,
    baris: List<String> = emptyList()
) {
    Column(
        modifier
            .fillMaxWidth()
            .muncul()
            .clip(BentukKartu)
            .background(GradienUtama)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(Color.White, CircleShape))
            Text(keterangan, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(judul, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        baris.forEach { Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp) }
        Button(
            onClick = onClick,
            enabled = tombolAktif,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = BentukPil,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = UnsBlue,
                disabledContainerColor = Color.White.copy(alpha = 0.55f),
                disabledContentColor = UnsBlue.copy(alpha = 0.7f)
            )
        ) { Text(tombol, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun RadarPemindai(selesai: Int, total: Int, modifier: Modifier = Modifier) {
    val skala = listOf(1f, 0.75f, 0.54f)
    val alfa = skala.indices.map { i ->
        val dalamKeluar = skala.size - 1 - i
        val nyala = total > 0 && selesai * skala.size >= (dalamKeluar + 1) * total
        val a by animateFloatAsState(if (nyala) 0.9f else 0.3f, tween(450), label = "cincinRadar$i")
        a
    }
    Box(modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val pusat = center
            skala.forEachIndexed { i, s ->
                drawCircle(
                    Color.White.copy(alpha = alfa[i]),
                    radius = size.minDimension / 2 * s - 2.dp.toPx(),
                    center = pusat,
                    style = Stroke(2.dp.toPx())
                )
            }
        }
        Box(
            Modifier.size(92.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Wifi, contentDescription = null, Modifier.size(40.dp), tint = UnsBlue)
        }
    }
}

@Composable
fun LangkahScan(selesai: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..total) {
            val selesaiIni = selesai >= i
            val berjalan = !selesaiIni && selesai == i - 1
            val warnaLatar by animateColorAsState(
                if (berjalan) Color.White else Color.White.copy(alpha = 0.18f), tween(300), label = "langkahLatar"
            )
            val warnaTeks by animateColorAsState(if (berjalan) UnsBlue else Color.White, tween(300), label = "langkahTeks")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(warnaLatar)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        selesaiIni -> "Scan $i ✓"
                        berjalan -> "Scan $i…"
                        else -> "Scan $i"
                    },
                    color = warnaTeks,
                    fontSize = 12.sp,
                    fontWeight = if (berjalan) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LingkaranIkon(
    ukuran: Dp,
    warna: Color,
    modifier: Modifier = Modifier,
    isi: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.size(ukuran).background(warna.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
        content = isi
    )
}

@Composable
fun StatistikHeader(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (angka, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(angka, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
            }
        }
    }
}
