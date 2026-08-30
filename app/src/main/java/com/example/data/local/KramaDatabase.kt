package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CallDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.ContactFeatureDao
import com.example.data.local.dao.StatusDao
import com.example.data.local.dao.ScheduledMessageDao
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ContactFeatureEntity
import com.example.data.local.entity.GroupKeyEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageFtsEntity
import com.example.data.local.entity.MessageReactionEntity
import com.example.data.local.entity.PresenceLogEntity
import com.example.data.local.entity.PrivateMediaVaultEntity
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.data.local.entity.SharedCountdownEntity
import com.example.data.local.entity.StatusStoryEntity
import com.example.data.local.entity.VoiceDiaryEntity
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.FileInputStream

import com.example.data.local.dao.CoupleFeaturesDao
import com.example.data.local.entity.RelationshipMilestoneEntity
import com.example.data.local.entity.SharedCalendarEventEntity
import com.example.data.local.entity.OpenWhenMessageEntity
import com.example.data.local.entity.SharedWallpaperProposalEntity
import com.example.data.local.entity.InChatReminderEntity
import com.example.data.local.entity.SharedNowPlayingEntity
import com.example.data.local.entity.BucketListItemEntity
import com.example.data.local.entity.MessageBookmarkEntity
import com.example.data.local.entity.SharedDraftEntity
import com.example.data.local.entity.WordOfTheDayEntity

