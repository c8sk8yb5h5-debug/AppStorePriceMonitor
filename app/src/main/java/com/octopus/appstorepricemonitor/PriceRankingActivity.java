package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Environment;
import android.os.Build;
import android.content.pm.PackageInfo;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import org.jsoup.Jsoup;
import java.util.List;

public class PriceRankingActivity extends Activity {
    private static final String PREFS = "price_ranking";
    private static final String DEFAULT_URL = "https://appstoreprice.org/zh/apps/6448311069?plan=1";
    // The four plan URLs are kept in one place so they can be adjusted if the source site changes its plan IDs.
    private static final String[] PRODUCT_NAMES = {"ChatGPT Plus", "ChatGPT Go", "Gemini AI Pro", "Gemini AI Plus", "设置"};
    private static final String[] PRODUCT_URLS = {
        "https://appstoreprice.org/zh/apps/6448311069?plan=0",
        "https://appstoreprice.org/zh/apps/6448311069?plan=1",
        "https://appstoreprice.org/zh/apps/6477489729?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=1",
        ""
    };

    private LinearLayout rowsContainer;
    private LinearLayout productTabs;
    private FrameLayout bottomNav;
    private LinearLayout navItems;
    private TextView floatingActiveIcon;
    private TextView currentHeading;
    private View cardNotch;
    private View navShape;
    private View tableHeader;
    private ScrollView priceScroll;
    private GestureDetector swipeDetector;
    private boolean changingProduct = false;
    private TextView statusView;
    private ProgressBar progress;
    private String currentUrl;
    private int selectedProduct = 0;

