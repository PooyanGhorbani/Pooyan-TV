package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** GitHub stable-release updates and user-selected APKs; install requires Android consent. */
public final class UpdaterActivity extends Activity {
    private static final int API_LIMIT = 400000;
    private static final int PICK_APK = 8841;
    private static final java.util.Set<String> prompted =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView status;
    private Button check, choose;
    private volatile boolean busy;
    private File pendingInstall;

    private static final class Release {
        String tag, url, sha, name, notes;
    }

    private static boolean stablePackage(String name) {
        return "com.pooyan.namakadetv.tv".equals(name);
    }
    /** The v0.8.3+ preview retains its own package and signing lineage. */
    private static boolean previewPackage(String name) {
        return "com.pooyan.namakadetv.dev083catalog".equals(name);
    }
    private static boolean supportedChannel(String name) {
        return stablePackage(name) || previewPackage(name);
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 28, 32, 28);
        layout.setBackgroundColor(0xff0B0D14);
        scroll.addView(layout);
        setContentView(scroll);

        TextView heading = new TextView(this);
        heading.setText("POOYAN TV  ·  مدیریت آپدیت");
        heading.setTextColor(-1);
        heading.setTextSize(24);
        layout.addView(heading);

        TextView version = new TextView(this);
        version.setText("نسخهٔ نصب‌شده: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "شناسه: " + getPackageName() + "\n"
                + (stablePackage(getPackageName()) ? "کانال: پایدار (GitHub Releases)"
                        : previewPackage(getPackageName()) ? "کانال: پیش‌نمایش (GitHub Pre-releases)"
                        : "کانال: پیش‌نمایش قدیمی؛ فقط APK سازگار با شناسه و امضای یکسان"));
        version.setTextColor(0xffdddddd);
        version.setTextSize(15);
        version.setPadding(0, 14, 0, 15);
        layout.addView(version);

        status = new TextView(this);
        status.setTextColor(-1);
        status.setTextSize(17);
        status.setPadding(0, 12, 0, 22);
        status.setText("هنگام هر اجرای تازهٔ برنامه، انتشارهای GitHub یک بار در پس‌زمینه بررسی می‌شوند. "
                + "در این صفحه هر زمان خواستی می‌توانی دوباره بررسی کنی. نصب با تأیید تو و Android انجام می‌شود.");
        layout.addView(status);

        check = new Button(this);
        check.setText("بررسی نسخهٔ جدید در GitHub");
        check.setOnClickListener(v -> checkNow());
        layout.addView(check);

        choose = new Button(this);
        choose.setText("نصب نسخهٔ جدید از فایل APK (حافظه / USB)");
        choose.setOnClickListener(v -> pickLocalApk());
        layout.addView(choose);

