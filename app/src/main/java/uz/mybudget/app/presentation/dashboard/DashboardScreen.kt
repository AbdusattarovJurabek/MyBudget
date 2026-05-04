@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
package uz.mybudget.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.mybudget.app.R
import uz.mybudget.app.data.model.Transaction
import uz.mybudget.app.data.repository.TransactionRepository
import uz.mybudget.app.util.formatMoney
import java.util.Calendar
import java.util.Locale

private val categoryIconMap: Map<String, ImageVector> = mapOf(
    "Oylik maosh" to Icons.Default.Payments,
    "Biznes" to Icons.Default.Business,
    "Freelance" to Icons.Default.Work,
    "Keshbek" to Icons.Default.Savings,
    "Sovg'a" to Icons.Default.CardGiftcard,
    "Oziq-ovqat" to Icons.Default.Restaurant,
    "Transport" to Icons.Default.DirectionsBus,
    "Taksi" to Icons.Default.LocalTaxi,
    "Benzin" to Icons.Default.LocalGasStation,
    "Kommunal" to Icons.Default.Home,
    "Internet" to Icons.Default.Wifi,
    "Kiyim" to Icons.Default.Checkroom,
    "Sog'liq" to Icons.Default.MedicalServices,
    "O'yin-kulgi" to Icons.Default.Celebration,
    "Boshqa" to Icons.Default.Category
)

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val repo = remember { TransactionRepository() }
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("Hammasi") }
    var showSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf("income") }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val incomeColor = MaterialTheme.colorScheme.secondary
    val expenseColor = MaterialTheme.colorScheme.error

    fun load() = scope.launch {
        loading = true
        try {
            list = when (selectedPeriod) {
                "Haftalik" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) cal.add(Calendar.DAY_OF_YEAR, -1)
                    repo.getInRange(cal.timeInMillis, System.currentTimeMillis())
                }
                "Oylik" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    repo.getInRange(cal.timeInMillis, System.currentTimeMillis())
                }
                else -> repo.getLatest(500)
            }
        } catch (_: Exception) { }
        loading = false
    }

    LaunchedEffect(selectedPeriod) { load() }
    LaunchedEffect(showSheet) { if (showSheet) fabExpanded = false }

    val income = list.filter { it.type == "income" }.sumOf { it.amount }
    val expense = list.filter { it.type == "expense" }.sumOf { it.amount }
    val balance = income - expense

    if (showDeleteDialog && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                    }
                }
            },
            title = { Text("O'chirishni tasdiqlaysizmi?", textAlign = TextAlign.Center) },
            text = { Text("Ushbu operatsiya butunlay o'chiriladi va qayta tiklab bo'lmaydi.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repo.delete(selectedTransaction!!.id)
                            load()
                            showDeleteDialog = false
                            selectedTransaction = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("O'chirish", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Bekor qilish") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "BUDJET",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = {
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Chiqish",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            SpeedDialFab(
                expanded = fabExpanded,
                onExpandToggle = { fabExpanded = !fabExpanded },
                incomeColor = incomeColor,
                expenseColor = expenseColor,
                onIncomeClick = {
                    transactionToEdit = null
                    sheetType = "income"
                    showSheet = true
                },
                onExpenseClick = {
                    transactionToEdit = null
                    sheetType = "expense"
                    showSheet = true
                }
            )
        }
    ) { padding ->
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false; transactionToEdit = null },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                AddTransactionSheetContent(
                    type = sheetType,
                    transactionToEdit = transactionToEdit,
                    onSaved = { showSheet = false; transactionToEdit = null; load() },
                    repo = repo
                )
            }
        }

        if (showActionSheet && selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { showActionSheet = false },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                    Text("Amallar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Tahrirlash", style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showActionSheet = false
                                transactionToEdit = selectedTransaction
                                sheetType = selectedTransaction!!.type
                                showSheet = true
                            }
                    )
                    ListItem(
                        headlineContent = { Text("O'chirish", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showActionSheet = false
                                showDeleteDialog = true
                            }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp)
        ) {
            item { MainBalanceCard(balance, income, expense) }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hammasi", "Haftalik", "Oylik").forEach { period ->
                            val isSelected = selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPeriod = period },
                                label = { Text(period, style = MaterialTheme.typography.labelLarge) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "So'nggi operatsiyalar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (list.isNotEmpty() && !loading) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${list.size} ta",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (list.isEmpty()) {
                item { EmptyTransactionsState() }
            } else {
                val grouped = list.groupBy {
                    val cal = Calendar.getInstance().also { c -> c.timeInMillis = it.date }
                    val now = Calendar.getInstance()
                    val sdf = java.text.SimpleDateFormat("dd MMMM, yyyy", Locale("uz"))
                    when {
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "Bugun"
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "Kecha"
                        else -> sdf.format(java.util.Date(it.date))
                    }
                }
                grouped.forEach { (date, dayItems) ->
                    item(key = "header_$date") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val dailyNet = dayItems.sumOf { if (it.type == "income") it.amount else -it.amount }
                            val netColor = if (dailyNet >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            Text(
                                text = (if (dailyNet >= 0) "+" else "") + formatMoney(dailyNet),
                                style = MaterialTheme.typography.labelMedium,
                                color = netColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    items(dayItems, key = { it.id }) { transaction ->
                        SwipeableTransactionItem(
                            t = transaction,
                            onLongClick = {
                                selectedTransaction = transaction
                                showActionSheet = true
                            },
                            onDelete = {
                                selectedTransaction = transaction
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDialFab(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    incomeColor: Color,
    expenseColor: Color,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedDialItem(label = "Kirim qo'shish", color = incomeColor, icon = Icons.Default.Add, onClick = onIncomeClick)
                SpeedDialItem(label = "Chiqim qo'shish", color = expenseColor, icon = Icons.Default.Remove, onClick = onExpenseClick)
            }
        }

        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            label = "fabRotation"
        )
        FloatingActionButton(
            onClick = onExpandToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(6.dp, 8.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (expanded) "Yopish" else "Qo'shish",
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun SpeedDialItem(label: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 4.dp
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MainBalanceCard(balance: Double, income: Double, expense: Double) {
    val brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, Color(0xFF818CF8))
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 170.dp, y = (-50).dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(x = (-40).dp, y = 70.dp)
                    .background(Color.White.copy(alpha = 0.04f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Umumiy Balans",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatMoney(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "O'zbekiston so'mi",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BalanceMiniStat(
                        label = "Kirim",
                        amount = income,
                        color = Color(0xFF6EE7B7),
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    BalanceMiniStat(
                        label = "Chiqim",
                        amount = expense,
                        color = Color(0xFFFCA5A5),
                        icon = Icons.AutoMirrored.Filled.TrendingDown
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceMiniStat(label: String, amount: Double, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            formatMoney(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            "Hozircha operatsiyalar yo'q",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Kirim yoki chiqim qo'shish uchun\n+ tugmasini bosing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun SwipeableTransactionItem(
    t: Transaction,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        },
        positionalThreshold = { total -> total * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                label = "swipeBg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "O'chirish",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        TransactionItem(t, onLongClick)
    }
}

@Composable
fun TransactionItem(t: Transaction, onLongClick: () -> Unit) {
    val isIncome = t.type == "income"
    val color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val icon = categoryIconMap[t.category]
        ?: if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = {}),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = color.copy(0.1f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = t.category, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.category,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (t.note.isNotBlank()) {
                    Text(
                        t.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (isIncome) "+" else "-") + formatMoney(t.amount),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}

@Composable
fun AddTransactionSheetContent(
    type: String,
    transactionToEdit: Transaction? = null,
    onSaved: () -> Unit,
    repo: TransactionRepository
) {
    val scope = rememberCoroutineScope()
    var amount by remember {
        mutableStateOf(
            transactionToEdit?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    val isIncome = type == "income"
    var category by remember { mutableStateOf(transactionToEdit?.category ?: if (isIncome) "Oylik maosh" else "Oziq-ovqat") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableLongStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("Tanlash") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Bekor qilish") } }
        ) { DatePicker(state = datePickerState) }
    }

    val typeColor = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    val categories = if (isIncome) {
        listOf(
            "Oylik maosh" to Icons.Default.Payments,
            "Biznes" to Icons.Default.Business,
            "Freelance" to Icons.Default.Work,
            "Keshbek" to Icons.Default.Savings,
            "Sovg'a" to Icons.Default.CardGiftcard,
            "Boshqa" to Icons.Default.Category
        )
    } else {
        listOf(
            "Oziq-ovqat" to Icons.Default.Restaurant,
            "Transport" to Icons.Default.DirectionsBus,
            "Taksi" to Icons.Default.LocalTaxi,
            "Benzin" to Icons.Default.LocalGasStation,
            "Kommunal" to Icons.Default.Home,
            "Internet" to Icons.Default.Wifi,
            "Kiyim" to Icons.Default.Checkroom,
            "Sog'liq" to Icons.Default.MedicalServices,
            "O'yin-kulgi" to Icons.Default.Celebration,
            "Boshqa" to Icons.Default.Category
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = typeColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                if (isIncome) "Kirim qo'shish" else "Chiqim qo'shish",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    java.text.SimpleDateFormat("dd MMMM yyyy", Locale("uz")).format(java.util.Date(date)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amount = it },
            label = { Text("Summa") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = typeColor
            ),
            visualTransformation = ThousandSeparatorTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            suffix = {
                Text("so'm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )

        Spacer(Modifier.height(24.dp))
        Text("Kategoriya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            categories.forEach { (name, icon) ->
                CategorySelectionItem(
                    name = name,
                    icon = icon,
                    isSelected = category == name,
                    selectedColor = typeColor,
                    onClick = { category = name }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Izoh (ixtiyoriy)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
        )

        AnimatedVisibility(visible = error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val value = amount.toDoubleOrNull()
                if (value == null || value <= 0) { error = "Summani to'g'ri kiriting"; return@Button }
                loading = true
                scope.launch {
                    try {
                        if (transactionToEdit != null) {
                            repo.update(transactionToEdit.copy(type = type, amount = value, category = category, note = note, date = date))
                        } else {
                            repo.add(Transaction(type = type, amount = value, category = category, note = note, date = date))
                        }
                        onSaved()
                    } catch (e: Exception) { error = e.localizedMessage; loading = false }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = typeColor),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (transactionToEdit != null) "Saqlash" else "Qo'shish",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategorySelectionItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            shadowElevation = if (isSelected) 4.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = name, tint = contentColor, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            if (reversedIndex % 3 == 0 && reversedIndex != 0) builder.append(' ')
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
                    if (reversedIndex % 3 == 0 && reversedIndex != 0) spacesCount++
                }
                return if (offset > integerPart.length) integerPart.length + spacesCount + (offset - integerPart.length) else offset + spacesCount
            }
            override fun transformedToOriginal(offset: Int): Int {
                return transformedText.substring(0, offset.coerceAtMost(transformedText.length)).count { it != ' ' }
            }
        }
        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}