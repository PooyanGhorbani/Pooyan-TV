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
    public CatalogImages(Activity a){activity=a;}
    private static boolean isPoster(String url){return ImageUrlPolicy.allowed(url);}
    public void load(ImageView view,String url){
        if(!isPoster(url))return;
        view.setTag(url);
        Bitmap cached=memory.get(url);
        if(cached!=null){view.setImageBitmap(cached);return;}
        pool.submit(()->{
            HttpURLConnection con=null;Bitmap b=null;
            try {
                String target=url;
                for(int hop=0;hop<=2;hop++){
                    con=(HttpURLConnection)new URL(target).openConnection();
                    con.setConnectTimeout(7500);con.setReadTimeout(7500);con.setInstanceFollowRedirects(false);
                    con.setRequestProperty("Accept","image/jpeg,image/webp,image/png");
                    int code=con.getResponseCode();
                    if(code>=300&&code<400){
                        String next=con.getHeaderField("Location");con.disconnect();con=null;
                        if(next==null)return;
                        String resolved=new URL(new URL(target),next).toString();
                        if(!isPoster(resolved))return;
                        target=resolved;continue;
                    }
                    if(code!=200)return;
                    break;
                }
                if(con==null)return;
                String type=con.getContentType();
                if(type==null || !type.startsWith("image/"))return;
                if(con.getContentLength()>2_200_000)return;
                try(InputStream is=con.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buff=new byte[8192];int n;
                    while((n=is.read(buff))!=-1){if(out.size()+n>2_200_000)return;out.write(buff,0,n);}
                    byte[] bytes=out.toByteArray();
                    BitmapFactory.Options opts=new BitmapFactory.Options(); opts.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                    opts.inSampleSize=1;
                    while(opts.outWidth/opts.inSampleSize>900 || opts.outHeight/opts.inSampleSize>900)opts.inSampleSize*=2;
                    opts.inJustDecodeBounds=false;
                    b=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                }
            }catch(Exception ignored){}finally{if(con!=null)con.disconnect();}
            if(b!=null){memory.put(url,b);Bitmap finalImage=b;activity.runOnUiThread(()->{
                if(!activity.isFinishing()&&url.equals(view.getTag()))view.setImageBitmap(finalImage);
            });}
        });
    }
    public void close(){pool.shutdownNow();}
}
