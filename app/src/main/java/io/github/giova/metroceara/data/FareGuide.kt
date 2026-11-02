package io.github.giova.metroceara.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * O guia da passagem (seção `guia_passagem` do regras.json): quem paga meia, quem
 * tem gratuidade, onde tirar o cartão e o que levar. Todo o texto vem do arquivo,
 * para corrigir sem versão nova quando o Metrofor mudar uma regra. Tudo vem das
 * páginas do Metrofor, e cada tema leva à sua página para a pessoa conferir.
 */
data class FareGuide(
    /** Frase do topo que diz de onde vêm as informações. */
    val source: String,
    /** Quando as páginas do Metrofor foram conferidas ("nov/2026"). */
    val checked: String,
    /** Página do Metrofor com as tarifas. */
    val link: String?,
    val topics: List<FareTopic>,
    /** Quadro para quem paga inteira (Cartão Múltiplo com nome). */
    val fullFare: FareCard?,
)

/** Um tema do guia (meia de estudante, gratuidade...), que abre numa tela própria. */
data class FareTopic(
    val title: String,
    val icon: String,
    /** Uma linha, no botão da tela Passagem. */
    val summary: String,
    /** Parágrafo no topo da tela do tema. */
    val intro: String?,
    /** Página do Metrofor sobre o tema. */
    val link: String?,
    val notices: List<DatedNotice>,
    val sections: List<FareSection>,
    /** Aviso em vermelho: o erro que faz a pessoa pagar inteira. */
    val alert: String?,
    val note: String?,
) {
    /** Os avisos com data que valem em [day] ("AAAA-MM-DD"), como o prazo da renovação. */
    fun noticesOn(day: String): List<String> = notices.filter { day >= it.from && day <= it.until }.map { it.text }
}

/** Uma parte da tela do tema: quadros (título e texto) e itens de lista. */
data class FareSection(val title: String, val boxes: List<Pair<String, String>>, val items: List<String>)

data class FareCard(val title: String, val text: String, val button: String?, val link: String?)

/** Aviso que só aparece entre [from] e [until] ("AAAA-MM-DD", inclusive). */
data class DatedNotice(val from: String, val until: String, val text: String)

internal fun parseFareGuide(o: JSONObject): FareGuide {
    fun link(o: JSONObject) = o.optString("link").takeIf { it.startsWith("https://") }
    fun text(o: JSONObject, key: String) = o.optString(key).trim().takeIf { it.isNotEmpty() }
    fun strings(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.getString(i) } }.orEmpty()
    fun objects(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.getJSONObject(i) } }.orEmpty()
    return FareGuide(
        source = o.getString("fonte"),
        checked = o.getString("conferido"),
        link = link(o),
        topics = objects(o.optJSONArray("temas")).map { t ->
            FareTopic(
                title = t.getString("titulo"),
                icon = t.optString("icone"),
                summary = t.getString("resumo"),
                intro = text(t, "texto"),
                link = link(t),
                notices = objects(t.optJSONArray("avisos")).map {
                    DatedNotice(it.getString("de"), it.getString("ate"), it.getString("texto"))
                },
                sections = objects(t.optJSONArray("secoes")).map { s ->
                    FareSection(
                        title = s.getString("titulo"),
                        boxes = objects(s.optJSONArray("quadros")).map { it.getString("titulo") to it.getString("texto") },
                        items = strings(s.optJSONArray("itens")),
                    )
                },
                alert = text(t, "alerta"),
                note = text(t, "nota"),
            )
        },
        fullFare = o.optJSONObject("inteira")?.let {
            FareCard(it.getString("titulo"), it.getString("texto"), text(it, "botao"), link(it))
        },
    )
}
