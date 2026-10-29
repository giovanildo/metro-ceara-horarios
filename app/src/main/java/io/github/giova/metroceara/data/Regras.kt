package io.github.giova.metroceara.data

import android.content.Context
import java.io.File
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

/** A regra de bike de um dia especial: as linhas (vazio = todas com bike) e as janelas (vazio = sem bike). */
data class BikeSpecialDay(val lines: Set<String>, val windows: List<BikeWindow>)

/** Endereço de uma estação e, se o GTFS a põe no lugar errado, a posição certa. */
data class StationInfo(
    val name: String,
    val address: String,
    val lat: Double?,
    val lon: Double?,
    /** Debaixo da terra: o sinal do celular (e o GPS) costuma falhar. */
    val underground: Boolean = false,
    /** Aviso livre do regras.json, mostrado na tela da estação abaixo da passagem. */
    val notice: String? = null,
)

/**
 * Um dia de operação especial anunciado numa notícia do Metrofor (feriado, ENEM,
 * eleição): a manchete, o link, se a notícia diz que o horário não muda, e as
 * linhas que não circulam (nomes em minúsculas).
 */
data class SpecialOperation(val title: String, val link: String?, val normal: Boolean, val stopped: Set<String>)

/** Uma integração entre duas estações, com a passagem em cada sentido. */
data class IntegracaoRegra(
    val a: String,
    val b: String,
    val walkMinutes: Int,
    val note: String,
    val aToB: TransferRule,
    val bToA: TransferRule,
)

/**
 * As regras do Metrofor que não vêm no GTFS (bike no metrô, integrações,
 * passagem e horários de pico), de `assets/regras.json` — o mesmo arquivo que o
 * `tools/build_db.py` e a versão web usam. O espelho publica uma cópia dele, que
 * o app baixa a cada abertura ([update]): mudar uma regra no GitHub chega aos
 * celulares sem versão nova. Sem cópia baixada (ou com erro nela), vale a do APK.
 */
