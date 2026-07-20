package uz.mybudget.app.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.mybudget.app.data.backup.ExcelBackupManager
import uz.mybudget.app.data.repository.TransactionRepository
import uz.mybudget.app.security.PinManager

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember(context) { TransactionRepository(context) }
    val backupManager = remember(context) { ExcelBackupManager(context) }
    val pinManager = remember(context) { PinManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val message = runCatching {
                    val transactions = repository.getAll()
                    backupManager.export(uri, transactions)
                    "${transactions.size} ta operatsiya Excel faylga saqlandi"
                }.getOrElse { "Eksport xatosi: ${it.localizedMessage ?: "noma’lum xato"}" }
                busy = false
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val message = runCatching {
                    val transactions = backupManager.import(uri)
                    val count = repository.importAll(transactions)
                    "$count ta operatsiya xotiraga kiritildi"
                }.getOrElse { "Import xatosi: ${it.localizedMessage ?: "noma’lum xato"}" }
                busy = false
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    if (showPinDialog) {
        ChangePinDialog(
            onDismiss = { showPinDialog = false },
            onSave = { currentPin, newPin ->
                if (pinManager.changePin(currentPin, newPin)) {
                    showPinDialog = false
                    scope.launch { snackbarHostState.showSnackbar("PIN kod yangilandi") }
                    true
                } else {
                    false
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SOZLAMALAR",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Ma’lumotlar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    SettingsCard(
                        title = "Excel’ga eksport",
                        description = "Barcha kirim-chiqimlarni .xlsx fayl qilib, xohlagan papkaga saqlang.",
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                    ) {
                        Button(
                            onClick = { exportLauncher.launch(ExcelBackupManager.defaultFileName()) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Excel fayl yaratish") }
                    }
                }
                item {
                    SettingsCard(
                        title = "Eski ma’lumotlarni import qilish",
                        description = "Oldingi .xlsx yoki .csv faylni tanlab, operatsiyalarni telefon xotirasiga kiriting.",
                        icon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                    ) {
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        XLSX_MIME,
                                        "application/vnd.ms-excel",
                                        "text/csv",
                                        "text/comma-separated-values"
                                    )
                                )
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Fayldan tiklash") }
                    }
                }
                item {
                    Text(
                        "Xavfsizlik",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    SettingsCard(
                        title = "PIN kodni almashtirish",
                        description = "Email va parol kerak emas. PIN telefonning o‘zida xeshlangan holda saqlanadi.",
                        icon = { Icon(Icons.Default.LockReset, contentDescription = null) }
                    ) {
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("PINni o‘zgartirish") }
                    }
                }
            }

            if (busy) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    tonalElevation = 8.dp
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(22.dp).size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    action: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { icon() }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN kodni almashtirish") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinInput("Hozirgi PIN", currentPin) {
                    currentPin = it
                    error = null
                }
                PinInput("Yangi PIN", newPin) {
                    newPin = it
                    error = null
                }
                PinInput("Yangi PINni takrorlang", confirmation) {
                    confirmation = it
                    error = null
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    newPin.length !in 4..6 -> error = "Yangi PIN 4–6 raqam bo‘lishi kerak"
                    newPin != confirmation -> error = "Yangi PIN kodlar mos emas"
                    !onSave(currentPin, newPin) -> error = "Hozirgi PIN noto‘g‘ri"
                }
            }) { Text("Saqlash") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } }
    )
}

@Composable
private fun PinInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
