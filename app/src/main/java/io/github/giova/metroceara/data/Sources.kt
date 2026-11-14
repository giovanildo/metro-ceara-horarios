package io.github.giova.metroceara.data

import org.json.JSONObject

/**
 * De onde vem uma informação mostrada ao usuário e quando foi conferida (seção
 * `fontes` do regras.json). O app não é oficial: dizer a fonte e até quando ela
 * foi verificada é o que permite à pessoa confiar e conferir.
 */
data class InfoSource(
    val id: String,
    val title: String,
    val kind: Kind,
    val origin: String,
    /** Explicação mais longa, só na lista "De onde vem cada informação". */
    val detail: String?,
    val link: String?,
    /** Desde quando vale, quando isso ajuda a confiar ("preço sem reajuste desde mar/2019"). */
    val since: String?,
    /** Mês em que alguém olhou a fonte e a informação continuava valendo. */
    val checked: YearMonth?,
    /** De quanto em quanto tempo conferir de novo; null = não se aplica. */
    val reviewMonths: Int?,
    /** Acompanhado todo dia pelo app (as notícias de operação especial). */
    val daily: Boolean,
) {
    enum class Kind { METROFOR, THIRD_PARTY, APP }

    val nextCheck: YearMonth? get() = checked?.let { c -> reviewMonths?.let { c.plus(it) } }

    /** A próxima conferência já passou: a informação pode ter mudado. */
    fun overdue(today: YearMonth): Boolean = nextCheck?.let { today > it } ?: false

    /**
     * O link curto embaixo da informação: "Fonte", ou "Fonte · pode ter mudado"
     * quando a conferência atrasou (isso não pode ficar escondido); estimativa ou
     * observação do app leva o próprio nome ("Estimativa do app").
     */
    fun label(today: YearMonth): String = when {
        kind == Kind.APP -> origin
        overdue(today) -> "Fonte · pode ter mudado"
        else -> "Fonte"
    }

    /** O que a janela da fonte mostra, uma coisa por linha. */
    fun facts(today: YearMonth): List<String> = buildList {
        if (kind == Kind.APP) {
            add(origin + (checked?.let { " (${it.label()})" } ?: ""))
            return@buildList
        }
        add("Fonte: $origin")
        since?.let { add(it.replaceFirstChar(Char::uppercaseChar)) }
        if (daily) add("Acompanhado todo dia")
        checked?.let { add("Conferido em ${it.label()}") }
        nextCheck?.let {
            add(if (today > it) "Conferência atrasada desde ${it.label()}: pode ter mudado" else "Próxima conferência: ${it.label()}")
        }
    }

    /**
     * A linha completa, na lista "De onde vem cada informação":
     * "Fonte: site do Metrofor · preço sem reajuste desde mar/2019 · conferido em
     * nov/2026 · próxima conferência: mai/2027". Estimativa ou observação do app
     * não leva "Fonte:" nem próxima conferência, para não parecer dado oficial.
     */
    fun line(today: YearMonth): String = buildList {
        add(if (kind == Kind.APP) origin else "Fonte: $origin")
        since?.let { add(it) }
        when {
            daily -> add("acompanhado todo dia")
            kind == Kind.APP -> checked?.let { add(it.label()) }
            else -> {
                checked?.let { add("conferido em ${it.label()}") }
                nextCheck?.let {
                    add(if (today > it) "conferência atrasada desde ${it.label()}: pode ter mudado" else "próxima conferência: ${it.label()}")
                }
            }
        }
    }.joinToString(" · ")
}

/** Ano e mês, para as datas de conferência ("2026-11" → nov/2026). */
data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    fun plus(months: Int): YearMonth {
        val total = year * 12 + (month - 1) + months
        return YearMonth(total / 12, total % 12 + 1)
    }

    fun label(): String = "${MONTHS[month - 1]}/$year"

    override fun compareTo(other: YearMonth): Int = compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

        /** "2026-11" ou "2026-11-02" (o dia é ignorado); outro formato dá null. */
        fun parse(text: String?): YearMonth? {
            val parts = text?.trim()?.split("-") ?: return null
            if (parts.size < 2) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            return YearMonth(year, month)
        }
    }
}

internal fun parseSources(o: JSONObject): Map<String, InfoSource> =
    o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { id ->
        val s = o.getJSONObject(id)
        InfoSource(
            id = id,
            title = s.getString("titulo"),
            kind = when (s.optString("tipo")) {
                "app" -> InfoSource.Kind.APP
                "terceiros" -> InfoSource.Kind.THIRD_PARTY
                else -> InfoSource.Kind.METROFOR
            },
            origin = s.getString("fonte"),
            detail = s.optString("detalhe").trim().takeIf { it.isNotEmpty() },
            link = s.optString("link").takeIf { it.startsWith("https://") },
            since = s.optString("desde").trim().takeIf { it.isNotEmpty() },
            checked = YearMonth.parse(s.optString("conferido")),
            reviewMonths = s.optInt("revisar_em_meses").takeIf { it > 0 },
            daily = s.optBoolean("diario"),
        )
    }
