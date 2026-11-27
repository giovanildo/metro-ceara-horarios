package io.github.giova.metroceara.trip

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.data.DevLog
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Busca aparelhos Bluetooth (BLE) durante o modo viagem, para ver se algum equipamento
 * fixo é característico de cada estação (pedido de 27/11/2026: "vai que…"). Só na versão
 * de teste (permissões em src/campo/AndroidManifest.xml), dentro da opção de torres e
 * redes ([RadioRecorder]). Não conecta a nada e não liga o Bluetooth: se estiver
 * desligado, `ble_start` diz e nada é gravado.
 *
 * A cada [EVERY_MS], um evento `ble` com os aparelhos vistos nesse intervalo (`d`, até
 * [MAX_DEVICES], do sinal mais forte): o endereço resumido em 8 letras (`h`, hash), o
 * sinal mais forte (`l`), quantos anúncios (`k`), o fabricante (`m`, código do Bluetooth
 * SIG; 76 = Apple, 6 = Microsoft, 117 = Samsung) e se aceita conexão (`c`). Celulares
 * trocam de endereço a cada poucos minutos; equipamento fixo costuma manter.
 *
 * Com a tela apagada, o Android pausa buscas sem filtro; por isso a busca leva um filtro
 * vazio (aceita tudo). Se mesmo assim parar, os eventos somem com a tela apagada.
 */
class BleRecorder(private val context: Context, private val handler: Handler) {

    private class Seen(var rssi: Int, var count: Int, val maker: Int?, val connectable: Boolean?)

    private val seen = HashMap<String, Seen>()
    private var running = false
    private val scanner by lazy {
        context.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handler.post { add(result) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            handler.post { results.forEach(::add) }
        }

        override fun onScanFailed(errorCode: Int) {
            DevLog.log("ble_start", "error" to "busca falhou: $errorCode")
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            flush()
            handler.postDelayed(this, EVERY_MS)
        }
    }

    @SuppressLint("MissingPermission") // conferida em canScan()
    fun start() {
        if (!canScan(context)) {
            DevLog.log("ble_start", "error" to "sem permissão de Bluetooth")
            return
        }
        val s = scanner
        if (s == null) {
            DevLog.log("ble_start", "error" to "Bluetooth desligado")
            return
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        try {
            s.startScan(listOf(ScanFilter.Builder().build()), settings, callback)
            running = true
            DevLog.log("ble_start", "ok" to true)
            handler.postDelayed(tick, EVERY_MS)
        } catch (e: Exception) {
            DevLog.log("ble_start", "error" to e.toString())
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!running) return
        running = false
        runCatching { scanner?.stopScan(callback) }
        handler.removeCallbacks(tick)
        flush()
    }

    private fun add(r: ScanResult) {
        if (!running) return
        val key = hash(r.device.address.orEmpty())
        val maker = r.scanRecord?.manufacturerSpecificData?.takeIf { it.size() > 0 }?.keyAt(0)
        val connectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) r.isConnectable else null
        val s = seen.getOrPut(key) { Seen(r.rssi, 0, maker, connectable) }
        s.count++
        if (r.rssi > s.rssi) s.rssi = r.rssi
    }

    private fun flush() {
        if (seen.isEmpty()) {
            if (running) DevLog.log("ble", "n" to 0)
            return
        }
        val list = JSONArray()
        seen.entries.sortedByDescending { it.value.rssi }.take(MAX_DEVICES).forEach { (h, s) ->
            list.put(JSONObject().put("h", h).put("l", s.rssi).put("k", s.count).put("m", s.maker).put("c", s.connectable))
        }
        DevLog.log("ble", "n" to seen.size, "d" to list)
        seen.clear()
    }

    private fun hash(address: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(address.lowercase().toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val EVERY_MS = 5_000L
        private const val MAX_DEVICES = 30

        /** Android 12+: "dispositivos por perto" (BLUETOOTH_SCAN); antes: a localização e as permissões antigas. */
        fun permissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        fun canScan(context: Context): Boolean {
            val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN
            else Manifest.permission.BLUETOOTH_ADMIN
            return ContextCompat.checkSelfPermission(context, needed) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
