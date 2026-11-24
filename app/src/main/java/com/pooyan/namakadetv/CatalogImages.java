package com.pooyan.namakadetv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded, on-demand metadata art; never sends user tokens/cookies to image hosts. */
public final class CatalogImages {
    private final Activity activity;
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(14*1024){
        @Override protected int sizeOf(String k,Bitmap b){return Math.max(1,b.getByteCount()/1024);}
    };
    private static final java.util.Set<CatalogImages> INSTANCES =
        java.util.Collections.newSetFromMap(new java.util.WeakHashMap<CatalogImages,Boolean>());
    public CatalogImages(Activity a){activity=a;synchronized(INSTANCES){INSTANCES.add(this);}}
    public static void clearAllMemory(){synchronized(INSTANCES){for(CatalogImages image:INSTANCES)image.memory.evictAll();}}
    private static boolean isPoster(String url){return ImageUrlPolicy.allowed(url);}
    private final java.util.Set<String> inFlight=new java.util.HashSet<>();
    private final java.util.Map<String,Long> failedUntil=new java.util.HashMap<>();
    public void load(ImageView view,String url){
        if(!isPoster(url))return;
        if(!url.equals(view.getTag()))view.setImageDrawable(null);view.setTag(url);
        android.view.ViewTreeObserver.OnPreDrawListener listener=new android.view.ViewTreeObserver.OnPreDrawListener(){
            public boolean onPreDraw(){
                if(!url.equals(view.getTag())){if(view.getViewTreeObserver().isAlive())view.getViewTreeObserver().removeOnPreDrawListener(this);return true;}
                android.graphics.Rect visible=new android.graphics.Rect();
                if(!view.isShown()||!view.getGlobalVisibleRect(visible)){if(view.getDrawable()!=null)view.setImageDrawable(null);return true;}
                if(view.getDrawable()!=null)return true;
                Bitmap cached=memory.get(url);if(cached!=null)view.setImageBitmap(cached);
                else if(!inFlight.contains(url)&&failedUntil.getOrDefault(url,0L)<=System.currentTimeMillis())fetch(view,url);
                return true;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);
        view.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(android.view.View v){if(v.getViewTreeObserver().isAlive()){v.getViewTreeObserver().removeOnPreDrawListener(listener);v.getViewTreeObserver().addOnPreDrawListener(listener);}}
            public void onViewDetachedFromWindow(android.view.View v){if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnPreDrawListener(listener);}
        });
    }
    private void fetch(ImageView view,String url){
        if(pool.isShutdown())return;inFlight.add(url);
        pool.submit(()->{Bitmap bitmap=download(url);if(bitmap!=null)memory.put(url,bitmap);
            activity.runOnUiThread(()->{inFlight.remove(url);
                if(bitmap==null){if(failedUntil.size()>=256)failedUntil.clear();failedUntil.put(url,System.currentTimeMillis()+30000);}else failedUntil.remove(url);
                if(!activity.isFinishing()&&!activity.isDestroyed())activity.getWindow().getDecorView().invalidate();
            });
        });
    }
    private Bitmap download(String url){
        HttpURLConnection con=null;
        try{
            String target=url;
            for(int hop=0;hop<=2;hop++){
                con=(HttpURLConnection)new URL(target).openConnection();con.setConnectTimeout(7500);con.setReadTimeout(7500);con.setInstanceFollowRedirects(false);con.setRequestProperty("Accept","image/jpeg,image/webp,image/png");
                int code=con.getResponseCode();
                if(code>=300&&code<400){String next=con.getHeaderField("Location");con.disconnect();con=null;if(next==null)return null;target=new URL(new URL(target),next).toString();if(!isPoster(target))return null;continue;}
                if(code!=200)return null;break;
            }
            if(con==null||con.getContentType()==null||!con.getContentType().startsWith("image/")||con.getContentLength()>2200000)return null;
            try(InputStream input=con.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] chunk=new byte[8192];int n;while((n=input.read(chunk))!=-1){if(out.size()+n>2200000)return null;out.write(chunk,0,n);}
                byte[] bytes=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                opts.inSampleSize=1;while(opts.outWidth/opts.inSampleSize>900||opts.outHeight/opts.inSampleSize>900)opts.inSampleSize*=2;
                opts.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
            }
        }catch(Exception ignored){return null;}finally{if(con!=null)con.disconnect();}
    }
    public void close(){pool.shutdownNow();}
}
