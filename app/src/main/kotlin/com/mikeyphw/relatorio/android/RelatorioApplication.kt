package com.mikeyphw.relatorio.android

import android.app.Application
import androidx.room.Room
import com.mikeyphw.relatorio.android.backup.ReportBackupManager
import com.mikeyphw.relatorio.android.data.UserPreferencesStore
import com.mikeyphw.relatorio.persistence.ReportDatabase
import com.mikeyphw.relatorio.persistence.ReportRepository

class RelatorioApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            ReportDatabase::class.java,
            "relatorio-mensal-ad.db",
        ).build()

        container = AppContainer(
            repository = ReportRepository(database.reportDao()),
            preferences = UserPreferencesStore(applicationContext),
            backupManager = ReportBackupManager(applicationContext),
        )
    }
}

data class AppContainer(
    val repository: ReportRepository,
    val preferences: UserPreferencesStore,
    val backupManager: ReportBackupManager,
)
