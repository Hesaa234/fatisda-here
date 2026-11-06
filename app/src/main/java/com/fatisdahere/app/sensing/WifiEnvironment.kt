package com.fatisdahere.app.sensing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** Izin lokasi wajib supaya Android mau memberikan hasil scan Wi-Fi. */
fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Pesan masalah kalau Wi-Fi atau lokasi HP mati; null kalau siap scan. */
fun wifiEnvironmentProblem(context: Context): String? {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    if (!wifi.isWifiEnabled) return "Wi-Fi mati. Nyalakan Wi-Fi terlebih dahulu."
    val location = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(location)) return "Lokasi HP mati. Nyalakan lokasi agar scan Wi-Fi bisa dibaca."
    return null
}
