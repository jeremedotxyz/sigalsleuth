package com.celltracker.ui

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.celltracker.R
import com.celltracker.data.CellTowerData
import com.celltracker.data.NetworkType
import com.celltracker.databinding.ActivityMainBinding
import com.celltracker.scanner.CellScanner
import com.celltracker.scanner.ScannerService
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanner: CellScanner
    private lateinit var adapter: CellTowerAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var scanCount = 0
    private var lastLocation: Location? = null
    private var radarAnimator: ValueAnimator? = null

    private val SCAN_INTERVAL = 3_000L

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startScanning()
        } else {
            showPermissionDenied()
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                performScan()
                handler.postDelayed(this, SCAN_INTERVAL)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanner = CellScanner(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupRecyclerView()
        setupButtons()
        updateScanStatus("READY", false)
    }

    private fun setupRecyclerView() {
        adapter = CellTowerAdapter { tower -> showTowerDetail(tower) }
        binding.rvTowers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            itemAnimator?.changeDuration = 150
        }
    }

    private fun setupButtons() {
        binding.btnScan.setOnClickListener {
            if (isScanning) stopScanning() else checkPermissionsAndScan()
        }
        binding.btnBackground.setOnClickListener {
            toggleBackgroundService()
        }
    }

    private fun checkPermissionsAndScan() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startScanning()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startScanning() {
        isScanning = true
        scanCount = 0
        binding.btnScan.text = "■ STOP"
        binding.btnScan.setBackgroundColor(ContextCompat.getColor(this, R.color.stop_red))
        updateScanStatus("SCANNING", true)
        startRadarAnimation()
        fetchLocation()
        handler.post(scanRunnable)
    }

    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacks(scanRunnable)
        binding.btnScan.text = "▶ SCAN"
        binding.btnScan.setBackgroundColor(ContextCompat.getColor(this, R.color.scan_green))
        updateScanStatus("IDLE — ${adapter.itemCount} towers cached", false)
        stopRadarAnimation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                lastLocation = loc
                updateLocationDisplay(loc)
            }
        }
    }

    private fun performScan() {
        scanCount++
        val startTime = System.currentTimeMillis()
        val towers = scanner.scan()
        val elapsed = System.currentTimeMillis() - startTime

        runOnUiThread {
            adapter.submitList(towers.toList())
            updatePrimarySignal(towers)
            updateStats(towers.size, elapsed)
            animateScanPulse()
            fetchLocation()
        }
    }

    private fun updatePrimarySignal(towers: List<CellTowerData>) {
        val serving = towers.firstOrNull { it.isRegistered } ?: towers.firstOrNull()
        if (serving != null) {
            binding.tvSignalValue.text = "${serving.signalStrength}"
            binding.tvSignalUnit.text = "dBm"
            binding.tvNetworkTypePrimary.text = serving.type.label
            binding.tvOperatorPrimary.text = serving.operatorName.ifBlank { "Unknown Operator" }

            val qualityColor = when (serving.signalStrength) {
                in -65..0    -> ContextCompat.getColor(this, R.color.signal_excellent)
                in -80..-66  -> ContextCompat.getColor(this, R.color.signal_good)
                in -95..-81  -> ContextCompat.getColor(this, R.color.signal_fair)
                else         -> ContextCompat.getColor(this, R.color.signal_poor)
            }
            binding.tvSignalValue.setTextColor(qualityColor)

            val quality = serving.signalQuality.label
            binding.tvSignalQuality.text = quality
            binding.tvSignalQuality.setTextColor(qualityColor)

            // Signal meter bar
            val pct = ((serving.signalStrength + 120).coerceIn(0, 60).toFloat() / 60f)
            binding.pbPrimarySignal.progress = (pct * 100).toInt()
        } else {
            binding.tvSignalValue.text = "—"
            binding.tvNetworkTypePrimary.text = "NO SIGNAL"
            binding.tvOperatorPrimary.text = "No towers found"
            binding.tvSignalQuality.text = ""
        }
    }

    private fun updateLocationDisplay(location: Location?) {
        if (location != null) {
            val lat = String.format("%.5f", location.latitude)
            val lon = String.format("%.5f", location.longitude)
            val acc = String.format("%.0f", location.accuracy)
            binding.tvLocation.text = "$lat°N  $lon°W"
            binding.tvLocationAccuracy.text = "±${acc}m"
        } else {
            binding.tvLocation.text = "GPS acquiring…"
            binding.tvLocationAccuracy.text = ""
        }
    }

    private fun updateStats(towerCount: Int, elapsed: Long) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        binding.tvLastScan.text = "LAST SCAN  ${sdf.format(Date())}"
        binding.tvTowerCount.text = "$towerCount"
        binding.tvTowerLabel.text = if (towerCount == 1) "TOWER" else "TOWERS"
        binding.tvScanCount.text = "SCAN #$scanCount"
        binding.tvScanTime.text = "${elapsed}ms"
    }

    private fun updateScanStatus(msg: String, active: Boolean) {
        binding.tvScanStatus.text = msg
        binding.tvStatusDot.setTextColor(
            ContextCompat.getColor(this,
                if (active) R.color.scan_green else R.color.unknown_gray)
        )
    }

    private fun startRadarAnimation() {
        radarAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                binding.ivRadar.rotation = it.animatedValue as Float
            }
            start()
        }
        binding.ivRadar.visibility = View.VISIBLE
    }

    private fun stopRadarAnimation() {
        radarAnimator?.cancel()
        binding.ivRadar.visibility = View.INVISIBLE
    }

    private fun animateScanPulse() {
        binding.cardPrimary.animate()
            .scaleX(1.01f).scaleY(1.01f).setDuration(80)
            .withEndAction {
                binding.cardPrimary.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
    }

    private fun toggleBackgroundService() {
        val intent = Intent(this, ScannerService::class.java)
        if (isServiceRunning()) {
            intent.action = ScannerService.ACTION_STOP
            binding.btnBackground.text = "BG OFF"
        } else {
            intent.action = ScannerService.ACTION_START
            binding.btnBackground.text = "BG ON"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                return
            }
        }
        startService(intent)
    }

    private fun isServiceRunning(): Boolean {
        // Simplified check — in production use a bound service or shared pref flag
        return false
    }

    private fun showTowerDetail(tower: CellTowerData) {
        val dialog = TowerDetailDialog(tower)
        dialog.show(supportFragmentManager, "tower_detail")
    }

    private fun showPermissionDenied() {
        binding.tvScanStatus.text = "PERMISSIONS REQUIRED"
        binding.tvScanStatus.setTextColor(ContextCompat.getColor(this, R.color.stop_red))
    }

    override fun onResume() {
        super.onResume()
        if (isScanning) fetchLocation()
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        radarAnimator?.cancel()
        super.onDestroy()
    }
}
