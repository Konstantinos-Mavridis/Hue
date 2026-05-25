package com.hue.data.pantone.repository

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.hue.core.color.model.*
import com.hue.data.pantone.db.*
import com.hue.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HistoryRepositoryTest {

    private val db = mockk<HueDatabase>()
    private val dao = mockk<ScanHistoryDao>()
    private val gson = Gson()
    private lateinit var repo: HistoryRepositoryImpl

    @Before fun setup() {
        every { db.scanHistoryDao() } returns dao
        repo = HistoryRepositoryImpl(db, gson)
    }

    @After fun tearDown() { unmockkAll() }

    private fun makeSeasonResult(season: Season = Season.AUTUMN) = SeasonResult(
        primarySeason = season,
        secondarySeason = null,
        primaryWeight = 0.8,
        temperature = ColorTemperature.WARM,
        confidence = 0.8,
        lightness = LightnessCategory.MEDIUM,
        chroma = ChromaCategory.MUTED,
        explanation = "Test explanation"
    )

    private fun makeAnalysis(
        id: Long = 1L,
        season: Season = Season.AUTUMN
    ) = FabricAnalysis(
        id = id,
        timestamp = 1700000000000L,
        thumbnailPath = "/tmp/test.jpg",
        dominantRgb = RgbColor(180, 90, 45),
        dominantLab = LabColor(52.0, 20.0, 30.0),
        topMatches = emptyList(),
        season = makeSeasonResult(season),
        regionVariance = 50.0
    )

    private fun makeEntity(analysis: FabricAnalysis): ScanHistoryEntity =
        ScanHistoryEntity.fromDomain(analysis, gson)

    // ── getAllScans ────────────────────────────────────────────────────────

    @Test fun `getAllScans returns mapped domain objects`() = runTest {
        val entities = listOf(makeEntity(makeAnalysis(1L)), makeEntity(makeAnalysis(2L)))
        every { dao.getAll() } returns flowOf(entities)

        repo.getAllScans().collect { scans ->
            assertThat(scans).hasSize(2)
        }
    }

    @Test fun `getAllScans returns empty list when none saved`() = runTest {
        every { dao.getAll() } returns flowOf(emptyList())

        repo.getAllScans().collect { scans ->
            assertThat(scans).isEmpty()
        }
    }

    // ── getScansBySeason ──────────────────────────────────────────────────

    @Test fun `getScansBySeason filters by season`() = runTest {
        val entity = makeEntity(makeAnalysis(1L, Season.WINTER))
        every { dao.getBySeason("WINTER") } returns flowOf(listOf(entity))

        repo.getScansBySeason("WINTER").collect { scans ->
            assertThat(scans).hasSize(1)
            assertThat(scans[0].season.primarySeason).isEqualTo(Season.WINTER)
        }
    }

    // ── saveScan ──────────────────────────────────────────────────────────

    @Test fun `saveScan inserts and returns new id`() = runTest {
        val analysis = makeAnalysis()
        val slot = slot<ScanHistoryEntity>()
        coEvery { dao.insert(capture(slot)) } returns 99L

        val resultId = repo.saveScan(analysis)
        assertThat(resultId).isEqualTo(99L)
        assertThat(slot.captured.thumbnailPath).isEqualTo("/tmp/test.jpg")
        assertThat(slot.captured.primarySeason).isEqualTo("AUTUMN")
    }

    @Test fun `saveScan serializes top matches to JSON`() = runTest {
        val match = PantoneMatchResult(
            color = PantoneFhiColor(
                code = "18-1550 TCX", name = "Burnt Orange",
                lab = LabColor(52.0, 28.0, 35.0), hex = "#C05C28",
                temperature = ColorTemperature.WARM,
                seasons = listOf(Season.AUTUMN)
            ),
            deltaE = 2.3
        )
        val analysis = makeAnalysis().copy(topMatches = listOf(match))
        coEvery { dao.insert(any()) } returns 1L

        repo.saveScan(analysis)
        coVerify { dao.insert(match { it.topMatchesJson.contains("18-1550 TCX") }) }
    }

    // ── deleteScan ────────────────────────────────────────────────────────

    @Test fun `deleteScan calls DAO deleteById`() = runTest {
        coEvery { dao.deleteById(5L) } just Runs

        repo.deleteScan(5L)
        coVerify { dao.deleteById(5L) }
    }

    // ── getScanById ────────────────────────────────────────────────────────

    @Test fun `getScanById returns domain object when found`() = runTest {
        val entity = makeEntity(makeAnalysis(7L))
        coEvery { dao.getById(7L) } returns entity

        val result = repo.getScanById(7L)
        assertThat(result).isNotNull()
    }

    @Test fun `getScanById returns null when not found`() = runTest {
        coEvery { dao.getById(999L) } returns null

        assertThat(repo.getScanById(999L)).isNull()
    }

    // ── clearAll ──────────────────────────────────────────────────────────

    @Test fun `clearAll delegates to DAO`() = runTest {
        coEvery { dao.clearAll() } just Runs

        repo.clearAll()
        coVerify { dao.clearAll() }
    }

    // ── ScanHistoryEntity serialisation round-trip ─────────────────────────

    @Test fun `fromDomain and toDomain round-trip preserves season`() {
        val analysis = makeAnalysis(season = Season.WINTER)
        val entity = ScanHistoryEntity.fromDomain(analysis, gson)
        val recovered = entity.toDomain(gson)
        assertThat(recovered.season.primarySeason).isEqualTo(Season.WINTER)
    }

    @Test fun `fromDomain and toDomain round-trip preserves rgb`() {
        val analysis = makeAnalysis()
        val entity = ScanHistoryEntity.fromDomain(analysis, gson)
        val recovered = entity.toDomain(gson)
        assertThat(recovered.dominantRgb.r).isEqualTo(180)
        assertThat(recovered.dominantRgb.g).isEqualTo(90)
        assertThat(recovered.dominantRgb.b).isEqualTo(45)
    }

    @Test fun `fromDomain preserves timestamp`() {
        val analysis = makeAnalysis()
        val entity = ScanHistoryEntity.fromDomain(analysis, gson)
        assertThat(entity.timestamp).isEqualTo(1700000000000L)
    }
}
