package com.fatisdahere.app.sensing

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.fatisdahere.app.positioning.WifiReading
import com.fatisdahere.app.positioning.WifiScan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

interface WifiScanner {
    suspend fun scan(count: Int): List<WifiScan>
}

class AndroidWifiScanner(context: Context, private val ssid: String) : WifiScanner {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** Jumlah scan pada pemanggilan terakhir yang tidak menghasilkan data baru (kena throttling Android). */
    var staleScanCount: Int = 0
        private set

    override suspend fun scan(count: Int): List<WifiScan> = scan(count) {}

    suspend fun scan(count: Int, onProgress: (Int) -> Unit): List<WifiScan> {
        staleScanCount = 0
        val results = mutableListOf<WifiScan>()
        repeat(count) {
            results += scanOnce()
            onProgress(it + 1)
        }
        return results
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun scanOnce(): WifiScan {
        val updated = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updated.complete(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false))
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val fresh = try {
            if (wifiManager.startScan()) {
                withTimeoutOrNull(SCAN_TIMEOUT_MS) { updated.await() } == true
            } else {
                false
            }
        } finally {
            appContext.unregisterReceiver(receiver)
        }
        if (!fresh) staleScanCount++

        val readings = wifiManager.scanResults
            .filter { it.SSID == ssid }
            .map { WifiReading(it.BSSID.lowercase(), it.level) }
        return WifiScan(readings, System.currentTimeMillis())
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 10_000L
    }
}
