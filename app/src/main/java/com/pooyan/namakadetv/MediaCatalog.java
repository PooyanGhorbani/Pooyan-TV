package com.pooyan.namakadetv;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Official Cinemeta public catalog, cached for intermittent regional connectivity. */
public final class MediaCatalog {
    public interface Callback { void done(List<CatalogMovie> items, String error); }
    public interface DetailCallback { void done(CatalogMovie movie, String error); }
    private final Activity activity;
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final SharedPreferences cache;
    public MediaCatalog(Activity a) { activity=a; cache=a.getSharedPreferences("pooyan_catalog_cache",Context.MODE_PRIVATE); }
    public void list(String url,String fallbackType,Callback cb) {
        pool.execute(()->{
            List<CatalogMovie> movies=new ArrayList<>(); String error=null;
            try {
                String raw=getJson(url); if(raw==null)throw new IllegalStateException("empty response");
                JSONArray arr=new JSONObject(raw).optJSONArray("metas");
                if(arr==null)throw new IllegalStateException("catalog unavailable");
                for(int i=0;i<Math.min(arr.length(),24);i++) {
                    JSONObject obj=arr.optJSONObject(i);if(obj==null)continue;
                    CatalogMovie movie=CatalogMovie.parse(obj,fallbackType);if(movie!=null)movies.add(movie);
                }
                if(!url.contains("/search="))cache.edit().putString(url,raw).apply();
            }catch(Exception e){
                android.util.Log.w("PooyanCatalog", "Catalog request failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                error="اتصال به کاتالوگ برقرار نشد";
                try {
                    JSONArray arr=new JSONObject(cache.getString(url,"{}")).optJSONArray("metas");
                    if(arr!=null)for(int i=0;i<Math.min(arr.length(),24);i++) {
                        JSONObject obj=arr.optJSONObject(i); if(obj==null)continue;
                        CatalogMovie movie=CatalogMovie.parse(obj,fallbackType);if(movie!=null)movies.add(movie);
                    }
                    if(!movies.isEmpty())error="نمایش داده‌های ذخیره‌شده (آفلاین)";
                }catch(Exception ignored){}
            }
            List<CatalogMovie> result=movies;String status=error;
            activity.runOnUiThread(()->cb.done(result,status));
        });
    }
    public void detail(CatalogMovie movie,DetailCallback cb) {
        pool.execute(()->{
            CatalogMovie full=movie;String error=null;
            try {
                String raw=getJson(CatalogPolicy.meta(movie.type,movie.id));
                JSONObject obj=new JSONObject(raw).optJSONObject("meta");
                if(obj!=null){CatalogMovie parsed=CatalogMovie.parse(obj,movie.type);if(parsed!=null)full=parsed;}
            }catch(Exception e){error="اطلاعات تکمیلی فعلاً در دسترس نیست";}
            CatalogMovie result=full;String status=error;
            activity.runOnUiThread(()->cb.done(result,status));
        });
    }
    private static String getJson(String url) throws Exception {
        if(!url.startsWith(CatalogPolicy.BASE+"/"))throw new SecurityException("bad catalog host");
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/json");
        try {
            if(connection.getResponseCode()!=200)throw new IllegalStateException("HTTP "+connection.getResponseCode());
            int announced=connection.getContentLength();
            if(announced>1_500_000)throw new IllegalStateException("too large");
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){if(out.size()+n>1_500_000)throw new IllegalStateException("too large");out.write(buf,0,n);}
                return out.toString(StandardCharsets.UTF_8.name());
            }
        }finally{connection.disconnect();}
    }
    public void close(){pool.shutdownNow();}
}
