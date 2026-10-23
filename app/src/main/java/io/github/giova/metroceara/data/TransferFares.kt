package io.github.giova.metroceara.data

/** Se a integração exige pagar outra passagem. */
enum class TransferFare { FREE, PAID, UNKNOWN }

/** A regra de uma integração, num sentido, com um motivo opcional para mostrar. */
data class TransferRule(val fare: TransferFare, val reason: String? = null)

/**
 * Regra de passagem de uma integração, **por sentido** (de → para): em
 * Parangaba, ir da Sul para a Nordeste é grátis, mas voltar é pago — entrar
 * na estação da Linha Sul cobra a passagem. Não vem do GTFS (que só tem
 * horários): fica em `assets/regras.json`, e quem confirma é quem anda de
 * metrô. Sem regra confirmada fica [TransferFare.UNKNOWN] — e o app não fala
 * de passagem.
 */
fun transferRule(fromStopName: String, toStopName: String): TransferRule {
    val from = fromStopName.trim().lowercase()
    val to = toStopName.trim().lowercase()
    Regras.current.integracoes.forEach {
        val a = it.a.trim().lowercase()
        val b = it.b.trim().lowercase()
        if (a == from && b == to) return it.aToB
        if (b == from && a == to) return it.bToA
    }
    return TransferRule(TransferFare.UNKNOWN)
}
