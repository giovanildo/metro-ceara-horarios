package io.github.giova.metroceara.data

/** Abaixo disso, a diferença de precisão entre duas posições é só ruído. */
private const val SAME_ACCURACY_METERS = 30f

/** A precisão do Android cobre 68% dos casos; o dobro dela cobre quase todos. */
private const val ACCURACY_RADII = 2f

/** A posição que a tela inicial está mostrando e quando ela foi aceita. */
data class ShownFix(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val atMillis: Long,
)

/**
 * Se a posição atual deve continuar na tela no lugar da nova.
 *
 * Nos registros de campo, a rede mandava posições com 600 m ou mais de incerteza
 * logo depois de uma de 5 m, e a distância pulava de 1,5 km para 765 m. Uma posição
 * menos precisa fica de fora enquanto a atual for recente ([holdMillis]). Depois
 * disso, só entra se contradisser a atual: parado, o GPS não manda nada novo, e sem
 * esse teste a posição ruim voltaria assim que o tempo acabasse.
 */
fun ShownFix?.keepsOver(
    lat: Double,
    lon: Double,
    accuracyMeters: Float,
    nowMillis: Long,
    holdMillis: Long,
): Boolean {
    if (this == null || accuracyMeters <= maxOf(this.accuracyMeters, SAME_ACCURACY_METERS)) return false
    if (nowMillis - atMillis <= holdMillis) return true
    return distanceMeters(this.lat, this.lon, lat, lon) <= accuracyMeters * ACCURACY_RADII
}
