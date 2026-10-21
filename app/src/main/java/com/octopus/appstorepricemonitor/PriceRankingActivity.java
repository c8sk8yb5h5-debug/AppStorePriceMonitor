package com.octopus.appstorepricemonitor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final String[] PRODUCT_NAMES = {"ChatGPT Go", "ChatGPT Plus", "Gemini AI Pro", "Gemini AI Plus"};
    private static final String[] PRODUCT_URLS = {
        "https://appstoreprice.org/zh/apps/6448311069?plan=1",
        "https://appstoreprice.org/zh/apps/6448311069?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=0",
        "https://appstoreprice.org/zh/apps/6477489729?plan=1"
    };

    private LinearLayout rowsContainer;
    private LinearLayout productTabs;
    private FrameLayout bottomNav;
    private LinearLayout navItems;
    private TextView floatingActiveIcon;
    private TextView currentHeading;
    private View cardNotch;
    private View navShape;
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
        for (int i = 0; i < PRODUCT_URLS.length; i++) if (PRODUCT_URLS[i].equals(currentUrl)) selectedProduct = i;

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
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.rgb(157, 174, 111));
        cardBg.setCornerRadius(dp(30));
        card.setBackground(cardBg);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(-1, -1);
        cardLp.bottomMargin = dp(52);
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
        table.addView(makeHeader());
        rowsContainer = new LinearLayout(this);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        table.addView(rowsContainer, new LinearLayout.LayoutParams(-1, -2));
        horizontal.addView(table);
        priceScroll = new ScrollView(this);
        priceScroll.setFillViewport(false);
        priceScroll.addView(horizontal);
        card.addView(priceScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        cardNotch = new View(this);
        GradientDrawable notchBg = new GradientDrawable();
        notchBg.setShape(GradientDrawable.OVAL);
        notchBg.setColor(Color.rgb(157, 174, 111));
        cardNotch.setBackground(notchBg);
        FrameLayout.LayoutParams notchLp = new FrameLayout.LayoutParams(dp(104), dp(100), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        notchLp.bottomMargin = dp(2);
        contentFrame.addView(cardNotch, notchLp);

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

    private void loadPrices() {
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
        navItems.setPadding(dp(2), dp(4), dp(2), dp(2));
        navItems.setBackgroundColor(Color.TRANSPARENT);
        navShape = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                int[] order = {1, 0, 2, 3, -1};
                int slot = 0;
                for (int i = 0; i < order.length; i++) if (order[i] == selectedProduct) slot = i;
                float cx = w * (slot + 0.5f) / 5f;
                float notch = dp(35), top = dp(13), r = dp(36);
                Path path = new Path();
                path.moveTo(0, top + r);
                path.quadTo(0, top, r, top);
                path.lineTo(Math.max(r, cx - notch), top);
                path.cubicTo(cx - notch * 0.35f, top, cx - notch * 0.65f, h - dp(5), cx, h - dp(5));
                path.cubicTo(cx + notch * 0.65f, h - dp(5), cx + notch * 0.35f, top, cx + notch, top);
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
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(-1, dp(70), Gravity.BOTTOM);
        shell.addView(navItems, barLp);

        String[] symbols = {"✳", "G", "✦", "✧", "⚙"};
        String[] descriptions = {"ChatGPT Plus", "ChatGPT Go", "Gemini Pro", "Gemini Plus", "设置"};
        int[] productIndex = {1, 0, 2, 3, -1};
        for (int i = 0; i < symbols.length; i++) {
            final int index = productIndex[i];
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView icon = new TextView(this);
            icon.setText(symbols[i]);
            icon.setTextSize(i == 1 ? 19 : 22);
            icon.setGravity(Gravity.CENTER);
            icon.setContentDescription(descriptions[i]);
            icon.setTextColor(Color.rgb(225, 227, 218));
            item.addView(icon, new LinearLayout.LayoutParams(-1, dp(42)));
            View dot = new View(this);
            item.addView(dot, new LinearLayout.LayoutParams(dp(4), dp(4)));
            item.setOnClickListener(v -> {
                if (index < 0) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    selectProduct(index, index >= selectedProduct ? 1 : -1);
                }
            });
            navItems.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }

        floatingActiveIcon = new TextView(this);
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
        String[] symbols = {"✳", "G", "✦", "✧", "⚙"};
        int[] productIndex = {1, 0, 2, 3, -1};
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
        floatingActiveIcon.setText(glyph);
        final int targetSlot = selectedSlot;
        if (navShape != null) navShape.invalidate();
        if (cardNotch != null) {
            cardNotch.post(() -> {
                int frameWidth = ((View) cardNotch.getParent()).getWidth();
                float centerX = frameWidth * (targetSlot + 0.5f) / 5f;
                FrameLayout.LayoutParams notchParams = (FrameLayout.LayoutParams) cardNotch.getLayoutParams();
                notchParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                notchParams.leftMargin = Math.round(centerX - dp(52));
                cardNotch.setLayoutParams(notchParams);
            });
        }
        floatingActiveIcon.post(() -> {
            float centerX = bottomNav.getWidth() * (targetSlot + 0.5f) / 5f;
            float offsetX = centerX - bottomNav.getWidth() / 2f;
            floatingActiveIcon.animate().cancel();
            floatingActiveIcon.setTranslationY(dp(40));
            floatingActiveIcon.setScaleX(0.58f);
            floatingActiveIcon.setScaleY(0.58f);
            floatingActiveIcon.animate().translationX(offsetX).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(520).setInterpolator(new android.view.animation.OvershootInterpolator(1.35f)).start();
        });
    }

    private void selectProduct(int index, int direction) {
        if (index < 0 || index >= PRODUCT_URLS.length || index == selectedProduct || changingProduct) return;
        changingProduct = true;
        int width = Math.max(priceScroll.getWidth(), dp(280));
        priceScroll.animate().translationX(-direction * width * 0.28f).alpha(0.55f)
                .setDuration(150).withEndAction(() -> {
                    selectedProduct = index;
                    currentUrl = PRODUCT_URLS[index];
                    currentHeading.setText(PRODUCT_NAMES[selectedProduct].toUpperCase());
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("url", currentUrl).apply();
                    renderProductTabs();
                    renderBottomNavigation();
                    priceScroll.setTranslationX(direction * width * 0.28f);
                    priceScroll.setAlpha(0.55f);
                    loadPrices();
                    priceScroll.animate().translationX(0).alpha(1f).setDuration(260)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> changingProduct = false).start();
                }).start();
    }

    @Override public void onBackPressed() { super.onBackPressed(); }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
