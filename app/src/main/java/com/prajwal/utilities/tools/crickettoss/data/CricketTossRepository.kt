package com.prajwal.utilities.tools.crickettoss.data

import com.prajwal.utilities.tools.crickettoss.data.db.TossDao
import com.prajwal.utilities.tools.crickettoss.data.db.TossEntity
import kotlinx.coroutines.flow.Flow

class CricketTossRepository(private val tossDao: TossDao) {
    val recentTosses: Flow<List<TossEntity>> = tossDao.getRecentTosses()

    suspend fun saveToss(result: String) {
        tossDao.insertToss(TossEntity(result = result))
        tossDao.clearOldTosses()
    }
}
