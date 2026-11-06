package com.fatisdahere.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.TextPrimary
import com.fatisdahere.app.ui.theme.UnsBlue
import com.fatisdahere.app.ui.theme.UnsNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Foto bukti Izin/Sakit.
 *
 * Firebase Storage berbayar, jadi foto disimpan sebagai TEKS di Firestore:
 *   foto dari galeri -> dikecilkan (sisi terpanjang maks 1280 px) -> JPEG -> teks Base64
 * Satu dokumen Firestore maksimal 1 MiB, jadi teksnya dijaga di bawah BATAS_TEKS.
 * Kalau masih kebesaran, kualitas JPEG diturunkan, lalu ukuran gambar diperkecil lagi.
 *
 * Dosen melihatnya dengan kebalikannya: teks Base64 -> byte JPEG -> gambar.
 */
object BuktiFoto {
    private const val SISI_MAKS = 1280
    private const val SISI_MIN = 480

    /** ~700 ribu karakter Base64 = ~525 KB JPEG. Aman di bawah batas dokumen 1 MiB. */
    const val BATAS_TEKS = 700_000

    /**
     * Membaca foto dari galeri dan mengubahnya jadi teks Base64 yang sudah dikompres.
     * Berat (decode gambar), jadi panggil dari Dispatchers.IO / Default.
     * Melempar IllegalArgumentException dengan pesan yang bisa ditampilkan kalau gagal.
     */
    fun kompres(context: Context, uri: Uri): String {
        val resolver = context.contentResolver

        // 1. Baca ukuran asli saja (belum memuat gambarnya).
        // Catatan: dengan inJustDecodeBounds, decodeStream SELALU mengembalikan null dan hanya
        // mengisi outWidth/outHeight, jadi yang dicek di sini hanya apakah file bisa dibuka.
        val ukuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val aliran = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Foto tidak bisa dibuka.")
        aliran.use { BitmapFactory.decodeStream(it, null, ukuran) }
        if (ukuran.outWidth <= 0 || ukuran.outHeight <= 0) {
            throw IllegalArgumentException("File yang dipilih bukan foto.")
        }

        // 2. Muat dalam ukuran yang sudah dikecilkan (hemat memori untuk foto kamera 12 MP+)
        var sample = 1
        while (max(ukuran.outWidth, ukuran.outHeight) / (sample * 2) >= SISI_MAKS) sample *= 2
        val opsi = BitmapFactory.Options().apply { inSampleSize = sample }
        val mentah = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
            ?: throw IllegalArgumentException("Foto tidak bisa dibaca.")

        // 3. Putar sesuai orientasi kamera (foto HP sering tersimpan miring)
        val derajat = try {
            resolver.openInputStream(uri)?.use { derajatExif(ExifInterface(it)) } ?: 0
        } catch (e: Exception) {
            0
        }
        var gambar = perkecil(putar(mentah, derajat), SISI_MAKS)

        // 4. Kompres sampai muat
        while (true) {
            for (kualitas in intArrayOf(75, 65, 55, 45, 35)) {
                val teks = keBase64(gambar, kualitas)
                if (teks.length <= BATAS_TEKS) return teks
            }
            val sisiBaru = (max(gambar.width, gambar.height) * 0.75f).roundToInt()
            if (sisiBaru < SISI_MIN) throw IllegalArgumentException("Foto terlalu besar. Coba foto lain.")
            gambar = perkecil(gambar, sisiBaru)
        }
    }

    /** Teks Base64 -> gambar. null kalau teksnya rusak. */
    fun keBitmap(base64: String): Bitmap? = try {
        val byte = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(byte, 0, byte.size)
    } catch (e: Exception) {
        null
    }

    /** Perkiraan ukuran file untuk ditampilkan, misalnya "180 KB". */
    fun ukuranTeks(base64: String): String = "${base64.length * 3 / 4 / 1024} KB"

    private fun keBase64(gambar: Bitmap, kualitas: Int): String {
        val keluar = ByteArrayOutputStream()
        gambar.compress(Bitmap.CompressFormat.JPEG, kualitas, keluar)
        return Base64.encodeToString(keluar.toByteArray(), Base64.NO_WRAP)
    }

    private fun perkecil(gambar: Bitmap, sisiMaks: Int): Bitmap {
        val sisi = max(gambar.width, gambar.height)
        if (sisi <= sisiMaks) return gambar
        val skala = sisiMaks.toFloat() / sisi
        return Bitmap.createScaledBitmap(
            gambar,
            (gambar.width * skala).roundToInt().coerceAtLeast(1),
            (gambar.height * skala).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun putar(gambar: Bitmap, derajat: Int): Bitmap {
        if (derajat == 0) return gambar
        val matriks = Matrix().apply { postRotate(derajat.toFloat()) }
        return Bitmap.createBitmap(gambar, 0, 0, gambar.width, gambar.height, matriks, true)
    }

    private fun derajatExif(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

/**
 * Dialog untuk dosen: menampilkan keterangan dan foto bukti Izin/Sakit seorang mahasiswa.
 * Foto baru diunduh saat dialog dibuka (lewat [muatFoto]).
 *
 * @param muatFoto mengambil teks Base64 foto, null kalau mahasiswa tidak mengunggah foto
 */
@Composable
fun BuktiDialog(
    judul: String,
    keterangan: String?,
    muatFoto: suspend () -> String?,
    onDismiss: () -> Unit
) {
    var foto by remember { mutableStateOf<ImageBitmap?>(null) }
    var pesan by remember { mutableStateOf<String?>("Memuat foto...") }

    LaunchedEffect(Unit) {
        pesan = try {
            val teks = muatFoto()
            if (teks == null) {
                "Tidak ada foto bukti."
            } else {
                val gambar = withContext(Dispatchers.Default) { BuktiFoto.keBitmap(teks) }
                foto = gambar?.asImageBitmap()
                if (gambar == null) "Foto bukti rusak dan tidak bisa ditampilkan." else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Gagal memuat foto: ${pesanGalat(e)}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(judul, fontWeight = FontWeight.Bold, color = UnsNavy) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Keterangan", fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.6f))
                    Text(keterangan?.ifBlank { null } ?: "-", fontSize = 14.sp, color = TextPrimary)
                }
                foto?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Foto bukti",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                pesan?.let { HelperText(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = UnsBlue, fontWeight = FontWeight.Bold) }
        }
    )
}

/**
 * Ringkasan keterangan Izin/Sakit di kartu daftar (Pantau Sesi, Rekap Kehadiran).
 * Tidak tampil apa-apa kalau tidak ada keterangan maupun foto.
 */
@Composable
fun KeteranganRingkas(keterangan: String?, adaBukti: Boolean, onLihat: () -> Unit) {
    if (keterangan.isNullOrBlank() && !adaBukti) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!keterangan.isNullOrBlank()) {
            Text(
                "\u201C$keterangan\u201D",
                fontSize = 13.sp,
                color = TextPrimary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            if (adaBukti) "Lihat keterangan & foto bukti" else "Lihat keterangan",
            Modifier.clickable(onClick = onLihat).padding(vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = UnsBlue
        )
    }
}
