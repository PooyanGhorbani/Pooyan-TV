package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single remote-friendly settings and health center; does not store browsing or diagnostic secrets. */
public final class MaintenanceActivity extends Activity {
    private TextView status;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state){super.onCreate(state);
        ScrollView scroll=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(28,25,28,25);l.setBackgroundColor(0xff0B0D14);scroll.addView(l);setContentView(scroll);
        TextView title=new TextView(this);title.setText("POOYAN TV · مرکز تنظیمات v"+BuildConfig.VERSION_NAME);title.setTextSize(24);title.setTextColor(-1);l.addView(title);
        status=new TextView(this);status.setTextColor(0xffdddddd);status.setTextSize(16);status.setPadding(0,15,0,14);status.setText(SourceRules.summary(this));l.addView(status);
        add(l,"مدیریت آپدیت: GitHub / نصب APK سازگار",()->startActivity(new Intent(this,UpdaterActivity.class)));
        add(l,"آپدیت قواعد منابع (بدون نصب APK)",()->{info("در حال بررسی تنظیمات عمومی منابع…");SourceRules.refresh(this,this::info);});
        add(l,"پشتیبان‌گیری / بازیابی خانواده",()->startActivity(new Intent(this,BackupActivity.class)));
        add(l,"بررسی سلامت ارتباط با منابع",this::checkSources);
        add(l,"نمایش وضعیت حافظهٔ موقت",()->info("کش فایل‌های موقت برنامه: "+bytes(getCacheDir())/(1024*1024)+" مگابایت · پوسترها در حافظهٔ RAM محدودند."));
        add(l,"پاک‌سازی حافظهٔ موقت",()->new AlertDialog.Builder(this).setTitle("حذف فایل‌های موقت؟")
            .setMessage("فقط فایل‌های APK موقت و کش داخلی پاک می‌شوند؛ پروفایل‌ها و تاریخچه حفظ می‌شوند.")
            .setPositiveButton("پاک‌سازی",(d,w)->{deleteTree(new File(getCacheDir(),"updates"));CatalogImages.clearAllMemory();info("فایل آپدیت موقت و کش تصویری RAM پاک شد.");})
            .setNegativeButton("لغو",null).show());
        add(l,"بازگشت",this::finish);
    }
    private void add(LinearLayout l,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setOnClickListener(v->action.run());l.addView(b);}
    private void info(String text){runOnUiThread(()->{if(!isFinishing())status.setText(text);});}
    private static long bytes(File file){if(!file.exists())return 0;if(file.isFile())return file.length();long n=0;File[] children=file.listFiles();if(children!=null)for(File f:children)n+=bytes(f);return n;}
    private static void deleteTree(File file){if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File f:children)deleteTree(f);}if(file.exists())file.delete();}
    private void checkSources(){info("در حال بررسی اتصال عمومی…");
        io.execute(()->{
            StringBuilder report=new StringBuilder();
            for(String raw:new String[]{"https://farsiplex.com/","https://namakadeh.com/"}){
                HttpURLConnection c=null;try{
                    c=(HttpURLConnection)new URL(raw).openConnection();c.setRequestMethod("HEAD");c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setInstanceFollowRedirects(false);
                    int code=c.getResponseCode();report.append(raw).append("  HTTP ").append(code).append("\n");
                }catch(Exception e){report.append(raw).append("  اتصال برقرار نشد\n");}finally{if(c!=null)c.disconnect();}
            }
            report.append("\n").append(SourceRules.summary(this));info(report.toString());
        });
    }
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
