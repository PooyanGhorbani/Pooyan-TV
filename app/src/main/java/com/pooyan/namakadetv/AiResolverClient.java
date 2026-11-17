package com.pooyan.namakadetv;

import android.app.Activity;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Optional HTTPS client for a user-deployed resolver; no DeepSeek API key is stored in the APK. */
public final class AiResolverClient {
    public interface Callback { void receive(String mediaUrl,String message); }
    private final Activity activity;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    public AiResolverClient(Activity activity){this.activity=activity;}
    public void resolve(String endpoint,String accessCode,String page,String title,Callback callback){
        executor.execute(()->{
            String found="", message="";HttpURLConnection connection=null;
            try {
                String origin=AiResolverUrlPolicy.normalize(endpoint);
                if(origin.isEmpty()||accessCode==null||accessCode.trim().length()<12||!FarsiPlexUrlPolicy.mediaType(page).matches("movie|series|episode"))
                    throw new IllegalArgumentException("آدرس سرویس یا کد اتصال معتبر نیست.");
                connection=(HttpURLConnection)new URL(origin+"/resolve").openConnection();
                connection.setRequestMethod("POST");
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(9000);connection.setReadTimeout(19000);
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
                connection.setRequestProperty("Accept","application/json");
                connection.setRequestProperty("X-Pooyan-Token",accessCode.trim());
                connection.setDoOutput(true);
                JSONObject req=new JSONObject();req.put("pageUrl",page);req.put("title",title);
                byte[] body=req.toString().getBytes(StandardCharsets.UTF_8);
                if(body.length>4096)throw new IllegalArgumentException("درخواست بیش از حد بزرگ است.");
                try(java.io.OutputStream out=connection.getOutputStream()){out.write(body);}
                int http=connection.getResponseCode();
                if(http!=200){
                    if(http==401)throw new IllegalStateException("کد اتصال Worker نادرست است.");
                    throw new IllegalStateException("خطای سرور: HTTP "+http);
                }
                String type=connection.getContentType();
                if(type==null||!type.toLowerCase(java.util.Locale.ROOT).contains("application/json"))throw new IllegalStateException("پاسخ سرور JSON نیست.");
                String response;
                try(InputStream input=connection.getInputStream()){
                    byte[] data=new byte[8193];int total=0,n;
                    while(total<data.length && (n=input.read(data,total,data.length-total))!=-1) total+=n;
                    if(total>8192)throw new IllegalStateException("پاسخ غیرعادی سرور");
                    response=new String(data,0,total,StandardCharsets.UTF_8);
                }
                JSONObject result=new JSONObject(response);
                if("playable".equals(result.optString("status"))&&result.optBoolean("verified")){
                    String candidate=result.optString("mediaUrl");
                    if(StreamDetector.isDirectMedia(candidate))found=candidate;
                }
                String reason=result.optString("reason","");
                if(reason.length()>160)reason=reason.substring(0,160);
                message=found.isEmpty()
                    ? "هیچ لینک مستقیم عمومی و قابل‌تأییدی پیدا نشد. شاید اتصال رسمی پخش لازم باشد."+(reason.isEmpty()?"":"\nجزئیات سرور: "+reason)
                    : "یک منبع مستقیم با بررسی HTTP پیدا شد؛ پخش روی تلویزیون هنوز باید آزمایش شود.";
                if("error".equals(result.optString("status")))message="خطای بررسی: "+result.optString("reason","نامشخص");
            }catch(Exception e){message="بررسی لینک انجام نشد: "+e.getMessage();}
            finally{if(connection!=null)connection.disconnect();}
            final String url=found,info=message;
            activity.runOnUiThread(()->callback.receive(url,info));
        });
    }
    public void close(){executor.shutdownNow();}
}
