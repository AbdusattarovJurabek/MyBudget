package uz.mybudget.app.presentation.transaction

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.mybudget.app.data.model.Transaction
import uz.mybudget.app.data.repository.TransactionRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(type: String, onSaved: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    
    val isIncome = type == "income"
    val initialCategory = if (isIncome) "Oylik maosh" else "Oziq-ovqat"
    var category by remember { mutableStateOf(initialCategory) }
    var note by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val categories = if (isIncome) {
        listOf("Oylik maosh", "Biznes", "Qo‘shimcha ish", "Keshbek", "Sovg‘a", "Dividendlar", "Ijara", "Sotuv", "Boshqa")
    } else {
        listOf("Oziq-ovqat", "Kafe-restoran", "Transport", "Benzin/Mashina", "Uy-ro‘zg‘or", "Internet va aloqa", "Kommunal to‘lovlar", "Kiyim-kechak", "Sog‘liq", "Ta'lim", "O'yin-kulgi", "Xayriya", "Kredit/Qarz", "Sport", "Maishiy texnika", "Boshqa")
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            if (isIncome) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isIncome) "Kirim qo‘shish" else "Chiqim qo‘shish",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                // Amount Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Summani kiriting",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            TextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() || it == '.' }) {
                                        amount = newValue
                                    }
                                },
                                placeholder = { Text("0.00", fontSize = 28.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                ),
                                visualTransformation = ThousandSeparatorTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Category Selection
                Text(
                    "Kategoriya",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isIncome) Color(0xFF4CAF50).copy(0.2f) else Color(0xFFF44336).copy(0.2f),
                                selectedLabelColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Izoh (ixtiyoriy)") },
                    placeholder = { Text("Nima uchun sarflandi?") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3
                )

                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Save Button
                Button(
                    onClick = {
                        val value = amount.toDoubleOrNull()
                        if (value == null || value <= 0) {
                            error = "Summani to'g'ri kiriting"
                            return@Button
                        }
                        loading = true
                        scope.launch {
                            try {
                                repo.add(Transaction(type = type, amount = value, category = category, note = note))
                                onSaved()
                            } catch (e: Exception) {
                                error = e.localizedMessage ?: "Saqlashda xatolik"
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Saqlash", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = originalText.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        val builder = StringBuilder()
        for (i in integerPart.indices) {
            builder.append(integerPart[i])
            val reversedIndex = integerPart.length - 1 - i
            if (reversedIndex % 3 == 0 && reversedIndex != 0) {
                builder.append(' ')
            }
        }
        builder.append(decimalPart)
        val transformedText = builder.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val integerOffset = if (offset > integerPart.length) integerPart.length else offset
                var spacesCount = 0
                for (i in 0 until integerOffset) {
                    val reversedIndex = integerPart.length - 1 - i
                    if (reversedIndex % 3 == 0 && reversedIndex != 0) {
                        spacesCount++
                    }
                }
                return if (offset > integerPart.length) {
                    integerPart.length + spacesCount + (offset - integerPart.length)
                } else {
                    offset + spacesCount
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedText.substring(0, offset.coerceAtMost(transformedText.length))
                    .count { it != ' ' }
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}
