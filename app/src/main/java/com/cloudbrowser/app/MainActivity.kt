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
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.webkit.PermissionRequest
import android.webkit.GeolocationPermissions

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
            hint = "Search or enter HTTPS website"
            isSingleLine = true
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

        browser = createBrowser()
        root.addView(browser, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        browser.loadUrl("https://www.google.com")
    }


    private fun createBrowser(): WebView {
        return WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            // Improve compatibility with HTML5 video players.
            settings.mediaPlaybackRequiresUserGesture = false

            // Keep local files inaccessible and block insecure mixed content.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true

            // Some media providers require third-party cookies; device permissions remain denied.
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    request.deny()
                }
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    callback.invoke(origin, false, false)
                }
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean = false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (request.isForMainFrame && uri.scheme?.lowercase() != "https") {
                        Toast.makeText(this@MainActivity, "VAbro blocks non-HTTPS pages for safety.", Toast.LENGTH_LONG).show()
                        return true
                    }
                    return false
                }
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    address.setText(url)
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    Toast.makeText(this@MainActivity, "Secure connection failed. Page blocked.", Toast.LENGTH_LONG).show()
                }
                override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                    if (view === browser) recoverBrowser(view)
                    return true
                }
            }
        }
    }

    private fun recoverBrowser(crashed: WebView) {
        val parent = crashed.parent as? LinearLayout
        val index = parent?.indexOfChild(crashed) ?: -1
        val params = crashed.layoutParams
        parent?.removeView(crashed)
        crashed.destroy()
        if (parent != null && index >= 0) {
            browser = createBrowser()
            parent.addView(browser, index, params)
            val current = address.text.toString().trim()
            val target = if (current.startsWith("https://", ignoreCase = true)) current else "https://www.google.com"
            browser.loadUrl(target)
            Toast.makeText(this, "The page stopped unexpectedly. VAbro restored it.", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigate() {
        val text = address.text.toString().trim()
        if (text.isEmpty()) return
        val url = if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http"))) {
            "https://www.google.com/search?q=" + android.net.Uri.encode(text)
        } else {
            val normalized = if (text.startsWith("http://", ignoreCase = true)) {
                Toast.makeText(this, "HTTP is blocked. Trying HTTPS instead.", Toast.LENGTH_LONG).show()
                text.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
            } else if (!text.startsWith("https://", ignoreCase = true)) {
                "https://$text"
            } else text
            normalized
        }
        val parsed = android.net.Uri.parse(url)
        if (parsed.scheme != "https") {
            Toast.makeText(this, "Only HTTPS websites are allowed.", Toast.LENGTH_LONG).show()
            return
        }
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
