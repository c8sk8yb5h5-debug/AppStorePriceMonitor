package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PriceRankingActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private static final String DEFAULT_URL = "https://appstoreprice.org/zh/apps/6448311069?plan=1";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("url");
        if (url == null || !url.startsWith("https://appstoreprice.org/zh/apps/")) {
            url = getSharedPreferences("price_ranking", MODE_PRIVATE).getString("url", DEFAULT_URL);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(2), dp(8), dp(2));

        TextView title = new TextView(this);
        title.setText("价格哨兵  ·  全球订阅价格");
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setTextColor(Color.rgb(23, 32, 51));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button settings = new Button(this);
        settings.setText("监控设置");
        header.addView(settings, new LinearLayout.LayoutParams(dp(100), dp(42)));

        Button refresh = new Button(this);
        refresh.setText("刷新");
        header.addView(refresh, new LinearLayout.LayoutParams(dp(72), dp(42)));
        root.addView(header);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua.replace("; wv", "").replace(" Mobile", ""));
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String pageUrl) {
                super.onPageFinished(view, pageUrl);
                String js = "(function(){"
                    + "var s=document.createElement('style');"
                    + "s.innerHTML='html,body{width:100%!important;max-width:100%!important;}"
                    + "aside,[class*=sidebar],[class*=Sidebar],[class*=advert],[class*=advertisement],[id*=sidebar]{display:none!important;}"
                    + "main,[class*=main-content],[class*=content]{max-width:none!important;width:100%!important;}' ;"
                    + "document.head.appendChild(s);"
                    + "var all=document.querySelectorAll('body *');"
                    + "for(var i=0;i<all.length;i++){var e=all[i];if(e.children.length===0){var t=(e.innerText||e.textContent||'').trim();"
                    + "if(t==='价格分布'||t==='礼品卡商城'){var p=e;for(var j=0;j<4&&p.parentElement;j++)p=p.parentElement;p.style.display='none';}}}"
                    + "})();";
                view.evaluateJavascript(js, null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        settings.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        refresh.setOnClickListener(v -> webView.reload());
        webView.loadUrl(url);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
