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
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Opt-in, versioned updater: verified download; Android retains final installation consent. */
public final class UpdaterActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private TextView status;
    private Button check;
    private volatile boolean busy;
    private static final int API_LIMIT=240000;
    private static final class Release {
        String tag,url,sha,name,notes;int versionCode;
    }
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(32,28,32,28);layout.setBackgroundColor(0xff0B0D14);
        TextView heading=new TextView(this);heading.setText("POOYAN TV  ·  آپدیت نرم‌افزار");heading.setTextColor(-1);heading.setTextSize(24);layout.addView(heading);
        TextView version=new TextView(this);version.setText("نسخهٔ فعلی: "+BuildConfig.VERSION_NAME+"  ·  "+getPackageName());version.setTextColor(0xffdddddd);version.setPadding(0,14,0,15);layout.addView(version);
        status=new TextView(this);status.setTextColor(-1);status.setTextSize(17);status.setText("فقط نسخه‌های منتشرشده در GitHub رسمی پروژه بررسی می‌شوند. نصب با تأیید خودت انجام می‌شود.");status.setPadding(0,12,0,22);layout.addView(status);
        check=new Button(this);check.setText("بررسی نسخهٔ جدید در GitHub");check.setOnClickListener(v->checkNow());layout.addView(check);
        Button close=new Button(this);close.setText("بازگشت");close.setOnClickListener(v->finish());layout.addView(close);setContentView(layout);check.requestFocus();
    }
    private void show(String s){runOnUiThread(()->{if(!isFinishing())status.setText(s);});}
    private void setBusy(boolean v){busy=v;runOnUiThread(()->{if(check!=null)check.setEnabled(!v);});}
    private static String readLimited(InputStream in,int limit)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1){if(bytes.size()+n>limit)throw new IllegalStateException("پاسخ بیش از حد بزرگ است");bytes.write(buf,0,n);}return bytes.toString("UTF-8");
    }
    private static HttpURLConnection connection(String url)throws Exception{
        String current=url;
        for(int hop=0;hop<5;hop++){
            if(!UpdatePolicy.allowedDownloadHop(current)&&!UpdatePolicy.RELEASES_API.equals(current))throw new SecurityException("انتقال به میزبان غیرمجاز");
            HttpURLConnection c=(HttpURLConnection)new URL(current).openConnection();c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(12000);c.setReadTimeout(16000);c.setRequestProperty("User-Agent","PooyanTV-Updater/0.7.5");
            int status=c.getResponseCode();
            if(status==301||status==302||status==303||status==307||status==308){String loc=c.getHeaderField("Location");
                if(loc==null){c.disconnect();throw new IllegalStateException("هدایت نامعتبر");}
                current=new URL(new URL(current),loc).toString();c.disconnect();continue;
            }
            if(status!=200){c.disconnect();throw new IllegalStateException("GitHub HTTP "+status);}return c;
        }
        throw new IllegalStateException("هدایت‌های بیش از حد");
    }
    private static Release select(JSONArray releases,String current)throws Exception{
        Release best=null;
        for(int i=0;i<Math.min(15,releases.length());i++){
            JSONObject r=releases.optJSONObject(i);if(r==null||r.optBoolean("draft"))continue;
            String tag=r.optString("tag_name","");if(!UpdatePolicy.newer(tag,current))continue;
            JSONArray assets=r.optJSONArray("assets");if(assets==null)continue;
            for(int j=0;j<assets.length();j++){
                JSONObject a=assets.optJSONObject(j);if(a==null)continue;
                String name=a.optString("name","");String url=a.optString("browser_download_url","");
                if(!name.endsWith("-premium-stable.apk")||!UpdatePolicy.validAsset(url,tag,name))continue;
                String digest=UpdatePolicy.sha256(a.optString("digest",""));
                if(digest.isEmpty())continue; // Refuse unverified or legacy artifacts
                if(best==null||UpdatePolicy.compareVersions(tag,best.tag)>0){best=new Release();best.tag=tag;best.name=name;best.url=url;best.sha=digest;best.notes=r.optString("body","");}
            }
        }
        return best;
    }
    private static final java.util.Set<String> prompted=java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    /** Quiet asynchronous check at every Activity creation; no user data leaves the device. */
    public static void maybeNotify(Activity activity){
        if(!activity.getPackageName().endsWith(".tv"))return; // Ignore side-by-side debug editions
        new Thread(()->{
            HttpURLConnection c=null;
            try{
                c=connection(UpdatePolicy.RELEASES_API);
                String payload;try(InputStream in=c.getInputStream()){payload=readLimited(in,API_LIMIT);}finally{c.disconnect();}
                Release release=select(new JSONArray(payload),BuildConfig.VERSION_NAME);
                if(release==null)return;
                activity.runOnUiThread(()->notifyWhenForeground(activity,release,0));
            }catch(Exception ignored){if(c!=null)c.disconnect();}
        },"pooyan-update-check").start();
    }
    private static void notifyWhenForeground(Activity activity,Release release,int attempt){
        if(activity.isFinishing()||activity.isDestroyed())return;
        // Never interrupt playback: the home activity loses window focus during PlayerActivity.
        if(!activity.hasWindowFocus()){
            if(attempt<180)new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                ()->notifyWhenForeground(activity,release,attempt+1),2000);
            return;
        }
        if(!prompted.add(release.tag))return; // Once per running process and release.
        new AlertDialog.Builder(activity).setTitle("آپدیت جدید Pooyan TV")
            .setMessage("نسخهٔ "+release.tag+" در GitHub منتشر شده.")
            .setNegativeButton("بعداً",null)
            .setPositiveButton("جزئیات و دانلود",(d,w)->activity.startActivity(new Intent(activity,UpdaterActivity.class)))
            .show();
    }
    private void checkNow(){if(busy)return;setBusy(true);show("در حال دریافت اطلاعات انتشار از GitHub…");
        io.execute(()->{
            try {
                HttpURLConnection c=connection(UpdatePolicy.RELEASES_API);
                String payload;try(InputStream in=c.getInputStream()){payload=readLimited(in,API_LIMIT);}finally{c.disconnect();}
                Release release=select(new JSONArray(payload),BuildConfig.VERSION_NAME);
                if(release==null){show("نسخهٔ جدیدِ دارای فایل APK و اثرانگشت SHA256 معتبر پیدا نشد.");return;}
                runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("نسخهٔ "+release.tag+" پیدا شد")
                    .setMessage("تغییرات انتشار:\n"+release.notes.substring(0,Math.min(700,release.notes.length()))+"\n\nدانلود و نصب با تأیید شما انجام می‌شود. پروفایل‌ها و تاریخچه نباید پاک شوند.")
                    .setNegativeButton("بعداً",null).setPositiveButton("دریافت آپدیت",(d,w)->download(release)).show();});
            }catch(Exception ex){show("خطای بررسی آپدیت: "+ex.getMessage());}
            finally{setBusy(false);}
        });
    }
    private void download(Release release){if(busy)return;setBusy(true);show("در حال دریافت "+release.tag+"…");
        io.execute(()->{
            File apk=null;
            try{
                File folder=new File(getCacheDir(),"updates");if(!folder.exists()&&!folder.mkdirs())throw new IllegalStateException("ساخت پوشه ممکن نیست");
                apk=new File(folder,"pooyan-verified.apk");if(apk.exists()&&!apk.delete())throw new IllegalStateException("فایل قبلی قابل حذف نیست");
                HttpURLConnection c=connection(release.url);int length=c.getContentLength();
                if(length>UpdatePolicy.MAX_APK_BYTES)throw new IllegalStateException("فایل بسیار بزرگ است");
                MessageDigest md=MessageDigest.getInstance("SHA-256");long total=0;
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(apk)){
                    byte[] chunk=new byte[32768];int n;long last=0;
                    while((n=in.read(chunk))!=-1){total+=n;
                        if(total>UpdatePolicy.MAX_APK_BYTES)throw new IllegalStateException("حجم فایل از حد مجاز بیشتر است");
                        out.write(chunk,0,n);md.update(chunk,0,n);
                        if(total-last>2*1024*1024){last=total;show("دریافت: "+(total/(1024*1024))+" مگابایت");}
                    }
                }finally{c.disconnect();}
                StringBuilder actual=new StringBuilder();for(byte b:md.digest())actual.append(String.format(Locale.ROOT,"%02x",b&255));
                if(!release.sha.equals(actual.toString()))throw new SecurityException("SHA256 فایل مطابق انتشار GitHub نیست");
                validatePackage(apk);
                File verified=apk;
                runOnUiThread(()->{if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("دانلود و بررسی موفق")
                  .setMessage("فایل "+release.tag+" با SHA256 و امضای برنامهٔ نصب‌شده بررسی شد. برای نصب باید مجوز سیستم Android را تأیید کنی.")
                  .setNegativeButton("بعداً",null).setPositiveButton("نصب",(d,w)->install(verified)).show();});
                show("APK بررسی شد. در پنجرهٔ نصب، تأیید کاربر لازم است.");
            }catch(Exception e){if(apk!=null)apk.delete();show("آپدیت انجام نشد: "+e.getMessage());}
            finally{setBusy(false);}
        });
    }
    private void validatePackage(File apk)throws Exception{
        PackageManager pm=getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo next=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        PackageInfo current=pm.getPackageInfo(getPackageName(),flags);
        if(next==null||next.packageName==null||!getPackageName().equals(next.packageName))throw new SecurityException("شناسهٔ APK با برنامهٔ فعلی یکسان نیست؛ آپدیت ایمن نیست");
        long nextCode=Build.VERSION.SDK_INT>=28?next.getLongVersionCode():next.versionCode;
        long currentCode=Build.VERSION.SDK_INT>=28?current.getLongVersionCode():current.versionCode;
        if(nextCode<=currentCode)throw new SecurityException("شمارهٔ نسخه جدیدتر نیست");
        if(Build.VERSION.SDK_INT>=28&&(current.signingInfo==null||next.signingInfo==null))throw new SecurityException("امضای برنامه نامشخص است");
        Signature[] a=Build.VERSION.SDK_INT>=28?current.signingInfo.getApkContentsSigners():current.signatures;
        Signature[] b=Build.VERSION.SDK_INT>=28?next.signingInfo.getApkContentsSigners():next.signatures;
        if(a==null||b==null||a.length==0||a.length!=b.length)throw new SecurityException("امضای APK قابل تطبیق نیست");
        for(int i=0;i<a.length;i++)if(!Arrays.equals(a[i].toByteArray(),b[i].toByteArray()))throw new SecurityException("کلید امضای نسخه‌ها متفاوت است");
    }
    private void install(File apk){
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(this).setTitle("اجازهٔ نصب آپدیت")
                .setMessage("Android ابتدا باید اجازهٔ نصب نسخهٔ دریافت‌شده توسط Pooyan TV را بدهد. پس از فعال‌کردن آن، دوباره روی نصب کلیک کن.")
                .setPositiveButton("تنظیمات Android",(d,w)->{
                    try{startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(Exception ex){show("تنظیمات نصب در این دستگاه در دسترس نیست");}
                }).setNegativeButton("لغو",null).show();return;
        }
        try{
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".updates",apk);
            Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }catch(Exception e){show("برنامهٔ نصب Android در دسترس نیست: "+e.getMessage());}
    }
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
