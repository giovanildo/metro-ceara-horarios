package io.github.giova.metroceara.trip

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellIdentityNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.data.DevLog
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Grava as torres de celular e as redes Wi-Fi durante o modo viagem, só no registro
 * de testes e só com a opção de desenvolvedor ligada ([DevLog.radioEnabled]). Não muda
 * nada na viagem: é para ver se cada estação subterrânea tem uma "assinatura" de rádio
 * (a torre muda ou o sinal sobe ao chegar à plataforma; redes Wi-Fi fixas da estação),
 * para saber em que estação o metrô está quando o GPS some (análise de 27/11/2026).
 *
 * Eventos (não começam com "trip_", então ficam fora do registro da viagem que a pessoa
 * pode mandar):
 * - `radio_start`: permissões, Wi-Fi ligado, operadora;
 * - `cell` a cada [CELL_EVERY_MS]: cada torre que o celular vê (`c`), com o tipo, se é a
 *   que está em uso (`reg`), a identificação (`ci`, `pci`, `tac`/`lac`) e o sinal (`dbm`);
 * - `wifi` quando chega uma busca nova: as redes (`w`) com o endereço resumido em 8
 *   letras (`h`, um hash: igual em toda viagem, sem guardar o endereço), o sinal (`l`) e a
 *   frequência (`f`). O nome da rede só vai (`s`) se parecer da estação ou pública
 *   ([PUBLIC_SSID]): redes de casas e lojas ficam só no hash.
 * - `wifi_scan` quando o Android recusa a busca (ele limita a 4 a cada 2 minutos).
 * O Wi-Fi só existe na versão de teste (build "campo"); no app da Play ficam só as torres.
 */
class RadioRecorder(private val context: Context) {

