package io.github.grupo6.capturaprospectos.data.local

import java.util.UUID

/** Catálogo base de admisión 2027 y referencias determinísticas por versión. */
internal object DemoSeed {
    const val VERSION = "g6-synthetic-context-v1"
    const val CATALOG = "g6-synthetic-catalog-v1"
    const val CONSENT = "g6-synthetic-consent-v1"
    const val CONSENT_TEXT = "Autorizo a Duoc UC para almacenar y tratar mis datos personales y preferencias académicas ingresados en este formulario con la finalidad de recibir información sobre el proceso de admisión, actividades vocacionales y oferta académica. La aceptación debe registrarse explícitamente en cada captura."
    fun id(key: String): String = UUID.nameUUIDFromBytes("$VERSION/$key".toByteArray(Charsets.UTF_8)).toString()
    val catalog = CatalogVersionRow(CATALOG, "OFFICIAL_REVIEWED", "Catálogo Admisión Duoc UC 2027", 2027)
    val sites = listOf(
        DemoSiteRow(id("site-1"), "Sede Viña del Mar", CATALOG, true),
        DemoSiteRow(id("site-2"), "Sede Valparaíso", CATALOG, true),
        DemoSiteRow(id("site-3"), "Sede Alameda (Santiago Centro)", CATALOG, true),
        DemoSiteRow(id("site-4"), "Sede Antonio Varas (Providencia)", CATALOG, true),
        DemoSiteRow(id("site-5"), "Sede Maipú", CATALOG, true),
        DemoSiteRow(id("site-6"), "Sede San Joaquín", CATALOG, true),
        DemoSiteRow(id("site-7"), "Sede Concepción", CATALOG, true),
        DemoSiteRow(id("site-8"), "Sede Puerto Montt", CATALOG, true),
    )
    val profiles = listOf(
        DemoProfileRow(id("captor-a"), "Captador A", "CAPTOR"),
        DemoProfileRow(id("captor-b"), "Captador B", "CAPTOR"),
        DemoProfileRow(id("supervisor"), "Supervisor", "SUPERVISOR"),
    )
    val scopes = listOf(
        ProfileSiteRow(id("captor-a"), id("site-1")),
        ProfileSiteRow(id("captor-b"), id("site-2")),
        ProfileSiteRow(id("supervisor"), id("site-1")),
        ProfileSiteRow(id("supervisor"), id("site-2")),
    )
    val events = listOf(
        DemoEventRow(id("event-a"), id("site-1"), "Visita Liceo Bicentenario Viña del Mar", "2026-10-01T12:00:00Z", "2026-11-30T23:00:00Z", "America/Santiago", null, null, "Liceo Bicentenario Viña del Mar", "Viña del Mar"),
        DemoEventRow(id("event-a2"), id("site-1"), "Feria Vocacional Sede Viña del Mar", "2026-10-01T12:00:00Z", "2026-11-30T23:00:00Z", "America/Santiago", null, null, null, null),
        DemoEventRow(id("event-b"), id("site-2"), "Visita Colegio Salesiano Valparaíso", "2026-10-01T12:00:00Z", "2026-11-30T23:00:00Z", "America/Santiago", null, null, null, null),
    )
    val assignments = listOf(
        DemoAssignmentRow(id("event-a"), id("captor-a")),
        DemoAssignmentRow(id("event-a2"), id("captor-a")),
        DemoAssignmentRow(id("event-b"), id("captor-b")),
    )
    val programs = listOf(
        DemoProgramRow(id("program-1"), "Ingeniería en Informática", CATALOG, "Escuela de Informática y Telecomunicaciones", true),
        DemoProgramRow(id("program-2"), "Ingeniería en Marketing Digital", CATALOG, "Duoc Online", true),
        DemoProgramRow(id("program-3"), "Administración en Turismo y Hospitalidad", CATALOG, "Escuela de Turismo y Hospitalidad", true),
        DemoProgramRow(id("program-4"), "Analista Programador", CATALOG, "Escuela de Informática y Telecomunicaciones", true),
        DemoProgramRow(id("program-5"), "Ingeniería en Administración Mención Finanzas", CATALOG, "Escuela de Administración y Negocios", true),
        DemoProgramRow(id("program-6"), "Ingeniería en Construcción", CATALOG, "Escuela de Construcción", true),
        DemoProgramRow(id("program-7"), "Diseño Gráfico", CATALOG, "Escuela de Diseño", true),
        DemoProgramRow(id("program-8"), "Gastronomía Internacional", CATALOG, "Escuela de Gastronomía", true),
        DemoProgramRow(id("program-9"), "Técnico en Enfermería", CATALOG, "Escuela de Salud y Bienestar", true),
        DemoProgramRow(id("program-10"), "Ingeniería en Mecánica Automotriz y Autotrónica", CATALOG, "Escuela de Ingeniería y Recursos Naturales", true),
    )
    val mentions = listOf(
        DemoMentionRow(id("mention-a"), id("program-3"), "Administración Hotelera"),
        DemoMentionRow(id("mention-b"), id("program-3"), "Ecoturismo"),
        DemoMentionRow(id("mention-c"), id("program-3"), "Gestión de Destinos Turísticos"),
        DemoMentionRow(id("mention-d"), id("program-3"), "Hospitality Management"),
    )
}
