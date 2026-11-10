package com.example.vehiclemanager.core.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object Formatters {
    fun formatMoney(cents: Long): String {
        val amount = BigDecimal.valueOf(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f €", amount)
    }

    fun formatFuelPrice(priceCents: Long): String {
        val price = BigDecimal.valueOf(priceCents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f €/L", price)
    }

    fun formatCostPerKm(centsX100: Long?): String {
        if (centsX100 == null) return "No disponible"
        val costEuros = BigDecimal.valueOf(centsX100).divide(BigDecimal(10000), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f €/km", costEuros)
    }

    fun formatDistance(distanceKm: Long): String {
        return "$distanceKm km"
    }

    fun formatMonthlySpend(cents: Long): String {
        val amount = BigDecimal.valueOf(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f €/mes", amount)
    }

    fun formatConsumption(litersPer100KmX100: Long?): String {
        if (litersPer100KmX100 == null) return "—"
        val consumption = BigDecimal.valueOf(litersPer100KmX100).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f L/100 km", consumption)
    }

    fun formatLiters(litersX100: Int): String {
        val liters = BigDecimal.valueOf(litersX100.toLong()).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return String.format(Locale.US, "%.2f L", liters)
    }
}
