package com.recall.app.room

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.recall.app.room.dao.BaseNoteDao
import com.recall.app.room.dao.CommonDao
import com.recall.app.room.dao.LabelDao

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 6)
abstract class RecallDatabase : RoomDatabase() {

    abstract fun getLabelDao(): LabelDao
    abstract fun getCommonDao(): CommonDao
    abstract fun getBaseNoteDao(): BaseNoteDao

    fun checkpoint() {
        getBaseNoteDao().query(SimpleSQLiteQuery("pragma wal_checkpoint(FULL)"))
    }

    companion object {

        const val DatabaseName = "RecallDatabase"

        @Volatile
        private var instance: RecallDatabase? = null

        /**
         * allowMainThreadQueries() is only used in [com.recall.app.ReminderReceiver.onReceive]
         * when an alarm fires off!
         */
        fun getDatabase(app: Application): RecallDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(app, RecallDatabase::class.java, DatabaseName)
                    .addMigrations(Migration2, Migration3, Migration4, Migration5, Migration6)
                    .allowMainThreadQueries()
                    .build()
                this.instance = instance
                return instance
            }
        }

        object Migration2 : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        object Migration3 : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `images` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration4 : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `audios` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration5 : Migration(4, 5) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `reminder` TEXT")
            }
        }

        object Migration6 : Migration(5, 6) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `embedding` BLOB")
            }
        }
    }
}
