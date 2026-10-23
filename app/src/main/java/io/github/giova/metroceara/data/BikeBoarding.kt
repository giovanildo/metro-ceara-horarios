package io.github.giova.metroceara.data

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

/** A regra que vale nesta estação desta linha, ou `null` se lá não há aviso de bike. */
fun bikeRuleFor(stationName: String, lineName: String): BikeRule? {
    // As regras ficam em assets/regras.json (seção "bike"), não aqui.
    val rules = Regras.current
    return rules.bikeByStation[stationName.trim().lowercase()] ?: rules.bikeByLine[lineName.trim().lowercase()]
}

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
