package com.octopus.appstorepricemonitor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PriceMonitorWorker extends Worker {
    public static final String PREFS = "price_sentinel";
    public static final String URLS_KEY = "watch_urls";
    public static final String THRESHOLD_KEY = "threshold_percent";
    private static final String BASELINE_KEY = "price_baseline_v2";
    private static final String CHANNEL_ID = "price_changes";

    public PriceMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String urlsJson = prefs.getString(URLS_KEY, "");
        if (urlsJson == null || urlsJson.trim().isEmpty()) return Result.success();
        JSONArray urls;
        JSONObject previous;
        try { urls = new JSONArray(urlsJson); previous = new JSONObject(prefs.getString(BASELINE_KEY, "{}")); }
        catch (Exception e) { return Result.failure(); }
        double threshold = Math.max(1, prefs.getFloat(THRESHOLD_KEY, 5f));
        JSONObject current = new JSONObject();
        int changed = 0;
        boolean hasAnyRows = false;
        try {
            for (int i = 0; i < urls.length(); i++) {
                String url = urls.optString(i, "").trim();
                if (!isAllowedUrl(url)) continue;
                String html = Jsoup.connect(url).userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/123 Mobile Safari/537.36")
                        .timeout(25000).followRedirects(true).ignoreHttpErrors(false).get().html();
                List<PriceRow> rows = PricePageParser.parse(html);
                if (rows.isEmpty()) continue; // Never replace a valid baseline with an empty/failed parse.
                hasAnyRows = true;
                String appLabel = titleForUrl(url);
                JSONObject oldApp = previous.optJSONObject(url);
                JSONObject newApp = new JSONObject();
                for (PriceRow row : rows) {
                    String key = row.country + "|" + row.currency;
                    JSONObject val = new JSONObject();
                    val.put("amount", row.amount);
                    val.put("shown", row.displayedPrice);
                    newApp.put(key, val);
                    if (oldApp == null) continue; // first successful run establishes a baseline
                    JSONObject oldVal = oldApp.optJSONObject(key);
                    if (oldVal == null || !row.currency.equals(oldVal.optString("currency", row.currency))) continue;
                    double oldAmount = oldVal.optDouble("amount", row.amount);
                    if (oldAmount <= 0 || Math.abs(row.amount - oldAmount) < 0.000001) continue;
                    double pct = Math.abs(row.amount - oldAmount) / oldAmount * 100.0;
                    if (pct >= threshold) {
                        sendNotification(ctx, appLabel, row, oldAmount, pct, url);
                        changed++;
                    }
                }
                current.put(url, newApp);
            }
        } catch (Exception e) {
            return Result.retry();
        }
        if (!hasAnyRows) return Result.retry();
        // Preserve prior app baselines if one monitored page temporarily fails.
        for (int i = 0; i < urls.length(); i++) {
            String url = urls.optString(i, "").trim();
            if (!current.has(url) && previous.has(url)) {
                try { current.put(url, previous.getJSONObject(url)); } catch (Exception ignored) {}
            }
        }
        // Mark currency explicitly for future comparisons.
        try {
            for (int i = 0; i < urls.length(); i++) {
                String url = urls.optString(i, "").trim();
                JSONObject app = current.optJSONObject(url);
                if (app == null) continue;
                for (String key : app.keySet()) {
                    JSONObject v = app.optJSONObject(key);
                    if (v != null) v.put("currency", key.substring(key.lastIndexOf('|') + 1));
                }
            }
            prefs.edit().putString(BASELINE_KEY, current.toString()).putLong("last_check_ms", System.currentTimeMillis()).apply();
        } catch (Exception e) { return Result.retry(); }
        return Result.success();
    }

    private static boolean isAllowedUrl(String s) {
        try { URL u = new URL(s); return "https".equalsIgnoreCase(u.getProtocol()) && "appstoreprice.org".equalsIgnoreCase(u.getHost()) && u.getPath().matches("/zh/apps/[0-9]+/?"); }
        catch (Exception e) { return false; }
    }

    private static String titleForUrl(String url) {
        if (url.contains("6448311069")) return "ChatGPT Plus";
        if (url.contains("6477489729")) return "Google AI Pro";
        return "App Store 订阅价格";
    }

    private static void sendNotification(Context ctx, String title, PriceRow row, double oldAmount, double pct, String url) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "订阅价格变动", NotificationManager.IMPORTANCE_DEFAULT));
        String direction = row.amount < oldAmount ? "降价" : "涨价";
        String text = row.country + "：" + row.currency + " " + fmt(oldAmount) + " → " + fmt(row.amount) + "（" + direction + " " + String.format(Locale.ROOT, "%.1f%%", pct) + "）";
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title + "价格变动")
                .setContentText(text).setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\n查看：" + url))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true);
        nm.notify((title + row.country + row.currency).hashCode(), b.build());
    }
    private static String fmt(double v) { return v == Math.rint(v) ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.2f", v); }
}
