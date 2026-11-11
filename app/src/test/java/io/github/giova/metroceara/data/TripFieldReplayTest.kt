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

    private class Replay(
        val plan: TripPlan,
        val reached: Map<Int, Double>,
        /** Quando a tela passou a dizer "posição estimada" (GPS perdido), por estação. */
        val lostAt: Map<Int, Double>,
        val departure: Double?,
    )

    /** [until]: segue o relógio depois da última posição (o túnel não manda nenhuma). */
    private fun replay(file: String, until: Double? = null): Replay {
        val lines = javaClass.classLoader!!.getResourceAsStream("campo/$file")!!
            .bufferedReader(Charsets.UTF_8).readLines().filter { !it.startsWith("#") }.map { it.split('\t') }
        val stops = lines.filter { it[0] == "stop" }
            .map { TripStop(it[1], it[1], it[2].toDouble(), it[3].toDouble(), it[4].toInt(), underground = it[1] in UNDERGROUND) }
        val fixes = lines.filter { it[0] == "fix" }
        val plan = TripPlan("1", "Linha Sul", stops.last().name, stops)
        val estimator = TripEstimator(plan)
        val reached = mutableMapOf<Int, Double>()
        val lostAt = mutableMapOf<Int, Double>()
        var departure: Double? = null
        fun tick(now: Double) {
            val progress = estimator.progress(now)
            for (i in 0..progress.index) reached.putIfAbsent(i, now)
            if (progress.gpsSilentMinutes != null) lostAt.putIfAbsent(progress.index, now)
            departure = progress.departureMinutes
        }
        var clock = fixes.first()[1].toDouble()
        tick(clock)
        for (fix in fixes) {
            val t = fix[1].toDouble()
            while (clock + TICK < t) { clock += TICK; tick(clock) }
            estimator.onLocation(fix[3].toDouble(), fix[4].toDouble(), fix[5].toFloat(), t, fromNetwork = fix[2] == "network")
            tick(t)
        }
        if (until != null) while (clock < until) { clock += TICK; tick(clock) }
        return Replay(plan, reached, lostAt, departure)
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
        // O metrô chegou a Chico 15:55; sem o adiantamento no destino, a chegada
        // não pode ficar muito depois disso.
        assertTrue(log, r.at("Chico da Silva")!! <= min(15, 57.0))
    }

    /**
     * 10/11: plano no metrô das 13:24, ele pegou o anterior (~13:04). Superfície
     * toda pelo GPS; o satélite sumiu às 13:35:34, saindo de Padre Cícero. Pelos
     * prints: já em Benfica quando a tela ainda dizia Padre Cícero "pelo GPS";
     * chegada real a José de Alencar ~13:40 (o app declarou 13:38:52).
     */
    @Test
    fun `ida Carlito - José de Alencar, sem ganhar tempo no túnel`() {
        val r = replay("2026-11-10-carlito-jose-de-alencar.tsv", until = min(13, 45.0))
        val log = r.dump() + "\nperdido em: " + r.lostAt.mapKeys { r.plan.stops[it.key].name }.mapValues { hm(it.value) } +
            "\nsaída: " + hm(r.departure)
        // A saída é a do metrô que ele pegou, não a das 13:24.
        assertTrue(log, r.departure!! in min(13, 3.0)..min(13, 6.0))
        // Padre Cícero pelo GPS (13:34:43).
        assertTrue(log, r.at("Padre Cícero")!! in min(13, 34.0)..min(13, 35.0))
        // Saindo para o túnel, "estimada" logo (antes: 13:36:52, com ele já em Benfica).
        val lost = r.lostAt.filterKeys { it >= r.plan.stops.indexOfFirst { s -> s.name == "Padre Cícero" } }.values.minOrNull()
        assertTrue(log, lost != null && lost <= min(13, 36.5))
        // Aviso de descida (Benfica) até quando ele estava lá.
        assertTrue(log, r.at("Benfica")!! <= min(13, 36.75))
        // A chegada não vem antes de o metrô chegar, nem muito depois.
        assertTrue(log, r.at("José de Alencar")!! in min(13, 39.5)..min(13, 41.5))
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
