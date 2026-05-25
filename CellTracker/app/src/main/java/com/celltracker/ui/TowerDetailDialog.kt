package com.celltracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.celltracker.R
import com.celltracker.data.CellTowerData
import com.celltracker.data.NetworkType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class TowerDetailDialog(private val tower: CellTowerData) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_tower_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

        fun tv(id: Int) = view.findViewById<TextView>(id)

        tv(R.id.tvDetailType).text       = tower.type.label
        tv(R.id.tvDetailRegistered).text = if (tower.isRegistered) "SERVING CELL" else "NEIGHBOR CELL"
        tv(R.id.tvDetailSignal).text     = "${tower.signalStrength} dBm  ${tower.signalBars}"
        tv(R.id.tvDetailQuality).text    = tower.signalQuality.label
        tv(R.id.tvDetailOperator).text   = tower.operatorName.ifBlank { "Unknown" }
        tv(R.id.tvDetailMcc).text        = tower.mcc
        tv(R.id.tvDetailMnc).text        = tower.mnc
        tv(R.id.tvDetailCellId).text     = tower.displayCellId
        tv(R.id.tvDetailLac).text        = tower.displayLac
        tv(R.id.tvDetailPci).text        = tower.displayPci
        tv(R.id.tvDetailEarfcn).text     = tower.displayEarfcn
        tv(R.id.tvDetailTimestamp).text  = sdf.format(Date(tower.timestamp))

        // LTE/NR specific
        val lteGroup = view.findViewById<View>(R.id.groupLteNr)
        if (tower.type == NetworkType.LTE || tower.type == NetworkType.NR) {
            lteGroup.visibility = View.VISIBLE
            tv(R.id.tvDetailRsrp).text      = tower.displayRsrp
            tv(R.id.tvDetailRsrq).text      = tower.displayRsrq
            tv(R.id.tvDetailBandwidth).text = tower.displayBandwidth
            if (tower.type == NetworkType.LTE) {
                tv(R.id.tvDetailRssnr).text = if (tower.rssnr != Int.MIN_VALUE) "${tower.rssnr} dB" else "—"
                tv(R.id.tvDetailCqi).text   = if (tower.cqi != Int.MIN_VALUE) "${tower.cqi}" else "—"
            }
        } else {
            lteGroup.visibility = View.GONE
        }
    }
}
