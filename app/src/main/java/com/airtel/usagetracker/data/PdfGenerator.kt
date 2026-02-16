package com.airtel.usagetracker.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.airtel.usagetracker.data.models.DailyUsage
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdfGenerator(private val context: Context) {

    fun generateReport(
        outputStream: OutputStream,
        dailyUsages: List<DailyUsage>,
        summaryTotalGb: Double,
        dailyAverageGb: Double,
        peakDay: LocalDate,
        peakUsageGb: Double
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Airtel Usage Report", 50f, 50f, paint)

        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated on: ${LocalDate.now()}", 50f, 80f, paint)

        // Summary Section
        drawSummary(canvas, paint, summaryTotalGb, dailyAverageGb, peakDay, peakUsageGb)

        // Table Header
        val startY = 200f
        drawTableHeader(canvas, paint, startY)

        // Table Rows
        var currentY = startY + 30f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 12f

        // Show last 30 days
        val last30Days = dailyUsages.sortedByDescending { it.date }.take(30)

        last30Days.forEach { usage ->
            if (currentY > 800f) {
                // Simple pagination handling: just stop for now to avoid complexity of multi-page logic in V1
                // or we could finishPage and startPage. For MVP, let's just clip.
                return@forEach
            }
            drawTableRow(canvas, paint, currentY, usage)
            currentY += 25f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    private fun drawSummary(
        canvas: Canvas,
        paint: Paint,
        totalGb: Double,
        avgGb: Double,
        peakDay: LocalDate,
        peakGb: Double
    ) {
        val startY = 120f
        val boxWidth = 500f
        val boxHeight = 60f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(50f, startY, 50f + boxWidth, startY + boxHeight, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 14f
        val colWidth = boxWidth / 3

        // Column 1: Total
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Total Usage", 60f, startY + 25f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${String.format("%.1f", totalGb)} GB", 60f, startY + 45f, paint)

        // Column 2: Average
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Daily Average", 60f + colWidth, startY + 25f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${String.format("%.1f", avgGb)} GB", 60f + colWidth, startY + 45f, paint)

        // Column 3: Peak
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Peak Usage", 60f + 2 * colWidth, startY + 25f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${peakDay.format(DateTimeFormatter.ofPattern("MMM dd"))} (${String.format("%.1f", peakGb)} GB)", 60f + 2 * colWidth, startY + 45f, paint)
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float) {
        paint.color = Color.LTGRAY
        canvas.drawRect(50f, y - 20f, 545f, y + 10f, paint)
        paint.color = Color.BLACK
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 12f

        canvas.drawText("Date", 60f, y, paint)
        canvas.drawText("Total (GB)", 200f, y, paint)
        canvas.drawText("Upload", 300f, y, paint)
        canvas.drawText("Download", 400f, y, paint)
        canvas.drawText("Recs", 500f, y, paint)
        
        // Horizontal line
        paint.strokeWidth = 2f
        canvas.drawLine(50f, y + 10f, 545f, y + 10f, paint)
    }

    private fun drawTableRow(canvas: Canvas, paint: Paint, y: Float, usage: DailyUsage) {
        paint.color = Color.BLACK
        canvas.drawText(usage.date.toString(), 60f, y, paint)
        canvas.drawText(String.format("%.2f", usage.toGigabytes()), 200f, y, paint)
        
        val txGb = usage.txBytes / (1024.0 * 1024.0 * 1024.0)
        val rxGb = usage.rxBytes / (1024.0 * 1024.0 * 1024.0)
        
        canvas.drawText(String.format("%.2f", txGb), 300f, y, paint)
        canvas.drawText(String.format("%.2f", rxGb), 400f, y, paint)
        canvas.drawText(usage.recordCount.toString(), 500f, y, paint)
        
        // Separator
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y + 10f, 545f, y + 10f, paint)
    }
}
