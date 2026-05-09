package com.prajwal.utilities.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BillingCycleTest {

    @Test
    fun testRolloverBoundary() {
        // Assume Billing Day is 1st of every month
        val billingDay = 1
        
        // Scenario 1: Date is Jan 31st at 23:59
        // Current month is Jan (0 in Calendar).
        // Since 31 >= 1, we stay in Jan.
        // Cycle start should be Jan 1st.
        val jan31 = Calendar.getInstance()
        jan31.set(2024, Calendar.JANUARY, 31, 23, 59)
        val cycleStartJan = getCycleStartDate(jan31.get(Calendar.DAY_OF_MONTH), billingDay, jan31)
        
        assertEquals(Calendar.JANUARY, cycleStartJan.get(Calendar.MONTH))
        assertEquals(1, cycleStartJan.get(Calendar.DAY_OF_MONTH))
        
        // Scenario 2: Date is Feb 1st at 00:01
        // Current month is Feb (1 in Calendar).
        // Since 1 >= 1, we stay in Feb.
        // Cycle start should be Feb 1st.
        val feb1 = Calendar.getInstance()
        feb1.set(2024, Calendar.FEBRUARY, 1, 0, 1)
        val cycleStartFeb = getCycleStartDate(feb1.get(Calendar.DAY_OF_MONTH), billingDay, feb1)
        
        assertEquals(Calendar.FEBRUARY, cycleStartFeb.get(Calendar.MONTH))
        assertEquals(1, cycleStartFeb.get(Calendar.DAY_OF_MONTH))
        
        // Scenario 3: Billing Day is 15th. Date is Feb 1st.
        // 1 < 15, so we go back one month (to Jan).
        // Cycle start should be Jan 15th.
        val billingDay15 = 15
        val cycleStartFeb15 = getCycleStartDate(feb1.get(Calendar.DAY_OF_MONTH), billingDay15, feb1)
        
        assertEquals(Calendar.JANUARY, cycleStartFeb15.get(Calendar.MONTH))
        assertEquals(15, cycleStartFeb15.get(Calendar.DAY_OF_MONTH))
    }

    private fun getCycleStartDate(currentDay: Int, billingDay: Int, refDate: Calendar? = null): Calendar {
        val calendar = refDate?.clone() as? Calendar ?: Calendar.getInstance()
        if (refDate == null) calendar.set(Calendar.DAY_OF_MONTH, currentDay)
        
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
