package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class PriceRankingActivity extends Activity {
    private WebView webView;
    private ProgressBar progress;
    private static final String DEFAULT_URL = "https://appstoreprice.org/zh/apps/6448311069?plan=1";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the system notification/status bar visible and reserve its space explicitly.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.rgb(248, 250, 253));
        getWindow().setNavigationBarColor(Color.rgb(248, 250, 253));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        String url = getIntent().getStringExtra("url");
        if (url == null || !url.startsWith("https://appstoreprice.org/zh/apps/")) {
            url = getSharedPreferences("price_ranking", MODE_PRIVATE)
                    .getString("url", DEFAULT_URL);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 250, 253));
        root.setFitsSystemWindows(true);
        int statusBarHeight = getSystemBarHeight("status_bar_height");
        int navigationBarHeight = getSystemBarHeight("navigation_bar_height");
        root.setPadding(0, statusBarHeight, 0, 0);

        // Small, reachable controls below the system status bar; no page title or extra content.
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), 0, dp(8), 0);
        toolbar.setBackgroundColor(Color.rgb(248, 250, 253));

        Button settings = new Button(this);
        settings.setText("监控设置");
        settings.setTextSize(13);
        toolbar.addView(settings, new LinearLayout.LayoutParams(0, dp(42), 1));

        Button refresh = new Button(this);
        refresh.setText("刷新");
        refresh.setTextSize(13);
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(76), dp(42)));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(44)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(248, 250, 253));
        WebSettings settingsWeb = webView.getSettings();
        settingsWeb.setJavaScriptEnabled(true);
        settingsWeb.setDomStorageEnabled(true);
        settingsWeb.setLoadsImagesAutomatically(true);
        settingsWeb.setSupportZoom(true);
        settingsWeb.setBuiltInZoomControls(true);
        settingsWeb.setDisplayZoomControls(false);
        settingsWeb.setUseWideViewPort(true);
        settingsWeb.setLoadWithOverviewMode(false);
        String ua = settingsWeb.getUserAgentString();
        settingsWeb.setUserAgentString(ua.replace("; wv", "").replace(" Mobile", ""));

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String pageUrl) {
                super.onPageFinished(view, pageUrl);
                view.evaluateJavascript("setTimeout(function(){(" + rankingOnlyScript() + ")();},500);", null);
                view.postDelayed(() -> view.evaluateJavascript("(" + rankingOnlyScript() + ")();", null), 1600);
                view.postDelayed(() -> view.evaluateJavascript("(" + rankingOnlyScript() + ")();", null), 3000);
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

    private String rankingOnlyScript() {
        return "function(){try{"
            + "var norm=function(e){return ((e.innerText||e.textContent||'').replace(/\\s+/g,' ').trim());};"
            + "var nodes=Array.from(document.querySelectorAll('body *'));var target=null;"
            + "var heads=nodes.filter(function(e){return e.children.length===0&&norm(e)==='排名';});"
            + "for(var h of heads){var p=h;for(var i=0;i<10&&p&&p!==document.body;i++,p=p.parentElement){"
            + "var t=norm(p);if(t.includes('地区')&&t.includes('原价')&&t.includes('CNY')&&t.includes('状态')){target=p;break;}}if(target)break;}"
            + "if(!target){var c=nodes.filter(function(e){var t=norm(e);return t.includes('排名')&&t.includes('地区')&&t.includes('原价')&&t.includes('CNY')&&t.includes('状态')&&t.length<60000;});"
            + "c.sort(function(a,b){return norm(a).length-norm(b).length;});target=c[0]||null;}"
            + "if(!target)return;"
            + "var style=document.getElementById('ranking-only-style');if(!style){style=document.createElement('style');style.id='ranking-only-style';document.head.appendChild(style);}"
            + "style.textContent='html,body{margin:0!important;padding:0!important;width:100%!important;max-width:100%!important;overflow-x:hidden!important;background:#f8fafd!important;}body *{box-sizing:border-box;}';"
            + "var chain=[],p=target;while(p&&p!==document.body){chain.push(p);p=p.parentElement;}if(!chain.length)return;"
            + "var top=chain[chain.length-1];Array.from(document.body.children).forEach(function(e){if(e!==top)e.style.setProperty('display','none','important');});"
            + "for(var k=chain.length-1;k>=0;k--){var parent=chain[k].parentElement;if(!parent)continue;Array.from(parent.children).forEach(function(e){if(e!==chain[k])e.style.setProperty('display','none','important');});}"
            + "target.style.setProperty('display','block','important');target.style.setProperty('visibility','visible','important');"
            + "target.style.setProperty('width','100%','important');target.style.setProperty('max-width','none','important');target.style.setProperty('margin','0','important');target.style.setProperty('padding','0','important');target.style.setProperty('overflow-x','auto','important');"
            + "}catch(e){}}";
    }

    private int getSystemBarHeight(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
