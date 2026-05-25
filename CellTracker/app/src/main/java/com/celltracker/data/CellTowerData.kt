package com.celltracker.data

/**
 * Unified cell tower data model covering LTE, NR (5G), GSM, WCDMA, CDMA.
 */
data class CellTowerData(
    val type: NetworkType,
    val signalStrength: Int,        // dBm
    val signalLevel: Int,           // 0–4
    val isRegistered: Boolean,
    val mcc: String,                // Mobile Country Code
    val mnc: String,                // Mobile Network Code
    val cellId: Long,               // CID / NCI
    val lac: Int,                   // Location Area Code (GSM/WCDMA) or TAC (LTE/NR)
    val pci: Int,                   // Physical Cell ID (LTE/NR)
    val earfcn: Int,                // EARFCN/ARFCN/UARFCN
    val bandwidth: Int,             // MHz, LTE/NR only
    val rsrp: Int,                  // dBm, LTE/NR only
    val rsrq: Int,                  // dB,  LTE/NR only
    val rssnr: Int,                 // dB,  LTE only
    val cqi: Int,                   // Channel Quality Indicator, LTE only
    val operatorName: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val signalBars: String get() = when (signalLevel) {
        4 -> "▂▄▆█"
        3 -> "▂▄▆░"
        2 -> "▂▄░░"
        1 -> "▂░░░"
        else -> "░░░░"
    }

    val signalQuality: SignalQuality get() = when {
        signalStrength >= -65  -> SignalQuality.EXCELLENT
        signalStrength >= -80  -> SignalQuality.GOOD
        signalStrength >= -95  -> SignalQuality.FAIR
        signalStrength >= -110 -> SignalQuality.POOR
        else                   -> SignalQuality.NO_SIGNAL
    }

    val displayCellId: String get() = if (cellId > 0) cellId.toString() else "—"
    val displayPci: String get() = if (pci >= 0) pci.toString() else "—"
    val displayBandwidth: String get() = if (bandwidth > 0) "${bandwidth} MHz" else "—"
    val displayRsrp: String get() = if (rsrp != Int.MIN_VALUE) "$rsrp dBm" else "—"
    val displayRsrq: String get() = if (rsrq != Int.MIN_VALUE) "$rsrq dB" else "—"
    val displayLac: String get() = if (lac > 0) lac.toString() else "—"
    val displayEarfcn: String get() = if (earfcn > 0) earfcn.toString() else "—"
}

enum class NetworkType(val label: String, val colorRes: Int) {
    NR("5G NR",     android.R.color.holo_green_light),
    LTE("4G LTE",   android.R.color.holo_blue_light),
    WCDMA("3G UMTS",android.R.color.holo_orange_light),
    GSM("2G GSM",   android.R.color.holo_red_light),
    CDMA("CDMA",    android.R.color.holo_purple),
    UNKNOWN("UNK",  android.R.color.darker_gray)
}

enum class SignalQuality(val label: String) {
    EXCELLENT("EXCELLENT"),
    GOOD("GOOD"),
    FAIR("FAIR"),
    POOR("POOR"),
    NO_SIGNAL("NO SIGNAL")
}

data class ScanSummary(
    val primarySignal: Int,          // dBm of serving cell
    val primaryType: NetworkType,
    val primaryOperator: String,
    val towersFound: Int,
    val scanDuration: Long,          // ms
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)
