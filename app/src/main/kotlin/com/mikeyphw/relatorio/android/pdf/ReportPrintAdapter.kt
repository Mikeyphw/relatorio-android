package com.mikeyphw.relatorio.android.pdf

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.PageRange
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal fun printPdf(context: Context, file: File, jobName: String) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        ?: error("O serviço de impressão não está disponível neste aparelho.")
    manager.print(
        jobName.take(80),
        ExistingPdfPrintAdapter(file, jobName),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build(),
    )
}

private class ExistingPdfPrintAdapter(
    private val file: File,
    private val documentName: String,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        if (!file.isFile || file.length() <= 0L) {
            callback.onLayoutFailed("O PDF não está disponível.")
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(documentName.take(120))
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        runCatching {
            FileInputStream(file).use { source ->
                FileOutputStream(destination.fileDescriptor).use { target ->
                    val buffer = ByteArray(16_384)
                    while (true) {
                        if (cancellationSignal.isCanceled) {
                            callback.onWriteCancelled()
                            return
                        }
                        val count = source.read(buffer)
                        if (count < 0) break
                        target.write(buffer, 0, count)
                    }
                    target.flush()
                }
            }
        }.onSuccess {
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }.onFailure { error ->
            callback.onWriteFailed(error.message ?: "Não foi possível enviar o PDF para impressão.")
        }
    }
}
