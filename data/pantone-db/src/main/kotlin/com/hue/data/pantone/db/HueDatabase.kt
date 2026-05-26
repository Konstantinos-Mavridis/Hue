package com.hue.data.pantone.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hue.data.pantone.seeding.PantoneDatabaseSeeder

@Database(
    entities = [PantoneFhiEntity::class, ScanHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HueDatabase : RoomDatabase() {
    abstract fun pantoneDao(): PantoneDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        private const val DB_NAME = "hue_database"

        fun create(context: Context, seeder: PantoneDatabaseSeeder): HueDatabase {
            return Room.databaseBuilder(context, HueDatabase::class.java, DB_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed synchronously — seedDirect() uses the raw SupportSQLiteDatabase
                        // handle so it never calls dbProvider.get(), avoiding a deadlock with
                        // Dagger's singleton lock that is still held during create().
                        seeder.seedDirect(db)
                    }
                })
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