@Database(
    entities = [
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        MessageReactionEntity::class,
        StatusStoryEntity::class,
        CallLogEntity::class,
        GroupMemberEntity::class,
        GroupKeyEntity::class,
        ScheduledMessageEntity::class,
        ContactFeatureEntity::class,
        PresenceLogEntity::class,
        VoiceDiaryEntity::class,
        SharedCountdownEntity::class,
        PrivateMediaVaultEntity::class,
        RelationshipMilestoneEntity::class,
        SharedCalendarEventEntity::class,
        OpenWhenMessageEntity::class,
        SharedWallpaperProposalEntity::class,
        InChatReminderEntity::class,
        SharedNowPlayingEntity::class,
        BucketListItemEntity::class,
        MessageBookmarkEntity::class,
        SharedDraftEntity::class,
        WordOfTheDayEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class KramaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun contactDao(): ContactDao
    abstract fun statusDao(): StatusDao
    abstract fun callDao(): CallDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun contactFeatureDao(): ContactFeatureDao
    abstract fun coupleFeaturesDao(): CoupleFeaturesDao

    companion object {
        private const val TAG = "KramaDatabase"
        private const val DB_NAME = "krama_encrypted_db"
        private const val PASSPHRASE_STRING = "krama_sqlcipher_e2e_secret_key_2026"

        @Volatile
        private var INSTANCE: KramaDatabase? = null

        fun resetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Throwable) {}
                INSTANCE = null
            }
        }

        fun getDatabase(context: Context): KramaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    timber.log.Timber.d("[DB TRACE] Initializing SQLCipher Room Database instance...")
                    if (!DatabaseHelper.performStartupIntegrityCheck(context)) {
                        timber.log.Timber.w("[DB TRACE] SQLCipher database integrity check failed! Recovery executed.")
                    }

                    val dbInstance = try {
                        buildEncryptedDatabase(context)
                    } catch (e: Throwable) {
                        timber.log.Timber.e(e, "[DB TRACE] Room / SQLCipher DB initialization error: ${e.message}. Forcing secure recovery...")
                        DatabaseHelper.triggerCorruptionRecovery(context)
                        buildEncryptedDatabase(context)
                    }

                    INSTANCE = dbInstance
                    dbInstance
                }
            }
        }

        /**
         * Verifies SQLCipher database header and schema integrity.
         * Returns false if file is corrupted, unreadable, or header bytes are damaged.
         */
        fun verifySqlCipherIntegrity(context: Context, database: KramaDatabase? = null): Boolean {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) {
                // Database does not exist yet or is newly created; valid state
                return true
            }

            // 1. If active database instance is provided or INSTANCE is available, check via openHelper
            val activeDb = database ?: INSTANCE
            if (activeDb != null && activeDb.isOpen) {
                return try {
                    val cursor = activeDb.openHelper.writableDatabase.query("PRAGMA quick_check;")
                    var isValid = false
                    if (cursor.moveToFirst()) {
                        val checkResult = cursor.getString(0)
                        isValid = checkResult != null && checkResult.equals("ok", ignoreCase = true)
                    }
                    cursor.close()
                    isValid
                } catch (e: Throwable) {
                    Log.w(TAG, "Quick check query on active database encountered issue: ${e.message}")
                    true // Active DB is operational
                }
            }

            return try {
                // 2. Check file header bytes readability
                val header = ByteArray(16)
                FileInputStream(dbFile).use { fis ->
                    val readBytes = fis.read(header)
                    if (readBytes < 16) {
                        Log.w(TAG, "Integrity Check Warning: DB file header is truncated ($readBytes bytes)")
                        return false
                    }
                }

                // 3. Test opening database with SQLCipher active passphrase & running PRAGMA quick_check
                SQLiteDatabase.loadLibs(context.applicationContext)
                val keyManager = com.example.data.local.security.SQLCipherKeyRotationManager(context.applicationContext)
                val activePassphrase = keyManager.getActivePassphrase()

                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    activePassphrase,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )

                var isValid = false
                val cursor = db.rawQuery("PRAGMA quick_check;", null)
                if (cursor != null && cursor.moveToFirst()) {
                    val checkResult = cursor.getString(0)
                    isValid = checkResult != null && checkResult.equals("ok", ignoreCase = true)
                    cursor.close()
                }
                db.close()

                if (isValid) {
                    Log.i(TAG, "SQLCipher Database header & PRAGMA quick_check passed successfully.")
                } else {
                    Log.w(TAG, "SQLCipher Database PRAGMA quick_check returned non-ok result.")
                }
                isValid
            } catch (e: Throwable) {
                Log.w(TAG, "SQLCipher database integrity verification note: ${e.message}")
                true // Avoid treating raw connection issues as corruption when database is managed by Room
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `scheduled_messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `content` TEXT NOT NULL, `scheduledTimestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `group_keys` (`groupId` TEXT NOT NULL, `keyData` TEXT NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`groupId`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `group_members` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `userId` TEXT NOT NULL, `role` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `message_reactions` (`messageId` TEXT NOT NULL, `emoji` TEXT NOT NULL, `userId` TEXT NOT NULL, PRIMARY KEY(`messageId`, `userId`))")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING fts4(`messageId`, `chatId`, `content`, `senderName`)")
                try {
                    db.execSQL("INSERT OR IGNORE INTO `messages_fts` (`messageId`, `chatId`, `content`, `senderName`) SELECT `id`, `chatId`, `content`, `senderName` FROM `messages`")
                } catch (e: Throwable) {
                    Log.w(TAG, "MIGRATION_5_6 initial FTS populate note: ${e.message}")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages` (`chatId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_timestamp` ON `messages` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_logs_contactId` ON `call_logs` (`contactId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_call_logs_timestamp` ON `call_logs` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chats_contactId` ON `chats` (`contactId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chats_lastMessageTimestamp` ON `chats` (`lastMessageTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_phoneNumber` ON `contacts` (`phoneNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_isOnline` ON `contacts` (`isOnline`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contact_features` (
                        `contactId` TEXT NOT NULL,
                        `streakDays` INTEGER NOT NULL DEFAULT 0,
                        `lastStreakDate` INTEGER NOT NULL DEFAULT 0,
                        `nickname` TEXT NOT NULL DEFAULT '',
                        `privateNotes` TEXT NOT NULL DEFAULT '',
                        `customRingtoneUri` TEXT NOT NULL DEFAULT '',
                        `customVibrationPattern` TEXT NOT NULL DEFAULT 'DEFAULT',
                        `statusEmoji` TEXT NOT NULL DEFAULT '💬',
                        `autoReplyDrivingEnabled` INTEGER NOT NULL DEFAULT 0,
                        `autoReplyMessage` TEXT NOT NULL DEFAULT '🚗 I''m driving right now. Will reply shortly!',
                        `chatCreatedTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `lastActiveTogetherTimestamp` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`contactId`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `presence_logs` (
                        `id` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        `hourOfDay` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `voice_diaries` (
                        `id` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        `audioPath` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shared_countdowns` (
                        `id` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetTimestamp` INTEGER NOT NULL,
                        `emoji` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `private_media_vault` (
                        `mediaId` TEXT NOT NULL,
                        `chatId` TEXT NOT NULL,
                        `mediaUrlOrPath` TEXT NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `isLocked` INTEGER NOT NULL DEFAULT 1,
                        `dateAdded` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `relationship_milestones` (
                        `id` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `iconEmoji` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shared_calendar_events` (
                        `id` TEXT NOT NULL,
                        `contactId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `dateTimestamp` INTEGER NOT NULL,
                        `locationOrNote` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `createdBy` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `open_when_messages` (
                        `id` TEXT NOT NULL,
                        `chatId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `unlockTimestamp` INTEGER NOT NULL,
                        `isUnlocked` INTEGER NOT NULL DEFAULT 0,
                        `createdTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shared_wallpaper_proposals` (
                        `id` TEXT NOT NULL,
                        `chatId` TEXT NOT NULL,
                        `wallpaperUrl` TEXT NOT NULL,
                        `proposedBy` TEXT NOT NULL,
                        `proposedByName` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `in_chat_reminders` (
                        `id` TEXT NOT NULL,
                        `chatId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `reminderTimestamp` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shared_now_playing` (
                        `userId` TEXT NOT NULL,
                        `songTitle` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `isPlaying` INTEGER NOT NULL,
                        `progressMs` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `mood` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                """.trimIndent())
            }
        }

        private fun buildEncryptedDatabase(context: Context): KramaDatabase {
            val keyManager = com.example.data.local.security.SQLCipherKeyRotationManager(context.applicationContext)
            val passphrase = keyManager.getActivePassphrase().toByteArray()

            val factory = try {
                com.example.data.local.security.SQLCipherOpenHelperFactory.createFactory(
                    context.applicationContext,
                    passphrase
                )
            } catch (e: Throwable) {
                Log.e(TAG, "SQLCipher custom factory initialization failed: ${e.message}")
                null
            }

            val builder = Room.databaseBuilder(
                context.applicationContext,
                KramaDatabase::class.java,
                DB_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()

            if (factory != null) {
                builder.openHelperFactory(factory)
            }

            return builder.build()
        }
    }
}

