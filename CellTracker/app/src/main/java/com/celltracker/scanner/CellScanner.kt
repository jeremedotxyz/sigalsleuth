package com.celltracker.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import androidx.core.content.ContextCompat
import com.celltracker.data.CellTowerData
import com.celltracker.data.NetworkType

@SuppressLint("MissingPermission")
class CellScanner(private val context: Context) {

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun hasRequiredPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val phone = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return fine && phone
    }

    /**
     * Returns a list of all visible cell towers.
     * Requires ACCESS_FINE_LOCATION + READ_PHONE_STATE.
     */
    fun scan(): List<CellTowerData> {
        if (!hasRequiredPermissions()) return emptyList()
        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return emptyList()
            cellInfoList.mapNotNull { parseCellInfo(it) }
                .sortedByDescending { it.signalStrength }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getOperatorName(): String =
        telephonyManager.networkOperatorName.ifBlank { "Unknown Operator" }

    private fun parseCellInfo(cellInfo: CellInfo): CellTowerData? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr ->
                parseNr(cellInfo)
            cellInfo is CellInfoLte   -> parseLte(cellInfo)
            cellInfo is CellInfoWcdma -> parseWcdma(cellInfo)
            cellInfo is CellInfoGsm   -> parseGsm(cellInfo)
            cellInfo is CellInfoCdma  -> parseCdma(cellInfo)
            else                      -> null
        }
    }

    // ── NR (5G) ──────────────────────────────────────────────────────────────
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun parseNr(cell: CellInfoNr): CellTowerData {
        val id  = cell.cellIdentity as CellIdentityNr
        val sig = cell.cellSignalStrength as CellSignalStrengthNr
        return CellTowerData(
            type           = NetworkType.NR,
            signalStrength = sig.dbm,
            signalLevel    = sig.level,
            isRegistered   = cell.isRegistered,
            mcc            = id.mccString ?: "—",
            mnc            = id.mncString ?: "—",
            cellId         = id.nci,
            lac            = id.tac,
            pci            = id.pci,
            earfcn         = id.nrarfcn,
            bandwidth      = 0,
            rsrp           = sig.ssRsrp,
            rsrq           = sig.ssRsrq,
            rssnr          = sig.ssSinr,
            cqi            = Int.MIN_VALUE,
            operatorName   = telephonyManager.networkOperatorName
        )
    }

    // ── LTE (4G) ─────────────────────────────────────────────────────────────
    private fun parseLte(cell: CellInfoLte): CellTowerData {
        val id  = cell.cellIdentity
        val sig = cell.cellSignalStrength
        val bw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { id.bandwidth / 1000 } catch (e: Exception) { 0 }
        } else 0
        return CellTowerData(
            type           = NetworkType.LTE,
            signalStrength = sig.dbm,
            signalLevel    = sig.level,
            isRegistered   = cell.isRegistered,
            mcc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mccString ?: "—" else "—",
            mnc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mncString ?: "—" else "—",
            cellId         = id.ci.toLong(),
            lac            = id.tac,
            pci            = id.pci,
            earfcn         = id.earfcn,
            bandwidth      = bw,
            rsrp           = sig.rsrp,
            rsrq           = sig.rsrq,
            rssnr          = sig.rssnr,
            cqi            = sig.cqi,
            operatorName   = telephonyManager.networkOperatorName
        )
    }

    // ── WCDMA (3G) ───────────────────────────────────────────────────────────
    private fun parseWcdma(cell: CellInfoWcdma): CellTowerData {
        val id  = cell.cellIdentity
        val sig = cell.cellSignalStrength
        return CellTowerData(
            type           = NetworkType.WCDMA,
            signalStrength = sig.dbm,
            signalLevel    = sig.level,
            isRegistered   = cell.isRegistered,
            mcc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mccString ?: "—" else "—",
            mnc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mncString ?: "—" else "—",
            cellId         = id.cid.toLong(),
            lac            = id.lac,
            pci            = id.psc,
            earfcn         = id.uarfcn,
            bandwidth      = 0,
            rsrp           = Int.MIN_VALUE,
            rsrq           = Int.MIN_VALUE,
            rssnr          = Int.MIN_VALUE,
            cqi            = Int.MIN_VALUE,
            operatorName   = telephonyManager.networkOperatorName
        )
    }

    // ── GSM (2G) ─────────────────────────────────────────────────────────────
    private fun parseGsm(cell: CellInfoGsm): CellTowerData {
        val id  = cell.cellIdentity
        val sig = cell.cellSignalStrength
        return CellTowerData(
            type           = NetworkType.GSM,
            signalStrength = sig.dbm,
            signalLevel    = sig.level,
            isRegistered   = cell.isRegistered,
            mcc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mccString ?: "—" else "—",
            mnc            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) id.mncString ?: "—" else "—",
            cellId         = id.cid.toLong(),
            lac            = id.lac,
            pci            = id.bsic,
            earfcn         = id.arfcn,
            bandwidth      = 0,
            rsrp           = Int.MIN_VALUE,
            rsrq           = Int.MIN_VALUE,
            rssnr          = Int.MIN_VALUE,
            cqi            = Int.MIN_VALUE,
            operatorName   = telephonyManager.networkOperatorName
        )
    }

    // ── CDMA ─────────────────────────────────────────────────────────────────
    private fun parseCdma(cell: CellInfoCdma): CellTowerData {
        val id  = cell.cellIdentity
        val sig = cell.cellSignalStrength
        return CellTowerData(
            type           = NetworkType.CDMA,
            signalStrength = sig.dbm,
            signalLevel    = sig.level,
            isRegistered   = cell.isRegistered,
            mcc            = "—",
            mnc            = "—",
            cellId         = id.basestationId.toLong(),
            lac            = id.networkId,
            pci            = -1,
            earfcn         = -1,
            bandwidth      = 0,
            rsrp           = Int.MIN_VALUE,
            rsrq           = Int.MIN_VALUE,
            rssnr          = Int.MIN_VALUE,
            cqi            = Int.MIN_VALUE,
            operatorName   = telephonyManager.networkOperatorName
        )
    }
}