        Button close = new Button(this);
        close.setText("بازگشت");
        close.setOnClickListener(v -> finish());
        layout.addView(close);
        check.requestFocus();
    }

    private void show(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed() && status != null) status.setText(message);
        });
    }
    private void setBusy(boolean value) {
        busy = value;
        runOnUiThread(() -> {
            if (check != null) check.setEnabled(!value);
            if (choose != null) choose.setEnabled(!value);
        });
    }

    private static String readLimited(InputStream in, int limit) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (bytes.size() + n > limit) throw new IllegalStateException("پاسخ GitHub بیش از حد بزرگ است");
            bytes.write(buffer, 0, n);
        }
        return bytes.toString("UTF-8");
    }

    private static HttpURLConnection connection(String url) throws Exception {
        String current = url;
        for (int hop = 0; hop < 5; hop++) {
            if (!UpdatePolicy.RELEASES_API.equals(current) && !UpdatePolicy.allowedDownloadHop(current))
                throw new SecurityException("هدایت به سرور ناشناس مسدود شد");
            HttpURLConnection c = (HttpURLConnection) new URL(current).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(12000);
            c.setReadTimeout(16000);
            c.setRequestProperty("User-Agent", "PooyanTV-Updater/0.8.4");
            int response = c.getResponseCode();
            if (response == 301 || response == 302 || response == 303 || response == 307 || response == 308) {
                String location = c.getHeaderField("Location");
                if (location == null) { c.disconnect(); throw new IllegalStateException("هدایت ناقص GitHub"); }
                current = new URL(new URL(current), location).toString();
                c.disconnect();
                continue;
            }
            if (response != 200) { c.disconnect(); throw new IllegalStateException("GitHub HTTP " + response); }
            return c;
        }
        throw new IllegalStateException("هدایت‌های بیش از حد");
    }

    private static JSONArray fetchReleases() throws Exception {
        HttpURLConnection c = connection(UpdatePolicy.RELEASES_API);
        try (InputStream in = c.getInputStream()) {
            return new JSONArray(readLimited(in, API_LIMIT));
        } finally {
            c.disconnect();
        }
    }

    /** Separate signed channels; stable naming remains compatible with old clients. */
    private static Release select(JSONArray releases, String installedVersion, boolean previewChannel) {
        Release best = null;
        for (int i = 0; i < Math.min(40, releases.length()); i++) {
            JSONObject r = releases.optJSONObject(i);
            if (r == null || r.optBoolean("draft") || r.optBoolean("prerelease") != previewChannel) continue;
            String tag = r.optString("tag_name", "");
            if (!UpdatePolicy.newer(tag, installedVersion)) continue;
            JSONArray assets = r.optJSONArray("assets");
            if (assets == null) continue;
            for (int j = 0; j < assets.length(); j++) {
                JSONObject a = assets.optJSONObject(j);
                if (a == null) continue;
                String name = a.optString("name", "");
                String url = a.optString("browser_download_url", "");
                String expectedSuffix = previewChannel ? "-preview.apk" : "-premium-stable.apk";
                if (!name.endsWith(expectedSuffix) || !UpdatePolicy.validAsset(url, tag, name)) continue;
                String digest = UpdatePolicy.sha256(a.optString("digest", ""));
                if (digest.isEmpty()) continue;
                if (best == null || UpdatePolicy.compareVersions(tag, best.tag) > 0) {
                    best = new Release();
                    best.tag = tag;
                    best.name = name;
                    best.url = url;
                    best.sha = digest;
                    best.notes = r.optString("body", "");
                }
            }
        }
        return best;
    }

    /** One asynchronous release query at each MainActivity launch (not on every resume). */
    public static void maybeNotify(Activity activity) {
        new Thread(() -> {
            try {
                JSONArray releases = fetchReleases();
                activity.getSharedPreferences("pooyan_updates", MODE_PRIVATE).edit()
                        .putLong("last_successful_check", System.currentTimeMillis()).apply();
                if (!supportedChannel(activity.getPackageName())) return;
                Release latest = select(releases, BuildConfig.VERSION_NAME,
                        previewPackage(activity.getPackageName()));
                if (latest != null) activity.runOnUiThread(() -> notifyWhenForeground(activity, latest, 0));
            } catch (Exception ignored) {
                // Do not block the home screen when GitHub is unavailable; manual check reports errors.
            }
        }, "pooyan-update-startup").start();
    }

    private static void notifyWhenForeground(Activity activity, Release release, int attempt) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (!activity.hasWindowFocus()) {
            if (attempt < 180) new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    () -> notifyWhenForeground(activity, release, attempt + 1), 2000);
            return;
        }
        if (!prompted.add(release.tag)) return;
        new AlertDialog.Builder(activity)
                .setTitle("آپدیت جدید Pooyan TV")
                .setMessage("نسخهٔ " + release.tag + " برای همین کانال در GitHub منتشر شده. "
                        + "نصب فقط در صورت سازگاری شناسه و امضای دیجیتال انجام می‌شود.")
                .setNegativeButton("بعداً", null)
                .setPositiveButton("مدیریت آپدیت", (d, which) ->
                        activity.startActivity(new Intent(activity, UpdaterActivity.class)))
                .show();
    }

    private void checkNow() {
        if (busy) return;
        setBusy(true);
        show("در حال بررسی انتشارهای رسمی GitHub…");
        io.execute(() -> {
            try {
                if (!supportedChannel(getPackageName())) {
                    show("این APK آزمایشی قدیمی کانال انتشار خودکار ندارد. "
                            + "از گزینهٔ انتخاب APK سازگار از حافظه استفاده کن.");
                    return;
                }
                Release release = select(fetchReleases(), BuildConfig.VERSION_NAME,
                        previewPackage(getPackageName()));
                getSharedPreferences("pooyan_updates", MODE_PRIVATE).edit()
                        .putLong("last_successful_check", System.currentTimeMillis()).apply();
                if (release == null) {
                    show("نسخهٔ جدیدتری در کانال فعلی با APK و SHA-256 معتبر پیدا نشد. "
                            + "برای نصب APK سازگار از حافظه یا USB، دکمهٔ انتخاب فایل را بزن.");
                    return;
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    String notes = release.notes == null ? "" : release.notes;
                    new AlertDialog.Builder(this)
                            .setTitle("نسخهٔ " + release.tag + " در GitHub")
                            .setMessage("تغییرات:\n" + notes.substring(0, Math.min(650, notes.length()))
                                    + "\n\nاگر امضا و شناسهٔ بسته یکسان باشند، Android آپدیت را بدون حذف داده‌ها نصب می‌کند.")
                            .setNegativeButton("بعداً", null)
                            .setPositiveButton("دانلود و آپدیت", (dialog, which) -> download(release))
                            .show();
                });
            } catch (Exception e) {
                show("خطای بررسی آپدیت: " + e.getMessage()
                        + "\nاتصال GitHub را بررسی کن یا از فایل APK سازگار استفاده کن.");
            } finally {
                setBusy(false);
            }
        });
    }

    private File updateFile() throws Exception {
        File folder = new File(getCacheDir(), "updates");
        if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("ساخت پوشهٔ آپدیت ممکن نیست");
        File apk = new File(folder, "pooyan-verified.apk");
        if (apk.exists() && !apk.delete()) throw new IllegalStateException("پاک‌کردن فایل موقت ممکن نیست");
        return apk;
    }

    private static String hex(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (byte b : data) result.append(String.format(Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }

    private void showReady(File apk, String source, String digest) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            new AlertDialog.Builder(this)
                    .setTitle("APK جدید آمادهٔ نصب است")
                    .setMessage(source + "\nSHA-256: " + digest + "\n\n"
                            + "شناسهٔ برنامه، شمارهٔ نسخه و امضای دیجیتال با نسخهٔ نصب‌شده بررسی شدند. "
                            + "داده‌ها حذف نمی‌شوند؛ نصب باید در Android تأیید شود.")
                    .setNegativeButton("بعداً", null)
                    .setPositiveButton("نصب نسخهٔ جدید", (d, w) -> install(apk))
                    .show();
            status.setText("فایل جدید بررسی شد؛ برای نصب باید تأیید کنی.");
        });
    }

    private void download(Release release) {
        if (busy) return;
        setBusy(true);
        show("در حال دانلود " + release.tag + "…");
        io.execute(() -> {
            File apk = null;
            try {
                apk = updateFile();
                HttpURLConnection c = connection(release.url);
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long size = 0;
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(apk)) {
                    long contentLength = Build.VERSION.SDK_INT >= 24
                            ? c.getContentLengthLong() : c.getContentLength();
                    if (contentLength > UpdatePolicy.MAX_APK_BYTES)
                        throw new IllegalStateException("حجم APK بیش از حد مجاز است");
                    byte[] bytes = new byte[32768];
                    int n;
                    long reported = 0;
                    while ((n = in.read(bytes)) != -1) {
                        size += n;
                        if (size > UpdatePolicy.MAX_APK_BYTES) throw new IllegalStateException("APK بیش از حد بزرگ است");
                        out.write(bytes, 0, n);
                        sha.update(bytes, 0, n);
                        if (size - reported > 2L * 1024 * 1024) {
                            reported = size;
                            show("دانلود: " + (size / (1024 * 1024)) + " مگابایت");
                        }
                    }
                } finally { c.disconnect(); }
                if (size == 0) throw new IllegalStateException("فایل APK خالی بود");
                String digest = hex(sha.digest());
                if (!digest.equals(release.sha)) throw new SecurityException("SHA-256 فایل با GitHub مطابقت ندارد");
                validatePackage(apk);
                showReady(apk, "نسخهٔ " + release.tag + " از GitHub", digest);
            } catch (Exception e) {
                if (apk != null) apk.delete();
                show("آپدیت انجام نشد: " + e.getMessage());
            } finally { setBusy(false); }
        });
    }

    private void pickLocalApk() {
        if (busy) return;
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*"); // Broad for Android TV file managers that mislabel APK MIME types.
        try {
            startActivityForResult(picker, PICK_APK);
        } catch (Exception e) {
            show("انتخاب‌گر فایل در این تلویزیون موجود نیست؛ می‌توانی APK سازگار را با ADB نصب کنی.");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_APK || result != RESULT_OK || data == null || data.getData() == null || busy) return;
        Uri uri = data.getData();
        setBusy(true);
        show("در حال بررسی فایل APK انتخابی…");
        io.execute(() -> {
            File apk = null;
            try {
                apk = updateFile();
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long size = 0;
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(apk)) {
                    if (in == null) throw new IllegalStateException("بازکردن فایل ممکن نیست");
                    byte[] bytes = new byte[32768];
                    int n;
                    while ((n = in.read(bytes)) != -1) {
                        size += n;
                        if (size > UpdatePolicy.MAX_APK_BYTES) throw new IllegalStateException("فایل انتخابی بیش از حد بزرگ است");
                        out.write(bytes, 0, n);
                        sha.update(bytes, 0, n);
                    }
                }
                if (size == 0) throw new IllegalStateException("فایل APK خالی است");
                validatePackage(apk);
                showReady(apk, "APK انتخاب‌شده از حافظه یا USB", hex(sha.digest()));
            } catch (Exception e) {
                if (apk != null) apk.delete();
                show("APK انتخابی قابل آپدیت نیست: " + e.getMessage());
            } finally { setBusy(false); }
        });
    }

    /** Fail closed: no downgrade, cross-package overwrite or mismatched certificate. */
    private void validatePackage(File apk) throws Exception {
        PackageManager pm = getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo next = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        PackageInfo current = pm.getPackageInfo(getPackageName(), flags);
        if (next == null || !getPackageName().equals(next.packageName))
            throw new SecurityException("شناسهٔ APK با نسخهٔ نصب‌شده یکسان نیست");
        long newCode = Build.VERSION.SDK_INT >= 28 ? next.getLongVersionCode() : next.versionCode;
        long oldCode = Build.VERSION.SDK_INT >= 28 ? current.getLongVersionCode() : current.versionCode;
        if (newCode <= oldCode) throw new SecurityException("نسخهٔ انتخابی جدیدتر از نسخهٔ نصب‌شده نیست");
        if (Build.VERSION.SDK_INT >= 28 && (current.signingInfo == null || next.signingInfo == null))
            throw new SecurityException("اطلاعات امضای APK پیدا نشد");
        Signature[] a = Build.VERSION.SDK_INT >= 28 ? current.signingInfo.getApkContentsSigners() : current.signatures;
        Signature[] b = Build.VERSION.SDK_INT >= 28 ? next.signingInfo.getApkContentsSigners() : next.signatures;
        if (a == null || b == null || a.length == 0 || a.length != b.length)
            throw new SecurityException("امضای APK قابل تطبیق نیست");
        for (int i = 0; i < a.length; i++)
            if (!Arrays.equals(a[i].toByteArray(), b[i].toByteArray()))
                throw new SecurityException("کلید امضای APK با برنامهٔ نصب‌شده متفاوت است");
    }

    private void install(File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingInstall = apk;
            new AlertDialog.Builder(this)
                    .setTitle("اجازهٔ نصب APK")
                    .setMessage("Android باید اجازهٔ نصب برنامه از Pooyan TV را بدهد. پس از بازگشت، نصب ادامه پیدا می‌کند.")
                    .setPositiveButton("تنظیمات نصب", (d, w) -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception e) {
                            pendingInstall = null;
                            show("تنظیمات نصب این دستگاه در دسترس نیست");
                        }
                    })
                    .setNegativeButton("لغو", (d, w) -> pendingInstall = null)
                    .show();
            return;
        }
        pendingInstall = null;
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".updates", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            show("برنامهٔ نصب Android در دسترس نیست: " + e.getMessage());
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingInstall != null && (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())) {
            File apk = pendingInstall;
            pendingInstall = null;
            if (apk.isFile()) install(apk);
        }
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
