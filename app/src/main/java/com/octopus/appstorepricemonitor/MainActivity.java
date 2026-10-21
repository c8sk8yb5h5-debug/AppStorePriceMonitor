package com.octopus.appstorepricemonitor;

import android.Manifest;
import android.app.Activity;
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
import org.json.JSONArray;

public class MainActivity extends Activity {
    private EditText urlsInput;
    private EditText thresholdInput;
    private TextView status;
    private static final int NOTIFICATION_PERMISSION = 1001;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createChannel();
        requestNotificationPermission();
        buildUi();
        scheduleDailyCheck();
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
        status = new TextView(this); status.setTextColor(0xFF5B6472); status.setTextSize(13); addSpace(root, 14); root.addView(status);
        TextView caveat = new TextView(this); caveat.setText("注意：数据通过网页表格解析，网站结构变化可能导致读取失败。价格以当地货币比较，人民币折算波动不会单独触发提醒。后台检查频率受 Android / 鸿蒙兼容环境调度与省电策略影响。"); caveat.setTextSize(12); caveat.setTextColor(0xFF777F8B); addSpace(root, 16); root.addView(caveat);
        setContentView(root);
        save.setOnClickListener(v -> saveSettings());
        check.setOnClickListener(v -> { saveSettings(); WorkManager.getInstance(this).enqueue(OneTimeWorkRequest.from(PriceMonitorWorker.class)); status.setText("已提交立即检查任务。首次成功检查只会记录价格基线。"); });
        updateStatus();
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
