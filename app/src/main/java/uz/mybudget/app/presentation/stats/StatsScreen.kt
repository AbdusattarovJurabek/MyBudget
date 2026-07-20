@file:OptIn(ExperimentalMaterial3Api::class)
package uz.mybudget.app.presentation.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.launch
import uz.mybudget.app.R
import uz.mybudget.app.data.model.Transaction
import uz.mybudget.app.data.repository.TransactionRepository
import uz.mybudget.app.ui.theme.ChartPalette
import uz.mybudget.app.util.formatMoney
import java.util.Calendar

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val repo = remember(context) { TransactionRepository(context) }
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("Hammasi") }

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

    val totalIncome = list.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = list.filter { it.type == "expense" }.sumOf { it.amount }
    val categoryStats = list
        .filter { it.type == "expense" }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "STATISTIKA",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            }
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                item {
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
                }

                if (list.isEmpty()) {
                    item {
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
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                "Hozircha ma'lumotlar yo'q",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Operatsiyalar qo'shilgandan so'ng\nstatistika ko'rsatiladi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item { NetSavingsCard(totalIncome, totalExpense) }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryCard(
                                title = "Kirim",
                                amount = totalIncome,
                                color = MaterialTheme.colorScheme.secondary,
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = "Chiqim",
                                amount = totalExpense,
                                color = MaterialTheme.colorScheme.error,
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (totalExpense > 0) {
                        item {
                            Text(
                                "Xarajatlar tahlili",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        item { DonutChartCard(categoryStats, totalExpense) }

                        item {
                            Text(
                                "Toifalar bo'yicha",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(categoryStats) { (cat, amount) ->
                            CategoryStatItemPro(cat, amount, totalExpense)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetSavingsCard(income: Double, expense: Double) {
    val net = income - expense
    val isPositive = net >= 0
    val savingsRate = if (income > 0) ((net / income) * 100).toInt() else 0
    val color = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 220.dp, y = (-40).dp)
                    .background(color.copy(alpha = 0.05f), CircleShape)
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Sof tejamkorlik",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (if (isPositive) "+" else "") + formatMoney(net),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (income > 0) {
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$savingsRate%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                formatMoney(amount),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DonutChartCard(stats: List<Pair<String, Double>>, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                DonutChart(stats, total)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Jami", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(total).split(" ").first(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text("so'm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                stats.take(5).forEachIndexed { idx, (cat, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(getCategoryColor(cat), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                cat,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${((amount / total) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (stats.size > 5) {
                    Text(
                        "+${stats.size - 5} yana",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(stats: List<Pair<String, Double>>, total: Double) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    val gapAngle = if (stats.size > 1) 3f else 0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        stats.forEach { (cat, amount) ->
            val fullSweep = ((amount / total) * 360f).toFloat()
            val animatedSweep = fullSweep * animProgress.value
            val drawSweep = (animatedSweep - gapAngle).coerceAtLeast(0f)
            drawArc(
                color = getCategoryColor(cat),
                startAngle = startAngle,
                sweepAngle = drawSweep,
                useCenter = false,
                style = Stroke(width = 38f, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            startAngle += animatedSweep
        }
    }
}

@Composable
fun CategoryStatItemPro(category: String, amount: Double, total: Double) {
    val percent = (amount / total).toFloat()
    val color = getCategoryColor(category)

    var animated by remember { mutableStateOf(false) }
    val animatedPercent by animateFloatAsState(
        targetValue = if (animated) percent else 0f,
        animationSpec = tween(800, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "progress_$category"
    )
    LaunchedEffect(Unit) { animated = true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        category.take(1).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        category,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatMoney(amount),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPercent)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.65f))),
                                CircleShape
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${(percent * 100).toInt()}% jami xarajatdan",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    val index = category.hashCode().let { if (it < 0) -it else it } % ChartPalette.size
    return ChartPalette[index]
}
