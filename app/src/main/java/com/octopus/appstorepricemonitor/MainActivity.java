package com.octopus.appstorepricemonitor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.core.content.FileProvider;
import java.io.File;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends Activity {
    private EditText urlsInput;
    private EditText thresholdInput;
    private TextView status;
    private static final int NOTIFICATION_PERMISSION = 1001;
    private long updateDownloadId = -1;
    private BroadcastReceiver updateReceiver;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createChannel();
        requestNotificationPermission();
        buildUi();
        scheduleDailyCheck();
        registerUpdateReceiver();
        checkForUpdates(false);
    }

    private void buildUi() {
        int pad = dp(18);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFF7F8FA);
        TextView title = new TextView(this); title.setText("价格哨兵"); title.setTextSize(26); title.setTextColor(0xFF172033); title.setTypeface(null, 1); root.addView(title);
        TextView intro = new TextView(this); intro.setText("监控 AppStorePrice.org 多地区订阅价格。首次成功检查只建立基线；之后仅在价格变化达到阈值时发送系统通知。建议允许本应用后台运行。"); intro.setTextSize(14); intro.setTextColor(0xFF5B6472); addSpace(root, 8); root.addView(intro);
        TextView urlLabel = new TextView(this); urlLabel.setText("监控页面（每行一个链接）"); urlLabel.setTextSize(15); urlLabel.setTextColor(0xFF172033); addSpace(root, 20); root.addView(urlLabel);
        urlsInput = new EditText(this); urlsInput.setMinLines(4); urlsInput.setGravity(0x00800003 | 0x00000010); urlsInput.setText("https://appstoreprice.org/zh/apps/6448311069\nhttps://appstoreprice.org/zh/apps/6477489729"); urlsInput.setHint("https://appstoreprice.org/zh/apps/应用ID"); root.addView(urlsInput, new LinearLayout.LayoutParams(-1, dp(140)));
        TextView thresholdLabel = new TextView(this); thresholdLabel.setText("重大变动阈值（百分比）"); thresholdLabel.setTextSize(15); thresholdLabel.setTextColor(0xFF172033); addSpace(root, 16); root.addView(thresholdLabel);
        thresholdInput = new EditText(this); thresholdInput.setInputType(8194); thresholdInput.setText("5"); thresholdInput.setHint("例如 5 表示 5%"); root.addView(thresholdInput, new LinearLayout.LayoutParams(-1, dp(52)));
        Button save = new Button(this); save.setText("保存设置并启用每日监控"); addSpace(root, 16); root.addView(save, new LinearLayout.LayoutParams(-1, dp(52)));
        Button check = new Button(this); check.setText("立即检查一次"); root.addView(check, new LinearLayout.LayoutParams(-1, dp(52)));
        Button update = new Button(this); update.setText("检查应用更新"); root.addView(update, new LinearLayout.LayoutParams(-1, dp(52)));
        status = new TextView(this); status.setTextColor(0xFF5B6472); status.setTextSize(13); addSpace(root, 14); root.addView(status);
        TextView caveat = new TextView(this); caveat.setText("注意：数据通过网页表格解析，网站结构变化可能导致读取失败。价格以当地货币比较，人民币折算波动不会单独触发提醒。后台检查频率受 Android / 鸿蒙兼容环境调度与省电策略影响。"); caveat.setTextSize(12); caveat.setTextColor(0xFF777F8B); addSpace(root, 16); root.addView(caveat);
        setContentView(root);
        save.setOnClickListener(v -> saveSettings());
        check.setOnClickListener(v -> { saveSettings(); WorkManager.getInstance(this).enqueue(OneTimeWorkRequest.from(PriceMonitorWorker.class)); status.setText("已提交立即检查任务。首次成功检查只会记录价格基线。"); });
        update.setOnClickListener(v -> checkForUpdates(true));
        updateStatus();
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();
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
                        new AlertDialog.Builder(this).setTitle("发现新版本 " + latestVersion)
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
                    if (showUpToDate && !isFinishing()) Toast.makeText(this, "检查更新失败，请检查网络后重试", Toast.LENGTH_LONG).show();
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
        if (android.os.Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("需要允许安装应用")
                .setMessage("请允许“价格哨兵”安装应用，返回后再次点击检查更新。")
                .setNegativeButton("取消", null)
                .setPositiveButton("打开设置", (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).show();
            return;
        }
        try {
            File target = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "price-sentinel-update.apk");
            if (target.exists()) target.delete();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle("价格哨兵 " + tag)
                .setDescription("正在下载应用更新")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "price-sentinel-update.apk");
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            updateDownloadId = manager.enqueue(request);
            Toast.makeText(this, "已开始下载更新", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "无法开始下载，请稍后重试", Toast.LENGTH_LONG).show();
        }
    }

    private void registerUpdateReceiver() {
        updateReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != updateDownloadId) return;
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id));
                boolean success = false;
                if (cursor != null) {
                    if (cursor.moveToFirst()) success = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                    cursor.close();
                }
                if (!success) {
                    Toast.makeText(MainActivity.this, "更新下载失败，请检查网络后重试", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    File apk = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "price-sentinel-update.apk");
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", apk);
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setDataAndType(uri, "application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开系统安装器，请重试", Toast.LENGTH_LONG).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updateReceiver, filter);
    }

    @Override protected void onDestroy() {
        if (updateReceiver != null) {
            try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    private void saveSettings() {
        String[] lines = urlsInput.getText().toString().split("\\r?\\n");
        JSONArray arr = new JSONArray();
        for (String line : lines) {
            String s = line.trim();
            if (s.matches("https://appstoreprice\\.org/zh/apps/[0-9]+/?") && !contains(arr, s)) arr.put(s);
        }
        if (arr.length() == 0) { Toast.makeText(this, "请至少填写一个有效的 AppStorePrice.org 应用详情链接", Toast.LENGTH_LONG).show(); return; }
        float threshold = 5f;
        try { threshold = Float.parseFloat(thresholdInput.getText().toString().trim()); } catch (Exception ignored) {}
        if (threshold < 1 || threshold > 100) { Toast.makeText(this, "阈值请设置为 1 到 100 之间", Toast.LENGTH_SHORT).show(); return; }
        getSharedPreferences(PriceMonitorWorker.PREFS, MODE_PRIVATE).edit().putString(PriceMonitorWorker.URLS_KEY, arr.toString()).putFloat(PriceMonitorWorker.THRESHOLD_KEY, threshold).apply();
        scheduleDailyCheck();
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        status.setText("监控页面：" + arr.length() + " 个；重大变动阈值：" + threshold + "%；检查频率：约每 24 小时一次。");
    }

    private void scheduleDailyCheck() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PriceMonitorWorker.class, 24, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("price-sentinel-daily", ExistingPeriodicWorkPolicy.UPDATE, request);
    }
    private void updateStatus() {
        long last = getSharedPreferences(PriceMonitorWorker.PREFS, MODE_PRIVATE).getLong("last_check_ms", 0);
        status.setText(last == 0 ? "尚未成功检查。点击“立即检查一次”以建立价格基线。" : "上次成功检查时间：" + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", last));
    }
    private void createChannel() { if (Build.VERSION.SDK_INT >= 26) { NotificationManager nm = getSystemService(NotificationManager.class); nm.createNotificationChannel(new NotificationChannel("price_changes", "订阅价格变动", NotificationManager.IMPORTANCE_DEFAULT)); } }
    private void requestNotificationPermission() { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION); }
    private boolean contains(JSONArray arr, String s) { for (int i=0;i<arr.length();i++) if (s.equals(arr.optString(i))) return true; return false; }
    private void addSpace(LinearLayout root, int dp) { TextView s = new TextView(this); s.setHeight(dp(dp)); root.addView(s); }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}
