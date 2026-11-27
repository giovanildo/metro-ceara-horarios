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
import java.util.TreeMap

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
 * - `hpa`: pressão do barômetro, se o celular tiver;
 * - `rot`: giroscópio, média da rotação do celular em rad/s, e `rot_max` a maior
 *   (desde a 1.120). O metrô freando quase não gira o celular; a mão mexendo, sim:
 *   serve para separar "celular na mão" de "metrô andando" (análise de 26/11/2026).
 * - `mf`: magnetômetro, média da intensidade do campo em µT, `mf_sd` o quanto
 *   variou e `mf_rng` a maior menos a menor (desde 27/11/2026). O motor elétrico puxando
 *   ou freando mexe no campo dentro do vagão; parado, ele fica estável. A
 *   intensidade não muda quando o celular gira, então vale também com o celular na
 *   mão, quando o acelerômetro não serve (análise de 27/11/2026).
 * O evento não começa com "trip_", então não vai para o registro da viagem que a
 * pessoa pode mandar ([io.github.giova.metroceara.data.TripLog]).
 *
 * Com a tela apagada o Android junta as medições no próprio sensor e entrega de
 * tempos em tempos ([BATCH_MICROS]); como o resumo usa a hora de cada medição,
 * os segundos saem certos mesmo chegando atrasados. Cada sensor chega no seu
 * lote, então um segundo só é gravado quando todos os que estão chegando já passaram dele.
 */
class MotionRecorder(context: Context) : SensorEventListener {

