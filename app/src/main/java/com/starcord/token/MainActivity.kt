package com.starcord.token

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tokenLabel: TextView
    private lateinit var copyBtn: Button
    private lateinit var progress: ProgressBar

    private var currentToken: String? = null
    private val handler = Handler(Looper.getMainLooper())

    // JavaScript to pull the token from Discord's localStorage
    private val extractJs = """
        (function() {
            try {
                var t = localStorage.getItem('token');
                if (t) {
                    t = t.replace(/^"|"$/g, '');
                    if (t && t.length > 20) return t;
                }
                var ts = localStorage.getItem('tokens');
                if (ts) {
                    try {
                        var a = JSON.parse(ts);
                        if (a && a[0] && String(a[0]).length > 20) return String(a[0]);
                    } catch(e) {}
                }
            } catch(e) {}
            return null;
        })();
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView   = findViewById(R.id.webview)
        tokenLabel = findViewById(R.id.tokenLabel)
        copyBtn   = findViewById(R.id.copyBtn)
        progress  = findViewById(R.id.progress)

        setupWebView()
        startPolling()

        copyBtn.setOnClickListener { copyToken() }

        // Back navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled      = true
            domStorageEnabled      = true
            databaseEnabled        = true
            cacheMode              = WebSettings.LOAD_DEFAULT
            // Desktop UA so Discord loads its full web client
            userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
                extractToken()
            }
        }

        webView.loadUrl("https://discord.com/login")
    }

    private fun startPolling() {
        handler.post(object : Runnable {
            override fun run() {
                extractToken()
                handler.postDelayed(this, 2_000L)
            }
        })
    }

    private fun extractToken() {
        webView.evaluateJavascript(extractJs) { raw ->
            val token = raw
                ?.replace("\"", "")
                ?.replace("\\u003d", "=")
                ?.trim()
                ?.takeIf { it != "null" && it.length > 20 }

            if (!token.isNullOrEmpty() && token != currentToken) {
                currentToken = token
                val preview = "${token.take(26)}…"
                tokenLabel.text = "✦ Token ready  $preview"
                tokenLabel.setTextColor(ContextCompat.getColor(this, R.color.success))
                copyBtn.isEnabled = true
            }
        }
    }

    private fun copyToken() {
        val token = currentToken ?: run {
            Toast.makeText(this, "Login to Discord first", Toast.LENGTH_SHORT).show()
            return
        }
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("starcord_token", token))
        vibrate()
        Toast.makeText(this, "Token copied  ✦", Toast.LENGTH_SHORT).show()
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
                    .vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(40)
            }
        } catch (_: Exception) { /* ignore if no vibrator */ }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
