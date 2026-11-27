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
    /** Regra de um dia especial (feriado, ENEM, eleição): vale até no domingo. */
    val special: Boolean = false,
    /** Quantas bikes cabem por viagem; acima disso, espera o próximo trem. */
    val perTrip: Int? = null,
    /** De onde vem a regra (`fontes` do regras.json); a de dia especial vem da notícia do dia. */
    val sourceId: String? = null,
) {
    fun windowsFor(dayOfWeek: Int): List<BikeWindow> = when (dayOfWeek) {
        Calendar.SATURDAY -> saturday
        Calendar.SUNDAY -> sunday
        else -> weekday
    }
}

/**
 * A regra que vale hoje nesta estação desta linha, ou `null` se lá não há aviso
 * de bike. Num dia especial anunciado pelo Metrofor (regras.json,
 * "datas_especiais"), vale a regra do dia, para qualquer dia da semana.
 */
fun bikeRuleFor(stationName: String, lineName: String, today: String = ScheduleStore.today()): BikeRule? {
    // As regras ficam em assets/regras.json (seção "bike"), não aqui.
    val rules = Regras.current
    val line = lineName.trim().lowercase()
    val normal = normalBikeRule(stationName, lineName)
    val special = rules.bikeSpecialDays[today]?.takeIf { normal != null && (it.lines.isEmpty() || line in it.lines) }
        ?: return normal
    return BikeRule(special.windows, special.windows, special.windows, special = true, perTrip = normal?.perTrip)
}

/** A regra comum desta estação desta linha (a do regulamento), sem a de dia especial. */
fun normalBikeRule(stationName: String, lineName: String): BikeRule? =
    Regras.current.bikeByStation[stationName.trim().lowercase()] ?: Regras.current.bikeByLine[lineName.trim().lowercase()]

/**
 * Se dá para entrar com bicicleta agora.
 *
 * [boundary] é o fim da janela atual quando [allowed], ou o começo da próxima
 * janela de hoje quando não; `null` quer dizer "até o fim da operação" ou
 * "nenhuma janela hoje", respectivamente.
 */
data class BikeBoarding(
    val allowed: Boolean,
    val boundary: Int?,
    /** Hoje tem (ou teve) alguma janela de bike; falso = dia sem bike nenhuma. */
    val anyToday: Boolean = true,
    /** Operação especial (domingo) sem regra de bike anunciada: o app não sabe. */
    val unknown: Boolean = false,
) {
    companion object {
        val UNKNOWN = BikeBoarding(allowed = false, boundary = null, unknown = true)
    }
}

fun bikeBoarding(rule: BikeRule, dayOfWeek: Int, minutes: Int): BikeBoarding {
    val windows = rule.windowsFor(dayOfWeek).sortedBy { it.startMinutes }
    windows.forEach { window ->
        val end = window.endMinutes
        if (minutes >= window.startMinutes && (end == null || minutes < end)) return BikeBoarding(true, end)
    }
    val next = windows.map { it.startMinutes }.firstOrNull { it > minutes }
    return BikeBoarding(false, next, anyToday = windows.isNotEmpty())
}

private fun toMinutes(hhmm: String): Int {
    val (h, m) = hhmm.trim().split(":").map { it.toInt() }
    return h * 60 + m
}

/**
 * Horário para levar compras e volumes grandes: as janelas no mesmo formato das
 * de bike ([rule]) e as medidas ([sizes]), da segunda linha do aviso.
 */
data class VolumeRule(val rule: BikeRule, val sizes: String?)

/**
 * Se dá para levar volume grande agora, ou `null` quando não há o que avisar:
 * sem a regra, sem mais metrô hoje ([runningToday]), no domingo e em dia de
 * operação especial (a notícia do dia não fala de volumes).
 */
fun volumeBoarding(
    volumes: VolumeRule?,
    runningToday: Boolean,
    specialToday: Boolean,
    dayOfWeek: Int,
    minutes: Int,
): BikeBoarding? {
    if (volumes == null || !runningToday || specialToday || dayOfWeek == Calendar.SUNDAY) return null
    return bikeBoarding(volumes.rule, dayOfWeek, minutes)
}
