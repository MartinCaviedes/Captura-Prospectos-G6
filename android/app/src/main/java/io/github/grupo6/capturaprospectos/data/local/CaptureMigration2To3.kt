package io.github.grupo6.capturaprospectos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Sólo agrega tablas de contexto/catálogo; no inserta fixtures ni modifica capturas. */
internal object CaptureMigration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_catalog_versions` (`id` TEXT NOT NULL, `sourceKind` TEXT NOT NULL, `sourceReference` TEXT NOT NULL, `academicYear` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_sites` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `catalogVersion` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`catalogVersion`) REFERENCES `demo_catalog_versions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_sites_catalogVersion` ON `demo_sites` (`catalogVersion`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_profiles` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `role` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_profile_sites` (`profileId` TEXT NOT NULL, `siteId` TEXT NOT NULL, PRIMARY KEY(`profileId`, `siteId`), FOREIGN KEY(`profileId`) REFERENCES `demo_profiles`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`siteId`) REFERENCES `demo_sites`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_profile_sites_siteId` ON `demo_profile_sites` (`siteId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_events` (`id` TEXT NOT NULL, `siteId` TEXT NOT NULL, `name` TEXT NOT NULL, `startsAt` TEXT NOT NULL, `endsAt` TEXT NOT NULL, `timeZone` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, `establishment` TEXT, `establishmentCommune` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`siteId`) REFERENCES `demo_sites`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_events_siteId` ON `demo_events` (`siteId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_assignments` (`eventId` TEXT NOT NULL, `profileId` TEXT NOT NULL, PRIMARY KEY(`eventId`, `profileId`), FOREIGN KEY(`eventId`) REFERENCES `demo_events`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`profileId`) REFERENCES `demo_profiles`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_assignments_profileId` ON `demo_assignments` (`profileId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_programs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `catalogVersion` TEXT NOT NULL, `section` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`catalogVersion`) REFERENCES `demo_catalog_versions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_programs_catalogVersion` ON `demo_programs` (`catalogVersion`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_mentions` (`id` TEXT NOT NULL, `programId` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`programId`) REFERENCES `demo_programs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_mentions_programId` ON `demo_mentions` (`programId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `demo_context` (`singletonId` INTEGER NOT NULL, `seedVersion` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `installedAt` TEXT NOT NULL, `catalogVersion` TEXT NOT NULL, `consentVersion` TEXT NOT NULL, `profileId` TEXT, `eventId` TEXT, PRIMARY KEY(`singletonId`), FOREIGN KEY(`profileId`) REFERENCES `demo_profiles`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`eventId`) REFERENCES `demo_events`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`catalogVersion`) REFERENCES `demo_catalog_versions`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`consentVersion`) REFERENCES `consent_text`(`version`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_context_profileId` ON `demo_context` (`profileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_context_eventId` ON `demo_context` (`eventId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_context_catalogVersion` ON `demo_context` (`catalogVersion`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_demo_context_consentVersion` ON `demo_context` (`consentVersion`)")
    }
}
