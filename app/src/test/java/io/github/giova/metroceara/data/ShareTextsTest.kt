package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextsTest {
    private fun m(h: String) = h.substring(0, 2).toInt() * 60 + h.substring(3).toInt()

    @Test
    fun `próximos 4 de cada sentido, com o dia e a hora`() {
        val text = ShareTexts.station(
            "Parangaba", "Linha Sul", "2026-11-23", m("17:05"),
            listOf(
                ShareTexts.DirectionTimes("Chico da Silva", listOf("16:58", "17:16", "17:34", "17:52", "18:10", "18:28").map(::m)),
                ShareTexts.DirectionTimes("Carlito Benevides", listOf("17:15", "23:33").map(::m)),
            ),
        )
        assertEquals(
            "🚇 Próximos na estação Parangaba, hoje, segunda (23/11), a partir das 17:05:\n\n" +
                "Metrô da Linha Sul\n" +
                "sentido Chico da Silva: 17:16 · 17:34 · 17:52 · 18:10\n" +
                "sentido Carlito Benevides: 17:15 · 23:33 (o último)\n\n" +
                "Pela tabela de horários do Metrofor.\n\n" + ShareArrival.footer("estacao"),
            text,
        )
    }

    @Test
    fun `sentido sem mais metrô hoje`() {
        val text = ShareTexts.station("Iate", "Linha Nordeste", "2026-11-23", m("23:00"),
            listOf(ShareTexts.DirectionTimes("Parangaba", listOf(m("22:50")))))
        assertTrue(text, text.contains("VLT da Linha Nordeste\nsentido Parangaba: não tem mais hoje\n"))
    }

    @Test
    fun `operação especial com horário por linha, bike e fonte`() {
        val op = SpecialOperation(
            title = "Tem metrô e VLT, das 05:30 às 18:10 (varia por linha), menos VLT de Sobral e VLT do Cariri.",
            link = null, normal = false, stopped = emptySet(),
            hoursByLine = listOf("Linha Sul" to (m("05:30") to m("18:10")), "Linha Nordeste" to (m("05:30") to m("17:30"))),
            published = "2026-11-17",
        )
        val bike = BikeSpecialDay(setOf("linha sul", "linha nordeste"), listOf(BikeWindow("09:00")))
        assertEquals(
            "⚠️ Metrofor, sexta (20/11): horário especial no metrô e no VLT\n\n" +
                "Tem metrô e VLT, das 05:30 às 18:10 (varia por linha), menos VLT de Sobral e VLT do Cariri.\n\n" +
                "Linha Sul: 05:30 às 18:10\nLinha Nordeste: 05:30 às 17:30\n" +
                "🚲 Bike: a partir das 9h (Linha Sul e Linha Nordeste)\n\n" +
                "Fonte: Metrofor, notícia de 17/11\n\n" + ShareArrival.footer("aviso"),
            ShareTexts.operation(listOf("2026-11-20"), op, bike),
        )
    }

    @Test
    fun `bike, a semana e o hoje`() {
        val rule = BikeRule(listOf(BikeWindow("09:00", "15:00"), BikeWindow("20:00")), listOf(BikeWindow("15:00")), perTrip = 4)
        val text = ShareTexts.bike("Linha Sul", rule, "Bike elétrica ou de carga não pode.", "2026-11-23",
            "Pode embarcar com bike no metrô agora, até as 15:00", "Regulamento de bikes do Metrofor (2023)")
        assertEquals(
            "🚲 Bike no metrô da Linha Sul:\n\n" +
                "Segunda a sexta: das 9h às 15h e a partir das 20h\n" +
                "Sábado: a partir das 15h\n" +
                "Domingo e feriado: confira o aviso do dia\n" +
                "Até 4 bikes por viagem; se lotar, espera o próximo.\n" +
                "Bike elétrica ou de carga não pode.\n\n" +
                "Hoje, segunda (23/11): pode embarcar com bike no metrô agora, até as 15:00.\n\n" +
                "Fonte: Regulamento de bikes do Metrofor (2023)\n\n" + ShareArrival.footer("bike"),
            text,
        )
    }

    @Test
    fun `horas por extenso`() {
        assertEquals("9h", ShareTexts.hourLabel("09:00"))
        assertEquals("16h30", ShareTexts.hourLabel("16:30"))
        assertEquals("das 9h às 16h30 e das 19h às 23h",
            ShareTexts.windowsLabel(listOf(BikeWindow("09:00", "16:30"), BikeWindow("19:00", "23:00"))))
    }

    @Test
    fun `slug do nome igual ao do site`() {
        assertEquals("chico-da-silva", NameStory.slug("Chico da Silva"))
        assertEquals("expedicionarios", NameStory.slug("Expedicionarios - AE"))
        assertEquals("francisco-alves-marinho", NameStory.slug("Jereissati"))
        assertEquals("sao-joao-do-tauape", NameStory.slug("São João do Tauape"))
    }

    @Test
    fun `legenda do cartão do nome`() {
        val story = NameStory("chico-da-silva", "Homenageia o pintor Chico da Silva.", "Wikipédia", null)
        assertEquals(
            "🚉 Por que esse nome? Estação Chico da Silva\n\nHomenageia o pintor Chico da Silva.\n\n" +
                "Fonte: Wikipédia\nNa página da estação: https://giovanildo.github.io/metro-ceara-horarios/estacoes/chico-da-silva/\n\n" +
                ShareArrival.footer("nome"),
            ShareTexts.nameStory("Chico da Silva", story),
        )
    }
}
