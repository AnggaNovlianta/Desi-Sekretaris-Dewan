package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.Meeting
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    private const val TAG = "PdfExporter"

    fun exportMeetingMinutesToPdf(context: Context, meeting: Meeting) {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            
            // Standard A4 dimensions in points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val leftMargin = 45f
            val rightMargin = 550f
            val maxTextWidth = rightMargin - leftMargin
            
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            
            // Paint Configurations
            val paintText = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 10f
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }
            
            val paintBold = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }

            val paintSemiBold = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = android.graphics.Color.DKGRAY
                isAntiAlias = true
            }
            
            val paintHeaderGov = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = android.graphics.Color.rgb(15, 32, 67) // Navy blue
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val paintHeaderSecwan = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                color = android.graphics.Color.rgb(50, 50, 50)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            val paintHeaderAddr = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 7.5f
                color = android.graphics.Color.GRAY
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            val paintDocTitle = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 13f
                color = android.graphics.Color.rgb(15, 32, 67) // Navy blue
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val paintDocCategory = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
                color = android.graphics.Color.rgb(197, 160, 89) // Gold accent
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            val paintSectionTitle = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11f
                color = android.graphics.Color.rgb(15, 32, 67) // Navy blue
                isAntiAlias = true
            }
            
            val paintLinePrimary = Paint().apply {
                color = android.graphics.Color.rgb(15, 32, 67) // Navy
                strokeWidth = 2.5f
                isAntiAlias = true
            }
            
            val paintLineSecondary = Paint().apply {
                color = android.graphics.Color.rgb(197, 160, 89) // Gold accent
                strokeWidth = 1.2f
                isAntiAlias = true
            }

            val paintDivider = Paint().apply {
                color = android.graphics.Color.rgb(220, 220, 220)
                strokeWidth = 0.8f
                isAntiAlias = true
            }
            
            val paintAIBg = Paint().apply {
                color = android.graphics.Color.rgb(245, 247, 250) // Soft cool background tint
                style = Paint.Style.FILL
            }

            val paintAIBorder = Paint().apply {
                color = android.graphics.Color.rgb(220, 225, 235)
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            
            val paintAITextIndicator = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                textSize = 9f
                color = android.graphics.Color.rgb(40, 90, 160) // Blue theme
                isAntiAlias = true
            }

            var y = 50f
            
            // Helper function to print page headers on subsequent pages
            fun drawPageTemplate(canvasObj: Canvas, pageNum: Int) {
                // If it is the first page, draw the rich letterhead (Kop Surat)
                if (pageNum == 1) {
                    canvasObj.drawText("DEWAN PERWAKILAN RAKYAT DAERAH KOTA PRABUMULIH", (pageWidth / 2f), y, paintHeaderGov)
                    y += 15f
                    canvasObj.drawText("SEKRETARIAT DEWAN (SEKWAN) • SISTEM INFORMASI DESI", (pageWidth / 2f), y, paintHeaderSecwan)
                    y += 13f
                    canvasObj.drawText("Jl. Jenderal Sudirman No. 1, Kota Prabumulih, Sumatra Selatan 31121", (pageWidth / 2f), y, paintHeaderAddr)
                    y += 10f
                    
                    // Double border line (Navy + Gold)
                    canvasObj.drawLine(leftMargin, y, rightMargin, y, paintLinePrimary)
                    y += 3.5f
                    canvasObj.drawLine(leftMargin, y, rightMargin, y, paintLineSecondary)
                    y += 22f
                    
                    // Document Header Title
                    canvasObj.drawText("NOTULEN RAPAT & RINGKASAN RESMI AI", (pageWidth / 2f), y, paintDocTitle)
                    y += 12f
                    canvasObj.drawText("SISTEM ADMINISTRASI PARLEMEN DESI / NO. NOT-${meeting.id}", (pageWidth / 2f), y, paintDocCategory)
                    y += 24f
                } else {
                    // Recurring Header on subsequent pages
                    val paintHeaderRecur = Paint().apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 8f
                        color = android.graphics.Color.rgb(100, 110, 125)
                        isAntiAlias = true
                    }
                    val paintPageNum = Paint().apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textSize = 8f
                        color = android.graphics.Color.rgb(100, 110, 125)
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    
                    canvasObj.drawText("NOTULEN RESMI DPRD KOTA PRABUMULIH: ${meeting.title.uppercase()}", leftMargin, 38f, paintHeaderRecur)
                    canvasObj.drawText("Halaman $pageNum", rightMargin, 38f, paintPageNum)
                    canvasObj.drawLine(leftMargin, 44f, rightMargin, 44f, paintDivider)
                    y = 62f
                }
            }

            // Start drawing first page headers
            drawPageTemplate(canvas, pageNumber)
            
            // Helper function to handle word wrapping
            fun wrapText(paragraph: String, paint: Paint, maxWidth: Float): List<String> {
                val lines = mutableListOf<String>()
                if (paragraph.isEmpty()) {
                    lines.add("")
                    return lines
                }
                
                val rawLines = paragraph.split("\n")
                for (rawLine in rawLines) {
                    if (rawLine.trim().isEmpty()) {
                        lines.add("")
                        continue
                    }
                    val words = rawLine.split(" ")
                    var currentLine = StringBuilder()
                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                        val width = paint.measureText(testLine)
                        if (width <= maxWidth) {
                            currentLine.append(if (currentLine.isEmpty()) word else " $word")
                        } else {
                            if (currentLine.isNotEmpty()) {
                                lines.add(currentLine.toString())
                            }
                            currentLine = StringBuilder(word)
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                }
                return lines
            }

            // Check and handle page breaking helper
            fun checkPageOverflow(requiredHeight: Float): Canvas {
                if (y + requiredHeight > 785f) {
                    // Draw Footer on finishing page
                    val paintFooter = Paint().apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textSize = 7.5f
                        color = android.graphics.Color.GRAY
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("Dokumen ini sah secara hukum dan diterbitkan otomatis oleh Sekretariat DPRD Prabumulih", (pageWidth / 2f), 810f, paintFooter)
                    
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 0f
                    drawPageTemplate(canvas, pageNumber)
                }
                return canvas
            }
            
            // SECTION: METADATA DETAILS BOX (Grid style)
            val detailsLabelWidth = 115f
            val detailsValueWidth = maxTextWidth - detailsLabelWidth - 10f
            
            val metaData = listOf(
                "Nama / Topik Rapat" to meeting.title,
                "Tanggal Kegiatan" to meeting.date,
                "Waktu Sidang" to meeting.time,
                "Lokasi Pertemuan" to meeting.location,
                "Grup Konstituen" to meeting.recipientGroup,
                "Kategori Agenda" to meeting.category,
                "Status Notulen" to meeting.status
            )
            
            // Draw boxed container for details
            checkPageOverflow(140f)
            
            // Draw a subtle border frame around meeting details
            val frameTop = y
            var frameCursorY = y + 10f
            
            metaData.forEach { (label, value) ->
                if (value.isNotEmpty()) {
                    val wrappedVal = wrapText(value, paintText, detailsValueWidth)
                    val lineSpacingHeight = wrappedVal.size * 13f
                    checkPageOverflow(lineSpacingHeight + 8f)
                    
                    canvas.drawText(label, leftMargin + 8f, frameCursorY, paintBold)
                    
                    var internalY = frameCursorY
                    wrappedVal.forEach { line ->
                        canvas.drawText(line, leftMargin + detailsLabelWidth + 5f, internalY, paintText)
                        internalY += 13f
                    }
                    
                    frameCursorY = internalY + 3f
                }
            }
            
            // Draw frame lines
            val paintFrame = Paint().apply {
                color = android.graphics.Color.rgb(210, 215, 225)
                strokeWidth = 1f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(leftMargin, frameTop - 4f, rightMargin, frameCursorY, 6f, 6f, paintFrame)
            y = frameCursorY + 20f

            // SECTION 1: DAFTAR HADIR (ATTENDEES COHORT)
            if (meeting.attendeesList.isNotEmpty()) {
                canvas = checkPageOverflow(40f)
                canvas.drawText("I. DAFTAR HADIR (PESERTA RAPAT)", leftMargin, y, paintSectionTitle)
                y += 5f
                canvas.drawLine(leftMargin, y, rightMargin, y, paintLineSecondary)
                y += 14f
                
                val attendeeLines = wrapText("Pemberi Aspirasi / Anggota Dewan Terdaftar:\n${meeting.attendeesList}", paintText, maxTextWidth)
                attendeeLines.forEach { line ->
                    canvas = checkPageOverflow(13f)
                    val isHeading = line.startsWith("Pemberi")
                    canvas.drawText(line, leftMargin, y, if (isHeading) paintSemiBold else paintText)
                    y += 13f
                }
                y += 10f
            }

            // SECTION 2: JALANNYA SIDANG / CATATAN KASAR (RAW PROCEEDINGS)
            if (meeting.minutesContent.isNotEmpty()) {
                canvas = checkPageOverflow(40f)
                canvas.drawText("II. JALANNYA PERSIDANGAN & CATATAN DISKUSI", leftMargin, y, paintSectionTitle)
                y += 5f
                canvas.drawLine(leftMargin, y, rightMargin, y, paintLineSecondary)
                y += 14f
                
                val rawNotesLines = wrapText(meeting.minutesContent, paintText, maxTextWidth)
                rawNotesLines.forEach { line ->
                    canvas = checkPageOverflow(13f)
                    canvas.drawText(line, leftMargin, y, paintText)
                    y += 13f
                }
                y += 10f
            }

            // SECTION 3: RINGKASAN RESMI ASISTEN AI (AI SUMMARY REPORT)
            if (meeting.aiSummary.isNotEmpty()) {
                canvas = checkPageOverflow(40f)
                canvas.drawText("III. RINGKASAN REKOMENDASI DAN KEPUTUSAN (ASISTEN AI)", leftMargin, y, paintSectionTitle)
                y += 5f
                canvas.drawLine(leftMargin, y, rightMargin, y, paintLineSecondary)
                y += 16f
                
                // Wrap AI text
                val aiLines = wrapText(meeting.aiSummary, paintText, maxTextWidth - 24f) // narrower width for padding
                val aiTotalHeight = (aiLines.size * 13.5f) + 26f
                
                canvas = checkPageOverflow(aiTotalHeight + 10f)
                
                // Draw callout back-tint box
                val boxBottom = y + aiTotalHeight
                canvas.drawRoundRect(leftMargin, y - 4f, rightMargin, boxBottom, 8f, 8f, paintAIBg)
                canvas.drawRoundRect(leftMargin, y - 4f, rightMargin, boxBottom, 8f, 8f, paintAIBorder)
                
                // Left thick blue accent strip to emphasize AI advice
                val paintAccentStrip = Paint().apply {
                    color = android.graphics.Color.rgb(40, 90, 160)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(leftMargin, y - 4f, leftMargin + 4f, boxBottom, paintAccentStrip)
                
                // Draw Indicator Text
                y += 11f
                canvas.drawText("✦ RINGKASAN PRODUKTIVITAS RECHARTS-AI DESI:", leftMargin + 12f, y, paintAITextIndicator)
                y += 15f
                
                // Draw lines inside
                aiLines.forEach { line ->
                    canvas.drawText(line, leftMargin + 12f, y, paintText)
                    y += 13.5f
                }
                y += 15f
            }
            
            // FOOTER & SIGN-OFF BLOCK
            val signBlockHeight = 90f
            canvas = checkPageOverflow(signBlockHeight)
            y += 15f
            
            val paintSignTitle = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = android.graphics.Color.BLACK
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            
            val paintSignLine = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9f
                color = android.graphics.Color.BLACK
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            
            canvas.drawText("Prabumulih, " + meeting.date.substringAfter(", ").trim(), rightMargin - 15f, y, paintSignLine)
            y += 13f
            canvas.drawText("Sekretaris Dewan Perwakilan Rakyat Daerah,", rightMargin - 15f, y, paintSignTitle)
            
            // Draw digital verification seal stamp on the left if minutes are DISAHKAN
            if (meeting.status == "DISAHKAN") {
                val sealY = y - 5f
                val sealX = leftMargin + 10f
                
                // Draw decorative light green rounded rect for stamp
                val paintStampBg = Paint().apply {
                    color = android.graphics.Color.rgb(235, 247, 237) // soft green
                    style = Paint.Style.FILL
                }
                val paintStampBorder = Paint().apply {
                    color = android.graphics.Color.rgb(46, 125, 50) // green
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val paintStampText = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 6.5f
                    color = android.graphics.Color.rgb(46, 125, 50)
                    isAntiAlias = true
                }
                val paintStampTextNormal = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = 5.5f
                    color = android.graphics.Color.rgb(46, 125, 50)
                    isAntiAlias = true
                }

                // Draw Rounded Stamp Box
                canvas.drawRoundRect(sealX, sealY, sealX + 220f, sealY + 58f, 3f, 3f, paintStampBg)
                canvas.drawRoundRect(sealX, sealY, sealX + 220f, sealY + 58f, 3f, 3f, paintStampBorder)
                
                // Draw Stamp Labels
                canvas.drawText("TERVERIFIKASI & DISAHKAN DIGITAL", sealX + 45f, sealY + 12f, paintStampText)
                canvas.drawText("Sistem Informasi DESI DPRD Kota Prabumulih", sealX + 45f, sealY + 22f, paintStampTextNormal)
                canvas.drawText("Penandatangan: SEKRETARIS DPRD", sealX + 45f, sealY + 31f, paintStampTextNormal)
                canvas.drawText("Tanggal TTE: " + meeting.date.substringAfter(", ").trim(), sealX + 45f, sealY + 40f, paintStampTextNormal)
                canvas.drawText("Status: DOKUMEN SAH & RESMI (ORIGINAL)", sealX + 45f, sealY + 49f, paintStampText)
                
                // Draw simulated QR Code inside the seal box
                val qrLeft = sealX + 5f
                val qrTop = sealY + 10f
                val qrSize = 38f
                
                // outer qr box
                canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true })
                
                // Draw 3 standard corner alignment blocks
                canvas.drawRect(qrLeft + 2f, qrTop + 2f, qrLeft + 10f, qrTop + 10f, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50) })
                canvas.drawRect(qrLeft + qrSize - 10f, qrTop + 2f, qrLeft + qrSize - 2f, qrTop + 10f, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50) })
                canvas.drawRect(qrLeft + 2f, qrTop + qrSize - 10f, qrLeft + 10f, qrTop + qrSize - 2f, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50) })
                
                // draw inner white/bg dots for alignment blocks
                canvas.drawRect(qrLeft + 4f, qrTop + 4f, qrLeft + 8f, qrTop + 8f, Paint().apply { color = android.graphics.Color.rgb(235, 247, 237) })
                canvas.drawRect(qrLeft + qrSize - 8f, qrTop + 4f, qrLeft + qrSize - 4f, qrTop + 8f, Paint().apply { color = android.graphics.Color.rgb(235, 247, 237) })
                canvas.drawRect(qrLeft + 4f, qrTop + qrSize - 8f, qrLeft + 8f, qrTop + qrSize - 4f, Paint().apply { color = android.graphics.Color.rgb(235, 247, 237) })
                
                // draw center fake pattern
                canvas.drawRect(qrLeft + 14f, qrTop + 14f, qrLeft + 20f, qrTop + 20f, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50) })
                canvas.drawRect(qrLeft + 22f, qrTop + 22f, qrLeft + qrSize - 4f, qrTop + qrSize - 4f, Paint().apply { color = android.graphics.Color.rgb(46, 125, 50) })
            }
            
            y += 42f
            canvas.drawText("(................................................................)", rightMargin - 15f, y, paintSignLine)
            y += 13f
            canvas.drawText("NIP. 19740812 200312 1 002    ", rightMargin - 15f, y, paintSignLine)
            
            // Draw Footer on the final page
            val paintFooterFinal = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 7.5f
                color = android.graphics.Color.GRAY
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Dokumen ini sah secara hukum dan diterbitkan otomatis oleh Sekretariat DPRD Prabumulih", (pageWidth / 2f), 810f, paintFooterFinal)
            
            pdfDocument.finishPage(page)
            
            // Save to files
            // 1. Save in app's Download directory as safe local copy
            val localDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val safeFileName = "NOTULEN_DPRD_RAPAT_${meeting.id}_" + meeting.title.replace("\\s+".toRegex(), "_")
                .replace("[^a-zA-Z0-9_]".toRegex(), "") + ".pdf"
            val localPdfFile = File(localDownloadsDir, safeFileName)
            
            FileOutputStream(localPdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            
            // 2. Save file temporarily in cache to trigger standard ACTION_SEND file share provider flow
            val cachePdfFile = File(context.cacheDir, safeFileName)
            FileOutputStream(cachePdfFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            
            pdfDocument.close()
            
            // Trigger Intent Share to let the user print, export, or send via WhatsApp/Google Drive/Email/PDF viewers!
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cachePdfFile)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Notulen Resmi: ${meeting.title} - DPRD Kota Prabumulih")
                putExtra(Intent.EXTRA_TEXT, "Berikut dilampirkan salinan resmi Notulen Rapat DPRD Kota Prabumulih yang dirangkum dengan asisten AI DESI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Ekspor / Bagikan PDF Notulen Rapat")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            ToastUtils.show(context, "PDF berhasil dibuat di folder Downloads!")
            Log.d(TAG, "Successfully generated and shared PDF directory path: ${localPdfFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or sharing PDF: ", e)
            ToastUtils.show(context, "Gagal membuat PDF: ${e.localizedMessage}")
        }
    }
}
