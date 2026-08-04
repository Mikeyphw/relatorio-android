package com.mikeyphw.relatorio.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mikeyphw.relatorio.android.pdf.ReportPdfExporter
import com.mikeyphw.relatorio.android.pdf.ReportPdfOptions
import com.mikeyphw.relatorio.model.ReportDraftFactory
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportPdfExporterTest {
    @Test
    fun standardReportProducesReadablePdfFileAndContentUri() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = ReportDraftFactory.create(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(
                month = 8,
                year = 2026,
                congregation = "Primavera VI",
            ),
            nowEpochMillis = 1L,
        )
        val exporter = ReportPdfExporter(context)

        val file = exporter.generate(
            report,
            ReportPdfOptions(
                allTextBold = true,
                officialFormLayout = true,
            ),
        )
        val header = file.inputStream().use { input ->
            ByteArray(4).also { bytes -> assertEquals(4, input.read(bytes)) }
        }

        assertTrue(file.length() > 500L)
        assertEquals("%PDF", header.decodeToString())
        assertEquals("content", exporter.contentUri(file).scheme)
    }
}
