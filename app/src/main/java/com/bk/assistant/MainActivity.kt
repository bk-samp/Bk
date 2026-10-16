package com.bk.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        fun btn(text: String, action: () -> Unit) = col.addView(Button(this).apply {
            this.text = text; setOnClickListener { action() } })
        col.addView(TextView(this).apply { text = "BK – assistente de voz\nSiga os 3 passos:"; textSize = 20f })
        btn("1. Permitir microfone e notificações") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        btn("2. Ativar acessibilidade (BK Assistente)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("3. Iniciar BK") { startForegroundService(Intent(this, VoiceService::class.java)) }
        btn("Parar BK") { stopService(Intent(this, VoiceService::class.java)) }
        setContentView(col)
    }
}
