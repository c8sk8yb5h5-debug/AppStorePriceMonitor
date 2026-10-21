package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import org.jsoup.Jsoup;
import java.util.List;

public class PriceRankingActivity extends Activity {
    private static final String PREFS = "price_ranking";
    private static final String DEFAULT_URL = "https://appstoreprice.org/zh/apps/6448311069?plan=1";
    private static final String[] PRODUCT_NAMES = {"ChatGPT Go", "ChatGPT Plus", "Gemini AI Plus", "Gemini AI Pro"};
    private static final String[] PRODUCT_URLS = {
        "https://appstoreprice.org/zh/apps/6448311069?plan=1",
        "https://appstoreprice.org/zh/apps/6448311069?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=1",
        "https://appstoreprice.org/zh/apps/6477489729?plan=0"
    };

    private LinearLayout rowsContainer;
    private LinearLayout productTabs;
    private TextView statusView;
    private ProgressBar progress;
    private String currentUrl;
    private int selectedProduct = 0;

    private final int ink = Color.rgb(27, 37, 56);
    private final int muted = Color.rgb(104, 117, 139);
    private final int line = Color.rgb(222, 229, 238);
    private final int page = Color.rgb(248, 250, 253);
    private final int blue = Color.rgb(22, 138, 245);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(page);
        getWindow().setNavigationBarColor(page);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        currentUrl = getIntent().getStringExtra("url");
        if (currentUrl == null || !currentUrl.startsWith("https://appstoreprice.org/zh/apps/")) {
            currentUrl = getSharedPreferences(PREFS, MODE_PRIVATE).getString("url", DEFAULT_URL);
        }
        for (int i = 0; i < PRODUCT_URLS.length; i++) {
            if (PRODUCT_URLS[i].equals(currentUrl)) selectedProduct = i;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(page);
        // Android 15+/Harmony compatibility may lay content behind the system status bar.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(0, top, 0, bottom);
            return insets;
        });

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(2), dp(10), dp(2));
        TextView settings = new TextView(this);
        settings.setText("监控设置");
        settings.setTextSize(14);
        settings.setTextColor(ink);
        settings.setGravity(Gravity.CENTER);
        settings.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        toolbar.addView(settings, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView refresh = new TextView(this);
        refresh.setText("刷新 ↻");
        refresh.setTextSize(14);
        refresh.setTextColor(ink);
        refresh.setGravity(Gravity.CENTER);
        refresh.setOnClickListener(v -> loadPrices());
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(82), dp(44)));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(48)));

        productTabs = new LinearLayout(this);
        productTabs.setOrientation(LinearLayout.VERTICAL);
        productTabs.setPadding(dp(10), dp(2), dp(10), dp(7));
        addProductTabRow(productTabs, 0, 1);
        addProductTabRow(productTabs, 2, 3);
        root.addView(productTabs, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        statusView = new TextView(this);
        statusView.setText("正在读取全球价格…");
        statusView.setTextSize(12);
        statusView.setTextColor(muted);
        statusView.setPadding(dp(14), dp(7), dp(14), dp(7));
        root.addView(statusView);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(true);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundColor(Color.WHITE);
        table.setMinimumWidth(getResources().getDisplayMetrics().widthPixels);
        table.addView(makeHeader());
        rowsContainer = new LinearLayout(this);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        table.addView(rowsContainer, new LinearLayout.LayoutParams(-1, -2));
        horizontal.addView(table);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(horizontal);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        root.requestApplyInsets();
        renderProductTabs();
        loadPrices();
    }

    private void addProductTabRow(LinearLayout parent, int first, int second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView a = productButton(first);
        TextView b = productButton(second);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(a, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(40), 1);
        lp2.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(b, lp2);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private TextView productButton(int index) {
        TextView button = new TextView(this);
        button.setTag(index);
        button.setText(PRODUCT_NAMES[index]);
        button.setTextSize(13);
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
            View rowView = productTabs.getChildAt(r);
            if (!(rowView instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) rowView;
            for (int c = 0; c < row.getChildCount(); c++) {
                TextView button = (TextView) row.getChildAt(c);
                int index = (Integer) button.getTag();
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(10));
                if (index == selectedProduct) {
                    bg.setColor(Color.rgb(226, 240, 255));
                    bg.setStroke(dp(1), blue);
                    button.setTextColor(blue);
                    button.setTypeface(null, Typeface.BOLD);
                } else {
                    bg.setColor(Color.WHITE);
                    bg.setStroke(dp(1), line);
                    button.setTextColor(ink);
                    button.setTypeface(null, Typeface.NORMAL);
                }
                button.setBackground(bg);
            }
        }
    }

    private View makeHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(6), 0, dp(6), 0);
        header.setBackgroundColor(Color.rgb(242, 246, 251));
        header.addView(cell("排名", 0.55f, Gravity.CENTER, true, muted));
        header.addView(cell("地区", 1.35f, Gravity.START, true, muted));
        header.addView(cell("原价", 1.65f, Gravity.CENTER, true, muted));
        header.addView(cell("CNY", 1.0f, Gravity.CENTER, true, muted));
        header.addView(cell("状态", 0.75f, Gravity.CENTER, true, muted));
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

    private void loadPrices() {
        statusView.setText("正在更新 " + PRODUCT_NAMES[selectedProduct] + " 价格数据…");
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(12);
        rowsContainer.removeAllViews();
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
        item.setBackgroundColor(lowest ? Color.rgb(241, 251, 246) : Color.WHITE);
        item.addView(cell(String.valueOf(row.rank), 0.55f, Gravity.CENTER, false, muted));
        item.addView(cell(row.country, 1.35f, Gravity.START, false, ink));
        item.addView(cell(row.originalPrice, 1.65f, Gravity.CENTER, false, muted));
        item.addView(cell(row.cnyPrice, 1.0f, Gravity.CENTER, true, lowest ? Color.rgb(0, 158, 83) : ink));
        TextView badge = cell(lowest ? "最低" : (row.status.contains("最高") || row.status.equalsIgnoreCase("Highest") ? "最高" : ""), 0.75f, Gravity.CENTER, true,
                lowest ? Color.rgb(0, 145, 75) : muted);
        if (lowest) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(215, 249, 229));
            bg.setCornerRadius(dp(20));
            badge.setBackground(bg);
            badge.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        item.addView(badge);
        View divider = new View(this);
        divider.setBackgroundColor(line);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(item, new LinearLayout.LayoutParams(-1, -2));
        wrapper.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return wrapper;
    }

    @Override public void onBackPressed() { super.onBackPressed(); }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
