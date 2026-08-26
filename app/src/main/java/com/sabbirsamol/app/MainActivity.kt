package com.sabbir.amol // আপনার প্রজেক্টের প্যাকেজ নাম অনুযায়ী প্রয়োজন হলে পরিবর্তন করুন

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WebView তৈরি ও সেটআপ
        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true                  // জাভাস্ক্রিপ্ট সক্রিয় করা
        settings.domStorageEnabled = true                  // কাজা ট্র্যাকার ও ডার্ক মোডের লোকাল স্টোরেজ সাপোর্ট
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // assets ফোল্ডার থেকে আপনার মূল index.html লোড করা
        webView.loadUrl("file:///android_asset/index.html")

        // ব্যাক বাটন চাপলে অ্যাপ থেকে বের না হয়ে পেইজের ভেতরে ব্যাকে যাওয়ার হ্যান্ডলার
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
