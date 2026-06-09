package com.bambacorp.bambaerp;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {
    private static final String TAG = "BambaMain";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Enlever le titre
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 2. Colorer les barres système en blanc
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(Color.WHITE);
        }

        // 3. Icônes sombres sur fond blanc
        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decorView.setSystemUiVisibility(flags);

        setContentView(R.layout.activity_main);

        // 4. Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1
                );
            }
        }

        // 5. Get & log FCM token
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                getSharedPreferences("bamba_prefs", MODE_PRIVATE)
                    .edit().putString("fcm_token", token).apply();
            }
        });

        // 6. WebView setup
        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setBackgroundColor(Color.WHITE);

        // 7. JS Bridge — exposes FCM token to Odoo web pages
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public String getFcmToken() {
                return getSharedPreferences("bamba_prefs", MODE_PRIVATE)
                    .getString("fcm_token", "");
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // 8. Handle deep link from notification tap
        String url = getIntent().getStringExtra("url");
        webView.loadUrl(url != null ? url : "https://crm.bambacorporation.com/odoo/discuss");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
