package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Safe public metadata snapshot for offline fallback; never stores playable URLs or secret query strings. */
public final class UltimateCatalogCache {
    private final SharedPreferences prefs;
    private static final long KEEP_MS=7L*24*60*60*1000;
    public UltimateCatalogCache(Context context){prefs=context.getApplicationContext().getSharedPreferences("ultimate_public_catalog",0);}
    private String k(String kind){return "cache_"+("series".equals(kind)?"series":"movie");}
    public void save(String kind,List<FarsiPlexTitle> items){
        if(items==null||items.isEmpty())return;
        JSONArray all=new JSONArray();
        for(FarsiPlexTitle t:items){
            if(t==null||t.url==null||t.url.contains("?")||!t.url.startsWith("https://"))continue;
            if(all.length()>=120)break;
            try{all.put(new JSONObject().put("url",UltimateIdentity.safePage(t.url))
                .put("kind",t.kind).put("name",t.name).put("year",t.year)
                .put("genre",t.genre).put("rating",t.rating));}catch(Exception ignored){}
        }
        if(all.length()>0)prefs.edit().putString(k(kind),all.toString())
            .putLong("at_"+kind,System.currentTimeMillis()).apply();
    }
    public List<FarsiPlexTitle> read(String kind){
        List<FarsiPlexTitle> out=new ArrayList<>();
        long at=prefs.getLong("at_"+kind,0),now=System.currentTimeMillis();
        if(at<=0||now<at||now-at>KEEP_MS)return out;
        try{JSONArray arr=new JSONArray(prefs.getString(k(kind),"[]"));
            for(int i=0;i<Math.min(arr.length(),120);i++){
                JSONObject o=arr.optJSONObject(i);if(o==null)continue;
                String url=o.optString("url","");if(!url.startsWith("https://")||url.length()>1200)continue;
                String name=o.optString("name","");if(name.isEmpty())continue;
                FarsiPlexTitle t=new FarsiPlexTitle(url,kind,name);
                t.year=o.optString("year","");t.genre=o.optString("genre","");t.rating=o.optString("rating","");
                out.add(t);
            }
        }catch(Exception ignored){}
        return out;
    }
}
