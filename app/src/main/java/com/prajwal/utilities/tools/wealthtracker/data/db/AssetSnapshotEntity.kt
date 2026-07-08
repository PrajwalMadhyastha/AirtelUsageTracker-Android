package com.prajwal.utilities.tools.wealthtracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * A single timestamped snapshot of the user's entire portfolio.
 * Each row represents the state of all 5 asset classes at a point in time.
 * This enables the portfolio growth-over-time chart.
 */
@Entity(tableName = "asset_snapshots")
@JsonClass(generateAdapter = true)
data class AssetSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recordedAt: Long = System.currentTimeMillis(), // epoch ms

    // Invested amounts (cumulative cost basis)
    val equityInvested: Double = 0.0,   // Mutual funds + stocks
    val goldInvested: Double = 0.0,
    val debtInvested: Double = 0.0,
    val silverInvested: Double = 0.0,
    val reitsInvested: Double = 0.0,    // Real Estate Investment Trusts

    // Current market value
    val equityCurrent: Double = 0.0,
    val goldCurrent: Double = 0.0,
    val debtCurrent: Double = 0.0,
    val silverCurrent: Double = 0.0,
    val reitsCurrent: Double = 0.0
) {
    val totalInvested: Double get() = equityInvested + goldInvested + debtInvested + silverInvested + reitsInvested
    val totalCurrent: Double get() = equityCurrent + goldCurrent + debtCurrent + silverCurrent + reitsCurrent
    val totalGainLoss: Double get() = totalCurrent - totalInvested
    val totalGainLossPct: Double get() = if (totalInvested > 0) (totalGainLoss / totalInvested) * 100 else 0.0
}
