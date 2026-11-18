package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Data-only configuration; never downloads code, site URLs, scripts or arbitrary selectors. */
public final class SourceRules {
    private SourceRules(){}
    private static final String CONFIG = "https://raw.githubusercontent.com/PooyanGhorbani/Pooyan-TV/main/config/source-rules-v1.json";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private static final long PERIOD=24*60*60*1000L;
    public interface Callback {void done(String message);}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences("source_rules_v1",Context.MODE_PRIVATE);}
    private static void apply(int attempts,int http,int decode){SourceHealth.configure(attempts,http*1000L,decode*1000L);}
    public static void initialize(Context context){
        SharedPreferences p=prefs(context);
        apply(p.getInt("attempts",3),p.getInt("http",60),p.getInt("decode",600));
        if(System.currentTimeMillis()-p.getLong("last_checked",0)>PERIOD)refresh(context,null);
    }
    public static void refresh(Context context,Callback callback){
        Context app=context.getApplicationContext();
        EXEC.execute(()->{
            String message="تنظیمات منابع تغییری نکرد.";HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(CONFIG).openConnection();c.setConnectTimeout(9000);c.setReadTimeout(9000);
                c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
                if(c.getResponseCode()!=200||c.getContentLength()>8192)throw new IllegalStateException("پاسخ تنظیمات معتبر نیست");
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                try(InputStream in=c.getInputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){if(out.size()+n>8192)throw new IllegalStateException("config too large");out.write(b,0,n);}}
                JSONObject j=new JSONObject(out.toString("UTF-8"));
                if(!"pooyan-source-rules".equals(j.optString("type"))||j.optInt("schema")!=1)throw new IllegalStateException("قالب ناشناخته");
                int version=j.optInt("version",0);SharedPreferences p=prefs(app);
                int attempts=j.getInt("maxAttempts"),http=j.getInt("httpCooldownSec"),decode=j.getInt("decodeCooldownSec");
                if(version<1||attempts<1||attempts>3||http<30||http>180||decode<180||decode>900)throw new IllegalStateException("مقادیر غیرمجاز");
                p.edit().putLong("last_checked",System.currentTimeMillis()).apply();
                if(version>p.getInt("version",0)){
                    p.edit().putInt("version",version).putInt("attempts",attempts).putInt("http",http).putInt("decode",decode).apply();
                    apply(attempts,http,decode);message="قواعد ایمن منابع به نسخهٔ "+version+" بروزرسانی شدند.";
                }
            }catch(Exception e){message="آپدیت تنظیمات منابع ممکن نشد؛ تنظیمات قبلی حفظ شد. "+e.getMessage();}
            finally{if(c!=null)c.disconnect();}
            if(callback!=null)callback.done(message);
        });
    }
    public static String summary(Context c){SharedPreferences p=prefs(c);return "قواعد منابع: v"+p.getInt("version",0)+" · تلاش مجاز "+p.getInt("attempts",3)+" · آخرین بررسی "+(p.getLong("last_checked",0)>0?new java.util.Date(p.getLong("last_checked",0)).toString():"انجام نشده");}
}
