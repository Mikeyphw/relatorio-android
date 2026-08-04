package com.mikeyphw.relatorio.android.pdf

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.core.content.FileProvider
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportPdfContentFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportPdfExporter(private val context: Context) {
    suspend fun generate(report: MonthlyReport): File = withContext(Dispatchers.IO) {
        val content = ReportPdfContentFactory.create(report)
        val directory = File(context.cacheDir, ExportDirectoryName).apply {
            check(exists() || mkdirs()) { "Não foi possível preparar a pasta temporária do PDF." }
        }
        removeOldExports(directory)
        ensureFreeSpace(directory)

        val destination = File(directory, content.suggestedFileName)
        val temporary = File(directory, ".${content.suggestedFileName}.${System.nanoTime()}.tmp")
        runCatching {
            temporary.outputStream().buffered().use { output ->
                ReportPdfDocumentWriter.write(content, output)
            }
            check(temporary.isFile && temporary.length() > 0L) { "O arquivo PDF não foi criado." }
            if (destination.exists() && !destination.delete()) {
                error("Não foi possível substituir o PDF temporário anterior.")
            }
            check(temporary.renameTo(destination)) { "Não foi possível finalizar o PDF." }
            destination
        }.getOrElse { error ->
            temporary.delete()
            throw error
        }
    }

    suspend fun copyTo(file: File, destination: Uri) = withContext(Dispatchers.IO) {
        check(file.isFile && file.length() > 0L) { "O PDF temporário não está disponível." }
        val output = context.contentResolver.openOutputStream(destination, "wt")
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

    private fun ensureFreeSpace(directory: File) {
        val available = StatFs(directory.absolutePath).availableBytes
        check(available >= MinimumFreeBytes) {
            "Não há espaço livre suficiente para criar o PDF. Libere espaço e tente novamente."
        }
    }

    private fun removeOldExports(directory: File) {
        directory.listFiles()
            ?.filter { it.isFile && (it.extension.equals("pdf", ignoreCase = true) || it.extension == "tmp") }
            ?.sortedByDescending(File::lastModified)
            ?.drop((MaximumCachedExports - 1).coerceAtLeast(0))
            ?.forEach(File::delete)
    }

    private companion object {
        const val ExportDirectoryName = "pdf_exports"
        const val MaximumCachedExports = 12
        const val MinimumFreeBytes = 2L * 1024L * 1024L
    }
}
