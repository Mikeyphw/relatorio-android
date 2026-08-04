package com.mikeyphw.relatorio.android.backup

import android.content.Context
import android.net.Uri
import com.mikeyphw.relatorio.android.pdf.ReportPdfDocumentWriter
import com.mikeyphw.relatorio.android.pdf.ReportPdfOptions
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportBackupArchive
import com.mikeyphw.relatorio.model.ReportBackupXmlCodec
import com.mikeyphw.relatorio.model.ReportPdfContentFactory
import com.mikeyphw.relatorio.model.ReportStatus
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportBackupManager(private val context: Context) {
    suspend fun writeBackup(destination: Uri, archive: ReportBackupArchive) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Não foi possível abrir o local escolhido.")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(ReportBackupXmlCodec.encode(archive))
        }
    }

    suspend fun writeBundle(destination: Uri, archive: ReportBackupArchive): Int = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Não foi possível abrir o local escolhido.")
        val completedReports = archive.reports.filter { it.status == ReportStatus.Completed }
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupEntryName))
            zip.write(ReportBackupXmlCodec.encode(archive).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            val usedNames = mutableSetOf<String>()
            val pdfOptions = ReportPdfOptions(
                allTextBold = archive.preferences.pdfAllTextBold,
                officialFormLayout = archive.preferences.pdfOfficialFormLayout,
            )
            completedReports.forEach { report ->
                val content = ReportPdfContentFactory.create(report)
                val fileName = uniquePdfName(content.suggestedFileName, report, usedNames)
                zip.putNextEntry(ZipEntry("pdf/$fileName"))
                ReportPdfDocumentWriter.write(content, zip, pdfOptions)
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("LEIA-ME.txt"))
            zip.write(
                "Pacote criado pelo Relatório Mensal AD.\n".toByteArray(Charsets.UTF_8),
            )
            zip.write(
                "O arquivo $BackupEntryName permite restaurar os dados no aplicativo.\n".toByteArray(Charsets.UTF_8),
            )
            zip.closeEntry()
        }
        completedReports.size
    }

    suspend fun readBackup(source: Uri): ReportBackupArchive = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(source)
            ?: error("Não foi possível abrir o arquivo escolhido.")
        input.buffered().use(::readArchive)
    }

    private fun readArchive(input: InputStream): ReportBackupArchive {
        input.mark(4)
        val signature = ByteArray(4)
        val read = input.read(signature)
        input.reset()
        val isZip = read >= 2 && signature[0] == 'P'.code.toByte() && signature[1] == 'K'.code.toByte()
        val xml = if (isZip) readBackupXmlFromZip(input) else input.readLimitedText(MaximumBackupBytes)
        return ReportBackupXmlCodec.decode(xml)
    }

    private fun readBackupXmlFromZip(input: InputStream): String {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            var scanned = 0
            while (entry != null) {
                scanned += 1
                require(scanned <= MaximumZipEntries) { "O pacote contém arquivos demais." }
                if (!entry.isDirectory && entry.name == BackupEntryName) {
                    return zip.readLimitedText(MaximumBackupBytes)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        error("O pacote não contém $BackupEntryName.")
    }

    private fun InputStream.readLimitedText(maximumBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            require(total <= maximumBytes) { "O arquivo de backup é grande demais." }
            output.write(buffer, 0, count)
        }
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray()))
            .toString()
    }

    private fun uniquePdfName(
        suggested: String,
        report: MonthlyReport,
        usedNames: MutableSet<String>,
    ): String {
        val safe = sanitizeFileName(suggested).ifBlank { "RELATORIO.PDF" }
        if (usedNames.add(safe)) return safe
        val base = safe.removeSuffix(".PDF").removeSuffix(".pdf")
        val withId = "${base}_${report.id.take(8)}.PDF"
        usedNames += withId
        return withId
    }

    private fun sanitizeFileName(value: String): String = value
        .replace(UnsafeFileCharacters, "_")
        .take(MaximumFileNameLength)

    private companion object {
        const val BackupEntryName = "backup/relatorio-backup.xml"
        const val MaximumBackupBytes = 8 * 1024 * 1024
        const val MaximumZipEntries = 1_000
        const val MaximumFileNameLength = 180
        val UnsafeFileCharacters = "[\\\\/:*?\"<>|]".toRegex()
    }
}
