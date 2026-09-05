package com.example.airwave.ui.verify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper
import com.example.airwave.util.QrCodeHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import java.util.concurrent.Executors

/**
 * "Verify - My QR + Scan" bottom sheet.
 *
 * Purely local verification: the My QR tab renders the user's AirWave nickname
 * as a QR on-device, and the Scan tab reads AirWave verification QRs with the
 * camera. No Bluetooth connection, chat, backend or internet is involved.
 */
class VerifyBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "verify_sheet"

        private const val KEY_TAB = "verify_tab"
        private const val KEY_VERIFIED = "verify_verified"

        /** Minimum time between two decode attempts, to keep the analyzer light. */
        private const val DECODE_INTERVAL_MS = 500L
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var layoutMyQr: View
    private lateinit var layoutScan: View
    private lateinit var ivQrCode: ImageView
    private lateinit var tvQrName: TextView
    private lateinit var previewView: PreviewView
    private lateinit var tvScanHint: TextView
    private lateinit var tvScanInvalid: TextView
    private lateinit var layoutScanResult: View
    private lateinit var tvResultName: TextView
    private lateinit var btnScanAnother: MaterialButton
    private lateinit var layoutPermission: View
    private lateinit var tvPermissionMsg: TextView
    private lateinit var btnPermissionAction: MaterialButton
    private lateinit var layoutCameraUnavailable: View

    private var selectedTab = 0
    private var verifiedName: String? = null
    private var permissionDeniedCount = 0

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraBound = false
    private var lastDecodeAttempt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /**
     * Reused scratch buffer for NV21 conversion, so continuous scanning does not
     * allocate a fresh ByteArray on every camera frame. Only touched from the
     * single-threaded [analysisExecutor].
     */
    private var nv21Buffer: ByteArray? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!isAdded) return@registerForActivityResult
        if (granted) {
            startScanner()
        } else {
            onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = savedInstanceState?.getInt(KEY_TAB, 0) ?: 0
        verifiedName = savedInstanceState?.getString(KEY_VERIFIED)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayoutVerify)
        layoutMyQr = view.findViewById(R.id.layoutMyQr)
        layoutScan = view.findViewById(R.id.layoutScan)
        ivQrCode = view.findViewById(R.id.ivQrCode)
        tvQrName = view.findViewById(R.id.tvQrName)
        previewView = view.findViewById(R.id.previewView)
        tvScanHint = view.findViewById(R.id.tvScanHint)
        tvScanInvalid = view.findViewById(R.id.tvScanInvalid)
        layoutScanResult = view.findViewById(R.id.layoutScanResult)
        tvResultName = view.findViewById(R.id.tvResultName)
        btnScanAnother = view.findViewById(R.id.btnScanAnother)
        layoutPermission = view.findViewById(R.id.layoutPermission)
        tvPermissionMsg = view.findViewById(R.id.tvPermissionMsg)
        btnPermissionAction = view.findViewById(R.id.btnPermissionAction)
        layoutCameraUnavailable = view.findViewById(R.id.layoutCameraUnavailable)

        view.findViewById<ImageButton>(R.id.btnVerifyClose).setOnClickListener { dismiss() }
        btnScanAnother.setOnClickListener {
            verifiedName = null
            startScannerIfPossible()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        renderMyQr()
        // Select triggers the listener, which switches the visible content.
        tabLayout.getTabAt(selectedTab)?.select()
    }

    override fun onStart() {
        super.onStart()
        val d = dialog
        if (d is BottomSheetDialog) {
            val behavior = d.behavior
            // Open about half the screen, swipe-down dismissible, native feel.
            behavior.isHideable = true
            behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5f).toInt()
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_TAB, selectedTab)
        outState.putString(KEY_VERIFIED, verifiedName)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        releaseScanner()
        super.onDestroyView()
    }

    override fun onDestroy() {
        analysisExecutor.shutdown()
        super.onDestroy()
    }

    // ---------------- Tab switching ----------------

    private fun showTab(position: Int) {
        selectedTab = position
        val showQr = position == 0
        layoutMyQr.visibility = if (showQr) View.VISIBLE else View.GONE
        layoutScan.visibility = if (showQr) View.GONE else View.VISIBLE
        if (showQr) {
            releaseScanner()
        } else if (verifiedName != null) {
            showVerifiedUi()
        } else {
            startScannerIfPossible()
        }
    }

    // ---------------- My QR ----------------

    private fun renderMyQr() {
        // Same identity the rest of the app uses - never re-created here.
        val nickname = PreferencesHelper.nickname.trim().ifBlank { "AirWave User" }
        tvQrName.text = nickname
        val payload = QrCodeHelper.buildVerifyPayload(nickname)
        val sizePx = (resources.displayMetrics.density * 384).toInt().coerceAtLeast(384)
        val bitmap = QrCodeHelper.generateQr(payload, sizePx)
        if (bitmap != null) ivQrCode.setImageBitmap(bitmap)
    }

    // ---------------- Scan: states ----------------

    private fun startScannerIfPossible() {
        if (!isAdded) return
        val ctx = requireContext()
        if (!ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            showCameraUnavailable()
            return
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionRequired()
            // Only requested when the user actually opens the Scan tab.
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        startScanner()
    }

    private fun startScanner() {
        if (!isAdded || cameraBound) return
        val ctx = requireContext()
        showScannerUi()

        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            try {
                val provider = future.get()
                if (!isAdded || view == null) {
                    provider.unbindAll()
                    return@addListener
                }
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image -> analyzeFrame(image) }

                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                cameraProvider = provider
                cameraBound = true
            } catch (e: Exception) {
                if (isAdded) showCameraUnavailable()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun releaseScanner() {
        cameraBound = false
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignore
        }
        cameraProvider = null
    }

    private fun showScannerUi() {
        layoutPermission.visibility = View.GONE
        layoutScanResult.visibility = View.GONE
        layoutCameraUnavailable.visibility = View.GONE
        tvScanInvalid.visibility = View.GONE
        previewView.visibility = View.VISIBLE
        tvScanHint.visibility = View.VISIBLE
    }

    private fun showPermissionRequired() {
        layoutPermission.visibility = View.VISIBLE
        layoutScanResult.visibility = View.GONE
        layoutCameraUnavailable.visibility = View.GONE
        tvScanInvalid.visibility = View.GONE
        tvPermissionMsg.visibility = View.GONE
        btnPermissionAction.text = getString(R.string.verify_permission_grant)
        btnPermissionAction.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun onPermissionDenied() {
        if (!isAdded) return
        permissionDeniedCount++
        // First denial: offer a normal re-request. Later denials with no
        // rationale shown mean "don't ask again" - send the user to settings.
        val permanentlyDenied = permissionDeniedCount > 1 &&
            !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        layoutPermission.visibility = View.VISIBLE
        layoutScanResult.visibility = View.GONE
        layoutCameraUnavailable.visibility = View.GONE
        tvScanInvalid.visibility = View.GONE
        tvPermissionMsg.visibility = View.VISIBLE
        tvPermissionMsg.text = getString(R.string.verify_permission_denied_msg)
        btnPermissionAction.text = if (permanentlyDenied) {
            getString(R.string.verify_open_settings)
        } else {
            getString(R.string.verify_permission_grant)
        }
        btnPermissionAction.setOnClickListener {
            if (permanentlyDenied) openAppSettings() else permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCameraUnavailable() {
        releaseScanner()
        if (!isAdded) return
        layoutCameraUnavailable.visibility = View.VISIBLE
        layoutPermission.visibility = View.GONE
        layoutScanResult.visibility = View.GONE
        tvScanInvalid.visibility = View.GONE
        previewView.visibility = View.GONE
        tvScanHint.visibility = View.GONE
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${requireContext().packageName}")
                )
            )
        } catch (e: Exception) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ---------------- Scan: frame analysis ----------------

    private fun analyzeFrame(image: ImageProxy) {
        try {
            val now = SystemClock.uptimeMillis()
            if (now - lastDecodeAttempt < DECODE_INTERVAL_MS) return
            lastDecodeAttempt = now

            val nv21 = yuvToNv21(image) ?: return
            val text = QrCodeHelper.decodeQrFromNv21(nv21, image.width, image.height)
            if (text != null) {
                val name = QrCodeHelper.parseVerifyPayload(text)
                mainHandler.post {
                    if (!isAdded) return@post
                    if (name != null) {
                        showVerified(name)
                    } else {
                        showInvalidQr()
                    }
                }
            }
        } finally {
            image.close()
        }
    }

    private fun yuvToNv21(image: ImageProxy): ByteArray? {
        return try {
            val width = image.width
            val height = image.height
            val size = width * height * 3 / 2
            var output = nv21Buffer
            if (output == null || output.size != size) {
                output = ByteArray(size)
                nv21Buffer = output
            }

            val yPlane = image.planes[0]
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            val yBuffer = yPlane.buffer
            var position = 0
            for (row in 0 until height) {
                val rowStart = row * yRowStride
                for (col in 0 until width) {
                    output[position++] = yBuffer[rowStart + col * yPixelStride]
                }
            }

            val uPlane = image.planes[1]
            val vPlane = image.planes[2]
            val uRowStride = uPlane.rowStride
            val uPixelStride = uPlane.pixelStride
            val vRowStride = vPlane.rowStride
            val vPixelStride = vPlane.pixelStride
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer
            val halfWidth = width / 2
            val halfHeight = height / 2
            for (row in 0 until halfHeight) {
                val uRowStart = row * uRowStride
                val vRowStart = row * vRowStride
                for (col in 0 until halfWidth) {
                    output[position++] = vBuffer[vRowStart + col * vPixelStride]
                    output[position++] = uBuffer[uRowStart + col * uPixelStride]
                }
            }
            output
        } catch (e: Exception) {
            null
        }
    }

    private fun showVerified(name: String) {
        verifiedName = name
        releaseScanner()
        showVerifiedUi()
    }

    private fun showVerifiedUi() {
        val name = verifiedName
        if (name == null) {
            startScannerIfPossible()
            return
        }
        tvResultName.text = name
        layoutScanResult.visibility = View.VISIBLE
        layoutPermission.visibility = View.GONE
        layoutCameraUnavailable.visibility = View.GONE
        tvScanInvalid.visibility = View.GONE
        previewView.visibility = View.GONE
        tvScanHint.visibility = View.GONE
    }

    private fun showInvalidQr() {
        if (!isAdded) return
        // Non-AirWave QR: tell the user, keep scanning for a valid one.
        tvScanInvalid.visibility = View.VISIBLE
    }
}