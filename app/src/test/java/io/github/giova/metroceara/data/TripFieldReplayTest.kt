package io.github.giova.metroceara.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Viagens gravadas em campo, repassadas ao estimador posição por posição (com o
 * relógio de 15 s do serviço entre elas). Arquivos em src/test/resources/campo:
 * linhas "stop" (nome, lat, lon, horário em minutos) e "fix" (minuto do dia,
 * provedor, lat, lon, precisão).
 */
class TripFieldReplayTest {

    private class Replay(val plan: TripPlan, val reached: Map<Int, Double>)

    private fun replay(file: String): Replay {
        val lines = javaClass.classLoader!!.getResourceAsStream("campo/$file")!!
            .bufferedReader(Charsets.UTF_8).readLines().filter { !it.startsWith("#") }.map { it.split('\t') }
        val stops = lines.filter { it[0] == "stop" }
            .map { TripStop(it[1], it[1], it[2].toDouble(), it[3].toDouble(), it[4].toInt(), underground = it[1] in UNDERGROUND) }
        val fixes = lines.filter { it[0] == "fix" }
        val plan = TripPlan("1", "Linha Sul", stops.last().name, stops)
        val estimator = TripEstimator(plan)
        val reached = mutableMapOf<Int, Double>()
        fun tick(now: Double) {
            val index = estimator.progress(now).index
            for (i in 0..index) reached.putIfAbsent(i, now)
        }
        var clock = fixes.first()[1].toDouble()
        tick(clock)
        for (fix in fixes) {
            val t = fix[1].toDouble()
            while (clock + TICK < t) { clock += TICK; tick(clock) }
            estimator.onLocation(fix[3].toDouble(), fix[4].toDouble(), fix[5].toFloat(), t, fromNetwork = fix[2] == "network")
            tick(t)
        }
        return Replay(plan, reached)
    }

    private fun Replay.at(name: String) = reached[plan.stops.indexOfFirst { it.name == name }]
    private fun hm(m: Double?) = m?.let { "%02d:%02d:%02d".format((it / 60).toInt(), (it % 60).toInt(), ((it * 60) % 60).toInt()) }
    private fun Replay.dump() = plan.stops.joinToString("\n") { "${it.name}: ${hm(at(it.name))}" }.also { println(it) }

    /**
     * Ida de 04/11: embarcou em Maracanaú rumo a Carlito, o metrô passou por
     * Jereissati 15:09, ficou parado em Carlito até 15:18 e voltou. No túnel,
     * pelos prints: São Benedito 15:51, José de Alencar 15:52, Chico 15:55.
     */
    @Test
    fun `ida Jereissati - Chico da Silva`() {
        val r = replay("2026-11-04-jereissati-chico.tsv")
        val log = r.dump()
        // Parado em Carlito, a grade não pode pô-lo em Maracanaú (era 15:17).
        assertTrue(log, r.at("Maracanaú")!! >= min(15, 20.0))
        // Aviso de descida (São Benedito) até quando ele estava lá.
        assertTrue(log, r.at("São Benedito")!! <= min(15, 51.5))
        // Quem desce em José de Alencar tem de saber antes de passar dela.
        assertTrue(log, r.at("José de Alencar")!! <= min(15, 52.5))
        // E não chega antes da hora: Chico só depois de José de Alencar real.
        assertTrue(log, r.at("Chico da Silva")!! >= min(15, 53.0))
    }

    /** Volta de 04/11 (Benfica → Carlito), que já tinha dado certo: continua igual. */
    @Test
    fun `volta Benfica - Carlito Benevides`() {
        val r = replay("2026-11-04-benfica-carlito.tsv")
        val log = r.dump()
        // Na superfície, nada muito antes do GPS (Esperança 18:41:03 — pela grade
        // 18:40:14, como antes —, Aracapé 18:42:30).
        assertTrue(log, r.at("Conjunto Esperança")!! >= min(18, 40.0))
        assertTrue(log, r.at("Aracapé")!! >= min(18, 42.0))
        // GPS em Maracanaú às 18:51:55 e em Carlito às 18:55:59.
        assertTrue(log, r.at("Maracanaú")!! in min(18, 51.0)..min(18, 52.5))
        assertTrue(log, r.at("Carlito Benevides")!! in min(18, 55.0)..min(18, 56.5))
    }

    private fun min(h: Int, m: Double) = h * 60 + m

    private companion object {
        const val TICK = 0.25
        /** As marcadas "subterranea" no regras.json. */
        val UNDERGROUND = setOf("Benfica", "São Benedito", "José de Alencar", "Chico da Silva")
    }
}
