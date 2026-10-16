package com.bk.assistant

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.Normalizer
import java.util.Locale

class VoiceService : Service(), TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private var speaking = false
    private var conversing = false
    private var idleErrors = 0

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("bk", "BK", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "bk").setContentTitle("BK ouvindo")
            .setContentText("Diga \"oi BK\"").setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) else startForeground(1, n)
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        tts.language = Locale("pt", "BR")
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onError(id: String?) { main.post { speaking = false; listen() } }
            override fun onDone(id: String?) { main.post { speaking = false; listen() } }
        })
        listen()
    }

    private fun speak(text: String) {
        speaking = true
        recognizer?.cancel()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bk")
    }

    private fun listen() {
        if (speaking) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle?) {
                    val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (t.isNullOrBlank()) restart() else { idleErrors = 0; handle(t) }
                }
                override fun onError(e: Int) {
                    if (conversing && ++idleErrors >= 6) { conversing = false; idleErrors = 0 } // volta ao modo "oi BK"
                    restart()
                }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        }
    }

    private fun restart() { if (!speaking) main.postDelayed({ listen() }, 300) }

    private fun norm(s: String) =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

    private fun handle(raw: String) {
        val t = norm(raw)
        val a11y = BkAccessibilityService.instance
        if (!conversing) {
            if (Regex("\\boi (bk|b k|be ca|beca|bika)\\b").containsMatchIn(t)) {
                conversing = true
                a11y?.home()
                speak("Oi, o que posso ajudar?")
            } else restart()
            return
        }
        when {
            Regex("tchau|encerrar|pode parar|obrigado").containsMatchIn(t) -> { conversing = false; speak("Até mais!") }
            Regex("tela inicial|inicio|home").containsMatchIn(t) -> { a11y?.home(); speak("Pronto.") }
            t.contains("voltar") -> { a11y?.back(); speak("Voltei.") }
            t.contains("recentes") -> { a11y?.recents(); speak("Aqui estão os recentes.") }
            Regex("rolar para baixo|descer").containsMatchIn(t) -> { a11y?.scroll(true); speak("Rolando.") }
            Regex("rolar para cima|subir").containsMatchIn(t) -> { a11y?.scroll(false); speak("Rolando.") }
            t.startsWith("abrir ") -> openApp(t.removePrefix("abrir ").trim())
            else -> speak("Não entendi. Pode repetir?")
        }
    }

    private fun openApp(name: String) {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(launcher, 0).firstOrNull { norm(it.loadLabel(pm).toString()).contains(name) }
        val intent = match?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (intent != null) { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); speak("Abrindo $name.") }
        else speak("Não encontrei o aplicativo $name.")
    }

    override fun onDestroy() {
        recognizer?.destroy(); tts.shutdown(); super.onDestroy()
    }
}
