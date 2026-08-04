package com.mikeyphw.relatorio.model

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBackupTest {
    @Test
    fun xmlRoundTripPreservesReportsMovementsNotesAndPreferences() {
        val report = ReportDraftFactory.create(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "São José & Filhos"),
            nowEpochMillis = 100L,
        ).copy(
            status = ReportStatus.Completed,
            values = ReportDraftFactory.create(
                templateId = ReportTemplateId.AdTimoteo2026,
                identity = ReportIdentity(month = 8, year = 2026, congregation = "São José & Filhos"),
                nowEpochMillis = 100L,
            ).values.map { if (it.fieldId == "members.members") it.copy(value = 91) else it },
            movements = listOf(
                MemberMovement(
                    id = "movement-1",
                    memberName = "João <Silva>",
                    reasonCode = "BNA",
                    occurrence = "Batismo & recepção",
                ),
            ),
            notes = "Linha 1\nLinha 2 & mais 📋",
        )
        val source = ReportBackupArchive(
            createdAtEpochMillis = 500L,
            appVersion = "1.0.0",
            reports = listOf(report),
            preferences = BackupPreferencesSnapshot(
                congregation = "São José & Filhos",
                highContrast = true,
                pdfAllTextBold = true,
                pdfOfficialFormLayout = true,
            ),
        )

        val restored = ReportBackupXmlCodec.decode(ReportBackupXmlCodec.encode(source))

        assertEquals(source, restored)
    }

    @Test
    fun oldBackupWithoutPdfBoldPreferenceRestoresTheDefaultValue() {
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 20L,
            appVersion = "1.0.0",
            reports = emptyList(),
            preferences = BackupPreferencesSnapshot(highContrast = true),
        )
        val oldXml = ReportBackupXmlCodec.encode(archive)
            .replace(" pdf-all-text-bold=\"false\"", "")
            .replace(" pdf-official-form-layout=\"false\"", "")
            .withUpdatedChecksum()

        val restored = ReportBackupXmlCodec.decode(oldXml)

        assertFalse(restored.preferences.pdfAllTextBold)
        assertFalse(restored.preferences.pdfOfficialFormLayout)
        assertTrue(restored.preferences.highContrast)
    }

    @Test
    fun doctypeIsRejectedEvenWhenTheChecksumMatches() {
        val report = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "Central"),
            10L,
        )
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 20L,
            appVersion = "1.0.0",
            reports = listOf(report),
            preferences = BackupPreferencesSnapshot(),
        )
        val malicious = ReportBackupXmlCodec.encode(archive)
            .replace(
                "\n<relatorio-backup",
                "\n<!DOCTYPE x [<!ENTITY y SYSTEM=\"file:///etc/passwd\">]>\n<relatorio-backup",
            )
            .withUpdatedChecksum()

        val result = runCatching { ReportBackupXmlCodec.decode(malicious) }

        assertTrue(result.isFailure)
    }

    @Test
    fun previewSkipsEquivalentCompletedReportsAndProtectsCurrentDraft() {
        val existingCompleted = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "São José"),
            10L,
        ).copy(status = ReportStatus.Completed)
        val currentDraft = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 9, year = 2026, congregation = "Central"),
            20L,
        )
        val duplicate = existingCompleted.copy(id = "different-id")
        val backupDraft = currentDraft.copy(id = "backup-draft")
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 30L,
            appVersion = "1.0.0",
            reports = listOf(duplicate, backupDraft),
            preferences = BackupPreferencesSnapshot(),
        )

        val protectedPreview = ReportBackupPlanner.preview(archive, listOf(existingCompleted, currentDraft), false)
        assertEquals(1, protectedPreview.skippedDuplicateCount)
        assertEquals(1, protectedPreview.skippedDraftCount)
        assertTrue(protectedPreview.canRestore)
        assertTrue(protectedPreview.willRestorePreferences)

        val replacingPreview = ReportBackupPlanner.preview(archive, listOf(existingCompleted, currentDraft), true)
        assertEquals(listOf("backup-draft"), replacingPreview.importableReports.map { it.id })
        assertTrue(replacingPreview.willReplaceCurrentDraft)
    }

    @Test
    fun replacementAcceptsBackupDraftWithTheSameIdentifier() {
        val currentDraft = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "Atual"),
            10L,
        )
        val backupDraft = currentDraft.copy(
            identity = currentDraft.identity.copy(congregation = "Restaurada"),
            updatedAtEpochMillis = 20L,
        )
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 30L,
            appVersion = "1.0.0",
            reports = listOf(backupDraft),
            preferences = BackupPreferencesSnapshot(),
        )

        val preview = ReportBackupPlanner.preview(archive, listOf(currentDraft), true)

        assertEquals(listOf(currentDraft.id), preview.importableReports.map { it.id })
        assertEquals(0, preview.skippedDuplicateCount)
        assertTrue(preview.willReplaceCurrentDraft)
    }

    @Test
    fun replaceFlagDoesNotIgnoreDraftMovementIdsWhenBackupHasNoDraft() {
        val currentDraft = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "Atual"),
            10L,
        ).copy(
            movements = listOf(
                MemberMovement(id = "shared-movement", memberName = "Atual", reasonCode = "BNA"),
            ),
        )
        val completedFromBackup = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 9, year = 2026, congregation = "Backup"),
            20L,
        ).copy(
            id = "completed-backup",
            status = ReportStatus.Completed,
            movements = listOf(
                MemberMovement(id = "shared-movement", memberName = "Outra", reasonCode = "CMR"),
            ),
        )
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 30L,
            appVersion = "1.0.0",
            reports = listOf(completedFromBackup),
            preferences = BackupPreferencesSnapshot(),
        )

        val preview = ReportBackupPlanner.preview(archive, listOf(currentDraft), true)

        assertTrue(preview.canRestore)
        assertTrue(preview.willRestorePreferences)
        assertEquals(1, preview.skippedDuplicateCount)
        assertFalse(preview.willReplaceCurrentDraft)
    }


    @Test
    fun unsupportedPreferenceValuesAreRejected() {
        val report = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "Central"),
            10L,
        )
        val invalid = ReportBackupArchive(
            createdAtEpochMillis = 20L,
            appVersion = "1.0.0",
            reports = listOf(report),
            preferences = BackupPreferencesSnapshot(textSizePreferenceId = "gigante"),
        )

        val result = runCatching { ReportBackupXmlCodec.encode(invalid) }

        assertTrue(result.isFailure)
    }


    @Test
    fun changedBackupContentFailsIntegrityCheck() {
        val report = ReportDraftFactory.create(
            ReportTemplateId.AdTimoteo2026,
            ReportIdentity(month = 8, year = 2026, congregation = "Central"),
            10L,
        )
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 20L,
            appVersion = "1.0.0",
            reports = listOf(report),
            preferences = BackupPreferencesSnapshot(),
        )
        val changed = ReportBackupXmlCodec.encode(archive)
            .replace("congregation=\"Central\"", "congregation=\"Outra\"")

        val result = runCatching { ReportBackupXmlCodec.decode(changed) }

        assertTrue(result.isFailure)
    }


    @Test
    fun validBackupWithoutReportsCanRestorePreferences() {
        val archive = ReportBackupArchive(
            createdAtEpochMillis = 30L,
            appVersion = "1.0.1",
            reports = emptyList(),
            preferences = BackupPreferencesSnapshot(
                congregation = "Congregação restaurada",
                textSizePreferenceId = "extra_large",
                highContrast = true,
            ),
        )

        val preview = ReportBackupPlanner.preview(
            archive = archive,
            existingReports = emptyList(),
            replaceCurrentDraft = false,
        )

        assertTrue(preview.canRestore)
        assertTrue(preview.willRestorePreferences)
        assertTrue(preview.importableReports.isEmpty())
    }

    private fun String.withUpdatedChecksum(): String {
        val checksumRegex = Regex("""checksum-sha256="([0-9a-fA-F]{64})"""")
        val match = requireNotNull(checksumRegex.find(this))
        val placeholder = "0".repeat(64)
        val unsigned = replaceRange(match.groups[1]!!.range, placeholder)
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(unsigned.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return unsigned.replaceFirst(placeholder, checksum)
    }

}
