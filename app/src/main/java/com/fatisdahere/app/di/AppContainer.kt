package com.fatisdahere.app.di

import android.content.Context
import com.fatisdahere.app.data.FingerprintLoader
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.sensing.AndroidWifiScanner

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val fingerprint: FingerprintData by lazy { FingerprintLoader.load(appContext) }
    val positioningParams: PositioningParams by lazy { PositioningParams.default() }
    val positioningEngine: KnnPositioningEngine by lazy { KnnPositioningEngine(fingerprint, positioningParams) }
    val wifiScanner: AndroidWifiScanner by lazy { AndroidWifiScanner(appContext, fingerprint.ssid) }
}