    private val telephony = context.getSystemService(TelephonyManager::class.java)
    /** Wi-Fi só na versão de teste: o app da Play não tem a permissão (src/campo/AndroidManifest.xml). */
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        ?.takeIf { granted(Manifest.permission.ACCESS_WIFI_STATE) && granted(Manifest.permission.CHANGE_WIFI_STATE) }
    private val thread = HandlerThread("radio").apply { start() }
    private val handler = Handler(thread.looper)
    private var running = false
    /** A busca de Wi-Fi mais nova já gravada (relógio desde o boot, µs). */
    private var lastWifiMicros = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            handler.post { logWifi() }
        }
    }

    private val cellTick = object : Runnable {
        override fun run() {
            if (!running) return
            pollCells()
            handler.postDelayed(this, CELL_EVERY_MS)
        }
    }

    private val wifiTick = object : Runnable {
        override fun run() {
            if (!running) return
            scanWifi()
            handler.postDelayed(this, WIFI_EVERY_MS)
        }
    }

    fun start() {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        DevLog.log(
            "radio_start",
            "fine" to fine,
            "wifi" to (wifi != null),
            "wifi_on" to runCatching { wifi?.isWifiEnabled }.getOrNull(),
            "scan_always" to runCatching { wifi?.isScanAlwaysAvailable }.getOrNull(),
            "operator" to runCatching { telephony?.networkOperatorName }.getOrNull(),
        )
        running = true
        if (wifi != null) {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                null, handler, ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            handler.post(wifiTick)
        }
        if (telephony != null) handler.post(cellTick)
    }

    fun stop() {
        if (!running) {
            thread.quitSafely()
            return
        }
        running = false
        runCatching { context.unregisterReceiver(receiver) }
        handler.post {
            handler.removeCallbacksAndMessages(null)
            DevLog.log("radio_stop")
            thread.quitSafely()
        }
    }

    private fun pollCells() {
        val tm = telephony ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tm.requestCellInfoUpdate(
                    { handler.post(it) },
                    object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cells: MutableList<CellInfo>) = logCells(cells)
                        override fun onError(errorCode: Int, detail: Throwable?) {
                            // Sem resposta nova: grava a última que o Android tem.
                            logCells(runCatching { tm.allCellInfo }.getOrNull().orEmpty(), errorCode)
                        }
                    },
                )
            } else {
                logCells(tm.allCellInfo.orEmpty())
            }
        } catch (e: SecurityException) {
            DevLog.log("cell", "error" to e.message)
        }
    }

    private fun logCells(cells: List<CellInfo>, error: Int? = null) {
        if (!running) return
        val list = JSONArray()
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        for (cell in cells.sortedByDescending { it.isRegistered }.take(MAX_CELLS)) {
            val o = JSONObject()
            o.put("reg", cell.isRegistered)
            when (cell) {
                is CellInfoLte -> {
                    o.put("t", "lte")
                    o.put("ci", cell.cellIdentity.ci.known())
                    o.put("pci", cell.cellIdentity.pci.known())
                    o.put("tac", cell.cellIdentity.tac.known())
                    o.put("dbm", cell.cellSignalStrength.dbm.known())
                }
                is CellInfoWcdma -> {
                    o.put("t", "wcdma")
                    o.put("ci", cell.cellIdentity.cid.known())
                    o.put("pci", cell.cellIdentity.psc.known())
                    o.put("lac", cell.cellIdentity.lac.known())
                    o.put("dbm", cell.cellSignalStrength.dbm.known())
                }
                is CellInfoGsm -> {
                    o.put("t", "gsm")
                    o.put("ci", cell.cellIdentity.cid.known())
                    o.put("lac", cell.cellIdentity.lac.known())
                    o.put("dbm", cell.cellSignalStrength.dbm.known())
                }
                else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
                    val id = cell.cellIdentity as? CellIdentityNr
                    val signal = cell.cellSignalStrength as? CellSignalStrengthNr
                    o.put("t", "nr")
                    o.put("ci", id?.nci?.takeIf { it != Long.MAX_VALUE })
                    o.put("pci", id?.pci?.known())
                    o.put("tac", id?.tac?.known())
                    o.put("dbm", signal?.dbm?.known())
                } else {
                    o.put("t", cell.javaClass.simpleName)
                }
            }
            // Idade da medição, em segundos: o Android pode devolver uma antiga.
            val stamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cell.timestampMillis * 1_000_000L
            else @Suppress("DEPRECATION") cell.timeStamp
            o.put("age", ((nowNanos - stamp) / 1_000_000_000L).coerceAtLeast(0))
            list.put(o)
        }
        DevLog.log("cell", "n" to cells.size, "c" to list, "err" to error)
    }

    private fun scanWifi() {
        val w = wifi ?: return
        @Suppress("DEPRECATION")
        val ok = runCatching { w.startScan() }.getOrDefault(false)
        if (!ok) DevLog.log("wifi_scan", "ok" to false)
        // Mesmo recusada, outra busca (do Android ou de outro app) pode ter chegado.
        handler.postDelayed({ logWifi() }, WIFI_RESULT_WAIT_MS)
    }

    private fun logWifi() {
        if (!running) return
        val results: List<ScanResult> = try {
            wifi?.scanResults.orEmpty()
        } catch (e: SecurityException) {
            DevLog.log("wifi", "error" to e.message)
            return
        }
        val newest = results.maxOfOrNull { it.timestamp } ?: return
        if (newest <= lastWifiMicros) return
        lastWifiMicros = newest
        val nowMicros = SystemClock.elapsedRealtimeNanos() / 1000L
        val list = JSONArray()
        for (r in results.sortedByDescending { it.level }.take(MAX_WIFI)) {
            val o = JSONObject()
            o.put("h", hash(r.BSSID.orEmpty()))
            o.put("l", r.level)
            o.put("f", r.frequency)
            o.put("age", ((nowMicros - r.timestamp) / 1_000_000L).coerceAtLeast(0))
            ssid(r)?.takeIf { PUBLIC_SSID.containsMatchIn(it) }?.let { o.put("s", it) }
            list.put(o)
        }
        DevLog.log("wifi", "n" to results.size, "w" to list)
    }

    private fun ssid(r: ScanResult): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) r.wifiSsid?.toString()?.trim('"')
        else @Suppress("DEPRECATION") r.SSID

    /** Os 8 primeiros caracteres do SHA-256 do endereço: o mesmo em toda viagem, sem o endereço. */
    private fun hash(bssid: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bssid.lowercase().toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Valor que o Android usa para "desconhecido" vira null. */
    private fun Int.known(): Int? = takeIf { it != Int.MAX_VALUE }

    companion object {
        private const val CELL_EVERY_MS = 5_000L
        /** 4 buscas a cada 2 minutos é o que o Android deixa um app em primeiro plano fazer. */
        private const val WIFI_EVERY_MS = 30_000L
        private const val WIFI_RESULT_WAIT_MS = 6_000L
        private const val MAX_CELLS = 8
        private const val MAX_WIFI = 20
        /** Nomes de rede que valem guardar: do metrô, do governo, públicas. */
        private val PUBLIC_SSID = Regex("(?i)metr|vlt|gov|livre|gratis|free|publi|wifi.?for|fortaleza")
    }
}
