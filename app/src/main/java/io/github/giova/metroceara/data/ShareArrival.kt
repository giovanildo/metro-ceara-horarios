package io.github.giova.metroceara.data

import java.text.Normalizer

/**
 * "Avisar alguém": o texto pronto da viagem, para mandar pelo WhatsApp ou
 * outro app. Sem servidor e sem localização: só o texto que a pessoa escolhe
 * enviar. As estações vão com o horário do lado, uma parte delas ([pick]), e no
 * modo viagem os horários já trazem o atraso que o app percebeu.
 */
object ShareArrival {

    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=io.github.giova.metroceara"
    const val LITE = "https://giovanildo.github.io/metro-ceara-horarios/app/"
    const val SITE = "https://giovanildo.github.io/metro-ceara-horarios/"

    /**
     * O fim da mensagem: o app na loja (Android), a lite (iPhone, no navegador)
     * e o site. Quem manda não sabe o celular de quem recebe, então vão os três.
     * [origin]: que texto levou a pessoa à loja (avisar, estacao, aviso, bike,
     * nome). Vai no referrer como utm_source "app-<origin>", e o Play Console
     * mostra quantas instalações vieram de cada tipo de mensagem.
     */
    fun footer(origin: String) = listOf(
        "Metrô Ceará, horários do metrô e do VLT:",
        "Android: ${playStore("app-$origin")}",
        "iPhone: $LITE",
        "Site: $SITE",
    ).joinToString("\n")

    /** O link da loja com a origem no referrer ("utm_source=<source>", codificado). */
    fun playStore(source: String) = "$PLAY_STORE&referrer=utm_source%3D$source"

    /** Uma estação do trecho, com o horário (minutos desde a meia-noite). [anchor]: entra sempre (o terminal onde o metrô volta). */
    data class Stop(val name: String, val time: Int, val anchor: Boolean = false)

    /** Um trecho do roteiro, do jeito que a mensagem precisa. [stops] vazio: só a partida e a chegada. */
    data class Leg(
        val routeName: String,
        val headsign: String,
        val fromName: String,
        val toName: String,
        val departure: Int,
        val arrival: Int,
        val stops: List<Stop> = emptyList(),
    )

    /**
     * Quais estações de um trecho entram na mensagem, pelos índices: com 4 ou
     * menos, todas; senão a primeira, depois uma a cada 4, e a última. As de
     * [anchors] (integração, terminal) entram sempre e recomeçam a contagem; a da
     * contagem que cairia colada numa delas, ou na última, fica de fora.
     */
    fun pick(size: Int, anchors: Set<Int> = emptySet()): List<Int> {
        if (size <= 4) return (0 until size).toList()
        val fixed = (anchors.filter { it in 1 until size - 1 } + 0 + (size - 1)).toSortedSet().toList()
        val out = mutableListOf<Int>()
        fixed.zipWithNext { a, b ->
            out += a
            var i = a + STEP
            while (i < b) {
                if (b - i >= 2) out += i
                i += STEP
            }
        }
        return out + (size - 1)
    }

    /**
     * [legs]: os trechos que faltam (no modo viagem, a partir da próxima estação).
     * [onBoard]: já dentro do metrô. [placeName]: o lugar buscado, quando o destino
     * veio dele, com a caminhada em [walkMinutes]. [delayMinutes]: o atraso já
     * somado aos horários do primeiro trecho (modo viagem). [transfers]: as
     * integrações, cujas estações entram sempre na lista.
     */
    fun text(
        legs: List<Leg>,
        onBoard: Boolean,
        tomorrow: Boolean = false,
        placeName: String? = null,
        walkMinutes: Int? = null,
        delayMinutes: Int = 0,
        transfers: List<IntegracaoRegra> = Regras.current.integracoes,
    ): String {
        require(legs.isNotEmpty())
        val first = legs.first()
        val start = if (onBoard) {
            "Estou no ${line(first.routeName)}, sentido ${first.headsign}."
        } else {
            (if (tomorrow) "Amanhã vou pegar" else "Vou pegar") +
                " o ${vehicleName(first.routeName)} das ${formatTime(first.departure)} na estação ${name(first.fromName)}" +
                " (${first.routeName}, sentido ${first.headsign})."
        }
        val lines = mutableListOf("🚇 $start", "")
        legs.forEachIndexed { n, leg ->
            if (n > 0) lines += "🔄 Integração para o ${line(leg.routeName)}, sentido ${leg.headsign}"
            val stops = leg.stops.ifEmpty { listOf(Stop(leg.fromName, leg.departure), Stop(leg.toName, leg.arrival)) }
            val anchors = stops.indices.filter { stops[it].anchor || isTransfer(stops[it].name, transfers) }.toSet()
            pick(stops.size, anchors).forEach { i ->
                val stop = stops[i]
                val last = n == legs.lastIndex && i == stops.lastIndex
                lines += "${formatTime(stop.time)} · ${name(stop.name)}" + if (last) ", onde desço" else ""
            }
        }
        if (placeName != null) {
            lines += "🚶 De lá, a pé até $placeName" + (walkMinutes?.let { " (uns $it min)." } ?: ".")
        }
        lines += ""
        lines += if (delayMinutes >= 1) {
            "⏱️ O ${vehicleName(first.routeName)} está uns $delayMinutes min atrasado; os horários já contam isso."
        } else {
            "Previsão pela tabela de horários."
        }
        lines += ""
        lines += footer("avisar")
        return lines.joinToString("\n")
    }

    private const val STEP = 4

    /** "metrô da Linha Sul", "VLT da Linha Nordeste", "VLT Aeroporto". */
    private fun line(routeName: String) =
        if (routeName.startsWith("Linha")) "${vehicleName(routeName)} da $routeName" else routeName

    private fun name(n: String) = StationNames.official(JourneyPlanner.plainName(n))

    /** Estação de integração (regras.json), mesmo sem troca nela: vira ponto de referência na lista. */
    private fun isTransfer(stationName: String, transfers: List<IntegracaoRegra>): Boolean {
        val k = key(stationName)
        return transfers.any { key(it.a) == k || key(it.b) == k }
    }

    private fun key(n: String) = Normalizer.normalize(JourneyPlanner.plainName(n), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").trim().lowercase()
}