class Regras(
    val bikeByLine: Map<String, BikeRule>,
    val bikeByStation: Map<String, BikeRule>,
    val integracoes: List<IntegracaoRegra>,
    /** Faixas de pico, em minutos desde a meia-noite (início incluído, fim não). */
    val peak: List<IntRange> = emptyList(),
    /** Regra de bike de dias especiais ("AAAA-MM-DD"), no lugar da do dia da semana. */
    val bikeSpecialDays: Map<String, BikeSpecialDay> = emptyMap(),
    /** Por stop_id do GTFS. */
    val stations: Map<String, StationInfo> = emptyMap(),
    /** Passagem por linha (nome em minúsculas), já no texto mostrado. */
    val fares: Map<String, String> = emptyMap(),
    /** Dias de operação especial ("AAAA-MM-DD"), das notícias do Metrofor. */
    val operations: Map<String, SpecialOperation> = emptyMap(),
    /** Pico máximo (estação mais cheia), em minutos desde a meia-noite. */
    val peakMax: List<IntRange> = emptyList(),
    /** Minutos a mais numa integração no pico e no pico máximo. */
    val peakTransferMinutes: Int = 0,
    val peakMaxTransferMinutes: Int = 0,
    /** Quantas vezes o tempo a pé, por jeito de se deslocar ([Mobility.key]). */
    val mobilityFactors: Map<String, Double> = emptyMap(),
) {
    /** A partida cai num horário de pico (metrô costuma ir cheio)? */
    fun isPeak(minutes: Int): Boolean = peak.any { minutes in it }

    /**
     * Endereço e posição da estação [stopId]. O nome tem de bater: se o Metrofor
     * renumerar as estações, a informação fica de fora em vez de ir para a estação errada.
     */
    fun station(stopId: String, name: String): StationInfo? =
        stations[stopId]?.takeIf { plain(it.name) == plain(name) }

    fun fare(routeName: String): String? = fares[routeName.trim().lowercase()]

    /** Minutos a mais numa integração alcançada às [minutes]: estação cheia demora mais. */
    fun peakTransferExtra(minutes: Int): Int = when {
        peakMax.any { minutes in it } -> peakMaxTransferMinutes
        isPeak(minutes) -> peakTransferMinutes
        else -> 0
    }

    /** O Metrofor avisou que [routeName] não circula em [day]? */
    fun stopped(day: String, routeName: String): Boolean =
        operations[day]?.stopped?.contains(routeName.trim().lowercase()) == true

    companion object {
        /** Sem arquivo (ou com erro nele), o app segue sem avisos de bike e de passagem. */
        @Volatile var current = Regras(emptyMap(), emptyMap(), emptyList())
            private set

        private const val FILE = "regras.json"

        fun init(context: Context) {
            val downloaded = File(context.filesDir, FILE)
            runCatching { parse(downloaded.readText()) }
                .recoverCatching { context.assets.open(FILE).bufferedReader().use { parse(it.readText()) } }
                .onSuccess { current = it }
                .onFailure { DevLog.log("regras_erro", "erro" to it.toString()) }
        }

        /**
         * Troca as regras pelas [json] baixadas do espelho e guarda a cópia.
         * Arquivo com erro lança exceção e não muda nada.
         */
        fun update(context: Context, json: String) {
            val rules = parse(json)
            val target = File(context.filesDir, FILE)
            val temp = File(context.filesDir, "$FILE.tmp")
            temp.writeText(json)
            check(temp.renameTo(target)) { "não foi possível guardar $FILE" }
            current = rules
        }

        fun parse(json: String): Regras {
            val root = JSONObject(json)
            val bike = root.getJSONObject("bike")
            val regulamentos = bike.getJSONObject("regulamentos").let { r ->
                r.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { name ->
                    val o = r.getJSONObject(name)
                    BikeRule(
                        weekday = windows(o.getJSONArray("dias_uteis")),
                        saturday = windows(o.getJSONArray("sabado")),
                        sunday = windows(o.optJSONArray("domingo") ?: JSONArray()),
                    )
                }
            }
            fun byName(o: JSONObject): Map<String, BikeRule> =
                o.keys().asSequence().filterNot { it.startsWith("_") }
                    .associate { key -> key.trim().lowercase() to regulamentos.getValue(o.getString(key)) }
            val integracoes = root.getJSONArray("integracoes").let { list ->
                (0 until list.length()).map { i ->
                    val o = list.getJSONObject(i)
                    IntegracaoRegra(
                        a = o.getString("a"),
                        b = o.getString("b"),
                        walkMinutes = o.getInt("caminhada_min"),
                        note = o.optString("obs"),
                        aToB = fare(o, "a_para_b"),
                        bToA = fare(o, "b_para_a"),
                    )
                }
            }
            fun ranges(list: JSONArray?) = list?.let {
                (0 until it.length()).map { i ->
                    val w = it.getJSONArray(i)
                    clock(w.getString(0)) until clock(w.getString(1))
                }
            }.orEmpty()
            val pico = root.optJSONObject("pico")
            val peak = ranges(pico?.optJSONArray("faixas"))
            val mobility = root.optJSONObject("deslocamento")?.let { o ->
                o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { o.getDouble(it) }
            }.orEmpty()
            val specialDays = root.optJSONObject("datas_especiais")?.let { o ->
                o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { day ->
                    val d = o.getJSONObject(day)
                    val lines = d.optJSONArray("linhas") ?: JSONArray()
                    BikeSpecialDay(
                        lines = (0 until lines.length()).map { lines.getString(it).trim().lowercase() }.toSet(),
                        windows = windows(d.getJSONArray("bike")),
                    )
                }
            }.orEmpty()
            val stations = root.optJSONObject("estacoes")?.let { o ->
                o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { id ->
                    val s = o.getJSONObject(id)
                    val position = s.optJSONArray("posicao")
                    StationInfo(
                        name = s.getString("nome"),
                        address = s.getString("endereco"),
                        lat = position?.getDouble(0),
                        lon = position?.getDouble(1),
                        underground = s.optBoolean("subterranea"),
                        notice = s.optString("aviso").trim().takeIf { it.isNotEmpty() },
                    )
                }
            }.orEmpty()
            val fares = root.optJSONObject("passagem")?.let { o ->
                o.keys().asSequence().filterNot { it.startsWith("_") }.associate { it.trim().lowercase() to o.getString(it) }
            }.orEmpty()
            val operations = root.optJSONObject("operacoes")?.let { o ->
                o.keys().asSequence().filterNot { it.startsWith("_") }.associateWith { day ->
                    val op = o.getJSONObject(day)
                    val stopped = op.optJSONArray("paradas") ?: JSONArray()
                    SpecialOperation(
                        title = op.getString("titulo"),
                        link = op.optString("link").takeIf { it.startsWith("https://") },
                        normal = op.optBoolean("normal"),
                        stopped = (0 until stopped.length()).map { stopped.getString(it).trim().lowercase() }.toSet(),
                    )
                }
            }.orEmpty()
            return Regras(
                byName(bike.getJSONObject("linhas")), byName(bike.getJSONObject("estacoes")), integracoes, peak, specialDays,
                stations, fares, operations,
                peakMax = ranges(pico?.optJSONArray("maximo")),
                peakTransferMinutes = pico?.optInt("integracao_min") ?: 0,
                peakMaxTransferMinutes = pico?.optInt("integracao_maximo_min") ?: 0,
                mobilityFactors = mobility,
            )
        }

        /** "Alto da Brasília" e "Alto Da Brasilia" viram o mesmo texto. */
        private fun plain(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("[^A-Za-z0-9]"), "").lowercase()

        /** "HH:MM" em minutos desde a meia-noite. */
        private fun clock(text: String): Int = text.split(":").let { (h, m) -> h.toInt() * 60 + m.toInt() }

        private fun windows(array: JSONArray): List<BikeWindow> = (0 until array.length()).map { i ->
            val w = array.getJSONArray(i)
            BikeWindow(w.getString(0), if (w.isNull(1)) null else w.getString(1))
        }

        private fun fare(o: JSONObject, way: String): TransferRule {
            val fare = when (o.optString("passagem_$way")) {
                "gratis" -> TransferFare.FREE
                "paga" -> TransferFare.PAID
                else -> TransferFare.UNKNOWN
            }
            val reason = o.optString("motivo_$way").takeIf { it.isNotBlank() }
            return TransferRule(fare, reason)
        }
    }
}
