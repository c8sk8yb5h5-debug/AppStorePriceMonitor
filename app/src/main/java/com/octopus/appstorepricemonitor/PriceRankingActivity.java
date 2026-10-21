package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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
    // The four plan URLs are kept in one place so they can be adjusted if the source site changes its plan IDs.
    private static final String[] PRODUCT_NAMES = {"ChatGPT Go", "ChatGPT Plus", "Gemini AI Pro", "Gemini AI Plus"};
    private static final String[] PRODUCT_URLS = {
        "https://appstoreprice.org/zh/apps/6448311069?plan=1",
        "https://appstoreprice.org/zh/apps/6448311069?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=1"
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
    private final int page = Color.rgb(247, 249, 253);
    private final int blue = Color.rgb(22, 138, 245);
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
        for (int i = 0; i < PRODUCT_URLS.length; i++) if (PRODUCT_URLS[i].equals(currentUrl)) selectedProduct = i;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(page);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(dp(12), top + dp(4), dp(12), bottom);
            return insets;
        });

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), dp(4), dp(4), dp(6));
        TextView spacer = new TextView(this);
        spacer.setText("价格哨兵");
        spacer.setTextColor(ink);
        spacer.setTextSize(18);
        spacer.setTypeface(null, Typeface.BOLD);
        toolbar.addView(spacer, new LinearLayout.LayoutParams(0, dp(46), 1));
        TextView settings = circleAction("⚙", "监控设置");
        settings.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        toolbar.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView refresh = circleAction("↻", "刷新");
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        refreshLp.leftMargin = dp(10);
        toolbar.addView(refresh, refreshLp);
        refresh.setOnClickListener(v -> { refresh.animate().rotationBy(360).setDuration(360).start(); loadPrices(); });
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));

        productTabs = new LinearLayout(this);
        productTabs.setOrientation(LinearLayout.VERTICAL);
        productTabs.setPadding(0, dp(4), 0, dp(10));
        productTabs.addView(makeProductRow(true), new LinearLayout.LayoutParams(-1, dp(58)));
        productTabs.addView(makeProductRow(false), new LinearLayout.LayoutParams(-1, dp(58)));
        root.addView(productTabs, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
        statusView = new TextView(this);
        statusView.setText("正在读取全球价格…");
        statusView.setTextSize(12);
        statusView.setTextColor(muted);
        statusView.setPadding(dp(4), dp(8), dp(4), dp(8));
        root.addView(statusView);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(true);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackgroundColor(Color.WHITE);
        table.setMinimumWidth(getResources().getDisplayMetrics().widthPixels - dp(24));
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
