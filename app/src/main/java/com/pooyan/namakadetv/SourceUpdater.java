package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Opt-in manifest updates: only same-host paths of pre-existing public sources. */
public final class SourceUpdater {
    private static final String PREFS = "pooyan_source_updates";
    private static final long INTERVAL = 24L * 60 * 60 * 1000;
    public interface Callback { void onResult(String message); }
    private final SharedPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public SourceUpdater(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public String getManifestUrl() { return preferences.getString("manifest", ""); }
    public boolean setManifestUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (!trimmed.isEmpty() && (!StreamDetector.isHttpsUrl(trimmed) || trimmed.length() > 1024)) return false;
        preferences.edit().putString("manifest", trimmed).remove("last_checked").apply();
        return true;
    }
    public String overrideFor(SiteCatalog.Site site) {
        String value = preferences.getString("url_" + site.id, "");
        return SourceUpdatePolicy.accept(site.id, value) ? value : "";
    }
    public void check(boolean force, Callback callback) {
        String url = getManifestUrl();
        if (url.isEmpty()) { if (force) callback.onResult("برای به‌روزرسانی، نشانی فایل JSON خودت را در تنظیمات وارد کن."); return; }
        long now = System.currentTimeMillis();
        if (!force && now - preferences.getLong("last_checked", 0) < INTERVAL) return;
        executor.execute(() -> {
            String message;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(7000); connection.setReadTimeout(7000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200) throw new Exception("HTTP " + connection.getResponseCode());
                if (connection.getContentLength() > 32_768) throw new Exception("فایل بیش‌ازحد بزرگ است");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] b = new byte[4096]; int n;
                    while ((n = in.read(b)) != -1) {
                        if (out.size() + n > 32_768) throw new Exception("فایل بیش‌ازحد بزرگ است");
                        out.write(b, 0, n);
                    }
                }
                JSONObject data = new JSONObject(out.toString("UTF-8"));
                if (data.optInt("schema", -1) != 1) throw new Exception("نسخه فهرست پشتیبانی نمی‌شود");
                JSONArray sources = data.optJSONArray("sites");
                if (sources == null || sources.length() > 20) throw new Exception("فهرست نامعتبر است");
                SharedPreferences.Editor edit = preferences.edit(); int accepted=0;
                for (int i=0; i<sources.length(); i++) {
                    JSONObject obj = sources.optJSONObject(i);
                    if (obj == null) continue;
                    String id = obj.optString("id", ""), sourceUrl = obj.optString("url", "");
                    if (SourceUpdatePolicy.accept(id, sourceUrl)) {
                        edit.putString("url_" + id, sourceUrl); accepted++;
                    }
                }
                edit.putLong("last_checked", System.currentTimeMillis()).apply();
                message = accepted + " آدرس معتبر به‌روزرسانی شد. آدرس‌های خصوصی دست‌نخورده‌اند.";
            } catch (Exception e) { message = "دریافت فهرست به‌روزرسانی نشد: " + e.getMessage(); }
            finally { if (connection != null) connection.disconnect(); }
            callback.onResult(message);
        });
    }
    public void close() { executor.shutdownNow(); }
}
