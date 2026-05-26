package com.hue.data.pantone.seeding

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat
import com.hue.data.pantone.db.HueDatabase
import com.hue.data.pantone.db.PantoneDao
import com.hue.data.pantone.db.PantoneFhiEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class PantoneDatabaseSeederTest {

    private val mockProvider = mockk<Provider<HueDatabase>>()
    private val mockHueDb    = mockk<HueDatabase>()
    private val mockDao      = mockk<PantoneDao>()
    private lateinit var seeder: PantoneDatabaseSeeder

    @Before fun setup() {
        every { mockProvider.get() } returns mockHueDb
        every { mockHueDb.pantoneDao() } returns mockDao
        seeder = PantoneDatabaseSeeder(mockProvider)
    }

    @After fun tearDown() { unmockkAll() }

    // ── seedIfEmpty ────────────────────────────────────────────────────────────

    @Test fun `seedIfEmpty skips seeding when database already has entries`() = runTest {
        coEvery { mockDao.count() } returns 100

        seeder.seedIfEmpty()

        coVerify(exactly = 0) { mockDao.insertAll(any()) }
    }

    @Test fun `seedIfEmpty calls insertAll when database is empty`() = runTest {
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(any()) } just Runs

        seeder.seedIfEmpty()

        coVerify { mockDao.insertAll(any()) }
    }

    @Test fun `seedIfEmpty inserts a non-empty list of entities`() = runTest {
        val slot = slot<List<PantoneFhiEntity>>()
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(capture(slot)) } just Runs

        seeder.seedIfEmpty()

        assertThat(slot.captured).isNotEmpty()
    }

    @Test fun `seedIfEmpty entities have valid temperature values`() = runTest {
        val slot = slot<List<PantoneFhiEntity>>()
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(capture(slot)) } just Runs

        seeder.seedIfEmpty()

        slot.captured.forEach { entity ->
            assertThat(entity.temperature).isIn(listOf("WARM", "COOL", "NEUTRAL"))
        }
    }

    @Test fun `seedIfEmpty entities have valid season values`() = runTest {
        val slot = slot<List<PantoneFhiEntity>>()
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(capture(slot)) } just Runs

        seeder.seedIfEmpty()

        val validSeasons = setOf("SPRING", "SUMMER", "AUTUMN", "WINTER")
        slot.captured.forEach { entity ->
            entity.seasons.split(",").forEach { s ->
                assertThat(s.trim()).isIn(validSeasons)
            }
        }
    }

    @Test fun `seedIfEmpty entities have unique codes`() = runTest {
        val slot = slot<List<PantoneFhiEntity>>()
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(capture(slot)) } just Runs

        seeder.seedIfEmpty()

        val codes = slot.captured.map { it.code }
        assertThat(codes.distinct().size).isEqualTo(codes.size)
    }

    @Test fun `seedIfEmpty entities have non-empty names`() = runTest {
        val slot = slot<List<PantoneFhiEntity>>()
        coEvery { mockDao.count() } returns 0
        coEvery { mockDao.insertAll(capture(slot)) } just Runs

        seeder.seedIfEmpty()

        slot.captured.forEach { entity ->
            assertThat(entity.name).isNotEmpty()
        }
    }

    // ── seedDirect ─────────────────────────────────────────────────────────────

    @Test fun `seedDirect skips when table already has rows`() {
        val sqlDb  = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        every { sqlDb.query(any<String>(), any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getInt(0) } returns 5

        seeder.seedDirect(sqlDb)

        verify(exactly = 0) { sqlDb.beginTransactionNonExclusive() }
    }

    @Test fun `seedDirect begins transaction when table is empty`() {
        val sqlDb  = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        every { sqlDb.query(any<String>(), any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getInt(0) } returns 0
        every { sqlDb.beginTransactionNonExclusive() } just Runs
        every { sqlDb.execSQL(any(), any<Array<*>>()) } just Runs
        every { sqlDb.setTransactionSuccessful() } just Runs
        every { sqlDb.endTransaction() } just Runs

        seeder.seedDirect(sqlDb)

        verify { sqlDb.beginTransactionNonExclusive() }
        verify { sqlDb.setTransactionSuccessful() }
        verify { sqlDb.endTransaction() }
    }

    @Test fun `seedDirect calls endTransaction even on success`() {
        val sqlDb  = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        every { sqlDb.query(any<String>(), any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getInt(0) } returns 0
        every { sqlDb.beginTransactionNonExclusive() } just Runs
        every { sqlDb.execSQL(any(), any<Array<*>>()) } just Runs
        every { sqlDb.setTransactionSuccessful() } just Runs
        every { sqlDb.endTransaction() } just Runs

        seeder.seedDirect(sqlDb)

        verify { sqlDb.endTransaction() }
    }

    @Test fun `seedDirect inserts at least one row`() {
        val sqlDb  = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        every { sqlDb.query(any<String>(), any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getInt(0) } returns 0
        every { sqlDb.beginTransactionNonExclusive() } just Runs
        every { sqlDb.setTransactionSuccessful() } just Runs
        every { sqlDb.endTransaction() } just Runs
        val insertCalls = mutableListOf<Array<*>>()
        every { sqlDb.execSQL(any(), capture(insertCalls)) } just Runs

        seeder.seedDirect(sqlDb)

        assertThat(insertCalls).isNotEmpty()
    }

    @Test fun `seedDirect handles cursor returning false for moveToFirst`() {
        val sqlDb  = mockk<SupportSQLiteDatabase>()
        val cursor = mockk<Cursor>(relaxed = true)
        every { sqlDb.query(any<String>(), any()) } returns cursor
        every { cursor.moveToFirst() } returns false  // empty result set → count = 0
        every { sqlDb.beginTransactionNonExclusive() } just Runs
        every { sqlDb.execSQL(any(), any<Array<*>>()) } just Runs
        every { sqlDb.setTransactionSuccessful() } just Runs
        every { sqlDb.endTransaction() } just Runs

        seeder.seedDirect(sqlDb)

        verify { sqlDb.beginTransactionNonExclusive() }
    }
}
