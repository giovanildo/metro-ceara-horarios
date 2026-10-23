package io.github.giova.metroceara

import android.app.Application
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.Regras

/** Carrega as regras do Metrofor, inicia o registro de testes e grava travamentos nele antes de o app fechar. */
class MetroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DevLog.init(this)
        Regras.init(this)
        DevLog.log("app_start", *DevLog.deviceInfo(this))
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            DevLog.crash(error)
            previous?.uncaughtException(thread, error)
        }
    }
}
