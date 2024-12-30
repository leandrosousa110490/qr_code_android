package com.example.qr_code

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.qr_code.ui.theme.Qr_codeTheme
import com.google.zxing.BarcodeFormat 
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.List

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Qr_codeTheme {
                MainScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(activity: ComponentActivity) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationDrawerItem(
                    label = { Text("Gallery") },
                    icon = { Icon(Icons.Default.List, contentDescription = "Gallery") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            activity.startActivity(Intent(activity, GalleryActivity::class.java))
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("QR Code Generator") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                QRCodeGenerator()
            }
        }
    }
}

// Add QR Code content types
sealed class QRContentType {
    object Text : QRContentType()
    object URL : QRContentType()
    object WiFi : QRContentType()
    object Contact : QRContentType()
    object Email : QRContentType()
    object SMS : QRContentType()
}

data class WiFiData(
    var ssid: String = "",
    var password: String = "",
    var type: String = "WPA"
)

data class ContactData(
    var name: String = "",
    var phone: String = "",
    var email: String = "",
    var address: String = ""
)

data class EmailData(
    var email: String = "",
    var subject: String = "",
    var body: String = ""
)

data class SMSData(
    var phone: String = "",
    var message: String = ""
)

@Composable
fun QRCodeGenerator() {
    var selectedType by remember { mutableStateOf<QRContentType>(QRContentType.Text) }
    var expanded by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // State for different content types
    var textContent by remember { mutableStateOf("") }
    var wifiData by remember { mutableStateOf(WiFiData()) }
    var contactData by remember { mutableStateOf(ContactData()) }
    var emailData by remember { mutableStateOf(EmailData()) }
    var smsData by remember { mutableStateOf(SMSData()) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "QR Code Generator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Content Type Selector with enhanced styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column {
                Text(
                    text = "Select QR Code Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getContentTypeName(selectedType),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                listOf(
                    QRContentType.Text to "Plain Text",
                    QRContentType.URL to "Website URL",
                    QRContentType.WiFi to "Wi-Fi Network",
                    QRContentType.Contact to "Contact Card (vCard)",
                    QRContentType.Email to "Email Message",
                    QRContentType.SMS to "SMS Message"
                ).forEach { (type, name) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            selectedType = type
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Input fields in a Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dynamic content input fields based on selected type
                when (selectedType) {
                    QRContentType.Text, QRContentType.URL -> {
                        TextField(
                            value = textContent,
                            onValueChange = { textContent = it },
                            label = { Text(if (selectedType == QRContentType.Text) "Enter text" else "Enter URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    QRContentType.WiFi -> {
                        TextField(
                            value = wifiData.ssid,
                            onValueChange = { wifiData = wifiData.copy(ssid = it) },
                            label = { Text("Network Name (SSID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = wifiData.password,
                            onValueChange = { wifiData = wifiData.copy(password = it) },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Text(
                                text = "Network Type: ${wifiData.type}",
                                modifier = Modifier
                                    .clickable { expanded = true }
                                    .padding(8.dp)
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("WPA", "WEP", "nopass").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            wifiData = wifiData.copy(type = type)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    QRContentType.Contact -> {
                        TextField(
                            value = contactData.name,
                            onValueChange = { contactData = contactData.copy(name = it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = contactData.phone,
                            onValueChange = { contactData = contactData.copy(phone = it) },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = contactData.email,
                            onValueChange = { contactData = contactData.copy(email = it) },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = contactData.address,
                            onValueChange = { contactData = contactData.copy(address = it) },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    QRContentType.Email -> {
                        TextField(
                            value = emailData.email,
                            onValueChange = { emailData = emailData.copy(email = it) },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = emailData.subject,
                            onValueChange = { emailData = emailData.copy(subject = it) },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = emailData.body,
                            onValueChange = { emailData = emailData.copy(body = it) },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    QRContentType.SMS -> {
                        TextField(
                            value = smsData.phone,
                            onValueChange = { smsData = smsData.copy(phone = it) },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = smsData.message,
                            onValueChange = { smsData = smsData.copy(message = it) },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Function to generate content based on type
                fun generateContent(): String {
                    return when (selectedType) {
                        QRContentType.Text -> textContent
                        QRContentType.URL -> textContent
                        QRContentType.WiFi -> """
                            WIFI:T:${wifiData.type};
                            S:${wifiData.ssid};
                            P:${wifiData.password};;
                        """.trimIndent().replace("\n", "")
                        QRContentType.Contact -> """
                            BEGIN:VCARD
                            VERSION:3.0
                            FN:${contactData.name}
                            TEL:${contactData.phone}
                            EMAIL:${contactData.email}
                            ADR:${contactData.address}
                            END:VCARD
                        """.trimIndent()
                        QRContentType.Email -> {
                            val subject = Uri.encode(emailData.subject)
                            val body = Uri.encode(emailData.body)
                            "mailto:${emailData.email}?subject=$subject&body=$body"
                        }
                        QRContentType.SMS -> {
                            val body = Uri.encode(smsData.message)
                            "smsto:${smsData.phone}?body=$body"
                        }
                    }
                }

                // Generate button with enhanced styling
                Button(
                    onClick = { qrBitmap = generateQRCode(generateContent()) },
                    enabled = when (selectedType) {
                        QRContentType.Text, QRContentType.URL -> textContent.isNotEmpty()
                        QRContentType.WiFi -> wifiData.ssid.isNotEmpty() && wifiData.password.isNotEmpty()
                        QRContentType.Contact -> contactData.name.isNotEmpty()
                        QRContentType.Email -> emailData.email.isNotEmpty()
                        QRContentType.SMS -> smsData.phone.isNotEmpty()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Generate QR Code",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // QR Code display with enhanced styling
                qrBitmap?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(8.dp)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { 
                                        fileName = ""
                                        showSaveDialog = true 
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save")
                                }
                                
                                Button(
                                    onClick = { Utils.shareImage(context, bitmap) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Share")
                                }
                            }
                        }
                    }
                }

                // Add this save dialog
                if (showSaveDialog && qrBitmap != null) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        title = { Text("Save QR Code") },
                        text = {
                            Column {
                                Text("Enter a name for your QR code:", modifier = Modifier.padding(bottom = 8.dp))
                                OutlinedTextField(
                                    value = fileName,
                                    onValueChange = { fileName = it },
                                    label = { Text("File Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (fileName.isNotBlank()) {
                                        saveImage(context, qrBitmap!!, selectedType, fileName)
                                        showSaveDialog = false
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun getContentTypeName(type: QRContentType): String {
    return when (type) {
        QRContentType.Text -> "Text"
        QRContentType.URL -> "URL"
        QRContentType.WiFi -> "Wi-Fi"
        QRContentType.Contact -> "Contact"
        QRContentType.Email -> "Email"
        QRContentType.SMS -> "SMS"
    }
}

private fun generateQRCode(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }

    return bitmap
}

private fun saveImage(context: android.content.Context, bitmap: Bitmap, type: QRContentType, customName: String) {
    try {
        val timestamp = System.currentTimeMillis()
        val typeStr = getContentTypeName(type)
        val fileName = "QR_${typeStr}_${customName}_$timestamp.jpg"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            context.contentResolver.let { resolver ->
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = uri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, fileName)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
    }
}