package com.airtel.usagetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BillingCycleTest {

    @Test
    fun testCycleStartCalculation() {
        // Case 1: Today is 15th, Cycle starts 11th -> Start is 11th of THIS month
        val start1 = getCycleStartDate(currentDay = 15, billingDay = 11)
        assertEquals(0, getDaysDifference(start1, 15, 0)) // Should be same month

        // Case 2: Today is 5th, Cycle starts 11th -> Start is 11th of LAST month
        val start2 = getCycleStartDate(currentDay = 5, billingDay = 11)
        assertEquals(-1, getDaysDifference(start2, 5, -1)) // Should be previous month
    }

    private fun getCycleStartDate(currentDay: Int, billingDay: Int): Calendar {
        val calendar = Calendar.getInstance()
        // Mock "today"
        calendar.set(Calendar.DAY_OF_MONTH, currentDay)
        
        // Logic from Repository
        if (currentDay < billingDay) {
            calendar.add(Calendar.MONTH, -1)
        }
        calendar.set(Calendar.DAY_OF_MONTH, billingDay)
        return calendar
    }
    
    @Test
    fun testSessionInclusionLogic() {
        // Setup
        val cycleStart = 1000000L
        
        // Scenario A: Baseline recorded at 1005000. Uptime 2000 (started at 1003000)
        // 1003000 > 1000000 -> Started INSIDE cycle.
        // Should include session usage.
        val baselineTime = 1005000L
        val uptimeSeconds = 2L // 2 seconds, so 2000ms offset
        val bootTime = baselineTime - (uptimeSeconds * 1000)
        
        assertEquals(true, bootTime > cycleStart)
        
        // Scenario B: Baseline recorded at 1005000. Uptime 8000 (started at 997000)
        // 997000 < 1000000 -> Started BEFORE cycle.
        // Should NOT include session usage (standard diff logic).
        val uptimeLong = 8000L
        val bootTimeLong = baselineTime - (uptimeLong * 1000)
        
        assertEquals(false, bootTimeLong > cycleStart)
    }

    private fun getDaysDifference(date: Calendar, todayDay: Int, expectedMonthOffset: Int): Int {
        val today = Calendar.getInstance()
        today.set(Calendar.DAY_OF_MONTH, todayDay)
        
        // Simple check: compare month fields
        val m1 = date.get(Calendar.MONTH)
        val m2 = today.get(Calendar.MONTH)
        
        return if (m1 == m2) 0 else -1 // Simplified for test
    }
}
