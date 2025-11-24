package com.bybit.rebalancer.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

// Number Extensions
fun Double.roundTo(decimals: Int): Double {
    return BigDecimal(this)
        .setScale(decimals, RoundingMode.HALF_UP)
        .toDouble()
}

fun Double.formatAsPercentage(decimals: Int = 2): String {
    return "%.${"decimals"}f%%".format(this)
}

fun Double.formatAsUsd(decimals: Int = 2): String {
    return "$%.${"decimals"}f".format(this)
}

fun Double.formatAsCrypto(decimals: Int = 8): String {
    return BigDecimal(this)
        .setScale(decimals, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}

fun String.toSafeDouble(): Double {
    return this.toDoubleOrNull() ?: 0.0
}

fun String.toSafeLong(): Long {
    return this.toLongOrNull() ?: 0L
}

// Date/Time Extensions
fun Long.toFormattedTime(pattern: String = "HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDate(pattern: String = "dd/MM/yyyy HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Az önce"
        diff < 3_600_000 -> "${diff / 60_000} dakika önce"
        diff < 86_400_000 -> "${diff / 3_600_000} saat önce"
        diff < 604_800_000 -> "${diff / 86_400_000} gün önce"
        else -> toFormattedDate("dd/MM/yyyy")
    }
}

// String Extensions
fun String.maskApiKey(): String {
    if (this.length <= 8) return "****"
    return this.take(4) + "*".repeat(this.length - 8) + this.takeLast(4)
}

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (this.length > maxLength) {
        this.take(maxLength - suffix.length) + suffix
    } else {
        this
    }
}

// Collection Extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}

// BigDecimal Extensions
fun BigDecimal.formatForTrade(): String {
    return this.setScale(8, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}
