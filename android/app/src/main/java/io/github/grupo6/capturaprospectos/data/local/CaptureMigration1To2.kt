package io.github.grupo6.capturaprospectos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Copia íntegra v1 -> v2 en la transacción de Room. No infiere campos F2027 ni reinicia claves. */
internal object CaptureMigration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `g6_migration_consent_text` AS SELECT * FROM `consent_text`")
        db.execSQL("CREATE TABLE `g6_migration_captures` AS SELECT * FROM `captures`")
        db.execSQL("CREATE TABLE `g6_migration_capture_interests` AS SELECT * FROM `capture_interests`")
        db.execSQL("CREATE TABLE `g6_migration_active_registration_keys` AS SELECT * FROM `active_registration_keys`")
        db.execSQL("CREATE TABLE `g6_migration_local_audit` AS SELECT * FROM `local_audit`")
        db.execSQL("DROP TABLE `local_audit`")
        db.execSQL("DROP TABLE `active_registration_keys`")
        db.execSQL("DROP TABLE `capture_interests`")
        db.execSQL("DROP TABLE `captures`")
        db.execSQL("DROP TABLE `consent_text`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `consent_text` (`version` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`version`))")
        db.execSQL("INSERT INTO `consent_text` (`version`, `text`) SELECT `version`, `text` FROM `g6_migration_consent_text`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `captures` (`id` TEXT NOT NULL, `revision` INTEGER NOT NULL, `state` TEXT NOT NULL, `ownerId` TEXT NOT NULL, `siteId` TEXT NOT NULL, `eventId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `capturedAt` TEXT NOT NULL, `eventName` TEXT NOT NULL, `eventTimeZone` TEXT NOT NULL, `fullName` TEXT, `rut` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `educationLevel` TEXT, `establishment` TEXT, `contactChannel` TEXT, `notes` TEXT, `consentVersion` TEXT NOT NULL, `acceptedAt` TEXT NOT NULL, `recordedBy` TEXT NOT NULL, `acceptanceMode` TEXT NOT NULL, `locationSource` TEXT, `latitude` REAL, `longitude` REAL, `measuredAt` TEXT, `accuracyMeters` REAL, `confirmedAt` TEXT, `confirmedBy` TEXT, `contractVersion` TEXT NOT NULL DEFAULT 'LEGACY_1', `givenNames` TEXT, `familyNames` TEXT, `birthDate` TEXT, `residenceCommune` TEXT, `currentActivity` TEXT, `currentActivityOther` TEXT, `establishmentCommune` TEXT, `formPersonalOption` TEXT, `preferredSchedule` TEXT, `interestLevel` TEXT, `motivation` TEXT, `modality` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`consentVersion`) REFERENCES `consent_text`(`version`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_consentVersion` ON `captures` (`consentVersion`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_ownerId` ON `captures` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_siteId` ON `captures` (`siteId`)")
        db.execSQL("INSERT INTO `captures` (`id`, `revision`, `state`, `ownerId`, `siteId`, `eventId`, `deviceId`, `capturedAt`, `eventName`, `eventTimeZone`, `fullName`, `rut`, `email`, `phone`, `educationLevel`, `establishment`, `contactChannel`, `notes`, `consentVersion`, `acceptedAt`, `recordedBy`, `acceptanceMode`, `locationSource`, `latitude`, `longitude`, `measuredAt`, `accuracyMeters`, `confirmedAt`, `confirmedBy`) SELECT `id`, `revision`, `state`, `ownerId`, `siteId`, `eventId`, `deviceId`, `capturedAt`, `eventName`, `eventTimeZone`, `fullName`, `rut`, `email`, `phone`, `educationLevel`, `establishment`, `contactChannel`, `notes`, `consentVersion`, `acceptedAt`, `recordedBy`, `acceptanceMode`, `locationSource`, `latitude`, `longitude`, `measuredAt`, `accuracyMeters`, `confirmedAt`, `confirmedBy` FROM `g6_migration_captures`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `capture_interests` (`captureId` TEXT NOT NULL, `programId` TEXT NOT NULL, `offeringId` TEXT, `catalogVersion` TEXT NOT NULL, `programName` TEXT NOT NULL, `modality` TEXT, `priority` INTEGER, `section` TEXT, `mentionId` TEXT, `mentionName` TEXT, PRIMARY KEY(`captureId`, `programId`), FOREIGN KEY(`captureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_capture_interests_captureId_priority` ON `capture_interests` (`captureId`, `priority`)")
        db.execSQL("INSERT INTO `capture_interests` (`captureId`, `programId`, `offeringId`, `catalogVersion`, `programName`, `modality`) SELECT `captureId`, `programId`, `offeringId`, `catalogVersion`, `programName`, `modality` FROM `g6_migration_capture_interests`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `active_registration_keys` (`eventId` TEXT NOT NULL, `rut` TEXT NOT NULL, `captureId` TEXT NOT NULL, PRIMARY KEY(`eventId`, `rut`), FOREIGN KEY(`captureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_active_registration_keys_captureId` ON `active_registration_keys` (`captureId`)")
        db.execSQL("INSERT INTO `active_registration_keys` (`eventId`, `rut`, `captureId`) SELECT `eventId`, `rut`, `captureId` FROM `g6_migration_active_registration_keys`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_audit` (`id` TEXT NOT NULL, `captureId` TEXT NOT NULL, `actorId` TEXT NOT NULL, `occurredAt` TEXT NOT NULL, `action` TEXT NOT NULL, `resultingRevision` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`captureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_audit_captureId` ON `local_audit` (`captureId`)")
        db.execSQL("INSERT INTO `local_audit` (`id`, `captureId`, `actorId`, `occurredAt`, `action`, `resultingRevision`) SELECT `id`, `captureId`, `actorId`, `occurredAt`, `action`, `resultingRevision` FROM `g6_migration_local_audit`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `capture_site_preferences` (`captureId` TEXT NOT NULL, `siteId` TEXT NOT NULL, `name` TEXT NOT NULL, `catalogVersion` TEXT NOT NULL, PRIMARY KEY(`captureId`, `siteId`), FOREIGN KEY(`captureId`) REFERENCES `captures`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("DROP TABLE `g6_migration_consent_text`")
        db.execSQL("DROP TABLE `g6_migration_captures`")
        db.execSQL("DROP TABLE `g6_migration_capture_interests`")
        db.execSQL("DROP TABLE `g6_migration_active_registration_keys`")
        db.execSQL("DROP TABLE `g6_migration_local_audit`")
        db.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) { "Migración con referencias inválidas" } }
    }
}
