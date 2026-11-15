package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineNoticesTest {
    private val vagao = LineNotice("Vagão feminino: 2 vagões só para mulheres.", "vagao_feminino")
    private val regras = Regras(emptyMap(), emptyMap(), emptyList(), lineNotices = mapOf("linha sul" to listOf(vagao)))

    @Test
    fun `aparece em todas as estações da Linha Sul, com qualquer grafia do nome`() {
        assertEquals(listOf(vagao), regras.lineNotices("Linha Sul"))
        assertEquals(listOf(vagao), regras.lineNotices(" linha sul "))
    }

    @Test
    fun `não aparece nas outras linhas`() {
        assertTrue(regras.lineNotices("Linha Nordeste").isEmpty())
        assertTrue(regras.lineNotices("VLT Sobral - Linha Sul").isEmpty())
    }
}
