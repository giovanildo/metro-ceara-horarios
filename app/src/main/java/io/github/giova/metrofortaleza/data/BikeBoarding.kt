package io.github.giova.metrofortaleza.data

import java.util.Calendar

/**
 * Uma janela em que dá para entrar e sair com bicicleta: das [start] às [end]
 * ("HH:MM"). [end] `null` = até o fim da operação.
 */
data class BikeWindow(val start: String, val end: String? = null) {
    val startMinutes: Int get() = toMinutes(start)
    val endMinutes: Int? get() = end?.let(::toMinutes)
}

/** As janelas de bike de uma linha (ou estação), por tipo de dia. Lista vazia = sem bike. */
data class BikeRule(
    val weekday: List<BikeWindow>,
    val saturday: List<BikeWindow>,
    val sunday: List<BikeWindow> = emptyList(),
) {
    fun windowsFor(dayOfWeek: Int): List<BikeWindow> = when (dayOfWeek) {
        Calendar.SATURDAY -> saturday
        Calendar.SUNDAY -> sunday
        else -> weekday
    }
}

// ---------------------------------------------------------------------------
// PARÂMETROS — as regras de bike ficam todas aqui. Mudou o regulamento? Edite
// esta seção e gere uma versão nova. Ficam fora das Configurações de propósito:
// são regra do Metrofor, não preferência de quem usa o app.
// ---------------------------------------------------------------------------

/**
 * Regulamento "Bike é bem-vinda no metrô" (Metrofor, 2023): segunda a sexta das
 * 9h às 15h e a partir das 20h; sábado a partir das 15h; domingo não aparece.
 */
private val REGULAMENTO_2023 = BikeRule(
    weekday = listOf(BikeWindow("09:00", "15:00"), BikeWindow("20:00")),
    saturday = listOf(BikeWindow("15:00")),
)

/**
 * Regra de cada linha, pelo nome como aparece no app. Linha que não está aqui
 * (os VLTs, por enquanto) não tem aviso de bike — o regulamento não fala deles.
 */
private val BIKE_RULES_BY_LINE: Map<String, BikeRule> = mapOf(
    "Linha Sul" to REGULAMENTO_2023,
    "Linha Oeste" to REGULAMENTO_2023,
    "Linha Nordeste" to REGULAMENTO_2023,
)

/**
 * Exceções por estação, quando uma estação tiver regra diferente da sua linha
 * (por exemplo, sem bike numa estação lotada). Chave: nome da estação como no
 * app. Vazio hoje: todas seguem a regra da linha.
 */
private val BIKE_RULES_BY_STATION: Map<String, BikeRule> = emptyMap()

// ---------------------------------------------------------------------------

/** A regra que vale nesta estação desta linha, ou `null` se lá não há aviso de bike. */
fun bikeRuleFor(stationName: String, lineName: String): BikeRule? =
    BIKE_RULES_BY_STATION.entries.firstOrNull { it.key.equals(stationName.trim(), ignoreCase = true) }?.value
        ?: BIKE_RULES_BY_LINE.entries.firstOrNull { it.key.equals(lineName.trim(), ignoreCase = true) }?.value

/**
 * Se dá para entrar com bicicleta agora.
 *
 * [boundary] é o fim da janela atual quando [allowed], ou o começo da próxima
 * janela de hoje quando não; `null` quer dizer "até o fim da operação" ou
 * "nenhuma janela hoje", respectivamente.
 */
data class BikeBoarding(val allowed: Boolean, val boundary: Int?)

fun bikeBoarding(rule: BikeRule, dayOfWeek: Int, minutes: Int): BikeBoarding {
    val windows = rule.windowsFor(dayOfWeek).sortedBy { it.startMinutes }
    windows.forEach { window ->
        val end = window.endMinutes
        if (minutes >= window.startMinutes && (end == null || minutes < end)) return BikeBoarding(true, end)
    }
    val next = windows.map { it.startMinutes }.firstOrNull { it > minutes }
    return BikeBoarding(false, next)
}

private fun toMinutes(hhmm: String): Int {
    val (h, m) = hhmm.trim().split(":").map { it.toInt() }
    return h * 60 + m
}