    private final int ink = Color.rgb(27, 37, 20);
    private final int muted = Color.rgb(104, 117, 139);
    private final int line = Color.rgb(135, 154, 98);
    private final int page = Color.rgb(255, 220, 122);
    private final int blue = Color.rgb(255, 220, 122);
    private final int purple = Color.rgb(111, 77, 214);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(page);
        getWindow().setNavigationBarColor(page);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        currentUrl = getIntent().getStringExtra("url");
        if (currentUrl == null || !currentUrl.startsWith("https://appstoreprice.org/zh/apps/")) {
            currentUrl = getSharedPreferences(PREFS, MODE_PRIVATE).getString("url", DEFAULT_URL);
        }
        for (int i = 0; i < PRODUCT_URLS.length - 1; i++) if (PRODUCT_URLS[i].equals(currentUrl)) selectedProduct = i;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(255, 220, 122));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(12), insets.getSystemWindowInsetTop() + dp(4), dp(12), insets.getSystemWindowInsetBottom());
            return insets;
        });

        FrameLayout contentFrame = new FrameLayout(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(64));
        card.setBackground(new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                android.graphics.Rect b = getBounds();
                float w = b.width(), h = b.height();
                float radius = dp(30), bulge = dp(40);
                int[] order = {0, 1, 2, 3, 4};
                int slot = 0;
                for (int i = 0; i < order.length; i++) if (order[i] == selectedProduct) slot = i;
                float cx = w / 2f - dp(160) + dp(64) * (slot + 0.5f);
                float half = dp(52);
                Path path = new Path();
                path.moveTo(0, radius);
                path.quadTo(0, 0, radius, 0);
                path.lineTo(w - radius, 0);
                path.quadTo(w, 0, w, radius);
                float baseY = h - bulge;
                path.lineTo(w, baseY - dp(30));
                path.quadTo(w, baseY, w - dp(30), baseY);
                path.lineTo(cx + half, baseY);
                path.cubicTo(cx + dp(30), baseY, cx + dp(30), h, cx, h);
                path.cubicTo(cx - dp(30), h, cx - dp(30), baseY, cx - half, baseY);
                path.lineTo(dp(30), baseY);
                path.quadTo(0, baseY, 0, baseY - dp(30));
                path.close();
                paint.setColor(Color.rgb(157, 174, 111));
                canvas.drawPath(path, paint);
            }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
            @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(-1, -1, Gravity.TOP);
        cardLp.bottomMargin = dp(30);
        contentFrame.addView(card, cardLp);

        currentHeading = new TextView(this);
        currentHeading.setText(PRODUCT_NAMES[selectedProduct].toUpperCase());
        currentHeading.setTextSize(26);
        currentHeading.setTextColor(Color.WHITE);
        currentHeading.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        currentHeading.setGravity(Gravity.CENTER_VERTICAL);
        currentHeading.setSingleLine(true);
        currentHeading.setPadding(dp(10), 0, 0, 0);
        card.addView(currentHeading, new LinearLayout.LayoutParams(-1, dp(54)));
        View titleDivider = new View(this);
        titleDivider.setBackgroundColor(Color.rgb(177, 191, 137));
        card.addView(titleDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        card.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
        statusView = new TextView(this);
        statusView.setText("正在读取全球价格…");
        statusView.setTextSize(11);
        statusView.setTextColor(Color.rgb(58, 72, 45));
        statusView.setPadding(dp(4), dp(5), dp(4), dp(5));
        card.addView(statusView, new LinearLayout.LayoutParams(-1, dp(25)));
        statusView.setContentDescription("点击刷新价格");
        statusView.setOnClickListener(v -> loadPrices());

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(true);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundColor(Color.rgb(157, 174, 111));
        table.setMinimumWidth(getResources().getDisplayMetrics().widthPixels - dp(48));
        tableHeader = makeHeader();
        table.addView(tableHeader);
        rowsContainer = new LinearLayout(this);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        table.addView(rowsContainer, new LinearLayout.LayoutParams(-1, -2));
        horizontal.addView(table);
        priceScroll = new ScrollView(this);
        priceScroll.setFillViewport(false);
        priceScroll.addView(horizontal);
        card.addView(priceScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = makeBottomNavigation();
        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(112), Gravity.BOTTOM);
        navLp.leftMargin = dp(2);
        navLp.rightMargin = dp(2);
        contentFrame.addView(bottomNav, navLp);
        root.addView(contentFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        swipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(65) && Math.abs(dx) > Math.abs(dy) * 1.25f && Math.abs(velocityX) > dp(120)) {
                    selectProduct((selectedProduct + (dx < 0 ? 1 : PRODUCT_URLS.length - 1)) % PRODUCT_URLS.length, dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        setContentView(root);
        root.requestApplyInsets();
        renderBottomNavigation();
        loadPrices();
    }

    private TextView circleAction(String symbol, String description) {
        TextView view = new TextView(this);
        view.setText(symbol);
        view.setContentDescription(description);
        view.setTextSize(23);
        view.setTextColor(ink);
        view.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), line);
        view.setBackground(bg);
        view.setElevation(dp(2));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private LinearLayout makeProductRow(boolean chatGpt) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(3), dp(2), dp(3));
        TextView icon = new TextView(this);
        icon.setText(chatGpt ? "✳" : "✦");
        icon.setTextSize(27);
        icon.setTypeface(null, Typeface.BOLD);
        icon.setTextColor(chatGpt ? Color.rgb(16, 163, 127) : purple);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(chatGpt ? Color.rgb(225, 248, 241) : Color.rgb(240, 235, 255));
        icon.setBackground(iconBg);
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView first = productButton(chatGpt ? 1 : 2);
        TextView second = productButton(chatGpt ? 0 : 3);
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(0, dp(46), 1);
        buttonLp.leftMargin = dp(8);
        row.addView(first, buttonLp);
        LinearLayout.LayoutParams buttonLp2 = new LinearLayout.LayoutParams(0, dp(46), 1);
        buttonLp2.leftMargin = dp(8);
        row.addView(second, buttonLp2);
        return row;
    }

    private TextView productButton(int index) {
        TextView button = new TextView(this);
        button.setTag(index);
        button.setText(PRODUCT_NAMES[index].replace("ChatGPT ", "").replace("Gemini AI ", ""));
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setMaxLines(1);
        button.setOnClickListener(v -> {
            int chosen = (Integer) v.getTag();
            if (chosen == selectedProduct && PRODUCT_URLS[chosen].equals(currentUrl)) return;
            selectedProduct = chosen;
            currentUrl = PRODUCT_URLS[chosen];
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("url", currentUrl).apply();
            renderProductTabs();
            loadPrices();
        });
        return button;
    }

    private void renderProductTabs() {
        if (productTabs == null) return;
        for (int r = 0; r < productTabs.getChildCount(); r++) {
            LinearLayout row = (LinearLayout) productTabs.getChildAt(r);
            for (int c = 1; c < row.getChildCount(); c++) {
                TextView button = (TextView) row.getChildAt(c);
                int index = (Integer) button.getTag();
                boolean selected = index == selectedProduct;
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(14));
                bg.setColor(selected ? Color.rgb(226, 240, 255) : Color.WHITE);
                bg.setStroke(dp(selected ? 2 : 1), selected ? blue : line);
                button.setBackground(bg);
                button.setTextColor(selected ? blue : ink);
                button.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                button.setElevation(selected ? dp(2) : 0);
            }
        }
    }

    private View makeHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(6), 0, dp(6), 0);
        header.setBackgroundColor(Color.rgb(157, 174, 111));
        header.addView(cell("排名", 0.55f, Gravity.CENTER, true, ink));
        header.addView(cell("地区", 1.35f, Gravity.START, true, ink));
        header.addView(cell("原价", 1.65f, Gravity.CENTER, true, ink));
        header.addView(cell("CNY", 1.0f, Gravity.CENTER, true, ink));
        
        return header;
    }

    private TextView cell(String text, float weight, int gravity, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(12);
        v.setTextColor(color);
        v.setGravity(gravity | Gravity.CENTER_VERTICAL);
        v.setPadding(dp(4), dp(12), dp(4), dp(12));
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setMaxLines(1);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
        return v;
    }


    private void showSettingsInCard() {
        statusView.setVisibility(View.GONE);
        tableHeader.setVisibility(View.GONE);
        priceScroll.setVisibility(View.VISIBLE);
        rowsContainer.removeAllViews();
        rowsContainer.setBackground(new Drawable() {
            private final Paint watermarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Drawable watermark = getDrawable(R.drawable.ic_launcher_foreground);
            @Override public void draw(Canvas canvas) {
                if (watermark == null) return;
                android.graphics.Rect b = getBounds();
                int size = Math.min(dp(220), (int) (Math.min(b.width(), b.height()) * 0.72f));
                if (size <= 0) return;
                int left = b.left + (b.width() - size) / 2;
                int top = b.top + (b.height() - size) / 2;
                watermark.setBounds(left, top, left + size, top + size);
                watermark.setAlpha(22);
                watermark.draw(canvas);
                watermark.setAlpha(255);
            }
            @Override public void setAlpha(int alpha) { watermarkPaint.setAlpha(alpha); }
            @Override public void setColorFilter(android.graphics.ColorFilter filter) { watermarkPaint.setColorFilter(filter); }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(20));
        TextView label = new TextView(this);
        label.setText("重大变动阈值（百分比）");
        label.setTextSize(15);
        label.setTextColor(ink);
        panel.addView(label);
        android.widget.EditText threshold = new android.widget.EditText(this);
        threshold.setSingleLine(true);
        threshold.setInputType(8194);
        float saved = getSharedPreferences(PriceMonitorWorker.PREFS, MODE_PRIVATE).getFloat(PriceMonitorWorker.THRESHOLD_KEY, 5f);
        threshold.setText(String.valueOf((int) saved));
        threshold.setTextColor(ink);
        panel.addView(threshold, new LinearLayout.LayoutParams(-1, dp(48)));
        addSettingsButton(panel, "保存设置并启用每日监控", true, () -> {
            float value;
            try { value = Float.parseFloat(threshold.getText().toString().trim()); } catch (Exception e) { value = 0; }
            if (value < 1 || value > 100) {
                android.widget.Toast.makeText(this, "阈值请设置为 1 到 100", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            org.json.JSONArray urls = new org.json.JSONArray();
            urls.put("https://appstoreprice.org/zh/apps/6448311069");
            urls.put("https://appstoreprice.org/zh/apps/6477489729");
            getSharedPreferences(PriceMonitorWorker.PREFS, MODE_PRIVATE).edit()
                    .putString(PriceMonitorWorker.URLS_KEY, urls.toString())
                    .putFloat(PriceMonitorWorker.THRESHOLD_KEY, value).apply();
            android.widget.Toast.makeText(this, "设置已保存，每日监控已启用", android.widget.Toast.LENGTH_SHORT).show();
        });
        addSettingsButton(panel, "立即检查一次", false, () -> {
            androidx.work.WorkManager.getInstance(this).enqueue(androidx.work.OneTimeWorkRequest.from(PriceMonitorWorker.class));
            android.widget.Toast.makeText(this, "已提交价格检查任务", android.widget.Toast.LENGTH_SHORT).show();
        });
        String version = "未知";
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) version = info.versionName;
        } catch (Exception ignored) {}
        addSettingsButton(panel, "检查应用更新（v" + version + "）", false, () -> checkForUpdates(true));
        rowsContainer.addView(panel);
    }

    private void addSettingsButton(LinearLayout parent, String text, boolean primary, Runnable action) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(primary ? Color.WHITE : ink);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(primary ? Color.rgb(40, 90, 45) : Color.rgb(238, 239, 218));
        if (!primary) bg.setStroke(dp(1), line);
        button.setBackground(bg);
        button.setElevation(dp(1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(10);
        parent.addView(button, lp);
        button.setOnClickListener(v -> action.run());
    }


    private void checkForUpdates(boolean showUpToDate) {
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.github.com/repos/c8sk8yb5h5-debug/AppStorePriceMonitor/releases/latest");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "PriceSentinel-Android");
                if (connection.getResponseCode() != 200) throw new Exception("GitHub API HTTP " + connection.getResponseCode());
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                JSONObject release = new JSONObject(json.toString());
                String tag = release.optString("tag_name", "");
                String latest = tag.startsWith("v") ? tag.substring(1) : tag;
                String notes = release.optString("body", "此版本没有更新说明。");
                String downloadUrl = "";
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset != null && asset.optString("name", "").equals("app-release.apk")) {
                            downloadUrl = asset.optString("browser_download_url", "");
                            break;
                        }
                    }
                }
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                String current = info.versionName == null ? "0.1.0" : info.versionName;
                final String apkUrl = downloadUrl;
                final String releaseTag = tag;
                final String releaseNotes = notes;
                final String latestVersion = latest;
                final String currentVersion = current;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (compareVersions(latestVersion, currentVersion) > 0 && !apkUrl.isEmpty()) {
                        new AlertDialog.Builder(this)
                            .setTitle("发现新版本 " + latestVersion)
                            .setMessage("当前版本：" + currentVersion + "\n\n更新说明：\n" + releaseNotes + "\n\n下载并安装更新？")
                            .setNegativeButton("稍后", null)
                            .setPositiveButton("下载更新", (d, w) -> downloadUpdate(apkUrl, releaseTag))
                            .show();
                    } else if (showUpToDate) {
                        Toast.makeText(this, "当前已是最新版本（" + currentVersion + "）", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (showUpToDate && !isFinishing())
                        Toast.makeText(this, "检查更新失败，请检查网络后重试", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private int compareVersions(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? parseVersionPart(x[i]) : 0;
            int yi = i < y.length ? parseVersionPart(y[i]) : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        return 0;
    }

    private int parseVersionPart(String value) {
        String digits = value.replaceAll("[^0-9].*$", "");
        try { return Integer.parseInt(digits); } catch (Exception e) { return 0; }
    }

    private void downloadUpdate(String url, String tag) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("需要允许安装应用")
                .setMessage("请允许“价格哨兵”安装应用，返回后重新点击“检查应用更新”。")
                .setNegativeButton("取消", null)
                .setPositiveButton("打开设置", (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).show();
            return;
        }
        Toast.makeText(this, "正在下载更新包…", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            File temp = null;
            try {
                temp = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "price-sentinel-update.apk");
                if (temp.exists()) temp.delete();
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "PriceSentinel-Android");
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);
                try (java.io.InputStream in = conn.getInputStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(temp)) {
                    byte[] buffer = new byte[16384];
                    int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    out.flush();
                } finally {
                    conn.disconnect();
                }
                if (!temp.exists() || temp.length() < 100000) throw new Exception("APK 文件不完整");
                File apk = temp;
                runOnUiThread(() -> {
                    try {
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
                        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        install.setData(uri);
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                        startActivity(install);
                    } catch (Exception e) {
                        Toast.makeText(this, "已下载更新包，但无法打开安装器，请允许本应用安装未知应用后重试", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                if (temp != null && temp.exists()) temp.delete();
                runOnUiThread(() -> Toast.makeText(this, "更新下载失败，请检查网络后重试", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadPrices() {
        if (selectedProduct == 4) return;
        statusView.setText("正在更新 " + PRODUCT_NAMES[selectedProduct] + " 价格数据…");
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(12);
        final String requestedUrl = currentUrl;
        new Thread(() -> {
            try {
                String html = Jsoup.connect(requestedUrl)
                        .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                        .timeout(20000).get().html();
                List<PriceRow> prices = PricePageParser.parse(html);
                runOnUiThread(() -> {
                    if (isFinishing() || !requestedUrl.equals(currentUrl)) return;
                    progress.setVisibility(View.GONE);
                    rowsContainer.removeAllViews();
                    if (prices.isEmpty()) {
                        statusView.setText("暂时无法解析价格表，请点击刷新重试。");
                        TextView empty = new TextView(this);
                        empty.setText("没有读取到地区价格。网站页面结构可能已变化，请稍后重试。");
                        empty.setTextColor(muted);
                        empty.setPadding(dp(18), dp(24), dp(18), dp(24));
                        rowsContainer.addView(empty);
                        return;
                    }
                    statusView.setText("已更新 · " + prices.size() + " 个地区 · 按人民币价格排序");
                    for (PriceRow price : prices) rowsContainer.addView(makeRow(price));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || !requestedUrl.equals(currentUrl)) return;
                    progress.setVisibility(View.GONE);
                    statusView.setText("加载失败，请检查网络后点击刷新。");
                    TextView error = new TextView(this);
                    error.setText("无法连接价格数据源。请确认网络可访问 appstoreprice.org。");
                    error.setTextColor(muted);
                    error.setPadding(dp(18), dp(24), dp(18), dp(24));
                    rowsContainer.removeAllViews();
                    rowsContainer.addView(error);
                });
            }
        }).start();
    }

    private View makeRow(PriceRow row) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(6), 0, dp(6), 0);
        boolean lowest = row.status.contains("最低") || row.status.equalsIgnoreCase("Lowest");
        item.setBackgroundColor(lowest ? Color.rgb(177, 201, 137) : Color.rgb(157, 174, 111));
        item.addView(cell(String.valueOf(row.rank), 0.55f, Gravity.CENTER, false, ink));
        item.addView(cell(row.country, 1.30f, Gravity.START, false, ink));
        item.addView(cell(row.originalPrice, 1.65f, Gravity.CENTER, false, ink));
        item.addView(cell(row.cnyPrice, 1.0f, Gravity.CENTER, true, lowest ? Color.rgb(18, 91, 44) : Color.BLACK));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(135, 154, 98));
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(item, new LinearLayout.LayoutParams(-1, -2));
        wrapper.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return wrapper;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (swipeDetector != null) swipeDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private FrameLayout makeBottomNavigation() {
        FrameLayout shell = new FrameLayout(this);
        navItems = new LinearLayout(this);
        navItems.setGravity(Gravity.CENTER);
        navItems.setOrientation(LinearLayout.HORIZONTAL);
        navItems.setPadding(0, dp(4), 0, dp(2));
        navItems.setBackgroundColor(Color.TRANSPARENT);
        navShape = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                int[] order = {0, 1, 2, 3, 4};
                int slot = 0;
                for (int i = 0; i < order.length; i++) if (order[i] == selectedProduct) slot = i;
                float cx = w / 2f - dp(160) + dp(64) * (slot + 0.5f);
                float notch = dp(43), top = dp(9), r = dp(36);
                Path path = new Path();
                path.moveTo(0, top + r);
                path.quadTo(0, top, r, top);
                path.lineTo(Math.max(r, cx - notch), top);
                path.cubicTo(cx - dp(29), top, cx - dp(26), top + dp(24), cx, top + dp(24));
                path.cubicTo(cx + dp(26), top + dp(24), cx + dp(29), top, cx + notch, top);
                path.lineTo(w - r, top);
                path.quadTo(w, top, w, top + r);
                path.lineTo(w, h - r);
                path.quadTo(w, h, w - r, h);
                path.lineTo(r, h);
                path.quadTo(0, h, 0, h - r);
                path.close();
                paint.setColor(Color.BLACK);
                canvas.drawPath(path, paint);
            }
        };
        FrameLayout.LayoutParams shapeLp = new FrameLayout.LayoutParams(-1, dp(70), Gravity.BOTTOM);
        shell.addView(navShape, shapeLp);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(dp(320), dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        shell.addView(navItems, barLp);

        String[] symbols = {"GPT", "GPT", "✦", "✧", "⚙"};
        String[] descriptions = {"ChatGPT Plus", "ChatGPT Go", "Gemini Pro", "Gemini Plus", "设置"};
        int[] productIndex = {0, 1, 2, 3, 4};
        for (int i = 0; i < symbols.length; i++) {
            final int index = productIndex[i];
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView icon;
            if (i < 2) {
                icon = new ChatGptNavIcon(i == 0 ? "＋" : "G");
            } else {
                icon = new TextView(this);
                icon.setText(symbols[i]);
                icon.setTextSize(22);
                icon.setGravity(Gravity.CENTER);
                icon.setTextColor(Color.rgb(225, 227, 218));
            }
            icon.setContentDescription(descriptions[i]);
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(42)));
            View dot = new View(this);
            item.addView(dot, new LinearLayout.LayoutParams(dp(4), dp(4)));
            item.setOnClickListener(v -> {
                selectProduct(index, index >= selectedProduct ? 1 : -1);
            });
            navItems.addView(item, new LinearLayout.LayoutParams(dp(64), -1));
        }

        floatingActiveIcon = new ChatGptNavIcon("＋");
        floatingActiveIcon.setGravity(Gravity.CENTER);
        floatingActiveIcon.setTextSize(25);
        floatingActiveIcon.setTextColor(Color.rgb(18, 122, 92));
        floatingActiveIcon.setElevation(dp(6));
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(Color.rgb(255, 255, 249));
        floatingActiveIcon.setBackground(bubble);
        floatingActiveIcon.setContentDescription("当前订阅方案");
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(dp(62), dp(62), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        shell.addView(floatingActiveIcon, bubbleLp);
        return shell;
    }

    private void renderBottomNavigation() {
        if (bottomNav == null || navItems == null || floatingActiveIcon == null) return;
        String[] symbols = {"GPT", "GPT", "✦", "✧", "⚙"};
        int[] productIndex = {0, 1, 2, 3, 4};
        int selectedSlot = 0;
        for (int i = 0; i < navItems.getChildCount(); i++) {
            LinearLayout item = (LinearLayout) navItems.getChildAt(i);
            TextView icon = (TextView) item.getChildAt(0);
            boolean selected = productIndex[i] == selectedProduct;
            if (selected) selectedSlot = i;
            icon.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
            icon.setTextColor(Color.rgb(205, 208, 200));
            item.animate().scaleX(1f).scaleY(1f).translationY(0).setDuration(180).start();
            View dot = item.getChildAt(1);
            dot.setBackgroundColor(Color.TRANSPARENT);
        }
        String glyph = symbols[selectedSlot];
        final int targetSlot = selectedSlot;
        if (targetSlot < 2) {
            ((ChatGptNavIcon) floatingActiveIcon).setBadge(targetSlot == 0 ? "＋" : "G");
            ((ChatGptNavIcon) floatingActiveIcon).setLogoVisible(true);
            floatingActiveIcon.setText("");
        } else {
            ((ChatGptNavIcon) floatingActiveIcon).setLogoVisible(false);
            floatingActiveIcon.setText(glyph);
            floatingActiveIcon.setTextSize(25);
        }
        if (navShape != null) navShape.invalidate();
        floatingActiveIcon.post(() -> {
            float centerX = bottomNav.getWidth() / 2f - dp(160) + dp(64) * (targetSlot + 0.5f);
            float targetX = centerX - bottomNav.getWidth() / 2f;
            // Keep the bubble at its previous horizontal position, then lift it into the new slot.
            floatingActiveIcon.animate().cancel();
            float startX = floatingActiveIcon.getTranslationX();
            floatingActiveIcon.setTranslationX(startX);
            floatingActiveIcon.setTranslationY(dp(30));
            floatingActiveIcon.setScaleX(0.72f);
            floatingActiveIcon.setScaleY(0.72f);
            floatingActiveIcon.animate()
                    .translationX(targetX).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(560)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.8f))
                    .withEndAction(() -> floatingActiveIcon.animate().scaleX(1.04f).scaleY(0.96f)
                            .setDuration(90).withEndAction(() -> floatingActiveIcon.animate()
                                    .scaleX(1f).scaleY(1f).setDuration(140).start()).start())
                    .start();
        });
    }

    private void selectProduct(int index, int direction) {
        if (index < 0 || index >= PRODUCT_URLS.length || index == selectedProduct || changingProduct) return;
        changingProduct = true;
        selectedProduct = index;
        if (index < 4) currentUrl = PRODUCT_URLS[index];
        currentHeading.setText(PRODUCT_NAMES[selectedProduct].toUpperCase());
        if (selectedProduct < 4) getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("url", currentUrl).apply();
        renderBottomNavigation();
        if (navShape != null) navShape.invalidate();
        if (priceScroll != null && priceScroll.getParent() != null) {
            View card = (View) priceScroll.getParent();
            if (card.getBackground() != null) card.getBackground().invalidateSelf();
        }
        if (selectedProduct == 4) showSettingsInCard(); else {
            rowsContainer.setBackgroundColor(Color.TRANSPARENT);
            statusView.setVisibility(View.VISIBLE);
            tableHeader.setVisibility(View.VISIBLE);
            priceScroll.setVisibility(View.VISIBLE);
            loadPrices();
        }
        // No sideways content movement: only the selected navigation bubble performs the jelly lift.
        priceScroll.animate().alpha(0.94f).setDuration(100).withEndAction(() ->
                priceScroll.animate().alpha(1f).setDuration(180)
                        .withEndAction(() -> changingProduct = false).start()).start();
    }

    @Override public void onBackPressed() { super.onBackPressed(); }

    private GradientDrawable bubbleDrawable() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.rgb(255, 255, 249));
        return bg;
    }

    private final class ChatGptNavIcon extends TextView {
        private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String badge;
        private boolean logoVisible = true;
        ChatGptNavIcon(String badge) {
            super(PriceRankingActivity.this);
            this.badge = badge;
            setGravity(Gravity.CENTER);
            setText("");
        }
        void setBadge(String value) { badge = value; invalidate(); }
        void setLogoVisible(boolean value) { logoVisible = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!logoVisible) return;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = dp(5), ring = dp(4);
            logoPaint.setStyle(Paint.Style.STROKE);
            logoPaint.setStrokeWidth(dp(2));
            logoPaint.setStrokeCap(Paint.Cap.ROUND);
            logoPaint.setStrokeJoin(Paint.Join.ROUND);
            logoPaint.setColor(Color.rgb(16, 163, 127));
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60);
                float x = cx + (float)Math.cos(a) * ring;
                float y = cy + (float)Math.sin(a) * ring;
                canvas.drawCircle(x, y, radius, logoPaint);
            }
            logoPaint.setStyle(Paint.Style.FILL);
            logoPaint.setColor(Color.rgb(16, 163, 127));
            canvas.drawCircle(cx, cy, dp(2), logoPaint);
            float bx = cx + dp(9), by = cy + dp(9);
            logoPaint.setColor(Color.rgb(255, 220, 122));
            canvas.drawCircle(bx, by, dp(5), logoPaint);
            logoPaint.setColor(Color.rgb(27, 37, 20));
            logoPaint.setTextAlign(Paint.Align.CENTER);
            logoPaint.setTextSize(dp(7));
            logoPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(badge, bx, by + dp(2), logoPaint);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
