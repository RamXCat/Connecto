package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.api.DeviceApiClient
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeviceViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: DeviceViewModel,
    onDeviceFound: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Scan QR Code",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                ScannerView(
                    viewModel = viewModel,
                    onDeviceFound = onDeviceFound,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PermissionRequestView(
                    onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PermissionRequestView(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(TealAccent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Stitch uses the camera to scan connection QR codes displayed on your laptop.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = DarkBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Grant Camera Permission", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ScannerView(
    viewModel: DeviceViewModel,
    onDeviceFound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var scanStatus by remember { mutableStateOf("Point camera at laptop terminal QR code") }
    var isChecking by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Setup Preview
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        // Setup Analyzer
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val analyzer = QrCodeAnalyzer { qrValue ->
                            if (!isChecking) {
                                isChecking = true
                                coroutineScope.launch {
                                    parseAndValidateQrJson(
                                        qrJson = qrValue,
                                        viewModel = viewModel,
                                        onStatusUpdate = { scanStatus = it },
                                        onSuccess = {
                                            onDeviceFound()
                                        },
                                        onFailure = {
                                            isChecking = false
                                        }
                                    )
                                }
                            }
                        }

                        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Camera binding failed", e)
                        scanStatus = "Camera setup failed: ${e.localizedMessage}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay & HUD
        ScanFrameOverlay()

        // Scan hud status text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 60.dp, start = 24.dp, end = 24.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scanStatus,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull()
                    qrCode?.rawValue?.let {
                        onQrCodeScanned(it)
                    }
                }
                .addOnFailureListener {
                    // Ignore errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

suspend fun parseAndValidateQrJson(
    qrJson: String,
    viewModel: DeviceViewModel,
    onStatusUpdate: (String) -> Unit,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    try {
        onStatusUpdate("Reading config payload...")
        val json = JSONObject(qrJson)
        val service = json.optString("service", "")
        
        if (!service.equals("stitch", ignoreCase = true) && !service.equals("connecto", ignoreCase = true)) {
            onStatusUpdate("❌ Invalid QR (Not Stitch QR)")
            delay(2000)
            onFailure()
            return
        }

        val deviceName = json.getString("device")
        val ip = json.getString("ip")
        val port = json.getInt("port")
        val token = json.getString("token")
        
        onStatusUpdate("⚡ Connecting to '$deviceName'...")
        
        val fullIpAddress = "$ip:$port"

        viewModel.addAndActivateScannedDevice(
            name = deviceName,
            ipAddress = fullIpAddress,
            rawToken = token,
            type = "Laptop"
        ) { ok, msg ->
            if (ok) {
                onStatusUpdate("✅ Connected & Saved!")
                onSuccess()
            } else {
                onStatusUpdate(msg)
                onFailure()
            }
        }
    } catch (e: Exception) {
        onStatusUpdate("❌ Failed to parse QR config.")
        delay(2000)
        onFailure()
    }
}

@Composable
fun ScanFrameOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameSize = 250.dp.toPx()
        val frameLeft = (size.width - frameSize) / 2
        val frameTop = (size.height - frameSize) / 2
        
        // Draw dark translucent layer over everything
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Carve out a clear center square for our scanner target frame
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameSize, frameSize),
            blendMode = BlendMode.Clear
        )

        // Draw a neat bounding outline frame
        drawRect(
            color = TealAccent.copy(alpha = 0.3f),
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameSize, frameSize),
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw customized L-shaped high-tech corners
        val cornerLength = 30.dp.toPx()
        val cornerThickness = 4.dp.toPx()

        // Top Left corner
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft - 1.dp.toPx(), frameTop - 1.dp.toPx()),
            size = Size(cornerLength, cornerThickness)
        )
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft - 1.dp.toPx(), frameTop - 1.dp.toPx()),
            size = Size(cornerThickness, cornerLength)
        )

        // Top Right corner
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft + frameSize - cornerLength + 1.dp.toPx(), frameTop - 1.dp.toPx()),
            size = Size(cornerLength, cornerThickness)
        )
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft + frameSize - cornerThickness + 1.dp.toPx(), frameTop - 1.dp.toPx()),
            size = Size(cornerThickness, cornerLength)
        )

        // Bottom Left corner
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft - 1.dp.toPx(), frameTop + frameSize - cornerThickness + 1.dp.toPx()),
            size = Size(cornerLength, cornerThickness)
        )
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft - 1.dp.toPx(), frameTop + frameSize - cornerLength + 1.dp.toPx()),
            size = Size(cornerThickness, cornerLength)
        )

        // Bottom Right corner
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft + frameSize - cornerLength + 1.dp.toPx(), frameTop + frameSize - cornerThickness + 1.dp.toPx()),
            size = Size(cornerLength, cornerThickness)
        )
        drawRect(
            color = TealAccent,
            topLeft = Offset(frameLeft + frameSize - cornerThickness + 1.dp.toPx(), frameTop + frameSize - cornerLength + 1.dp.toPx()),
            size = Size(cornerThickness, cornerLength)
        )
    }
}