    private val manager = context.getSystemService(SensorManager::class.java)
    private val accel: Sensor? = sensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro: Sensor? = sensor(Sensor.TYPE_GYROSCOPE)
    private val baro: Sensor? = sensor(Sensor.TYPE_PRESSURE)
    private val magnet: Sensor? = sensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val thread = HandlerThread("motion").apply { start() }
    private val handler = Handler(thread.looper)
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }

    /** Um segundo em montagem (chave: segundo do relógio do sensor, desde o boot). */
    private class Second {
        var n = 0
        var sx = 0.0
        var sy = 0.0
        var sz = 0.0
        var sMag = 0.0
        var sMag2 = 0.0
        var minMag = Double.MAX_VALUE
        var maxMag = 0.0
        var rotN = 0
        var rotSum = 0.0
        var rotMax = 0.0
        var mfN = 0
        var mfSum = 0.0
        var mfSum2 = 0.0
        var mfMin = Double.MAX_VALUE
        var mfMax = 0.0
    }

    private val pending = TreeMap<Long, Second>()
    private var lastAccel = -1L
    private var lastGyro = -1L
    private var lastMagnet = -1L
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
            "gyro" to gyro?.let(::describe),
            "baro" to baro?.let(::describe),
            "magnet" to magnet?.let(::describe),
        )
        if (accel != null) m.registerListener(this, accel, PERIOD_MICROS, BATCH_MICROS, handler)
        if (gyro != null) m.registerListener(this, gyro, PERIOD_MICROS, BATCH_MICROS, handler)
        if (baro != null) m.registerListener(this, baro, BARO_PERIOD_MICROS, BATCH_MICROS, handler)
        if (magnet != null) m.registerListener(this, magnet, PERIOD_MICROS, BATCH_MICROS, handler)
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
            flush(Long.MAX_VALUE)
            DevLog.log("motion_stop")
            thread.quitSafely()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        val s = event.timestamp / 1_000_000_000L
        when (event.sensor.type) {
            Sensor.TYPE_PRESSURE -> {
                pressureSum += event.values[0]
                pressureCount++
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val mag = sqrt(x * x + y * y + z * z)
                val b = pending.getOrPut(s) { Second() }
                b.n++
                b.sx += x
                b.sy += y
                b.sz += z
                b.sMag += mag
                b.sMag2 += mag * mag
                b.minMag = min(b.minMag, mag)
                b.maxMag = max(b.maxMag, mag)
                lastAccel = max(lastAccel, s)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val rot = sqrt(x * x + y * y + z * z)
                val b = pending.getOrPut(s) { Second() }
                b.rotN++
                b.rotSum += rot
                b.rotMax = max(b.rotMax, rot)
                lastGyro = max(lastGyro, s)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val field = sqrt(x * x + y * y + z * z)
                val b = pending.getOrPut(s) { Second() }
                b.mfN++
                b.mfSum += field
                b.mfSum2 += field * field
                b.mfMin = min(b.mfMin, field)
                b.mfMax = max(b.mfMax, field)
                lastMagnet = max(lastMagnet, s)
            }
        }
        // Grava os segundos que todos os sensores já passaram. Um sensor ausente ou parado
        // (mais de MAX_LAG_S atrás do acelerômetro) é ignorado, sem acumular segundos.
        var ready = lastAccel
        if (gyro != null && lastGyro >= lastAccel - MAX_LAG_S) ready = min(ready, lastGyro)
        if (magnet != null && lastMagnet >= lastAccel - MAX_LAG_S) ready = min(ready, lastMagnet)
        flush(ready)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Grava, em ordem, os segundos anteriores a [before]. */
    private fun flush(before: Long) {
        while (pending.isNotEmpty() && pending.firstKey() < before) {
            val (second, b) = pending.pollFirstEntry().let { it.key to it.value }
            emit(second, b)
        }
    }

    private fun emit(second: Long, b: Second) {
        if (b.n == 0) return
        seconds++
        val mean = b.sMag / b.n
        val sd = sqrt(max(0.0, b.sMag2 / b.n - mean * mean))
        val mfMean = if (b.mfN > 0) b.mfSum / b.mfN else null
        val mfSd = mfMean?.let { sqrt(max(0.0, b.mfSum2 / b.mfN - it * it)) }
        // Hora de parede do segundo: o relógio do sensor conta desde o boot.
        val wall = System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - second * 1_000_000_000L) / 1_000_000L
        DevLog.log(
            "motion",
            "at" to clock.format(Date(wall)),
            "n" to b.n,
            "x" to round(b.sx / b.n, 100.0),
            "y" to round(b.sy / b.n, 100.0),
            "z" to round(b.sz / b.n, 100.0),
            "mag" to round(mean, 100.0),
            "sd" to round(sd, 1000.0),
            "rng" to round(b.maxMag - b.minMag, 100.0),
            "rot" to b.rotSum.takeIf { b.rotN > 0 }?.let { round(it / b.rotN, 1000.0) },
            "rot_max" to b.rotMax.takeIf { b.rotN > 0 }?.let { round(it, 1000.0) },
            "mf" to mfMean?.let { round(it, 100.0) },
            "mf_sd" to mfSd?.let { round(it, 1000.0) },
            "mf_rng" to mfMean?.let { round(b.mfMax - b.mfMin, 100.0) },
            "hpa" to pressureSum.takeIf { pressureCount > 0 }?.let { round(it / pressureCount, 100.0) },
        )
        pressureSum = 0.0
        pressureCount = 0
    }

    private fun sensor(type: Int): Sensor? = manager?.let { it.getDefaultSensor(type, true) ?: it.getDefaultSensor(type) }

    private fun describe(sensor: Sensor) =
        "${sensor.vendor} ${sensor.name}; wakeup=${sensor.isWakeUpSensor}; fifo=${sensor.fifoMaxEventCount}; " +
            "res=${sensor.resolution}; min_us=${sensor.minDelay}"

    private fun round(value: Double, scale: Double) = (value * scale).roundToLong() / scale

    companion object {
        /** ~50 medições por segundo (acelerômetro, giroscópio e magnetômetro); abaixo de 200/s não pede permissão. */
        private const val PERIOD_MICROS = 20_000
        private const val BARO_PERIOD_MICROS = 1_000_000
        /** Com a tela apagada, o sensor junta até isto antes de entregar. */
        private const val BATCH_MICROS = 5_000_000
        /** Quanto o giroscópio pode ficar atrás do acelerômetro (lotes de 5 s) antes de ser ignorado. */
        private const val MAX_LAG_S = 15L
    }
}
