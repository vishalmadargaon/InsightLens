package com.example.insightlens // <--- CHANGE THIS if your package is different

import android.graphics.Bitmap
import android.graphics.Matrix
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- 1. TTS & LANGUAGE SETUP ---
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val languages = mapOf(
        "English" to Locale.US,
        "Hindi" to Locale("hi", "IN"),
        "Kannada" to Locale("kn", "IN"),
        "Telugu" to Locale("te", "IN"),
        "Tamil" to Locale("ta", "IN")
    )

    var selectedLangName by remember { mutableStateOf("English") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.US
            }
        }
    }

    // --- 2. STATE ---
    val modes = listOf("Normal", "Food", "Text", "Code")
    var selectedMode by remember { mutableStateOf("Normal") }
    var aiResponse by remember { mutableStateOf("Ready to analyze...") }
    var isLoading by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // A. CAMERA LAYER
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Binding failed", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // B. LANGUAGE DROPDOWN (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(50))
                    .clickable { isDropdownExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Lang", tint = Color(0xFF4DE7FF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = selectedLangName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.background(Color.DarkGray)
            ) {
                languages.forEach { (name, locale) ->
                    DropdownMenuItem(
                        text = { Text(name, color = Color.White) },
                        onClick = {
                            selectedLangName = name
                            isDropdownExpanded = false
                            val result = tts?.setLanguage(locale)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Toast.makeText(context, "Voice not found. Check Settings.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // C. RESULT BOX (Top Center)
        if (aiResponse.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "INSIGHT ($selectedMode • $selectedLangName)",
                        color = Color(0xFF4DE7FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiResponse,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }

        // D. LOADING
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(60.dp),
                color = Color(0xFF4DE7FF),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        // E. CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                .padding(bottom = 32.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { mode ->
                    val isSelected = (selectedMode == mode)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = mode },
                        label = { Text(mode) },
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4DE7FF),
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = Color.White.copy(0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LargeFloatingActionButton(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        aiResponse = "Thinking in $selectedLangName..."

                        val basePrompt = when(selectedMode) {
                            "Food" -> "Estimate calories. Brief."
                            "Text" -> "Read text exactly."
                            "Code" -> "Explain code briefly."
                            else -> "Identify object."
                        }

                        // TURBO PROMPT: Limit word count for speed
                        val finalPrompt = "$basePrompt Respond in $selectedLangName. Max 20 words."

                        captureAndAnalyze(imageCapture, context, finalPrompt) { result ->
                            aiResponse = result
                            isLoading = false
                            tts?.speak(result, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(80.dp).border(4.dp, Color.Gray.copy(0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Capture", modifier = Modifier.size(40.dp))
            }
        }
    }
}

private fun captureAndAnalyze(
    imageCapture: ImageCapture,
    context: android.content.Context,
    prompt: String,
    onResult: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val originalBitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees.toFloat())
            image.close()

            // TURBO RESIZE: 360px for maximum speed
            val scaledBitmap = Bitmap.createScaledBitmap(
                originalBitmap, 360,
                (originalBitmap.height * (360.0 / originalBitmap.width)).toInt(), true
            )

            kotlinx.coroutines.GlobalScope.launch {
                val responseText = GeminiHelper.analyzeImage(scaledBitmap, prompt)
                launch(kotlinx.coroutines.Dispatchers.Main) { onResult(responseText) }
            }
        }
        override fun onError(exception: ImageCaptureException) { onResult("Error: ${exception.message}") }
    })
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}