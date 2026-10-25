package io.github.giova.metroceara.data

import kotlin.math.ceil

/** Como a pessoa se desloca nas integrações. [key] é o nome em regras.json, "deslocamento". */
enum class Mobility(val key: String, val defaultFactor: Double) {
    WALKING("andando", 1.0),
    RELAXED("sem_pressa", 1.5),
    BIKE("bicicleta", 1.5),
    WHEELCHAIR("cadeira_de_rodas", 2.0),
}

/**
 * Quantos minutos a pessoa precisa para trocar de linha numa integração que ela
 * alcança às [arrival]: o tempo a pé da estação vezes o perfil (arredondado
 * para cima), mais o pico e o ajuste das Configurações.
 */
fun transferMinutes(baseMinutes: Int, arrival: Int, settings: AppSettings, rules: Regras = Regras.current): Int {
    val factor = rules.mobilityFactors[settings.mobility.key] ?: settings.mobility.defaultFactor
    return ceil(baseMinutes * factor - 1e-9).toInt() + rules.peakTransferExtra(arrival) + settings.transferExtraMinutes
}
