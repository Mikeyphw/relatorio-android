package com.mikeyphw.relatorio.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReportEntity::class,
        ReportValueEntity::class,
        MemberMovementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
