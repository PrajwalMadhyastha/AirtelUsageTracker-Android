package com.prajwal.utilities.tools.wealthtracker.data

import com.prajwal.utilities.tools.wealthtracker.data.db.TransactionEntity
import com.prajwal.utilities.tools.wealthtracker.data.db.TransactionType
import java.util.Date
import kotlin.math.abs
import kotlin.math.pow

object XirrCalculator {
    private const val MAX_ITERATIONS = 100
    private const val TOLERANCE = 0.0000001
    private const val DAYS_IN_YEAR = 365.25

    data class CashFlow(val dateMillis: Long, val amount: Double)

    /**
     * Calculates the Extended Internal Rate of Return (XIRR).
     * @param transactions The list of transactions (BUY/SELL)
     * @param currentValue The current live value of the holding/portfolio
     * @return XIRR as a decimal (e.g., 0.15 for 15%). Returns 0.0 if calculation fails or is not possible.
     */
    fun calculateXirr(transactions: List<TransactionEntity>, currentValue: Double): Double {
        if (transactions.isEmpty()) return 0.0

        // Convert transactions into CashFlows
        // BUY = cash outflow (Negative)
        // SELL = cash inflow (Positive)
        val cashFlows = transactions.map {
            val amount = it.units * it.pricePerUnit
            CashFlow(
                dateMillis = it.timestamp,
                amount = if (it.type == TransactionType.BUY) -amount else amount
            )
        }.toMutableList()

        // Add current value as the final inflow today
        cashFlows.add(CashFlow(System.currentTimeMillis(), currentValue))

        // Sort by date just in case
        cashFlows.sortBy { it.dateMillis }

        return calculate(cashFlows)
    }

    private fun calculate(cashFlows: List<CashFlow>): Double {
        if (cashFlows.size < 2) return 0.0
        
        val t0 = cashFlows.first().dateMillis
        // Convert millis to fractional years
        val fractions = cashFlows.map { (it.dateMillis - t0) / (1000.0 * 60 * 60 * 24 * DAYS_IN_YEAR) }
        
        val maxFraction = fractions.maxOrNull() ?: 0.0
        // If the duration is less than 1 day, XIRR (annualized) is meaningless and explodes.
        if (maxFraction < 1.0 / 365.0) {
            return 0.0
        }

        val amounts = cashFlows.map { it.amount }

        var low = -0.999999
        var high = 10000.0 // Cap at 1,000,000% to prevent UI-breaking string lengths
        var rate = 0.0

        val fLow = xnpv(low, amounts, fractions)
        val fHigh = xnpv(high, amounts, fractions)

        if (fLow * fHigh > 0) {
            // If function doesn't cross zero, it's outside our bounds.
            if (fHigh > 0) return high
            if (fLow < 0) return low
        }

        for (i in 0 until 100) {
            rate = (low + high) / 2.0
            val f = xnpv(rate, amounts, fractions)
            
            if (abs(f) < TOLERANCE || (high - low) < TOLERANCE) {
                break
            }
            
            if (f > 0) {
                low = rate
            } else {
                high = rate
            }
        }
        
        return rate
    }

    private fun xnpv(rate: Double, amounts: List<Double>, fractions: List<Double>): Double {
        var sum = 0.0
        for (i in amounts.indices) {
            sum += amounts[i] / (1.0 + rate).pow(fractions[i])
        }
        return sum
    }

    private fun dxnpv(rate: Double, amounts: List<Double>, fractions: List<Double>): Double {
        var sum = 0.0
        for (i in amounts.indices) {
            if (fractions[i] > 0.0) {
                sum -= fractions[i] * amounts[i] / (1.0 + rate).pow(fractions[i] + 1.0)
            }
        }
        return sum
    }
}
