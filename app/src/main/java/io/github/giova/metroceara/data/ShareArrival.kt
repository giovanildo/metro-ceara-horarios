package io.github.giova.metroceara.data

/**
 * "Avisar alguém": o texto pronto da previsão de chegada, para mandar pelo
 * WhatsApp ou outro app. Sem servidor e sem localização: só o texto que a
 * pessoa escolhe enviar. O horário vem da tabela, por isso "por volta das".
 */
object ShareArrival {

    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=io.github.giova.metroceara"

    /** Um trecho do roteiro, do jeito que a mensagem precisa. */
    data class Leg(val routeName: String, val headsign: String, val fromName: String, val toName: String, val departure: Int)

    /**
     * [legs]: os trechos que faltam (no modo viagem, a partir do atual).
     * [arrival]: a chegada à última estação. [onBoard]: já dentro do metrô.
     * [placeName]: o lugar buscado, quando o destino veio dele.
     */
    fun text(
        legs: List<Leg>,
        arrival: Int,
        onBoard: Boolean,
        tomorrow: Boolean = false,
        placeName: String? = null,
        link: String = PLAY_STORE,
    ): String {
        require(legs.isNotEmpty())
        val first = legs.first()
        val start = if (onBoard) {
            "Estou no ${line(first.routeName)}, sentido ${first.headsign}."
        } else {
            (if (tomorrow) "Amanhã vou pegar" else "Vou pegar") +
                " o ${vehicleName(first.routeName)} das ${formatTime(first.departure)} na estação ${name(first.fromName)}" +
                ": ${first.routeName}, sentido ${first.headsign}."
        }
        val last = name(legs.last().toName)
        val transfers = legs.drop(1).mapIndexed { i, leg ->
            val at = name(leg.fromName)
            if (i == 0) "Em $at faço a integração para o ${line(leg.routeName)}" else "em $at para o ${line(leg.routeName)}"
        }
        val at = formatTime(arrival)
        val end = when {
            placeName != null -> "desço na estação $last por volta das $at e vou a pé até $placeName."
            else -> "chego na estação $last por volta das $at."
        }
        val finish = if (transfers.isEmpty()) end.replaceFirstChar { it.uppercase() } else transfers.joinToString(", ") + " e " + end
        return "🚇 $start\n$finish\n(previsão pela tabela de horários)\n\nHorários do metrô no app Metrô Ceará:\n$link"
    }

    /** "metrô da Linha Sul", "VLT da Linha Nordeste", "VLT Aeroporto". */
    private fun line(routeName: String) =
        if (routeName.startsWith("Linha")) "${vehicleName(routeName)} da $routeName" else routeName

    private fun name(n: String) = StationNames.official(JourneyPlanner.plainName(n))
}
