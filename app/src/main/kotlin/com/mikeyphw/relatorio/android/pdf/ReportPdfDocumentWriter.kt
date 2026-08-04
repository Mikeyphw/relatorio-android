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
    private const val OfficialMargin = 14f
    private const val OfficialColumnGap = 6f
    private const val OfficialRowHeight = 12.4f
    private const val OfficialHeaderRowHeight = 14f
    private const val OfficialSectionTitleHeight = 14f
    private const val OfficialMovementRows = 12
    private const val OfficialNoteLineHeight = 12.5f

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

    fun write(
        content: ReportPdfContent,
        output: OutputStream,
        options: ReportPdfOptions = ReportPdfOptions(),
    ) {
        val document = PdfDocument()
        try {
            val state = WriterState(document, options)
            when (content.templateId) {
                ReportTemplateId.AdTimoteo2026 -> {
                    if (options.officialFormLayout) {
                        drawOfficialStandardReport(state, content, options)
                    } else {
                        drawStandardReport(state, content, options)
                    }
                }
                ReportTemplateId.Complete -> drawCompleteReport(state, content, options)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawStandardReport(state: WriterState, content: ReportPdfContent, options: ReportPdfOptions) {
        var page = state.startPage()
        var y = drawFullHeader(page.canvas, content, options)
        val columnWidth = (PageWidth - Margin * 2 - ColumnGap) / 2f
        var leftY = y
        var rightY = y
        content.sections.forEachIndexed { index, section ->
            if (index < 3) {
                leftY = drawSection(page.canvas, Margin, leftY, columnWidth, section, options) + 5f
            } else {
                rightY = drawSection(
                    page.canvas,
                    Margin + columnWidth + ColumnGap,
                    rightY,
                    columnWidth,
                    section,
                    options,
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
            options = options,
        )
        y = firstMovementResult.bottom + 5f
        y = drawLegend(page.canvas, y, options)
        val noteResult = drawNotesBox(
            canvas = page.canvas,
            lines = wrapText(content.notes.ifBlank { " " }, textPaint, PageWidth - Margin * 2 - 10f, options),
            startIndex = 0,
            y = y + 5f,
            maximumHeight = BottomLimit - y - 5f,
            title = "ANOTAÇÕES E SUGESTÕES",
            options = options,
        )
        state.finishPage(page)

        drawContinuationPages(
            state = state,
            content = content,
            movementStartIndex = firstMovementResult.nextIndex,
            noteLines = noteResult.lines,
            noteStartIndex = noteResult.nextIndex,
            options = options,
        )
    }

    private fun drawOfficialStandardReport(
        state: WriterState,
        content: ReportPdfContent,
        options: ReportPdfOptions,
    ) {
        val page = state.startPage()
        val contentWidth = PageWidth - OfficialMargin * 2
        var y = drawOfficialHeader(page.canvas, content, options)
        val columnWidth = (contentWidth - OfficialColumnGap) / 2f
        val leftX = OfficialMargin
        val rightX = OfficialMargin + columnWidth + OfficialColumnGap

        val leftSections = listOfNotNull(
            content.sections.getOrNull(0),
            content.sections.getOrNull(1),
            content.sections.getOrNull(2),
        )
        val rightSections = listOfNotNull(
            content.sections.getOrNull(3),
            content.sections.getOrNull(4),
            content.sections.getOrNull(5),
        )
        val leftBottom = drawOfficialColumn(
            canvas = page.canvas,
            x = leftX,
            y = y,
            width = columnWidth,
            sections = leftSections,
            extraBlankRowsAfterFirstSection = 0,
            extraBlankRowsAfterLastSection = 1,
            options = options,
        )
        val rightBottom = drawOfficialColumn(
            canvas = page.canvas,
            x = rightX,
            y = y,
            width = columnWidth,
            sections = rightSections,
            extraBlankRowsAfterFirstSection = 2,
            extraBlankRowsAfterLastSection = 2,
            options = options,
        )
        y = max(leftBottom, rightBottom) + 12f

        val movementResult = drawOfficialMovementTable(
            canvas = page.canvas,
            content = content,
            startIndex = 0,
            y = y,
            rowsToDraw = OfficialMovementRows,
            options = options,
        )
        y = movementResult.bottom
        y = drawOfficialLegend(page.canvas, y, options)

        val noteLines = wrapText(content.notes.ifBlank { " " }, textPaint, contentWidth - 10f, options)
        val noteResult = drawOfficialNotesBox(
            canvas = page.canvas,
            lines = noteLines,
            startIndex = 0,
            y = y + 10f,
            bottom = BottomLimit,
            options = options,
        )
        state.finishPage(page)

        drawContinuationPages(
            state = state,
            content = content,
            movementStartIndex = movementResult.nextIndex,
            noteLines = noteResult.lines,
            noteStartIndex = noteResult.nextIndex,
            options = options,
        )
    }

    private fun drawOfficialHeader(canvas: Canvas, content: ReportPdfContent, options: ReportPdfOptions): Float {
        var y = 26f
        val left = OfficialMargin
        val right = PageWidth - OfficialMargin
        drawFilledBox(canvas, left, y, right, y + 20f, lightFillPaint)
        titlePaint.textSize = 10.8f
        drawText(
            canvas,
            ellipsize(
                "${content.ministry} - Pastor Presidente: ${content.presidentPastor}",
                titlePaint,
                right - left - 10f,
                options,
            ),
            PageWidth / 2f,
            y + 14.3f,
            titlePaint,
            options,
        )

        y += 20f
        drawBox(canvas, left, y, right, y + 20f)
        boldPaint.textSize = 8.6f
        drawText(
            canvas,
            "RELATÓRIO MENSAL DOS TRABALHOS REALIZADO - MÊS:",
            left + 72f,
            y + 13f,
            boldPaint,
            options,
        )
        drawHorizontalLine(canvas, left + 350f, right - 125f, y + 13.5f)
        drawText(canvas, content.periodPtBr.substringBefore(" de "), left + 365f, y + 13f, boldPaint, options)
        drawText(canvas, "Ano: ${content.periodPtBr.substringAfterLast(' ')}", right - 96f, y + 13f, boldPaint, options)

        y += 20f
        drawBox(canvas, left, y, right, y + 17f)
        val identityWidth = (right - left) / 3f
        drawOfficialInlineIdentity(canvas, left, y, identityWidth, "REGIONAL:", content.regional, options)
        drawOfficialInlineIdentity(canvas, left + identityWidth, y, identityWidth, "CONGREGAÇÃO:", content.congregation, options)
        drawOfficialInlineIdentity(canvas, left + identityWidth * 2f, y, identityWidth, "PR. DIRIGENTE:", content.leadPastor, options)
        return y + 21f
    }

    private fun drawOfficialInlineIdentity(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        label: String,
        value: String,
        options: ReportPdfOptions,
    ) {
        boldPaint.textSize = 7.4f
        drawText(canvas, label, x + 7f, y + 11.5f, boldPaint, options)
        textPaint.textSize = 7.2f
        drawText(canvas, ellipsize(value, textPaint, width - 76f, options), x + 75f, y + 11.5f, textPaint, options)
    }

    private fun drawOfficialColumn(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        sections: List<ReportPdfSection>,
        extraBlankRowsAfterFirstSection: Int,
        extraBlankRowsAfterLastSection: Int,
        options: ReportPdfOptions,
    ): Float {
        var currentY = y
        drawOfficialTableHeader(canvas, x, currentY, width, options)
        currentY += OfficialHeaderRowHeight
        sections.forEachIndexed { index, section ->
            currentY = drawOfficialSection(canvas, x, currentY, width, section, options)
            val blankRows = when (index) {
                0 -> extraBlankRowsAfterFirstSection
                sections.lastIndex -> extraBlankRowsAfterLastSection
                else -> 0
            }
            repeat(blankRows) {
                drawOfficialEmptySectionRow(canvas, x, currentY, width)
                currentY += OfficialRowHeight
            }
        }
        return currentY
    }

    private fun drawOfficialTableHeader(canvas: Canvas, x: Float, y: Float, width: Float, options: ReportPdfOptions) {
        val orderWidth = 40f
        val quantityWidth = 44f
        drawFilledBox(canvas, x, y, x + width, y + OfficialHeaderRowHeight, lightFillPaint)
        drawVerticalLine(canvas, x + orderWidth, y, y + OfficialHeaderRowHeight)
        drawVerticalLine(canvas, x + width - quantityWidth, y, y + OfficialHeaderRowHeight)
        boldPaint.textSize = 7.5f
        drawCenteredText(canvas, "ORD", x, y, orderWidth, OfficialHeaderRowHeight, boldPaint, options)
        drawCenteredText(
            canvas,
            "DESCRIÇÃO",
            x + orderWidth,
            y,
            width - orderWidth - quantityWidth,
            OfficialHeaderRowHeight,
            boldPaint,
            options,
        )
        drawCenteredText(
            canvas,
            "QTDE",
            x + width - quantityWidth,
            y,
            quantityWidth,
            OfficialHeaderRowHeight,
            boldPaint,
            options,
        )
    }

    private fun drawOfficialSection(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        section: ReportPdfSection,
        options: ReportPdfOptions,
    ): Float {
        var currentY = y
        drawFilledBox(canvas, x, currentY, x + width, currentY + OfficialSectionTitleHeight, lightFillPaint)
        boldPaint.textSize = 7.2f
        drawCenteredText(
            canvas,
            officialTitle(section.titlePtBr),
            x,
            currentY,
            width,
            OfficialSectionTitleHeight,
            boldPaint,
            options,
        )
        currentY += OfficialSectionTitleHeight
        section.rows.forEach { row ->
            drawOfficialFieldRow(canvas, x, currentY, width, row, options)
            currentY += OfficialRowHeight
        }
        return currentY
    }

    private fun drawOfficialFieldRow(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        row: ReportPdfFieldRow,
        options: ReportPdfOptions,
    ) {
        val orderWidth = 40f
        val quantityWidth = 44f
        drawBox(canvas, x, y, x + width, y + OfficialRowHeight)
        drawVerticalLine(canvas, x + orderWidth, y, y + OfficialRowHeight)
        drawVerticalLine(canvas, x + width - quantityWidth, y, y + OfficialRowHeight)
        textPaint.textSize = 7.0f
        drawCenteredText(canvas, row.order.toString(), x, y, orderWidth, OfficialRowHeight, textPaint, options)
        drawText(
            canvas,
            ellipsize(row.labelPtBr, textPaint, width - orderWidth - quantityWidth - 7f, options),
            x + orderWidth + 4f,
            y + 8.9f,
            textPaint,
            options,
        )
        boldPaint.textSize = 7.0f
        drawCenteredText(
            canvas,
            row.value.takeIf { it > 0 }?.toString().orEmpty(),
            x + width - quantityWidth,
            y,
            quantityWidth,
            OfficialRowHeight,
            boldPaint,
            options,
        )
    }

    private fun drawOfficialEmptySectionRow(canvas: Canvas, x: Float, y: Float, width: Float) {
        val orderWidth = 40f
        val quantityWidth = 44f
        drawBox(canvas, x, y, x + width, y + OfficialRowHeight)
        drawVerticalLine(canvas, x + orderWidth, y, y + OfficialRowHeight)
        drawVerticalLine(canvas, x + width - quantityWidth, y, y + OfficialRowHeight)
    }

    private fun drawOfficialMovementTable(
        canvas: Canvas,
        content: ReportPdfContent,
        startIndex: Int,
        y: Float,
        rowsToDraw: Int,
        options: ReportPdfOptions,
    ): MovementDrawResult {
        var currentY = y
        val left = OfficialMargin
        val tableWidth = PageWidth - OfficialMargin * 2
        val widths = listOf(
            tableWidth * 0.07f,
            tableWidth * 0.30f,
            tableWidth * 0.08f,
            tableWidth * 0.08f,
            tableWidth * 0.36f,
            tableWidth * 0.11f,
        )
        drawFilledBox(canvas, left, currentY, left + tableWidth, currentY + OfficialHeaderRowHeight, lightFillPaint)
        drawCenteredText(
            canvas,
            "MOTIVO",
            left + widths.take(3).sum(),
            currentY,
            widths[3] + widths[4],
            OfficialHeaderRowHeight,
            boldPaint,
            options,
        )
        currentY += OfficialHeaderRowHeight
        drawFilledBox(canvas, left, currentY, left + tableWidth, currentY + OfficialHeaderRowHeight, veryLightFillPaint)
        drawColumns(canvas, left, currentY, OfficialHeaderRowHeight, widths)
        val headers = listOf("Nº REG", "NOME DO MEMBRO", "DATA", "CÓD", "OCORRÊNCIA", "DATA NACIM.")
        var x = left
        headers.forEachIndexed { index, header ->
            boldPaint.textSize = 7.2f
            drawCenteredText(canvas, header, x, currentY, widths[index], OfficialHeaderRowHeight, boldPaint, options)
            x += widths[index]
        }
        currentY += OfficialHeaderRowHeight

        val dataRows = minOf(rowsToDraw, (content.movements.size - startIndex).coerceAtLeast(0))
        repeat(rowsToDraw) { offset ->
            val movement = content.movements.getOrNull(startIndex + offset)
            drawBox(canvas, left, currentY, left + tableWidth, currentY + OfficialRowHeight)
            drawColumns(canvas, left, currentY, OfficialRowHeight, widths)
            if (movement != null) {
                drawOfficialMovementRow(canvas, movement, left, currentY, widths, options)
            }
            currentY += OfficialRowHeight
        }
        return MovementDrawResult(startIndex + dataRows, currentY)
    }

    private fun drawOfficialMovementRow(
        canvas: Canvas,
        movement: ReportPdfMovement,
        startX: Float,
        y: Float,
        widths: List<Float>,
        options: ReportPdfOptions,
    ) {
        val values = listOf(
            movement.registrationNumber,
            movement.memberName,
            movement.eventDatePtBr,
            movement.reasonCode,
            movement.occurrence,
            movement.birthDatePtBr,
        )
        var x = startX
        values.forEachIndexed { index, value ->
            textPaint.textSize = if (index == 1 || index == 4) 6.3f else 6.6f
            drawText(
                canvas,
                ellipsize(value, textPaint, widths[index] - 5f, options),
                x + 3f,
                y + 8.9f,
                textPaint,
                options,
            )
            x += widths[index]
        }
    }

    private fun drawOfficialLegend(canvas: Canvas, y: Float, options: ReportPdfOptions): Float {
        val left = OfficialMargin
        val width = PageWidth - OfficialMargin * 2
        val rowHeight = 12.4f
        val height = OfficialSectionTitleHeight + rowHeight * 4
        drawFilledBox(canvas, left, y, left + width, y + OfficialSectionTitleHeight, lightFillPaint)
        boldPaint.textSize = 7.4f
        drawCenteredText(canvas, "LEGENDA", left, y, width, OfficialSectionTitleHeight, boldPaint, options)
        drawBox(canvas, left, y + OfficialSectionTitleHeight, left + width, y + height)
        val columns = listOf(40f, width / 2f - 40f, 40f, width / 2f - 40f)
        var x = left
        columns.dropLast(1).forEach { columnWidth ->
            x += columnWidth
            drawVerticalLine(canvas, x, y + OfficialSectionTitleHeight, y + height)
        }
        val items = listOf(
            "FCO" to "Fora de comunhão",
            "CMR" to "Carta de mudança recebida",
            "RCO" to "Recebidos em comunhão",
            "BNA" to "Batismo nas águas",
            "EXC" to "Desligados",
            "FAL" to "Falecimento",
            "CME" to "Carta de mudança expedida",
            "" to "",
        )
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            val rowY = y + OfficialSectionTitleHeight + rowIndex * rowHeight
            drawHorizontalLine(canvas, left, left + width, rowY + rowHeight)
            val first = rowItems[0]
            val second = rowItems.getOrElse(1) { "" to "" }
            drawOfficialLegendItem(canvas, first, left, rowY, columns[0], columns[1], rowHeight, options)
            drawOfficialLegendItem(
                canvas,
                second,
                left + columns[0] + columns[1],
                rowY,
                columns[2],
                columns[3],
                rowHeight,
                options,
            )
        }
        return y + height
    }

    private fun drawOfficialLegendItem(
        canvas: Canvas,
        item: Pair<String, String>,
        x: Float,
        y: Float,
        codeWidth: Float,
        textWidth: Float,
        height: Float,
        options: ReportPdfOptions,
    ) {
        boldPaint.textSize = 7f
        textPaint.textSize = 7f
        drawCenteredText(canvas, item.first, x, y, codeWidth, height, boldPaint, options)
        drawText(canvas, ellipsize(item.second, textPaint, textWidth - 4f, options), x + codeWidth + 3f, y + 8.7f, textPaint, options)
    }

    private fun drawOfficialNotesBox(
        canvas: Canvas,
        lines: List<String>,
        startIndex: Int,
        y: Float,
        bottom: Float,
        options: ReportPdfOptions,
    ): NotesDrawResult {
        val left = OfficialMargin
        val width = PageWidth - OfficialMargin * 2
        val titleHeight = OfficialSectionTitleHeight
        val availableLines = ((bottom - y - titleHeight) / OfficialNoteLineHeight).toInt().coerceAtLeast(1)
        val end = minOf(lines.size, startIndex + availableLines)
        val actualLines = lines.subList(startIndex.coerceAtMost(lines.size), end)
        val boxBottom = y + titleHeight + availableLines * OfficialNoteLineHeight
        drawFilledBox(canvas, left, y, left + width, y + titleHeight, lightFillPaint)
        boldPaint.textSize = 7.5f
        drawCenteredText(canvas, "ANOTAÇÕES E SUGESTÕES", left, y, width, titleHeight, boldPaint, options)
        drawBox(canvas, left, y + titleHeight, left + width, boxBottom)
        repeat(availableLines) { index ->
            val lineY = y + titleHeight + (index + 1) * OfficialNoteLineHeight
            drawHorizontalLine(canvas, left, left + width, lineY)
        }
        actualLines.forEachIndexed { index, line ->
            textPaint.textSize = 7f
            drawText(canvas, line, left + 5f, y + titleHeight + 9f + index * OfficialNoteLineHeight, textPaint, options)
        }
        return NotesDrawResult(lines, end)
    }

    private fun officialTitle(title: String): String = when (title) {
        "Ministros e obreiros" -> "Total ministros - obreiros"
        "Total de membros" -> "Total membros"
        else -> title
    }

    private fun drawCompleteReport(state: WriterState, content: ReportPdfContent, options: ReportPdfOptions) {
        var page = state.startPage()
        var startY = drawFullHeader(page.canvas, content, options)
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
                    val bottom = drawSection(page.canvas, targetX, targetY, columnWidth, section, options) + 5f
                    if (preferLeft) leftY = bottom else rightY = bottom
                }
                alternativeY + height <= BottomLimit -> {
                    val bottom = drawSection(
                        page.canvas,
                        alternativeX,
                        alternativeY,
                        columnWidth,
                        section,
                        options,
                    ) + 5f
                    if (preferLeft) rightY = bottom else leftY = bottom
                }
                else -> {
                    state.finishPage(page)
                    page = state.startPage()
                    startY = drawCompactHeader(page.canvas, content, "Continuação das seções", options)
                    leftY = startY
                    rightY = startY
                    leftY = drawSection(page.canvas, Margin, leftY, columnWidth, section, options) + 5f
                }
            }
        }
        state.finishPage(page)

        drawContinuationPages(
            state = state,
            content = content,
            movementStartIndex = 0,
            noteLines = wrapText(content.notes.ifBlank { " " }, textPaint, PageWidth - Margin * 2 - 10f, options),
            noteStartIndex = 0,
            options = options,
        )
    }

    private fun drawContinuationPages(
        state: WriterState,
        content: ReportPdfContent,
        movementStartIndex: Int,
        noteLines: List<String>,
        noteStartIndex: Int,
        options: ReportPdfOptions,
    ) {
        var movementIndex = movementStartIndex
        var notesIndex = noteStartIndex
        do {
            val hasMovements = movementIndex < content.movements.size
            val hasNotes = notesIndex < noteLines.size
            if (!hasMovements && !hasNotes) return

            val page = state.startPage()
            var y = drawCompactHeader(page.canvas, content, "Movimentações e anotações", options)
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
                    options = options,
                )
                movementIndex = movementResult.nextIndex
                y = movementResult.bottom + 5f
                y = drawLegend(page.canvas, y, options)
            }
            if (notesIndex < noteLines.size) {
                val noteResult = drawNotesBox(
                    canvas = page.canvas,
                    lines = noteLines,
                    startIndex = notesIndex,
                    y = y + 5f,
                    maximumHeight = BottomLimit - y - 5f,
                    title = if (notesIndex == 0) "ANOTAÇÕES E SUGESTÕES" else "ANOTAÇÕES — CONTINUAÇÃO",
                    options = options,
                )
                notesIndex = noteResult.nextIndex
            }
            state.finishPage(page)
        } while (movementIndex < content.movements.size || notesIndex < noteLines.size)
    }

    private fun drawFullHeader(canvas: Canvas, content: ReportPdfContent, options: ReportPdfOptions): Float {
        var y = Margin
        drawFilledBox(canvas, Margin, y, PageWidth - Margin, y + 23f, lightFillPaint)
        titlePaint.textSize = 10.2f
        drawText(
            canvas,
            ellipsize(
                "${content.ministry} — Pastor Presidente: ${content.presidentPastor}",
                titlePaint,
                PageWidth - Margin * 2 - 12f,
                options,
            ),
            PageWidth / 2f,
            y + 15f,
            titlePaint,
            options,
        )
        y += 23f
        drawBox(canvas, Margin, y, PageWidth - Margin, y + 24f)
        titlePaint.textSize = 9f
        drawText(
            canvas,
            ellipsize("${content.titlePtBr} — ${content.periodPtBr}", titlePaint, PageWidth - Margin * 2 - 12f, options),
            PageWidth / 2f,
            y + 15.5f,
            titlePaint,
            options,
        )
        y += 24f
        val identityWidth = (PageWidth - Margin * 2) / 3f
        drawIdentityCell(canvas, Margin, y, identityWidth, "REGIONAL", content.regional, options)
        drawIdentityCell(canvas, Margin + identityWidth, y, identityWidth, "CONGREGAÇÃO", content.congregation, options)
        drawIdentityCell(canvas, Margin + identityWidth * 2, y, identityWidth, "PR. DIRIGENTE", content.leadPastor, options)
        return y + 28f + 6f
    }

    private fun drawCompactHeader(
        canvas: Canvas,
        content: ReportPdfContent,
        subtitle: String,
        options: ReportPdfOptions,
    ): Float {
        val y = Margin
        drawFilledBox(canvas, Margin, y, PageWidth - Margin, y + 28f, lightFillPaint)
        titlePaint.textSize = 9.2f
        drawText(
            canvas,
            ellipsize("${content.titlePtBr} — ${content.periodPtBr}", titlePaint, PageWidth - Margin * 2 - 12f, options),
            PageWidth / 2f,
            y + 12f,
            titlePaint,
            options,
        )
        titlePaint.textSize = 7.6f
        drawText(
            canvas,
            ellipsize("${content.congregation} — $subtitle", titlePaint, PageWidth - Margin * 2 - 12f, options),
            PageWidth / 2f,
            y + 22f,
            titlePaint,
            options,
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
        options: ReportPdfOptions,
    ) {
        drawBox(canvas, x, y, x + width, y + 28f)
        boldPaint.textSize = 6.7f
        drawText(canvas, label, x + 4f, y + 9f, boldPaint, options)
        textPaint.textSize = 7.2f
        drawText(canvas, ellipsize(value, textPaint, width - 8f, options), x + 4f, y + 21f, textPaint, options)
    }

    private fun sectionHeight(section: ReportPdfSection): Float =
        SectionTitleHeight + TableHeaderHeight + section.rows.size * SectionRowHeight

    private fun drawSection(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        section: ReportPdfSection,
        options: ReportPdfOptions,
    ): Float {
        var currentY = y
        drawFilledBox(canvas, x, currentY, x + width, currentY + SectionTitleHeight, lightFillPaint)
        boldPaint.textSize = 7.5f
        boldPaint.textAlign = Paint.Align.CENTER
        drawText(
            canvas,
            ellipsize(section.titlePtBr, boldPaint, width - 8f, options),
            x + width / 2f,
            currentY + 12f,
            boldPaint,
            options,
        )
        boldPaint.textAlign = Paint.Align.LEFT
        currentY += SectionTitleHeight

        val orderWidth = 27f
        val quantityWidth = 40f
        drawFilledBox(canvas, x, currentY, x + width, currentY + TableHeaderHeight, veryLightFillPaint)
        drawVerticalLine(canvas, x + orderWidth, currentY, currentY + TableHeaderHeight)
        drawVerticalLine(canvas, x + width - quantityWidth, currentY, currentY + TableHeaderHeight)
        drawCenteredText(canvas, "ORD", x, currentY, orderWidth, TableHeaderHeight, boldPaint, options)
        drawCenteredText(
            canvas,
            "DESCRIÇÃO",
            x + orderWidth,
            currentY,
            width - orderWidth - quantityWidth,
            TableHeaderHeight,
            boldPaint,
            options,
        )
        drawCenteredText(
            canvas,
            "QTDE",
            x + width - quantityWidth,
            currentY,
            quantityWidth,
            TableHeaderHeight,
            boldPaint,
            options,
        )
        currentY += TableHeaderHeight

        section.rows.forEach { row ->
            drawBox(canvas, x, currentY, x + width, currentY + SectionRowHeight)
            drawVerticalLine(canvas, x + orderWidth, currentY, currentY + SectionRowHeight)
            drawVerticalLine(canvas, x + width - quantityWidth, currentY, currentY + SectionRowHeight)
            drawCenteredText(canvas, row.order.toString(), x, currentY, orderWidth, SectionRowHeight, textPaint, options)
            textPaint.textSize = 7.1f
            drawText(
                canvas,
                ellipsize(row.labelPtBr, textPaint, width - orderWidth - quantityWidth - 8f, options),
                x + orderWidth + 4f,
                currentY + 9.7f,
                textPaint,
                options,
            )
            drawCenteredText(
                canvas,
                row.value.toString(),
                x + width - quantityWidth,
                currentY,
                quantityWidth,
                SectionRowHeight,
                boldPaint,
                options,
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
        options: ReportPdfOptions,
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
            drawCenteredText(canvas, header, x, currentY, widths[index], TableHeaderHeight, boldPaint, options)
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
            drawMovementRow(canvas, movement, content.movementIncludesBirthDate, Margin, currentY, widths, options)
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
        options: ReportPdfOptions,
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
            drawText(
                canvas,
                ellipsize(value, textPaint, widths[index] - 6f, options),
                x + 3f,
                y + 11.2f,
                textPaint,
                options,
            )
            x += widths[index]
        }
    }

    private fun drawLegend(canvas: Canvas, y: Float, options: ReportPdfOptions): Float {
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
            drawText(
                canvas,
                ellipsize(item, textPaint, columnWidth - 10f, options),
                Margin + column * columnWidth + 5f,
                y + 10f + row * 9f,
                textPaint,
                options,
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
        options: ReportPdfOptions,
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
        drawText(canvas, title, PageWidth / 2f, y + 12f, boldPaint, options)
        boldPaint.textAlign = Paint.Align.LEFT
        drawBox(canvas, Margin, y + titleHeight, Margin + width, y + boxHeight)
        actualLines.forEachIndexed { index, line ->
            textPaint.textSize = 7f
            drawText(canvas, line, Margin + 5f, y + titleHeight + 11f + index * lineHeight, textPaint, options)
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

    private fun drawHorizontalLine(canvas: Canvas, left: Float, right: Float, y: Float) {
        canvas.drawLine(left, y, right, y, borderPaint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: Paint,
        options: ReportPdfOptions,
    ) {
        val oldAlignment = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        val drawPaint = paint.forOptions(options)
        val baseline = y + height / 2f - (drawPaint.ascent() + drawPaint.descent()) / 2f
        drawText(canvas, ellipsize(text, paint, width - 4f, options), x + width / 2f, baseline, paint, options)
        paint.textAlign = oldAlignment
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        options: ReportPdfOptions,
    ) {
        canvas.drawText(text.display(options), x, y, paint.forOptions(options))
    }

    private fun ellipsize(text: String, paint: Paint, maximumWidth: Float, options: ReportPdfOptions): String {
        val displayText = text.display(options)
        val measurePaint = paint.forOptions(options)
        if (measurePaint.measureText(displayText) <= maximumWidth) return displayText
        val suffix = "…"
        var end = displayText.length
        while (end > 0 && measurePaint.measureText(displayText.substring(0, end) + suffix) > maximumWidth) {
            end--
        }
        return displayText.substring(0, end) + suffix
    }

    private fun wrapText(text: String, paint: Paint, maximumWidth: Float, options: ReportPdfOptions): List<String> {
        val displayText = text.display(options)
        if (displayText.isBlank()) return listOf(" ")
        val measurePaint = paint.forOptions(options)
        val result = mutableListOf<String>()
        displayText.lines().forEach { paragraph ->
            if (paragraph.isBlank()) {
                result += " "
            } else {
                var line = ""
                paragraph.trim().split(Regex("\\s+")).forEach { word ->
                    val candidate = if (line.isBlank()) word else "$line $word"
                    if (measurePaint.measureText(candidate) <= maximumWidth) {
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

    private fun String.display(options: ReportPdfOptions): String =
        if (options.uppercaseText) uppercase() else this

    private fun Paint.forOptions(options: ReportPdfOptions): Paint {
        if (!options.allTextBold || typeface?.style == Typeface.BOLD) return this
        return Paint(this).apply {
            typeface = Typeface.create(typeface ?: Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private class WriterState(
        private val document: PdfDocument,
        private val options: ReportPdfOptions,
    ) {
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
            drawText(page.canvas, "Página $pageNumber", PageWidth - Margin, PageHeight - 12f, footerPaint, options)
            document.finishPage(page)
        }
    }
}
