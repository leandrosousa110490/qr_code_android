package com.example.qr_code

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRGalleryScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGalleryScreen() {
    val context = LocalContext.current
    var qrImages by remember { mutableStateOf(listOf<QRImage>()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        qrImages = loadQRImages(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Gallery") },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (qrImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No QR codes saved yet")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(qrImages) { qrImage ->
                    QRImageCard(
                        qrImage = qrImage,
                        onImageRenamed = { refreshTrigger++ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRImageCard(
    qrImage: QRImage,
    onImageRenamed: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showDialog = true },
                    onLongPress = { showOptions = true }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = qrImage.bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(120.dp)
                    .padding(4.dp)
            )
            Text(
                text = extractQRType(qrImage.name),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Full screen dialog
    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    bitmap = qrImage.bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = { showDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        }
    }

    // Options dialog
    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("QR Code Options") },
            text = { Text("Choose an action") },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            Utils.shareImage(context, qrImage.bitmap)
                            showOptions = false
                        }
                    ) {
                        Icon(Icons.Default.Share, "Share")
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                    TextButton(
                        onClick = {
                            deleteImage(context, qrImage.path)
                            showOptions = false
                        }
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        )
    }
}

data class QRImage(
    val bitmap: android.graphics.Bitmap,
    val name: String,
    val path: String,
    val type: String = "Unknown"
)

private fun loadQRImages(context: android.content.Context): List<QRImage> {
    val images = mutableListOf<QRImage>()
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    
    picturesDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("QR_") && file.name.endsWith(".jpg")) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap?.let {
                    images.add(QRImage(it, file.name, file.absolutePath))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    return images.sortedByDescending { it.path }
}

private fun deleteImage(context: android.content.Context, path: String) {
    try {
        File(path).delete()
        Toast.makeText(context, "Image deleted successfully", Toast.LENGTH_SHORT).show()
        // Refresh the gallery
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(path))))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
    }
}

// Add this helper function to extract QR type from filename
private fun extractQRType(fileName: String): String {
    val regex = "QR_(.+?)_\\d+\\.jpg".toRegex()
    return regex.find(fileName)?.groupValues?.get(1) ?: "Unknown"
}
