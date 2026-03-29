package com.medialert.presentation.scan

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.medialert.R
import com.medialert.data.remote.GeminiMedication
import com.medialert.presentation.navigation.ScanMode
import com.medialert.utils.AppConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    scanMode: ScanMode,
    onNavigateBack: () -> Unit,
    onScanComplete: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Saved) onScanComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (scanMode) {
                            ScanMode.PRESCRIPTION -> stringResource(R.string.scan_prescription)
                            ScanMode.BOX -> stringResource(R.string.scan_box)
                            ScanMode.DOSE -> stringResource(R.string.scan_dose)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is ScanUiState.Idle, is ScanUiState.Capturing -> {
                    if (!cameraPermission.status.isGranted) {
                        CameraPermissionRequest(onRequest = { cameraPermission.launchPermissionRequest() })
                    } else {
                        CameraCapture(
                            onImageCaptured = { imagePath ->
                                viewModel.analyzeImage(imagePath, scanMode)
                            }
                        )
                    }
                }
                is ScanUiState.Analyzing -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.scan_analyzing))
                    }
                }
                is ScanUiState.PrescriptionResult -> {
                    val result = (uiState as ScanUiState.PrescriptionResult).result
                    PrescriptionResultContent(
                        medications = result.medicamentos,
                        onSave = { viewModel.saveMedicationsFromPrescription(it) },
                        onRetry = { viewModel.reset() }
                    )
                }
                is ScanUiState.BoxResult -> {
                    val result = (uiState as ScanUiState.BoxResult).result
                    BoxResultContent(
                        boxData = result,
                        onAccept = { onScanComplete() },
                        onRetry = { viewModel.reset() }
                    )
                }
                is ScanUiState.DoseResult -> {
                    val result = (uiState as ScanUiState.DoseResult).result
                    DoseResultContent(
                        doseData = result,
                        onConfirm = { onScanComplete() },
                        onRetry = { viewModel.reset() }
                    )
                }
                is ScanUiState.Error -> {
                    ErrorContent(
                        message = (uiState as ScanUiState.Error).message,
                        onRetry = { viewModel.reset() }
                    )
                }
                is ScanUiState.Saved -> { /* handled via LaunchedEffect */ }
            }
        }
    }
}

@Composable
private fun CameraCapture(onImageCaptured: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (e: Exception) {
                        Log.e("CameraX", "Bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture button
        FloatingActionButton(
            onClick = {
                val photoFile = createImageFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(e: ImageCaptureException) {
                            Log.e("CameraX", "Capture failed", e)
                        }
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onImageCaptured(photoFile.absolutePath)
                        }
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.action_capture))
        }
    }
}

@Composable
private fun PrescriptionResultContent(
    medications: List<GeminiMedication>,
    onSave: (List<GeminiMedication>) -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.scan_result_prescription, medications.size),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        medications.forEach { med ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(med.nombre, style = MaterialTheme.typography.titleSmall)
                    Text("${med.dosis} · ${med.frecuencia}", style = MaterialTheme.typography.bodySmall)
                    if (med.confianza < AppConstants.GEMINI_LOW_CONFIDENCE_THRESHOLD) {
                        Text(
                            stringResource(R.string.low_confidence),
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry, Modifier.weight(1f)) { Text(stringResource(R.string.action_retry)) }
            Button(onClick = { onSave(medications) }, Modifier.weight(1f)) { Text(stringResource(R.string.action_save_all)) }
        }
    }
}

@Composable
private fun BoxResultContent(
    boxData: com.medialert.data.remote.GeminiBoxResponse,
    onAccept: () -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.scan_result_box), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ResultRow(stringResource(R.string.field_name), boxData.nombreComercial, boxData.confianza)
                ResultRow(stringResource(R.string.field_active_ingredient), boxData.principioActivo, boxData.confianza)
                ResultRow(stringResource(R.string.field_concentration), boxData.concentracion, boxData.confianza)
                ResultRow(stringResource(R.string.field_form), boxData.formaFarmaceutica, boxData.confianza)
                boxData.laboratorio?.let { ResultRow(stringResource(R.string.field_laboratory), it, 1.0) }
                boxData.unidadesCaja?.let { ResultRow(stringResource(R.string.field_units), it.toString(), 1.0) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry, Modifier.weight(1f)) { Text(stringResource(R.string.action_retry)) }
            Button(onClick = onAccept, Modifier.weight(1f)) { Text(stringResource(R.string.action_accept)) }
        }
    }
}

@Composable
private fun DoseResultContent(
    doseData: com.medialert.data.remote.GeminiDoseResponse,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    val matchColor = if (doseData.coincideConEsperado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = matchColor.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (doseData.coincideConEsperado) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        tint = matchColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (doseData.coincideConEsperado) stringResource(R.string.dose_match_ok) else stringResource(R.string.dose_match_fail),
                        style = MaterialTheme.typography.titleSmall,
                        color = matchColor
                    )
                }
                if (!doseData.medicamentosDetectados.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(doseData.medicamentosDetectados.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
                doseData.observaciones?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry, Modifier.weight(1f)) { Text(stringResource(R.string.action_retry)) }
            Button(onClick = onConfirm, Modifier.weight(1f)) { Text(stringResource(R.string.action_confirm)) }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, confidence: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = if (confidence < AppConstants.GEMINI_LOW_CONFIDENCE_THRESHOLD) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.scan_error), style = MaterialTheme.typography.titleMedium)
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, null, Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.camera_permission_desc), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text(stringResource(R.string.action_grant_permission)) }
    }
}

private fun createImageFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val storageDir = context.filesDir
    return File(storageDir, "MEDIALERT_$timestamp.jpg")
}
