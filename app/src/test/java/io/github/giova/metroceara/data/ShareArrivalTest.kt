package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareArrivalTest {
    private val sul = ShareArrival.Leg("Linha Sul", "Carlito Benevides", "Benfica", "Parangaba", 15 * 60 + 4)
    private val rodape = "(previsão pela tabela de horários)\n\nHorários do metrô no app Metrô Ceará:\n${ShareArrival.PLAY_STORE}"

    @Test
    fun `antes de embarcar, viagem direta`() {
        assertEquals(
            "🚇 Vou pegar o metrô das 15:04 na estação Benfica: Linha Sul, sentido Carlito Benevides.\n" +
                "Chego na estação Parangaba por volta das 15:14.\n$rodape",
            ShareArrival.text(listOf(sul), 15 * 60 + 14, onBoard = false),
        )
    }

    @Test
    fun `dentro do metrô`() {
        val leg = ShareArrival.Leg("Linha Sul", "Chico da Silva", "Aracapé", "Juscelino Kubitscheck", 13 * 60 + 53)
        assertEquals(
            "🚇 Estou no metrô da Linha Sul, sentido Chico da Silva.\n" +
                "Chego na estação Juscelino Kubitscheck por volta das 14:06.\n$rodape",
            ShareArrival.text(listOf(leg), 14 * 60 + 6, onBoard = true),
        )
    }

    @Test
    fun `com integração cita a troca e a linha`() {
        val a = ShareArrival.Leg("Linha Sul", "Carlito Benevides", "São Benedito", "Parangaba", 10 * 60 + 34)
        val b = ShareArrival.Leg("Linha Nordeste", "Iate", "Parangaba - NE", "Papicu", 11 * 60 + 10)
        val text = ShareArrival.text(listOf(a, b), 11 * 60 + 40, onBoard = false)
        assertTrue(text, text.contains("\nEm Parangaba faço a integração para o VLT da Linha Nordeste e chego na estação Papicu por volta das 11:40.\n"))
    }

    @Test
    fun `amanhã, lugar buscado e VLT sem Linha no nome`() {
        val leg = ShareArrival.Leg("VLT Aeroporto", "Aeroporto", "Expedicionários", "Aeroporto", 8 * 60)
        val text = ShareArrival.text(listOf(leg), 8 * 60 + 12, onBoard = false, tomorrow = true, placeName = "Aeroporto Pinto Martins")
        assertTrue(text, text.startsWith("🚇 Amanhã vou pegar o VLT das 08:00 na estação Expedicionários: VLT Aeroporto, sentido Aeroporto.\n"))
        assertTrue(text, text.contains("\nDesço na estação Aeroporto por volta das 08:12 e vou a pé até Aeroporto Pinto Martins.\n"))
    }

    @Test
    fun `nome novo da estação`() {
        val leg = ShareArrival.Leg("Linha Sul", "Carlito Benevides", "Benfica", "Jereissati", 9 * 60)
        assertTrue(ShareArrival.text(listOf(leg), 9 * 60 + 30, onBoard = false).contains("estação Francisco Alves Marinho (Jereissati)"))
    }

    @Test
    fun `link Acompanhe a viagem leva os trechos e vai no lugar do link do app`() {
        val link = ShareArrival.trackLink("2026-11-21", listOf(
            ShareArrival.TrackLeg("1", "13", 432, "5", 439),
            ShareArrival.TrackLeg("3", "31", 450, "28", 479),
        ))
        assertEquals("https://giovanildo.github.io/metro-ceara-horarios/app/#/acompanhar/1.20261121.1-13-432-5-439_3-31-450-28-479", link)
        val leg = ShareArrival.Leg("Linha Sul", "Chico da Silva", "Mondubim", "Parangaba", 432)
        val text = ShareArrival.text(listOf(leg), 439, onBoard = false, track = link)
        assertTrue(text, text.endsWith("(previsão pela tabela de horários)\n\n📍 Acompanhe a viagem:\n$link"))
        assertTrue(text, !text.contains("play.google.com"))
    }

    @Test
    fun `sem os códigos (viagem salva antes) não há link`() {
        assertEquals(null, ShareArrival.trackLink("2026-11-21", listOf(ShareArrival.TrackLeg("", "13", 432, "5", 439))))
        assertEquals(null, ShareArrival.trackLink("2026-11-21", emptyList()))
    }
}
