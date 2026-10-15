package io.github.giova.metrofortaleza.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
 * As regras do Metrofor que não vêm no GTFS (bike no metrô, integrações e
 * passagem), lidas de `assets/regras.json` — o mesmo arquivo que o
 * `tools/build_db.py` e a versão web usam. Carregadas uma vez, ao abrir o app.
 */
class Regras(
    val bikeByLine: Map<String, BikeRule>,
    val bikeByStation: Map<String, BikeRule>,
    val integracoes: List<IntegracaoRegra>,
) {
    companion object {
        /** Sem arquivo (ou com erro nele), o app segue sem avisos de bike e de passagem. */
        @Volatile var current = Regras(emptyMap(), emptyMap(), emptyList())
            private set

        fun init(context: Context) {
            runCatching {
                context.assets.open("regras.json").bufferedReader().use { parse(it.readText()) }
            }.onSuccess { current = it }
                .onFailure { DevLog.log("regras_erro", "erro" to it.toString()) }
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
            return Regras(byName(bike.getJSONObject("linhas")), byName(bike.getJSONObject("estacoes")), integracoes)
        }

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
