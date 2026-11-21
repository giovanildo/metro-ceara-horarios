package io.github.giova.metroceara.trip

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import io.github.giova.metroceara.data.DevLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Grava o movimento do celular durante o modo viagem, só no registro de testes e
 * só com a opção de desenvolvedor ligada ([DevLog.motionEnabled]). Não muda nada
 * na viagem: é para estudar se as paradas do metrô no túnel (sem GPS) aparecem
 * no acelerômetro (análise de 19/11/2026).
 *
 * O acelerômetro mede ~50 vezes por segundo; o registro leva um resumo por
 * segundo (evento `motion`), pela hora da medição (`at`), não da gravação:
 * - `n`: quantas medições naquele segundo;
 * - `x`, `y`, `z`: média de cada eixo, em m/s² (com a gravidade; a inclinação
 *   do vetor mostra freada e arrancada, a força contínua para frente ou trás);
 * - `mag`: média da intensidade; `sd`: o quanto ela variou (a vibração do motor
 *   andando, quase zero parado); `rng`: a maior menos a menor;
 * - `hpa`: pressão do barômetro, se o celular tiver.
 * O evento não começa com "trip_", então não vai para o registro da viagem que a
 * pessoa pode mandar ([io.github.giova.metroceara.data.TripLog]).
 *
 * Com a tela apagada o Android junta as medições no próprio sensor e entrega de
 * tempos em tempos ([BATCH_MICROS]); como o resumo usa a hora de cada medição,
 * os segundos saem certos mesmo chegando atrasados.
 */
class MotionRecorder(context: Context) : SensorEventListener {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val accel: Sensor? = manager?.let { it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true) ?: it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private val baro: Sensor? = manager?.let { it.getDefaultSensor(Sensor.TYPE_PRESSURE, true) ?: it.getDefaultSensor(Sensor.TYPE_PRESSURE) }
    private val thread = HandlerThread("motion").apply { start() }
    private val handler = Handler(thread.looper)
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }

    // O segundo em montagem (pelo relógio do sensor, em ns desde o boot).
    private var second = -1L
    private var n = 0
    private var sx = 0.0
    private var sy = 0.0
    private var sz = 0.0
    private var sMag = 0.0
    private var sMag2 = 0.0
    private var minMag = Double.MAX_VALUE
    private var maxMag = 0.0
    private var pressureSum = 0.0
    private var pressureCount = 0
    private var running = false

    /** Quantos segundos de movimento já foram gravados nesta viagem. */
    @Volatile var seconds = 0
        private set

    fun start() {
        val m = manager ?: return
        DevLog.log(
            "motion_start",
            "accel" to accel?.let(::describe),
            "baro" to baro?.let(::describe),
        )
        if (accel != null) m.registerListener(this, accel, ACCEL_PERIOD_MICROS, BATCH_MICROS, handler)
        if (baro != null) m.registerListener(this, baro, BARO_PERIOD_MICROS, BATCH_MICROS, handler)
        running = accel != null || baro != null
    }

    fun stop() {
        if (!running) {
            thread.quitSafely()
            return
        }
        running = false
        manager?.unregisterListener(this)
        handler.post {
            emit()
            DevLog.log("motion_stop")
            thread.quitSafely()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        when (event.sensor.type) {
            Sensor.TYPE_PRESSURE -> {
                pressureSum += event.values[0]
                pressureCount++
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val s = event.timestamp / 1_000_000_000L
                if (s != second) {
                    emit()
                    second = s
                }
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val mag = sqrt(x * x + y * y + z * z)
                n++
                sx += x
                sy += y
                sz += z
                sMag += mag
                sMag2 += mag * mag
                minMag = min(minMag, mag)
                maxMag = max(maxMag, mag)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Grava o segundo montado e zera. */
    private fun emit() {
        if (n == 0) return
        seconds++
        val mean = sMag / n
        val sd = sqrt(max(0.0, sMag2 / n - mean * mean))
        // Hora de parede do segundo: o relógio do sensor conta desde o boot.
        val wall = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - second * 1_000_000_000L) / 1_000_000L
        DevLog.log(
            "motion",
            "at" to clock.format(Date(wall)),
            "n" to n,
            "x" to round(sx / n, 100.0),
            "y" to round(sy / n, 100.0),
            "z" to round(sz / n, 100.0),
            "mag" to round(mean, 100.0),
            "sd" to round(sd, 1000.0),
            "rng" to round(maxMag - minMag, 100.0),
            "hpa" to pressureSum.takeIf { pressureCount > 0 }?.let { round(it / pressureCount, 100.0) },
        )
        n = 0
        sx = 0.0; sy = 0.0; sz = 0.0
        sMag = 0.0; sMag2 = 0.0
        minMag = Double.MAX_VALUE; maxMag = 0.0
        pressureSum = 0.0; pressureCount = 0
    }

    private fun describe(sensor: Sensor) =
        "${sensor.vendor} ${sensor.name}; wakeup=${sensor.isWakeUpSensor}; fifo=${sensor.fifoMaxEventCount}; " +
            "res=${sensor.resolution}; min_us=${sensor.minDelay}"

    private fun round(value: Double, scale: Double) = (value * scale).roundToLong() / scale

    companion object {
        /** ~50 medições por segundo: dá para ver a vibração; abaixo de 200/s não pede permissão. */
        private const val ACCEL_PERIOD_MICROS = 20_000
        private const val BARO_PERIOD_MICROS = 1_000_000
        /** Com a tela apagada, o sensor junta até isto antes de entregar. */
        private const val BATCH_MICROS = 5_000_000
    }
}
