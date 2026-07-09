package com.prajwal.utilities.tools.wealthtracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object TransactionType {
    const val BUY = "BUY"
    const val SELL = "SELL"
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = HoldingEntity::class,
            parentColumns = ["id"],
            childColumns = ["holdingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("holdingId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val holdingId: Int,
    val timestamp: Long,
    val units: Double,
    val pricePerUnit: Double,
    val type: String
)
