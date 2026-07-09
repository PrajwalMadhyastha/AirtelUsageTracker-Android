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

        // Start with a guess of 10%
        var rate = 0.10
        
        for (i in 0 until MAX_ITERATIONS) {
            val f = xnpv(rate, amounts, fractions)
            val df = dxnpv(rate, amounts, fractions)
            
            if (abs(df) < TOLERANCE) break
            
            val newRate = rate - f / df
            if (abs(newRate - rate) < TOLERANCE) {
                return newRate
            }
            rate = newRate
        }
        
        // If it didn't converge properly, or it's nonsensical
        if (rate.isNaN() || rate.isInfinite()) return 0.0
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
