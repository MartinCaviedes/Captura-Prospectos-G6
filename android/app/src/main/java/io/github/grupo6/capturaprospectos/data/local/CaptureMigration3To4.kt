package io.github.grupo6.capturaprospectos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Auditoría aditiva: conserva filas previas sin inventar motivos ni revisiones históricas. */
internal object CaptureMigration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_audit ADD COLUMN previousRevision INTEGER")
        db.execSQL("ALTER TABLE local_audit ADD COLUMN reasonText TEXT")
        db.execSQL("ALTER TABLE local_audit ADD COLUMN changedFields TEXT")
    }
}
