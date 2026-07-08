package com.prajwal.utilities.tools.wealthtracker.data

import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import com.prajwal.utilities.tools.wealthtracker.data.db.WealthDao
import kotlinx.coroutines.flow.Flow

class WealthRepository(private val dao: WealthDao) {

    /** All snapshots newest-first — used for the history list and diversification (latest). */
    fun getAllSnapshots(): Flow<List<AssetSnapshotEntity>> = dao.getAllSnapshots()

    /** All snapshots oldest-first — used for the growth-over-time line chart. */
    fun getAllSnapshotsChronological(): Flow<List<AssetSnapshotEntity>> = dao.getAllSnapshotsAscending()

    /** Latest single snapshot — used to pre-fill the portfolio input form. */
    suspend fun getLatestSnapshot(): AssetSnapshotEntity? = dao.getLatestSnapshot()

    /** Insert a new timestamped snapshot. */
    suspend fun insertSnapshot(snapshot: AssetSnapshotEntity) = dao.insertSnapshot(snapshot)

    /** Delete a snapshot (accidental entry removal). */
    suspend fun deleteSnapshot(snapshot: AssetSnapshotEntity) = dao.deleteSnapshot(snapshot)
}
