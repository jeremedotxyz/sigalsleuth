package com.celltracker.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.celltracker.R
import com.celltracker.data.CellTowerData
import com.celltracker.data.NetworkType
import com.celltracker.databinding.ItemCellTowerBinding

class CellTowerAdapter(
    private val onItemClick: (CellTowerData) -> Unit
) : ListAdapter<CellTowerData, CellTowerAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCellTowerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val b: ItemCellTowerBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(tower: CellTowerData) {
            b.root.setOnClickListener { onItemClick(tower) }

            // Network type badge
            b.tvNetworkType.text = tower.type.label
            val typeColor = when (tower.type) {
                NetworkType.NR     -> R.color.nr_green
                NetworkType.LTE    -> R.color.lte_blue
                NetworkType.WCDMA  -> R.color.wcdma_orange
                NetworkType.GSM    -> R.color.gsm_red
                NetworkType.CDMA   -> R.color.cdma_purple
                NetworkType.UNKNOWN -> R.color.unknown_gray
            }
            val color = ContextCompat.getColor(b.root.context, typeColor)
            b.tvNetworkType.backgroundTintList = ColorStateList.valueOf(color)

            // Registered indicator
            b.ivRegistered.alpha = if (tower.isRegistered) 1f else 0.25f
            b.tvRegisteredLabel.text = if (tower.isRegistered) "SERVING" else "NEIGHBOR"

            // Signal strength
            b.tvSignalDbm.text = "${tower.signalStrength} dBm"
            b.tvSignalBars.text = tower.signalBars

            val signalColor = when (tower.signalStrength) {
                in -70..0    -> ContextCompat.getColor(b.root.context, R.color.signal_excellent)
                in -85..-71  -> ContextCompat.getColor(b.root.context, R.color.signal_good)
                in -100..-86 -> ContextCompat.getColor(b.root.context, R.color.signal_fair)
                else         -> ContextCompat.getColor(b.root.context, R.color.signal_poor)
            }
            b.tvSignalDbm.setTextColor(signalColor)
            b.tvSignalBars.setTextColor(signalColor)

            // Signal bar progress
            b.pbSignal.progress = ((tower.signalStrength + 120).coerceIn(0, 60) * 100 / 60)

            // Operator / MCC-MNC
            b.tvOperator.text = tower.operatorName.ifBlank { "Unknown" }
            b.tvMccMnc.text = "MCC ${tower.mcc} · MNC ${tower.mnc}"

            // Cell identity
            b.tvCellId.text = "CID: ${tower.displayCellId}"
            b.tvLac.text   = "LAC/TAC: ${tower.displayLac}"
            b.tvPci.text   = "PCI: ${tower.displayPci}"
            b.tvEarfcn.text = "ARFCN: ${tower.displayEarfcn}"

            // LTE/NR extras
            if (tower.rsrp != Int.MIN_VALUE) {
                b.tvRsrp.text = "RSRP: ${tower.displayRsrp}"
                b.tvRsrq.text = "RSRQ: ${tower.displayRsrq}"
                b.layoutLteExtras.visibility = android.view.View.VISIBLE
            } else {
                b.layoutLteExtras.visibility = android.view.View.GONE
            }

            // Bandwidth
            if (tower.bandwidth > 0) {
                b.tvBandwidth.text = "BW: ${tower.displayBandwidth}"
                b.tvBandwidth.visibility = android.view.View.VISIBLE
            } else {
                b.tvBandwidth.visibility = android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CellTowerData>() {
        override fun areItemsTheSame(a: CellTowerData, b: CellTowerData) =
            a.cellId == b.cellId && a.type == b.type
        override fun areContentsTheSame(a: CellTowerData, b: CellTowerData) =
            a.signalStrength == b.signalStrength && a.isRegistered == b.isRegistered
    }
}
