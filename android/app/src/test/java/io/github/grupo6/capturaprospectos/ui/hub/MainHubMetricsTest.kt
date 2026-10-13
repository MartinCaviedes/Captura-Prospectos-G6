package io.github.grupo6.capturaprospectos.ui.hub

import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.records.DeviceMetrics
import io.github.grupo6.capturaprospectos.domain.records.SiteMetrics
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainHubMetricsTest {

    private val eventId = UUID.randomUUID()
    private val siteId1 = UUID.randomUUID()
    private val siteId2 = UUID.randomUUID()

    @Test
    fun metricsComputesTotalsCorrectly() {
        val metrics = DeviceMetrics(
            eventId = eventId,
            totalCaptures = 15,
            readyCount = 10,
            draftCount = 3,
            needsContextCount = 1,
            cancelledCount = 1,
        )

        assertEquals(15, metrics.totalCaptures)
        assertEquals(10, metrics.readyCount)
        assertEquals(3, metrics.draftCount)
        assertEquals(1, metrics.needsContextCount)
        assertEquals(1, metrics.cancelledCount)
        assertTrue(metrics.siteMetrics.isEmpty())
    }

    @Test
    fun supervisorMetricsIncludesSiteBreakdown() {
        val site1 = SiteMetrics(
            siteId = siteId1,
            siteName = "Sede Viña del Mar",
            totalCount = 8,
            readyCount = 6,
            draftCount = 0,
            needsContextCount = 1,
            cancelledCount = 1,
        )
        val site2 = SiteMetrics(
            siteId = siteId2,
            siteName = "Sede Valparaíso",
            totalCount = 7,
            readyCount = 4,
            draftCount = 0,
            needsContextCount = 0,
            cancelledCount = 3,
        )

        val metrics = DeviceMetrics(
            eventId = eventId,
            totalCaptures = 15,
            readyCount = 10,
            draftCount = 0,
            needsContextCount = 1,
            cancelledCount = 4,
            siteMetrics = listOf(site1, site2),
        )

        assertEquals(2, metrics.siteMetrics.size)
        assertEquals("Sede Viña del Mar", metrics.siteMetrics[0].siteName)
        assertEquals(8, metrics.siteMetrics[0].totalCount)
        assertEquals("Sede Valparaíso", metrics.siteMetrics[1].siteName)
        assertEquals(7, metrics.siteMetrics[1].totalCount)
    }

    @Test
    fun emptyMetricsProducesZeroCounts() {
        val metrics = DeviceMetrics(
            eventId = eventId,
            totalCaptures = 0,
            readyCount = 0,
            draftCount = 0,
            needsContextCount = 0,
            cancelledCount = 0,
        )

        assertEquals(0, metrics.totalCaptures)
        assertEquals(0, metrics.readyCount)
        assertEquals(0, metrics.draftCount)
        assertEquals(0, metrics.needsContextCount)
        assertEquals(0, metrics.cancelledCount)
        assertTrue(metrics.siteMetrics.isEmpty())
    }
}
