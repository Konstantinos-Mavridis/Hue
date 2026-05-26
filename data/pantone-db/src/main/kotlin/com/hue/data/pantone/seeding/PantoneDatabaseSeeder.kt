package com.hue.data.pantone.seeding

import androidx.sqlite.db.SupportSQLiteDatabase
import com.hue.data.pantone.db.HueDatabase
import com.hue.data.pantone.db.PantoneFhiEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

class PantoneDatabaseSeeder @Inject constructor(
    private val dbProvider: Provider<HueDatabase>
) {
    /**
     * Seeds directly via the raw SQLite handle — safe to call synchronously from
     * RoomDatabase.Callback.onCreate() because it never touches dbProvider (which
     * would deadlock while Dagger's singleton lock is still held).
     */
    fun seedDirect(db: SupportSQLiteDatabase) {
        val count = db.query("SELECT COUNT(*) FROM pantone_fhi", emptyArray<Any?>())
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        if (count > 0) return

        db.beginTransactionNonExclusive()
        try {
            val seen = mutableSetOf<String>()
            PantoneSeedData.raw.forEach { row ->
                val code = row[0]
                if (!seen.add(code)) return@forEach
                val temp = when (row[6]) {
                    "W" -> "WARM"; "C" -> "COOL"; else -> "NEUTRAL"
                }
                val seasons = row[7].split(",").joinToString(",") { tag ->
                    when (tag.trim()) {
                        "SP" -> "SPRING"; "SU" -> "SUMMER"
                        "AU" -> "AUTUMN"; "WI" -> "WINTER"
                        else -> tag.trim()
                    }
                }
                db.execSQL(
                    "INSERT OR IGNORE INTO pantone_fhi" +
                        " (code,name,labL,labA,labB,hex,temperature,seasons)" +
                        " VALUES (?,?,?,?,?,?,?,?)",
                    arrayOf(code, row[1], row[2], row[3], row[4], row[5], temp, seasons)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val db = dbProvider.get()
        val count = db.pantoneDao().count()
        if (count == 0) seed(db)
    }

    private suspend fun seed(db: HueDatabase) {
        val entities = PantoneSeedData.raw.mapIndexed { idx, row ->
            val code    = row[0]
            val name    = row[1]
            val labL    = row[2].toDouble()
            val labA    = row[3].toDouble()
            val labB    = row[4].toDouble()
            val hex     = row[5]
            val temp    = when (row[6]) {
                "W"  -> "WARM"
                "C"  -> "COOL"
                else -> "NEUTRAL"
            }
            val seasons = row[7]
                .split(",")
                .joinToString(",") { tag ->
                    when (tag.trim()) {
                        "SP" -> "SPRING"
                        "SU" -> "SUMMER"
                        "AU" -> "AUTUMN"
                        "WI" -> "WINTER"
                        else -> tag.trim()
                    }
                }
            PantoneFhiEntity(
                code = "${code}_$idx".take(20).let { code },
                name = name,
                labL = labL, labA = labA, labB = labB,
                hex = hex,
                temperature = temp,
                seasons = seasons
            )
        }.distinctBy { it.code }

        db.pantoneDao().insertAll(entities)
    }
}
