package uz.mybudget.app.util

import java.text.NumberFormat
import java.util.*

fun formatMoney(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("uz", "UZ")).apply {
    currency = Currency.getInstance("UZS")
    maximumFractionDigits = 0
}.format(amount).replace("UZS", "so'm")
