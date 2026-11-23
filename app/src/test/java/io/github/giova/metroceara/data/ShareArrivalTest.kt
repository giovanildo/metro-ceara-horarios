package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareArrivalTest {
    private val rodape = "Previsão pela tabela de horários.\n\n${ShareArrival.FOOTER}"

    // A Linha Sul de Carlito Benevides a Chico da Silva, metrô das 07:00 de um dia útil.
    private val sul = listOf(
        "Carlito Benevides" to "07:00", "Jereissati" to "07:01", "Maracanaú" to "07:03", "Virgílio Távora" to "07:05",
        "Raquel de Queiroz" to "07:08", "Alto Alegre" to "07:11", "Aracapé" to "07:14", "Conjunto Esperança" to "07:16",
        "Mondubim" to "07:18", "Manoel Sátiro" to "07:20", "Vila Pery" to "07:22", "Parangaba" to "07:24",
        "Juscelino Kubitscheck" to "07:26", "Couto Fernandes" to "07:28", "Porangabussu" to "07:30", "Padre Cícero" to "07:31",
        "Benfica" to "07:34", "São Benedito" to "07:36", "José de Alencar" to "07:37", "Chico da Silva" to "07:40",
    ).map { (n, h) -> ShareArrival.Stop(n, minutes(h)) }

    private val nordeste = listOf(
        "Parangaba - NE" to "07:30", "Montese" to "07:33", "Expedicionarios" to "07:36", "Vila União" to "07:39",
        "Borges de Melo" to "07:44", "São João do Tauape" to "07:50", "Pontes Vieira" to "07:54", "Antônio Sales" to "07:57",
        "Papicu" to "08:00",
    ).map { (n, h) -> ShareArrival.Stop(n, minutes(h)) }

    private fun minutes(h: String) = h.substring(0, 2).toInt() * 60 + h.substring(3).toInt()

    private fun leg(route: String, headsign: String, stops: List<ShareArrival.Stop>) =
        ShareArrival.Leg(route, headsign, stops.first().name, stops.last().name, stops.first().time, stops.last().time, stops)

    private val livre = TransferRule(TransferFare.UNKNOWN)
    private val integracoes = listOf(
        IntegracaoRegra("Parangaba", "Parangaba - Ne", 2, "", livre, livre),
        IntegracaoRegra("Expedicionarios", "Expedicionarios - Ae", 3, "", livre, livre),
        IntegracaoRegra("Chico Da Silva", "Moura Brasil", 5, "", livre, livre),
    )

    @Test
    fun `de 4 em 4, com 4 ou menos todas, e a última sempre`() {
        assertEquals(listOf(0, 1, 2, 3), ShareArrival.pick(4))
        assertEquals(listOf(0, 4), ShareArrival.pick(5))
        assertEquals(listOf(0, 4, 8, 10), ShareArrival.pick(11))
        // A da contagem colada na última fica de fora (8 e 9).
        assertEquals(listOf(0, 4, 9), ShareArrival.pick(10))
    }

    @Test
    fun `a integração entra sempre e recomeça a contagem`() {
        assertEquals(listOf(0, 4, 8, 11, 15, 19), ShareArrival.pick(20, setOf(11)))
        // Colada na integração (12 e 13): fica de fora.
        assertEquals(listOf(0, 4, 8, 13, 17, 19), ShareArrival.pick(20, setOf(13)))
    }

    @Test
    fun `antes de embarcar, a Linha Sul inteira`() {
        assertEquals(
            "🚇 Vou pegar o metrô das 07:00 na estação Carlito Benevides (Linha Sul, sentido Chico da Silva).\n\n" +
                "07:00 · Carlito Benevides\n07:08 · Raquel de Queiroz\n07:18 · Mondubim\n07:24 · Parangaba\n" +
                "07:31 · Padre Cícero\n07:40 · Chico da Silva, onde desço\n\n$rodape",
            ShareArrival.text(listOf(leg("Linha Sul", "Chico da Silva", sul)), onBoard = false, transfers = integracoes),
        )
    }

    @Test
    fun `com integração, cada trecho conta separado`() {
        val text = ShareArrival.text(
            listOf(leg("Linha Sul", "Chico da Silva", sul.take(12)), leg("Linha Nordeste", "Iate", nordeste)), onBoard = false, transfers = integracoes,
        )
        assertTrue(text, text.contains(
            "\n07:00 · Carlito Benevides\n07:08 · Raquel de Queiroz\n07:18 · Mondubim\n07:24 · Parangaba\n" +
                "🔄 Integração para o VLT da Linha Nordeste, sentido Iate\n" +
                "07:30 · Parangaba\n07:36 · Expedicionarios\n07:54 · Pontes Vieira\n08:00 · Papicu, onde desço\n",
        ))
    }

    @Test
    fun `dentro do metrô, com atraso`() {
        val atrasado = sul.drop(12).take(7).map { it.copy(time = it.time + 3) }
        assertEquals(
            "🚇 Estou no metrô da Linha Sul, sentido Chico da Silva.\n\n" +
                "07:29 · Juscelino Kubitscheck\n07:37 · Benfica\n07:40 · José de Alencar, onde desço\n\n" +
                "⏱️ O metrô está uns 3 min atrasado; os horários já contam isso.\n\n" +
                ShareArrival.FOOTER,
            ShareArrival.text(listOf(leg("Linha Sul", "Chico da Silva", atrasado)), onBoard = true, delayMinutes = 3),
        )
    }

    @Test
    fun `sentado pelo terminal, o terminal entra`() {
        val ida = listOf("Benfica" to 600, "São Benedito" to 602, "José de Alencar" to 604, "Chico da Silva" to 607,
            "José de Alencar" to 615, "São Benedito" to 617)
            .mapIndexed { i, (n, m) -> ShareArrival.Stop(n, m, anchor = i == 3) }
        val text = ShareArrival.text(listOf(leg("Linha Sul", "Chico da Silva", ida)), onBoard = false)
        assertTrue(text, text.contains("\n10:00 · Benfica\n10:07 · Chico da Silva\n10:17 · São Benedito, onde desço\n"))
    }

    @Test
    fun `amanhã, lugar buscado e VLT sem Linha no nome`() {
        val leg = ShareArrival.Leg("VLT Aeroporto", "Aeroporto", "Expedicionários", "Aeroporto", 8 * 60, 8 * 60 + 12)
        val text = ShareArrival.text(listOf(leg), onBoard = false, tomorrow = true, placeName = "Aeroporto Pinto Martins", walkMinutes = 6)
        assertTrue(text, text.startsWith("🚇 Amanhã vou pegar o VLT das 08:00 na estação Expedicionários (VLT Aeroporto, sentido Aeroporto).\n\n"))
        assertTrue(text, text.contains("\n08:00 · Expedicionários\n08:12 · Aeroporto, onde desço\n🚶 De lá, a pé até Aeroporto Pinto Martins (uns 6 min).\n"))
    }

    @Test
    fun `os três links no fim`() {
        val leg = ShareArrival.Leg("Linha Sul", "Carlito Benevides", "Benfica", "Parangaba", 9 * 60, 9 * 60 + 10)
        assertTrue(ShareArrival.text(listOf(leg), onBoard = false).endsWith(
            "Metrô Ceará, horários do metrô e do VLT:\n" +
                "Android: https://play.google.com/store/apps/details?id=io.github.giova.metroceara\n" +
                "iPhone: https://giovanildo.github.io/metro-ceara-horarios/app/\n" +
                "Site: https://giovanildo.github.io/metro-ceara-horarios/",
        ))
    }

    @Test
    fun `nome novo da estação`() {
        val leg = ShareArrival.Leg("Linha Sul", "Carlito Benevides", "Benfica", "Jereissati", 9 * 60, 9 * 60 + 30)
        assertTrue(ShareArrival.text(listOf(leg), onBoard = false).contains("Francisco Alves Marinho (Jereissati), onde desço"))
    }
}
