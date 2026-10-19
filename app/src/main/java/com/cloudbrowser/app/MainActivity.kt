package com.cloudbrowser.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private val bg = Color.rgb(16, 18, 27)
    private val panel = Color.rgb(29, 32, 45)
    private val accent = Color.rgb(115, 128, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(10, 8, 10, 8)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 4, 4, 8)
        }

        fun button(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(panel)
            setOnClickListener { action() }
        }

        bar.addView(button("‹") { if (browser.canGoBack()) browser.goBack() },
            LinearLayout.LayoutParams(48, 48))
        bar.addView(button("›") { if (browser.canGoForward()) browser.goForward() },
            LinearLayout.LayoutParams(48, 48))
        bar.addView(button("↻") { browser.reload() },
            LinearLayout.LayoutParams(48, 48))

        address = EditText(this).apply {
            hint = "Search or enter website"
            singleLine = true
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(16, 0, 16, 0)
            setBackgroundColor(panel)
            imeOptions = EditorInfo.IME_ACTION_GO
            setSelectAllOnFocus(true)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    navigate()
                    true
                } else false
            }
        }
        bar.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        bar.addView(button("Go") { navigate() }, LinearLayout.LayoutParams(64, 48))
        root.addView(bar)

        browser = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    address.setText(url)
                }
            }
        }
        root.addView(browser, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        browser.loadUrl("https://www.google.com")
    }

    private fun navigate() {
        var text = address.text.toString().trim()
        if (text.isEmpty()) return
        val url = if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http"))) {
            "https://www.google.com/search?q=" + android.net.Uri.encode(text)
        } else if (!text.startsWith("http://") && !text.startsWith("https://")) {
            "https://$text"
        } else text
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(address.windowToken, 0)
        browser.loadUrl(url)
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::browser.isInitialized && browser.canGoBack()) browser.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
