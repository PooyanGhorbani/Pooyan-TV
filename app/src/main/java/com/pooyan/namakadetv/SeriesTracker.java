package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Per-profile episode count snapshots, only when a source returns a complete known list. */
public final class SeriesTracker {
    private SeriesTracker(){}
    private static String key(String url){
        try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder("series_");for(int i=0;i<12;i++)b.append(String.format(java.util.Locale.ROOT,"%02x",hash[i]&255));return b.toString();
        }catch(Exception e){return "series_unknown";}
    }
    public static int difference(int previous,int current){return previous<=0||current<=previous?0:current-previous;}
    public static int observe(Context context,String url,int count){
        if(count<=0||url==null||url.isEmpty())return 0;
        SharedPreferences p=ProfileStore.data(context,"series_tracker");String key=key(url);
        int old=p.getInt(key,0);
        if(count>old)p.edit().putInt(key,count).apply();
        return difference(old,count);
    }
    public static String state(String url,List<LibraryStore.Entry> completed,List<LibraryStore.Entry> watched,List<LibraryStore.Entry> resume){
        if(url==null||url.isEmpty())return "";
        if(contains(completed,url))return "✓ تماشا شده";
        if(contains(resume,url))return "▶ نیمه‌تمام";
        if(contains(watched,url))return "✓ تماشا شده";
        return "";
    }
    private static boolean contains(List<LibraryStore.Entry> list,String url){if(list==null)return false;for(LibraryStore.Entry e:list)if(url.equals(e.pageUrl))return true;return false;}
}
