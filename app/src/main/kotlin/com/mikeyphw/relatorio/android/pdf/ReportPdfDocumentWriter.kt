package com.mikeyphw.relatorio.android.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mikeyphw.relatorio.model.ReportPdfContent
import com.mikeyphw.relatorio.model.ReportPdfFieldRow
import com.mikeyphw.relatorio.model.ReportPdfMovement
import com.mikeyphw.relatorio.model.ReportPdfSection
import com.mikeyphw.relatorio.model.ReportTemplateId
import java.io.OutputStream
import kotlin.math.max

internal object ReportPdfDocumentWriter {
    private const val PageWidth = 595
    private const val PageHeight = 842
    private const val Margin = 24f
    private const val BottomLimit = PageHeight - 30f
    private const val ColumnGap = 10f
    private const val SectionTitleHeight = 18f
    private const val SectionRowHeight = 14f
    private const val TableHeaderHeight = 18f
    private const val MovementRowHeight = 17f
    private const val StandardFirstPageMovementRows = 10

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }
    private val lightFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 226, 226)
        style = Paint.Style.FILL
    }
    private val veryLightFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 244, 244)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 7.2f
    }
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 7.4f
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10.2f
        textAlign = Paint.Align.CENTER
    }

    fun write(content: ReportPdfContent, output: OutputStream) {
        val document = PdfDocument()
        try {
            val state = WriterState(document)
            when (content.templateId) {
                ReportTemplateId.AdTimoteo2026 -> drawStandardReport(state, content)
                ReportTemplateId.Complete -> drawCompleteReport(state, content)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawStandardReport(state: WriterState, content: ReportPdfContent) {
        var page = state.startPage()
        var y = drawFullHeader(page.canvas, content)
        val columnWidth = (PageWidth - Margin * 2 - ColumnGap) / 2f
        var leftY = y
        var rightY = y
        content.sections.forEachIndexed { index, section ->
            if (index < 3) {
                leftY = drawSection(page.canvas, Margin, leftY, columnWidth, section) + 5f
            } else {
                rightY = drawSection(
                    page.canvas,
                    Margin + columnWidth + ColumnGap,
                    rightY,
                    columnWidth,
                    section,
                ) + 5f
            }
        }
        y = max(leftY, rightY) + 2f
        val firstMovementResult = drawMovementTable(
            canvas = page.canvas,
            content = content,
            startIndex = 0,
            y = y,
            maximumRows = StandardFirstPageMovementRows,
            minimumRows = StandardFirstPageMovementRows,
        )
        y = firstMovementResult.bottom + 5f
        y = drawLegend(page.canvas, y)
        val noteResult = drawNotesBox(
            canvas = page.canvas,
            lines = wrapText(content.notes.ifBlank { " " }, textPaint, PageWidth - Margin * 2 - 10f),
            startIndex = 0,
            y = y + 5f,
            maximumHeight = BottomLimit - y - 5f,
            title = "ANOTAÇÕES E SUGESTÕES",
        )
        state.finishPage(page)

        drawContinuationPages(
            state = state,
            content = content,
            movementStartIndex = firstMovementResult.nextIndex,
            noteLines = noteResult.lines,
            noteStartIndex = noteResult.nextIndex,
        )
    }

    private fun drawCompleteReport(state: WriterState, content: ReportPdfContent) {
        var page = state.startPage()
        var startY = drawFullHeader(page.canvas, content)
        val columnWidth = (PageWidth - Margin * 2 - ColumnGap) / 2f
        var leftY = startY
        var rightY = startY

        content.sections.forEach { section ->
            val height = sectionHeight(section)
            val preferLeft = leftY <= rightY
            val targetX = if (preferLeft) Margin else Margin + columnWidth + ColumnGap
            val targetY = if (preferLeft) leftY else rightY
            val alternativeY = if (preferLeft) rightY else leftY
            val alternativeX = if (preferLeft) Margin + columnWidth + ColumnGap else Margin

            when {
                targetY + height <= BottomLimit -> {
                    val bottom = drawSection(page.canvas, targetX, targetY, columnWidth, section) + 5f
                    if (preferLeft) leftY = bottom else rightY = bottom
                }
                alternativeY + height <= BottomLimit -> {
                    val bottom = drawSection(page.canvas, alternativeX, alternativeY, columnWidth, section) + 5f
                    if (preferLeft) rightY = bottom else leftY = bottom
                }
                else -> {
                    state.finishPage(page)
                    page = state.startPage()
                    startY = drawCompactHeader(page.canvas, content, "Continuação das seções")
                    leftY = startY
                    rightY = startY
                    leftY = drawSection(page.canvas, Margin, leftY, columnWidth, section) + 5f
                }
            }
        }
        state.finishPage(page)

        drawContinuationPages(
            state = state,
            content = content,
            movementStartIndex = 0,
            noteLines = wrapText(content.notes.ifBlank { " " }, textPaint, PageWidth - Margin * 2 - 10f),
            noteStartIndex = 0,
        )
    }

    private fun drawContinuationPages(
        state: WriterState,
        content: ReportPdfContent,
        movementStartIndex: Int,
        noteLines: List<String>,
        noteStartIndex: Int,
    ) {
        var movementIndex = movementStartIndex
        var notesIndex = noteStartIndex
        do {
            val hasMovements = movementIndex < content.movements.size
            val hasNotes = notesIndex < noteLines.size
            if (!hasMovements && !hasNotes) return

            val page = state.startPage()
            var y = drawCompactHeader(page.canvas, content, "Movimentações e anotações")
            if (hasMovements) {
                val availableRows = ((BottomLimit - y - 115f) / MovementRowHeight)
                    .toInt()
                    .coerceAtLeast(1)
                val movementResult = drawMovementTable(
                    canvas = page.canvas,
                    content = content,
                    startIndex = movementIndex,
                    y = y,
                    maximumRows = availableRows,
                    minimumRows = 0,
                )
                movementIndex = movementResult.nextIndex
                y = movementResult.bottom + 5f
                y = drawLegend(page.canvas, y)
            }
            if (notesIndex < noteLines.size) {
                val noteResult = drawNotesBox(
                    canvas = page.canvas,
                    lines = noteLines,
                    startIndex = notesIndex,
                    y = y + 5f,
                    maximumHeight = BottomLimit - y - 5f,
                    title = if (notesIndex == 0) "ANOTAÇÕES E SUGESTÕES" else "ANOTAÇÕES — CONTINUAÇÃO",
                )
                notesIndex = noteResult.nextIndex
            }
            state.finishPage(page)
        } while (movementIndex < content.movements.size || notesIndex < noteLines.size)
    }

    private fun drawFullHeader(canvas: Canvas, content: ReportPdfContent): Float {
        var y = Margin
        drawFilledBox(canvas, Margin, y, PageWidth - Margin, y + 23f, lightFillPaint)
        titlePaint.textSize = 10.2f
        canvas.drawText(
            ellipsize(
                "${content.ministry} — Pastor Presidente: ${content.presidentPastor}",
                titlePaint,
                PageWidth - Margin * 2 - 12f,
            ),
            PageWidth / 2f,
            y + 15f,
            titlePaint,
        )
        y += 23f
        drawBox(canvas, Margin, y, PageWidth - Margin, y + 24f)
        titlePaint.textSize = 9f
        canvas.drawText(
            ellipsize("${content.titlePtBr} — ${content.periodPtBr}", titlePaint, PageWidth - Margin * 2 - 12f),
            PageWidth / 2f,
            y + 15.5f,
            titlePaint,
        )
        y += 24f
        val identityWidth = (PageWidth - Margin * 2) / 3f
        drawIdentityCell(canvas, Margin, y, identityWidth, "REGIONAL", content.regional)
        drawIdentityCell(canvas, Margin + identityWidth, y, identityWidth, "CONGREGAÇÃO", content.congregation)
        drawIdentityCell(canvas, Margin + identityWidth * 2, y, identityWidth, "PR. DIRIGENTE", content.leadPastor)
        return y + 28f + 6f
    }

    private fun drawCompactHeader(canvas: Canvas, content: ReportPdfContent, subtitle: String): Float {
        val y = Margin
        drawFilledBox(canvas, Margin, y, PageWidth - Margin, y + 28f, lightFillPaint)
        titlePaint.textSize = 9.2f
        canvas.drawText(
            ellipsize("${content.titlePtBr} — ${content.periodPtBr}", titlePaint, PageWidth - Margin * 2 - 12f),
            PageWidth / 2f,
            y + 12f,
            titlePaint,
        )
        titlePaint.textSize = 7.6f
        canvas.drawText(
            ellipsize("${content.congregation} — $subtitle", titlePaint, PageWidth - Margin * 2 - 12f),
            PageWidth / 2f,
            y + 22f,
            titlePaint,
        )
        return y + 34f
    }

    private fun drawIdentityCell(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        label: String,
        value: String,
    ) {
        drawBox(canvas, x, y, x + width, y + 28f)
        boldPaint.textSize = 6.7f
        canvas.drawText(label, x + 4f, y + 9f, boldPaint)
        textPaint.textSize = 7.2f
        canvas.drawText(ellipsize(value, textPaint, width - 8f), x + 4f, y + 21f, textPaint)
    }

    private fun sectionHeight(section: ReportPdfSection): Float =
        SectionTitleHeight + TableHeaderHeight + section.rows.size * SectionRowHeight

    private fun drawSection(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        section: ReportPdfSection,
    ): Float {
        var currentY = y
        drawFilledBox(canvas, x, currentY, x + width, currentY + SectionTitleHeight, lightFillPaint)
        boldPaint.textSize = 7.5f
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            ellipsize(section.titlePtBr.uppercase(), boldPaint, width - 8f),
            x + width / 2f,
            currentY + 12f,
            boldPaint,
        )
        boldPaint.textAlign = Paint.Align.LEFT
        currentY += SectionTitleHeight

        val orderWidth = 27f
        val quantityWidth = 40f
        drawFilledBox(canvas, x, currentY, x + width, currentY + TableHeaderHeight, veryLightFillPaint)
        drawVerticalLine(canvas, x + orderWidth, currentY, currentY + TableHeaderHeight)
        drawVerticalLine(canvas, x + width - quantityWidth, currentY, currentY + TableHeaderHeight)
        drawCenteredText(canvas, "ORD", x, currentY, orderWidth, TableHeaderHeight, boldPaint)
        drawCenteredText(
            canvas,
            "DESCRIÇÃO",
            x + orderWidth,
            currentY,
            width - orderWidth - quantityWidth,
            TableHeaderHeight,
            boldPaint,
        )
        drawCenteredText(
            canvas,
            "QTDE",
            x + width - quantityWidth,
            currentY,
            quantityWidth,
            TableHeaderHeight,
            boldPaint,
        )
        currentY += TableHeaderHeight

        section.rows.forEach { row ->
            drawBox(canvas, x, currentY, x + width, currentY + SectionRowHeight)
            drawVerticalLine(canvas, x + orderWidth, currentY, currentY + SectionRowHeight)
            drawVerticalLine(canvas, x + width - quantityWidth, currentY, currentY + SectionRowHeight)
            drawCenteredText(canvas, row.order.toString(), x, currentY, orderWidth, SectionRowHeight, textPaint)
            textPaint.textSize = 7.1f
            canvas.drawText(
                ellipsize(row.labelPtBr, textPaint, width - orderWidth - quantityWidth - 8f),
                x + orderWidth + 4f,
                currentY + 9.7f,
                textPaint,
            )
            drawCenteredText(
                canvas,
                row.value.toString(),
                x + width - quantityWidth,
                currentY,
                quantityWidth,
                SectionRowHeight,
                boldPaint,
            )
            currentY += SectionRowHeight
        }
        return currentY
    }

    private data class MovementDrawResult(val nextIndex: Int, val bottom: Float)

    private fun drawMovementTable(
        canvas: Canvas,
        content: ReportPdfContent,
        startIndex: Int,
        y: Float,
        maximumRows: Int,
        minimumRows: Int,
    ): MovementDrawResult {
        var currentY = y
        val tableWidth = PageWidth - Margin * 2
        val widths = movementColumnWidths(content.movementIncludesBirthDate, tableWidth)
        val headers = if (content.movementIncludesBirthDate) {
            listOf("Nº REG", "NOME DO MEMBRO", "DATA", "CÓD", "OCORRÊNCIA", "DATA NASC.")
        } else {
            listOf("Nº REG", "NOME DO MEMBRO", "DATA", "MOTIVO", "CÓD")
        }
        drawFilledBox(canvas, Margin, currentY, Margin + tableWidth, currentY + TableHeaderHeight, lightFillPaint)
        drawColumns(canvas, Margin, currentY, TableHeaderHeight, widths)
        var x = Margin
        headers.forEachIndexed { index, header ->
            drawCenteredText(canvas, header, x, currentY, widths[index], TableHeaderHeight, boldPaint)
            x += widths[index]
        }
        currentY += TableHeaderHeight

        val remaining = (content.movements.size - startIndex).coerceAtLeast(0)
        val dataRows = minOf(maximumRows, remaining)
        val rowsToDraw = max(minimumRows, dataRows)
        repeat(rowsToDraw) { offset ->
            val movement = content.movements.getOrNull(startIndex + offset)
            drawBox(canvas, Margin, currentY, Margin + tableWidth, currentY + MovementRowHeight)
            drawColumns(canvas, Margin, currentY, MovementRowHeight, widths)
            drawMovementRow(canvas, movement, content.movementIncludesBirthDate, Margin, currentY, widths)
            currentY += MovementRowHeight
        }
        return MovementDrawResult(startIndex + dataRows, currentY)
    }

    private fun movementColumnWidths(includeBirthDate: Boolean, total: Float): List<Float> =
        if (includeBirthDate) {
            listOf(total * 0.09f, total * 0.25f, total * 0.12f, total * 0.07f, total * 0.31f, total * 0.16f)
        } else {
            listOf(total * 0.10f, total * 0.30f, total * 0.14f, total * 0.37f, total * 0.09f)
        }

    private fun drawMovementRow(
        canvas: Canvas,
        movement: ReportPdfMovement?,
        includeBirthDate: Boolean,
        startX: Float,
        y: Float,
        widths: List<Float>,
    ) {
        if (movement == null) return
        val values = if (includeBirthDate) {
            listOf(
                movement.registrationNumber,
                movement.memberName,
                movement.eventDatePtBr,
                movement.reasonCode,
                movement.occurrence,
                movement.birthDatePtBr,
            )
        } else {
            listOf(
                movement.registrationNumber,
                movement.memberName,
                movement.eventDatePtBr,
                movement.reasonLabelPtBr,
                movement.reasonCode,
            )
        }
        var x = startX
        values.forEachIndexed { index, value ->
            textPaint.textSize = if (index == 1 || (includeBirthDate && index == 4) || (!includeBirthDate && index == 3)) {
                6.4f
            } else {
                6.7f
            }
            canvas.drawText(
                ellipsize(value, textPaint, widths[index] - 6f),
                x + 3f,
                y + 11.2f,
                textPaint,
            )
            x += widths[index]
        }
    }

    private fun drawLegend(canvas: Canvas, y: Float): Float {
        val items = listOf(
            "FCO — Fora de comunhão",
            "RCO — Recebidos em comunhão",
            "EXC — Desligados",
            "CME — Carta de mudança expedida",
            "CMR — Carta de mudança recebida",
            "BNA — Batismo nas águas",
            "FAL — Falecimento",
        )
        val width = PageWidth - Margin * 2
        val height = 42f
        drawBox(canvas, Margin, y, Margin + width, y + height)
        val columnWidth = width / 2f
        items.forEachIndexed { index, item ->
            val column = index % 2
            val row = index / 2
            textPaint.textSize = 6.8f
            canvas.drawText(
                ellipsize(item, textPaint, columnWidth - 10f),
                Margin + column * columnWidth + 5f,
                y + 10f + row * 9f,
                textPaint,
            )
        }
        return y + height
    }

    private data class NotesDrawResult(
        val lines: List<String>,
        val nextIndex: Int,
    )

    private fun drawNotesBox(
        canvas: Canvas,
        lines: List<String>,
        startIndex: Int,
        y: Float,
        maximumHeight: Float,
        title: String,
    ): NotesDrawResult {
        val safeHeight = maximumHeight.coerceAtLeast(42f)
        val width = PageWidth - Margin * 2
        val titleHeight = 18f
        val lineHeight = 10f
        val maximumLines = ((safeHeight - titleHeight - 8f) / lineHeight).toInt().coerceAtLeast(1)
        val end = minOf(lines.size, startIndex + maximumLines)
        val actualLines = lines.subList(startIndex.coerceAtMost(lines.size), end)
        val boxHeight = max(42f, titleHeight + 8f + actualLines.size * lineHeight)
        drawFilledBox(canvas, Margin, y, Margin + width, y + titleHeight, lightFillPaint)
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, PageWidth / 2f, y + 12f, boldPaint)
        boldPaint.textAlign = Paint.Align.LEFT
        drawBox(canvas, Margin, y + titleHeight, Margin + width, y + boxHeight)
        actualLines.forEachIndexed { index, line ->
            textPaint.textSize = 7f
            canvas.drawText(line, Margin + 5f, y + titleHeight + 11f + index * lineHeight, textPaint)
        }
        return NotesDrawResult(lines, end)
    }

    private fun drawColumns(
        canvas: Canvas,
        startX: Float,
        y: Float,
        height: Float,
        widths: List<Float>,
    ) {
        var x = startX
        widths.dropLast(1).forEach { width ->
            x += width
            drawVerticalLine(canvas, x, y, y + height)
        }
    }

    private fun drawFilledBox(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        fillPaint: Paint,
    ) {
        canvas.drawRect(left, top, right, bottom, fillPaint)
        canvas.drawRect(left, top, right, bottom, borderPaint)
    }

    private fun drawBox(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawRect(left, top, right, bottom, borderPaint)
    }

    private fun drawVerticalLine(canvas: Canvas, x: Float, top: Float, bottom: Float) {
        canvas.drawLine(x, top, x, bottom, borderPaint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: Paint,
    ) {
        val oldAlignment = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        val baseline = y + height / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(ellipsize(text, paint, width - 4f), x + width / 2f, baseline, paint)
        paint.textAlign = oldAlignment
    }

    private fun ellipsize(text: String, paint: Paint, maximumWidth: Float): String {
        if (paint.measureText(text) <= maximumWidth) return text
        val suffix = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maximumWidth) {
            end--
        }
        return text.substring(0, end) + suffix
    }

    private fun wrapText(text: String, paint: Paint, maximumWidth: Float): List<String> {
        if (text.isBlank()) return listOf(" ")
        val result = mutableListOf<String>()
        text.lines().forEach { paragraph ->
            if (paragraph.isBlank()) {
                result += " "
            } else {
                var line = ""
                paragraph.trim().split(Regex("\\s+")).forEach { word ->
                    val candidate = if (line.isBlank()) word else "$line $word"
                    if (paint.measureText(candidate) <= maximumWidth) {
                        line = candidate
                    } else {
                        if (line.isNotBlank()) result += line
                        line = word
                    }
                }
                if (line.isNotBlank()) result += line
            }
        }
        return result.ifEmpty { listOf(" ") }
    }

    private class WriterState(private val document: PdfDocument) {
        private var pageNumber = 0

        fun startPage(): PdfDocument.Page {
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PageWidth, PageHeight, pageNumber).create()
            return document.startPage(info)
        }

        fun finishPage(page: PdfDocument.Page) {
            val footerPaint = Paint(textPaint).apply {
                textSize = 6.8f
                textAlign = Paint.Align.RIGHT
            }
            page.canvas.drawText("Página $pageNumber", PageWidth - Margin, PageHeight - 12f, footerPaint)
            document.finishPage(page)
        }
    }
}
