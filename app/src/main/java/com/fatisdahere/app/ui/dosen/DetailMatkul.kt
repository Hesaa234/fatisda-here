
package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

@Composable
fun DetailMatkul(
    username: String,
    matkul: MataKuliah,
    riwayat: List<Pertemuan>,
    statusSesi: String?,
    onBukaSesi: () -> Unit,
    onKelolaPeserta: () -> Unit,
    onEditMatkul: () -> Unit,
    onRekap: () -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = {
            HeaderGradien(
                judul = matkul.nama,
                subjudul = "${matkul.kode} · ${matkul.ruang}",
                onBack = onBack,
                userName = username,
                onUserClick = onProfileClick
            ) {
                StatistikHeader(
                    listOf(
                        "${matkul.jumlahPeserta}" to "Peserta",
                        "${riwayat.size}" to "Pertemuan"
                    )
                )
            }
        },
        containerColor = PageBackground
    ) { padding ->

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                SectionCard {
                    PrimaryButton(
                        statusSesi ?: "Buka Sesi Absen",
                        onBukaSesi,
                        enabled = statusSesi == null
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryButton(
                            "Kelola Peserta",
                            onKelolaPeserta,
                            Modifier.weight(1f)
                        )

                        SecondaryButton(
                            "Edit Matkul",
                            onEditMatkul,
                            Modifier.weight(1f)
                        )
                    }

                    SecondaryButton(
                        "Rekap Kehadiran",
                        onRekap
                    )
                }
            }

            item {
                SectionTitle("Riwayat Pertemuan")
            }

            if (riwayat.isEmpty()) {
                item {
                    KartuKosong("Belum ada pertemuan.")
                }
            }

            items(riwayat) {
                KartuPertemuan(it)
            }
        }
    }
}

@Composable
private fun KartuPertemuan(p: Pertemuan) {
    ItemCard(inisial = "P${p.nomor}") {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ItemTitle("Pertemuan ${p.nomor}")
            if (p.terjadwal) StatusPill("Terjadwal", UnsBlue)

            Text(
                text = buildAnnotatedString {
                    append("Hadir: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("${p.hadir}")
                    }
                    append(" dari ${p.total}")
                },
                fontSize = 13.sp,
                color = TextPrimary
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                p.tanggal,
                fontSize = 13.sp,
                color = UnsBlue,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )

            ItemSubtitle(p.waktu)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun DetailMatkulPreview() {
    FatisdaTheme {
        DetailMatkul(
            username = "Dosen",
            matkul = MataKuliah(
                "WMC-A",
                "Wireless & Mobile Computing",
                "LAB 2 UPT",
                25
            ),
            riwayat = listOf(
                Pertemuan(3, 23, 25, "16 Okt 2026", "13:00 - 15:00"),
                Pertemuan(2, 22, 25, "9 Okt 2026", "13:00 - 15:00"),
                Pertemuan(1, 25, 25, "2 Okt 2026", "13:00 - 15:00")
            ),
            statusSesi = null,
            onBukaSesi = {},
            onKelolaPeserta = {},
            onEditMatkul = {},
            onRekap = {},
            onBack = {},
            onProfileClick = {}
        )
    }
}
