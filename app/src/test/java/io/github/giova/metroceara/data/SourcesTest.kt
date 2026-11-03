package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesTest {
    private fun source(
        kind: InfoSource.Kind = InfoSource.Kind.METROFOR,
        since: String? = null,
        checked: String? = "2026-11",
        review: Int? = 6,
        daily: Boolean = false,
        origin: String = "site do Metrofor",
    ) = InfoSource("x", "Título", kind, origin, null, null, since, YearMonth.parse(checked), review, daily)

    @Test
    fun `mês somado vira o ano`() {
        assertEquals(YearMonth(2027, 5), YearMonth(2026, 11).plus(6))
        assertEquals(YearMonth(2027, 11), YearMonth(2026, 11).plus(12))
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12).plus(1))
    }

    @Test
    fun `lê ano e mês e ignora o dia`() {
        assertEquals(YearMonth(2026, 11), YearMonth.parse("2026-11"))
        assertEquals(YearMonth(2026, 11), YearMonth.parse("2026-11-02"))
        assertNull(YearMonth.parse("nov/2026"))
        assertNull(YearMonth.parse("2026-13"))
        assertNull(YearMonth.parse(null))
    }

    @Test
    fun `fonte do Metrofor em dia mostra a próxima conferência`() {
        val s = source(since = "preço sem reajuste desde mar/2019")
        assertEquals(
            "Fonte: site do Metrofor · preço sem reajuste desde mar/2019 · conferido em nov/2026 · próxima conferência: mai/2027",
            s.line(YearMonth(2026, 11)),
        )
        // No próprio mês da próxima conferência ainda está em dia.
        assertFalse(s.overdue(YearMonth(2027, 5)))
    }

    @Test
    fun `passada a data, a linha avisa que pode ter mudado`() {
        val s = source()
        assertTrue(s.overdue(YearMonth(2027, 6)))
        assertEquals(
            "Fonte: site do Metrofor · conferido em nov/2026 · conferência atrasada desde mai/2027: pode ter mudado",
            s.line(YearMonth(2027, 6)),
        )
    }

    @Test
    fun `estimativa do app não parece dado oficial`() {
        val s = source(kind = InfoSource.Kind.APP, checked = null, review = null, origin = "Estimativa do app")
        assertEquals("Estimativa do app", s.line(YearMonth(2026, 11)))
        val observed = source(kind = InfoSource.Kind.APP, checked = "2026-10", review = null, origin = "Observado nas viagens de teste do app")
        assertEquals("Observado nas viagens de teste do app · out/2026", observed.line(YearMonth(2026, 11)))
    }

    @Test
    fun `fonte acompanhada todo dia não tem conferência`() {
        val s = source(checked = null, review = null, daily = true, origin = "notícias do Metrofor")
        assertEquals("Fonte: notícias do Metrofor · acompanhado todo dia", s.line(YearMonth(2026, 11)))
    }

    @Test
    fun `o link é curto e só cresce quando a conferência atrasou`() {
        val s = source(since = "preço sem reajuste desde mar/2019")
        assertEquals("Fonte", s.label(YearMonth(2026, 11)))
        assertEquals("Fonte · pode ter mudado", s.label(YearMonth(2027, 6)))
        val estimate = source(kind = InfoSource.Kind.APP, checked = null, review = null, origin = "Estimativa do app")
        assertEquals("Estimativa do app", estimate.label(YearMonth(2026, 11)))
    }

    @Test
    fun `a janela mostra uma coisa por linha`() {
        val s = source(since = "preço sem reajuste desde mar/2019")
        assertEquals(
            listOf(
                "Fonte: site do Metrofor",
                "Preço sem reajuste desde mar/2019",
                "Conferido em nov/2026",
                "Próxima conferência: mai/2027",
            ),
            s.facts(YearMonth(2026, 11)),
        )
        assertEquals("Conferência atrasada desde mai/2027: pode ter mudado", s.facts(YearMonth(2027, 6)).last())
        val observed = source(kind = InfoSource.Kind.APP, checked = "2026-10", review = null, origin = "Observado nas viagens de teste do app")
        assertEquals(listOf("Observado nas viagens de teste do app (out/2026)"), observed.facts(YearMonth(2026, 11)))
    }
}
