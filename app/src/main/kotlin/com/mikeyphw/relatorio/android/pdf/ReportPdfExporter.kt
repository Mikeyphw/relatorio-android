package com.mikeyphw.relatorio.android.pdf

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportPdfContentFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportPdfExporter(private val context: Context) {
    suspend fun generate(report: MonthlyReport): File = withContext(Dispatchers.IO) {
        val content = ReportPdfContentFactory.create(report)
        val directory = File(context.cacheDir, ExportDirectoryName).apply { mkdirs() }
        removeOldExports(directory)
        val destination = File(directory, content.suggestedFileName)
        destination.outputStream().buffered().use { output ->
            ReportPdfDocumentWriter.write(content, output)
        }
        check(destination.isFile && destination.length() > 0L) {
            "O arquivo PDF não foi criado."
        }
        destination
    }

    suspend fun copyTo(file: File, destination: Uri) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination)
            ?: error("Não foi possível abrir o local escolhido.")
        output.buffered().use { target ->
            file.inputStream().buffered().use { source -> source.copyTo(target) }
        }
    }

    fun contentUri(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun removeOldExports(directory: File) {
        directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending(File::lastModified)
            ?.drop((MaximumCachedExports - 1).coerceAtLeast(0))
            ?.forEach(File::delete)
    }

    private companion object {
        const val ExportDirectoryName = "pdf_exports"
        const val MaximumCachedExports = 12
    }
}
