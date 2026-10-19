package com.cloudbrowser.app

import android.app.Activity
import android.app.AlertDialog
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
    private lateinit var webContainer: LinearLayout
    private lateinit var tabsButton: Button
    private val tabs = mutableListOf<WebView>()
    private val tabTitles = mutableMapOf<WebView, String>()
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
            setPadding(8, 8, 8, 8)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 8)
        }

        fun button(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(panel)
            setOnClickListener { action() }
        }

        bar.addView(button("‹") { if (browser.canGoBack()) browser.goBack() },
            LinearLayout.LayoutParams(44, 48))
        bar.addView(button("›") { if (browser.canGoForward()) browser.goForward() },
            LinearLayout.LayoutParams(44, 48))
        bar.addView(button("↻") { browser.reload() },
            LinearLayout.LayoutParams(44, 48))

        address = EditText(this).apply {
            hint = "Search or enter HTTPS website"
            isSingleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 0, 12, 0)
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
        bar.addView(button("Go") { navigate() }, LinearLayout.LayoutParams(52, 48))
        root.addView(bar)

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
        }
        tabsButton = button("Tabs (1)") { showTabsDialog() }
        tabBar.addView(tabsButton, LinearLayout.LayoutParams(0, 42, 1f))
        tabBar.addView(button("+ New tab") { newTab("https://www.google.com") },
            LinearLayout.LayoutParams(112, 42))
        root.addView(tabBar)

        webContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(webContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        browser = createBrowser()
        tabs.add(browser)
        tabTitles[browser] = "New tab"
        webContainer.addView(browser, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        updateTabsButton()
        browser.loadUrl("https://www.google.com")
    }

    private fun updateTabsButton() {
        if (::tabsButton.isInitialized) tabsButton.text = "Tabs (${tabs.size})"
    }

    private fun showTabsDialog() {
        val labels = tabs.mapIndexed { index, web ->
            "${index + 1}. ${tabTitles[web] ?: web.url ?: "New tab"}"
        }.toTypedArray()
        val selected = tabs.indexOf(browser).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Open tabs")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                switchTab(tabs[which])
                dialog.dismiss()
            }
            .setPositiveButton("New tab") { _, _ -> newTab("https://www.google.com") }
            .setNeutralButton("Close current") { _, _ -> closeCurrentTab() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun newTab(url: String) {
        val web = createBrowser()
        tabs.add(web)
        tabTitles[web] = "New tab"
        switchTab(web)
        updateTabsButton()
        web.loadUrl(url)
    }

    private fun switchTab(target: WebView) {
        if (!tabs.contains(target)) return
        (target.parent as? android.view.ViewGroup)?.removeView(target)
        webContainer.removeAllViews()
        browser = target
        webContainer.addView(target, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        address.setText(target.url ?: "")
        updateTabsButton()
    }

    private fun closeCurrentTab() {
        if (tabs.size <= 1) {
            browser.loadUrl("https://www.google.com")
            tabTitles[browser] = "New tab"
            updateTabsButton()
            return
        }
        val old = browser
        val index = tabs.indexOf(old)
        webContainer.removeView(old)
        tabs.remove(old)
        tabTitles.remove(old)
        old.stopLoading()
        old.destroy()
        val next = tabs[(index - 1).coerceAtLeast(0)]
        switchTab(next)
        updateTabsButton()
    }

    private fun createBrowser(): WebView {
        return WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false

            // Restrict local-file access and insecure mixed content.
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (view != null && !title.isNullOrBlank()) tabTitles[view] = title.take(60)
                    updateTabsButton()
                }
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
                    if (view === browser) address.setText(url)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (view === browser) address.setText(url)
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    Toast.makeText(this@MainActivity, "Secure connection failed. Page blocked.", Toast.LENGTH_LONG).show()
                }
                @android.annotation.TargetApi(27)
                override fun onSafeBrowsingHit(
                    view: WebView,
                    request: WebResourceRequest,
                    threatType: Int,
                    callback: android.webkit.SafeBrowsingResponse
                ) {
                    callback.backToSafety(true)
                    Toast.makeText(this@MainActivity, "Safety warning: VAbro blocked a potentially harmful page.", Toast.LENGTH_LONG).show()
                }
                override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                    if (tabs.contains(view)) recoverBrowser(view)
                    return true
                }
            }
        }
    }

    private fun recoverBrowser(crashed: WebView) {
        val index = tabs.indexOf(crashed)
        val wasCurrent = crashed === browser
        (crashed.parent as? android.view.ViewGroup)?.removeView(crashed)
        crashed.destroy()
        if (index >= 0) {
            val replacement = createBrowser()
            tabs[index] = replacement
            tabTitles.remove(crashed)
            tabTitles[replacement] = "Restored tab"
            if (wasCurrent) browser = replacement
            if (wasCurrent) {
                webContainer.removeAllViews()
                webContainer.addView(replacement, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
            }
            val previousUrl = address.text.toString().trim()
            val target = if (previousUrl.startsWith("https://", true)) previousUrl else "https://www.google.com"
            replacement.loadUrl(target)
            updateTabsButton()
            Toast.makeText(this, "The page stopped unexpectedly. VAbro restored it.", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigate() {
        val text = address.text.toString().trim()
        if (text.isEmpty()) return
        val url = if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http", true))) {
            "https://www.google.com/search?q=" + android.net.Uri.encode(text)
        } else {
            val normalized = if (text.startsWith("http://", true)) {
                Toast.makeText(this, "HTTP is blocked. Trying HTTPS instead.", Toast.LENGTH_LONG).show()
                text.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
            } else if (!text.startsWith("https://", true)) {
                "https://$text"
            } else text
            normalized
        }
        if (android.net.Uri.parse(url).scheme != "https") {
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
        else if (tabs.size > 1) closeCurrentTab()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.toList().forEach { it.stopLoading(); it.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
