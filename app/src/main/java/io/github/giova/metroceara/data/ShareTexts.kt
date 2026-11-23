package io.github.giova.metroceara.data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Os textos de compartilhar (WhatsApp ou outro app) que não são o "Avisar alguém":
 * os próximos horários de uma estação, o aviso de operação especial e a regra de
 * bike. Todos dizem a que dia se referem (mensagem encaminhada vive dias) e
 * terminam com os links do app ([ShareArrival.footer]).
 */
object ShareTexts {

    /** Quantos horários de cada sentido vão na mensagem da estação. */
    const val NEXT_COUNT = 4

    /** Um sentido da estação: para onde vai e as partidas de hoje (minutos), em ordem. */
    data class DirectionTimes(val headsign: String, val departures: List<Int>)

    /**
     * Os próximos [NEXT_COUNT] metrôs de cada sentido da estação, a partir de [now].
     * [day]: o dia ("AAAA-MM-DD"), escrito por extenso no texto.
     */
    fun station(stationName: String, routeName: String, day: String, now: Int, directions: List<DirectionTimes>): String {
        val lines = mutableListOf(
            "🚇 Próximos na estação ${name(stationName)}, hoje, ${dayLabel(day)}, a partir das ${formatTime(now)}:",
            "",
            lineLabel(routeName).replaceFirstChar { it.uppercase() },
        )
        directions.forEach { d ->
            val next = d.departures.filter { it >= now }.take(NEXT_COUNT)
            lines += "sentido ${d.headsign}: " + when {
                next.isEmpty() -> "não tem mais hoje"
                next.size < NEXT_COUNT -> next.joinToString(" · ") { formatTime(it) } + " (o último)"
                else -> next.joinToString(" · ") { formatTime(it) }
            }
        }
        lines += ""
        lines += "Pela tabela de horários do Metrofor."
        return (lines + "" + ShareArrival.footer("estacao")).joinToString("\n")
    }

    /**
     * O aviso de operação especial: os dias, a manchete, o horário de cada linha
     * (da tabela da notícia), a bike do dia e a fonte.
     */
    fun operation(days: List<String>, op: SpecialOperation, bike: BikeSpecialDay?): String {
        val lines = mutableListOf(
            "⚠️ Metrofor, ${days.joinToString(", ") { dayLabel(it) }}: horário especial no metrô e no VLT",
            "",
            op.title,
        )
        if (op.hoursByLine.isNotEmpty()) {
            lines += ""
            op.hoursByLine.forEach { (line, h) -> lines += "$line: ${formatTime(h.first)} às ${formatTime(h.second)}" }
        }
        if (bike != null && bike.windows.isNotEmpty()) {
            val where = bike.lines.map { lower ->
                op.hoursByLine.firstOrNull { it.first.lowercase() == lower }?.first ?: titleCase(lower)
            }
            lines += "🚲 Bike: " + windowsLabel(bike.windows) +
                (if (where.isEmpty()) "" else " (${joinAnd(where)})")
        }
        lines += ""
        lines += "Fonte: Metrofor" + (op.published?.let { ", notícia de ${shortDate(it)}" } ?: "")
        return (lines + "" + ShareArrival.footer("aviso")).joinToString("\n")
    }

    /**
     * A regra de bike da linha, a semana inteira, e o que vale hoje ([todayStatus],
     * a mesma frase do aviso da tela). [rule]: a regra comum (não a de dia especial).
     */
    fun bike(
        routeName: String,
        rule: BikeRule,
        forbidden: String?,
        day: String,
        todayStatus: String,
        sourceTitle: String?,
    ): String {
        fun days(windows: List<BikeWindow>) = if (windows.isEmpty()) "não pode" else windowsLabel(windows)
        val lines = mutableListOf(
            "🚲 Bike no ${lineLabel(routeName)}:",
            "",
            "Segunda a sexta: ${days(rule.weekday)}",
            "Sábado: ${days(rule.saturday)}",
            "Domingo e feriado: confira o aviso do dia",
        )
        rule.perTrip?.let { lines += "Até $it bikes por viagem; se lotar, espera o próximo." }
        forbidden?.let { lines += it }
        lines += ""
        lines += "Hoje, ${dayLabel(day)}: ${todayStatus.replaceFirstChar { it.lowercase() }}."
            .replace("..", ".")
        lines += ""
        lines += "Fonte: " + (sourceTitle ?: "regulamento de bikes do Metrofor")
        return (lines + "" + ShareArrival.footer("bike")).joinToString("\n")
    }

    /** A legenda do cartão "Por que esse nome?": o texto, a fonte e a página da estação no site. */
    fun nameStory(stationName: String, story: NameStory): String {
        val name = name(stationName).replace(Regex("\\s*\\(.*\\)$"), "")
        return listOf(
            "🚉 Por que esse nome? Estação $name",
            "",
            story.text,
            "",
            "Fonte: ${story.source}",
            "Na página da estação: ${ShareArrival.SITE}estacoes/${story.slug}/",
            "",
            ShareArrival.footer("nome"),
        ).joinToString("\n")
    }

    /** "das 9h às 15h e a partir das 20h". */
    fun windowsLabel(windows: List<BikeWindow>): String = joinAnd(windows.map { w ->
        w.end?.let { "das ${hourLabel(w.start)} às ${hourLabel(it)}" } ?: "a partir das ${hourLabel(w.start)}"
    })

    /** "09:00" → "9h"; "16:30" → "16h30". */
    fun hourLabel(hhmm: String): String {
        val (h, m) = hhmm.split(":").map { it.trim().toInt() }
        return if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
    }

    /** "2026-11-23" → "segunda (23/11)". */
    fun dayLabel(day: String): String = runCatching {
        val zone = TimeZone.getTimeZone("America/Fortaleza")
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.parse(day)!!
        SimpleDateFormat("EEEE '('d/M')'", Locale("pt", "BR")).apply { timeZone = zone }.format(parsed)
            .replace("-feira", "")
    }.getOrDefault(day)

    /** "2026-11-17" → "17/11". */
    private fun shortDate(day: String): String = day.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}" else day }

    /** "metrô da Linha Sul", "VLT da Linha Nordeste", "VLT Aeroporto". */
    private fun lineLabel(routeName: String) =
        if (routeName.startsWith("Linha")) "${vehicleName(routeName)} da $routeName" else routeName

    private fun name(n: String) = StationNames.official(JourneyPlanner.plainName(n))

    private fun titleCase(s: String) = s.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private fun joinAnd(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " e " + items.last()
    }
}
