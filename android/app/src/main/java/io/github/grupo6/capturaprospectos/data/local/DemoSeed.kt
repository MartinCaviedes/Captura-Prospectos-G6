package io.github.grupo6.capturaprospectos.data.local

import java.util.UUID

/** Fixtures académicas; sin cuentas de integrantes ni oferta institucional. IDs estables por versión. */
internal object DemoSeed {
    const val VERSION = "g6-synthetic-context-v1"
    const val CATALOG = "g6-synthetic-catalog-v1"
    const val CONSENT = "g6-synthetic-consent-v1"
    const val CONSENT_TEXT = "DEMOSTRACIÓN ACADÉMICA: texto ficticio sin aprobación institucional. Se utilizarán únicamente datos inventados para ensayar la captura local. La aceptación debe registrarse explícitamente en cada captura."
    fun id(key: String): String = UUID.nameUUIDFromBytes("$VERSION/$key".toByteArray(Charsets.UTF_8)).toString()
    val catalog = CatalogVersionRow(CATALOG, "SYNTHETIC", "Fixture académico G6; no deriva de oferta institucional", 2027)
    val sites = (1..4).map { DemoSiteRow(id("site-$it"), "Sede ficticia $it — DEMO", CATALOG, true) }
    val profiles = listOf(
        DemoProfileRow(id("captor-a"), "Captador A — DEMO", "CAPTOR"),
        DemoProfileRow(id("captor-b"), "Captador B — DEMO", "CAPTOR"),
        DemoProfileRow(id("supervisor"), "Supervisor — DEMO", "SUPERVISOR"),
    )
    val scopes = listOf(ProfileSiteRow(id("captor-a"), id("site-1")), ProfileSiteRow(id("captor-b"), id("site-2")),
        ProfileSiteRow(id("supervisor"), id("site-1")), ProfileSiteRow(id("supervisor"), id("site-2")))
    val events = listOf(
        DemoEventRow(id("event-a"), id("site-1"), "Visita ficticia A — DEMO", "2026-10-01T12:00:00Z", "2026-11-01T23:00:00Z", "America/Santiago", null, null, "Colegio ficticio A", "Comuna ficticia A"),
        DemoEventRow(id("event-a2"), id("site-1"), "Feria ficticia A — DEMO", "2026-10-01T12:00:00Z", "2026-11-01T23:00:00Z", "America/Santiago", null, null, null, null),
        DemoEventRow(id("event-b"), id("site-2"), "Visita ficticia B — DEMO", "2026-10-01T12:00:00Z", "2026-11-01T23:00:00Z", "America/Santiago", null, null, null, null),
    )
    val assignments = listOf(DemoAssignmentRow(id("event-a"), id("captor-a")), DemoAssignmentRow(id("event-a2"), id("captor-a")), DemoAssignmentRow(id("event-b"), id("captor-b")))
    val programs = (1..4).map { DemoProgramRow(id("program-$it"), "Programa ficticio $it — DEMO", CATALOG, if (it == 2) "DEMO_ONLINE" else "DEMO_PHYSICAL", true) }
    val mentions = listOf(DemoMentionRow(id("mention-a"), id("program-3"), "Mención ficticia A — DEMO"), DemoMentionRow(id("mention-b"), id("program-3"), "Mención ficticia B — DEMO"))
}
