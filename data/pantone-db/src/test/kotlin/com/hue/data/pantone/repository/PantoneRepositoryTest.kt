package com.hue.data.pantone.repository

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.ColorTemperature
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.Season
import com.hue.data.pantone.db.HueDatabase
import com.hue.data.pantone.db.PantoneDao
import com.hue.data.pantone.db.PantoneFhiEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PantoneRepositoryTest {

    private val db = mockk<HueDatabase>()
    private val dao = mockk<PantoneDao>()
    private lateinit var repo: PantoneRepositoryImpl

    @Before fun setup() {
        every { db.pantoneDao() } returns dao
        repo = PantoneRepositoryImpl(db)
    }

    @After fun tearDown() { unmockkAll() }

    private fun makeEntity(
        code: String,
        labL: Double, labA: Double, labB: Double,
        temperature: String = "WARM",
        seasons: String = "AUTUMN"
    ) = PantoneFhiEntity(
        id = 1L, code = code, name = "Test Color $code",
        labL = labL, labA = labA, labB = labB,
        hex = "#AABBCC", temperature = temperature, seasons = seasons
    )

    // ── findTopMatches ────────────────────────────────────────────────────

    @Test fun `findTopMatches returns top N results sorted by deltaE`() = runTest {
        val entities = listOf(
            makeEntity("A", 50.0, 20.0, 30.0),    // similar to query
            makeEntity("B", 80.0, -30.0, 10.0),   // very different
            makeEntity("C", 52.0, 22.0, 28.0),    // very similar
        )
        coEvery { dao.getAll() } returns entities

        val queryLab = LabColor(51.0, 21.0, 29.0)
        val results = repo.findTopMatches(queryLab, topN = 2)

        assertThat(results).hasSize(2)
        // First result should be closest (C is closest to query)
        assertThat(results[0].deltaE).isLessThan(results[1].deltaE)
    }

    @Test fun `findTopMatches returns all when N exceeds database size`() = runTest {
        val entities = listOf(makeEntity("A", 50.0, 20.0, 30.0))
        coEvery { dao.getAll() } returns entities

        val results = repo.findTopMatches(LabColor(50.0, 20.0, 30.0), topN = 10)
        assertThat(results).hasSize(1)
    }

    @Test fun `findTopMatches returns empty for empty database`() = runTest {
        coEvery { dao.getAll() } returns emptyList()

        val results = repo.findTopMatches(LabColor(50.0, 20.0, 30.0), topN = 3)
        assertThat(results).isEmpty()
    }

    @Test fun `findTopMatches deltaE is non-negative`() = runTest {
        coEvery { dao.getAll() } returns listOf(makeEntity("A", 55.0, 10.0, -20.0))

        val results = repo.findTopMatches(LabColor(50.0, 20.0, 30.0), topN = 1)
        assertThat(results.first().deltaE).isAtLeast(0.0)
    }

    @Test fun `findTopMatches identical colour has deltaE near zero`() = runTest {
        val entity = makeEntity("EXACT", 55.0, 20.0, 30.0)
        coEvery { dao.getAll() } returns listOf(entity)

        val results = repo.findTopMatches(LabColor(55.0, 20.0, 30.0), topN = 1)
        assertThat(results.first().deltaE).isLessThan(0.01)
    }

    @Test fun `findTopMatches maps entity fields to domain model`() = runTest {
        val entity = makeEntity("18-1550 TCX", 52.0, 28.0, 35.0, "WARM", "AUTUMN,SPRING")
        coEvery { dao.getAll() } returns listOf(entity)

        val results = repo.findTopMatches(LabColor(52.0, 28.0, 35.0), topN = 1)
        val color = results.first().color
        assertThat(color.code).isEqualTo("18-1550 TCX")
        assertThat(color.temperature).isEqualTo(ColorTemperature.WARM)
        assertThat(color.seasons).containsExactly(Season.AUTUMN, Season.SPRING)
    }

    // ── getColorByCode ────────────────────────────────────────────────────

    @Test fun `getColorByCode returns mapped domain color when found`() = runTest {
        val entity = makeEntity("18-1550 TCX", 52.0, 28.0, 35.0)
        coEvery { dao.getByCode("18-1550 TCX") } returns entity

        val result = repo.getColorByCode("18-1550 TCX")
        assertThat(result).isNotNull()
        assertThat(result!!.code).isEqualTo("18-1550 TCX")
    }

    @Test fun `getColorByCode returns null when not found`() = runTest {
        coEvery { dao.getByCode("NONEXISTENT") } returns null

        assertThat(repo.getColorByCode("NONEXISTENT")).isNull()
    }

    // ── searchByName ──────────────────────────────────────────────────────

    @Test fun `searchByName returns flow of matching colors`() = runTest {
        val entities = listOf(makeEntity("A", 50.0, 10.0, 20.0))
        every { dao.searchByName("orange") } returns flowOf(entities)

        val results = repo.searchByName("orange")
        results.collect { list ->
            assertThat(list).hasSize(1)
        }
        verify { dao.searchByName("orange") }
    }

    // ── getTotalCount ─────────────────────────────────────────────────────

    @Test fun `getTotalCount delegates to DAO`() = runTest {
        coEvery { dao.count() } returns 250

        assertThat(repo.getTotalCount()).isEqualTo(250)
        coVerify { dao.count() }
    }

    @Test fun `getTotalCount returns zero for empty database`() = runTest {
        coEvery { dao.count() } returns 0

        assertThat(repo.getTotalCount()).isEqualTo(0)
    }
}
