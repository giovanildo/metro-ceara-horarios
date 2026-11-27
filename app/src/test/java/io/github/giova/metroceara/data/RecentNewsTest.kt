package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Todas as notícias": as do site do Metrofor dos últimos dias, sem repetir o aviso especial. */
class RecentNewsTest {
    private val volumes = MetroforNews("2026-11-26", "Black Friday: regras para volumes", "https://x/volumes")
    private val feriado = MetroforNews("2026-11-17", "Horários especiais nesta sexta, 20/11", "https://x/feriado")
    private val enem = MetroforNews("2026-11-25", "Domingo de Enem terá metrô de graça", "https://x/enem")
    private val semLink = MetroforNews("2026-11-24", "Campanha no metrô", null)

    private fun regras(vararg ops: Pair<String, SpecialOperation>) = Regras(
        emptyMap(), emptyMap(), emptyList(),
        operations = mapOf(*ops),
        news = listOf(feriado, semLink, volumes, enem),
    )

    @Test
    fun `da mais nova para a mais antiga, só da janela de dias`() {
        assertEquals(listOf(volumes, enem, semLink), regras().recentNews("2026-11-27", "2026-11-20"))
    }

    @Test
    fun `no máximo três`() {
        assertEquals(2, regras().recentNews("2026-11-27", "2026-11-01", max = 2).size)
    }

    @Test
    fun `sai a notícia que já é o aviso de operação especial de hoje em diante`() {
        val op = SpecialOperation("Domingo de Enem", "https://x/enem", normal = false, stopped = emptySet())
        assertEquals(listOf(volumes, semLink), regras("2026-11-29" to op).recentNews("2026-11-27", "2026-11-20"))
        // Operação que já passou não esconde nada.
        assertTrue(enem in regras("2026-11-20" to op).recentNews("2026-11-27", "2026-11-20"))
    }
}
