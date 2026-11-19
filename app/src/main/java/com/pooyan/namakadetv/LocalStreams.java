package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** User-provided, direct HTTPS media URLs; not a source of streaming rights. */
public final class LocalStreams {
    public static final class Saved {
        public String key,title,url;
        Saved(String k,String t,String u){key=k;title=t;url=u;}
    }
    private final SharedPreferences prefs;
    public LocalStreams(Context context){prefs=context.getSharedPreferences("pooyan_v02_streams",Context.MODE_PRIVATE);}
    public String get(String key){
        for(Saved s:all())if(s.key.equals(key))return s.url;
        return null;
    }
    public List<Saved> all(){
        List<Saved> res=new ArrayList<>();
        try {JSONArray a=new JSONArray(prefs.getString("saved","[]"));
            for(int i=0;i<Math.min(a.length(),200);i++){
                JSONObject j=a.optJSONObject(i);if(j==null)continue;
                String u=j.optString("url","");if(!StreamDetector.isDirectMedia(u))continue;
                res.add(new Saved(j.optString("key",""),j.optString("title","Untitled"),u));
            }
        }catch(Exception ignored){}
        return res;
    }
    public boolean put(String key,String title,String url){
        if(key==null||key.isEmpty()||!StreamDetector.isDirectMedia(url))return false;
        List<Saved> items=all();items.removeIf(s->s.key.equals(key));
        items.add(0,new Saved(key,title,url));
        write(items);return true;
    }
    public void delete(String key){List<Saved> items=all();items.removeIf(s->s.key.equals(key));write(items);}
    private void write(List<Saved> items){
        JSONArray arr=new JSONArray();
        try {for(int i=0;i<Math.min(items.size(),200);i++){
            Saved s=items.get(i);JSONObject j=new JSONObject();
            j.put("key",s.key);j.put("title",s.title);j.put("url",s.url);arr.put(j);
        }}catch(Exception ignored){}
        prefs.edit().putString("saved",arr.toString()).apply();
    }
}
