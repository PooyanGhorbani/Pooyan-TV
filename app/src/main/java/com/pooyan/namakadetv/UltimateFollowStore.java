package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Episode alerts only for followed shows, using known season/episode coordinates. */
public final class UltimateFollowStore {
    private final SharedPreferences prefs;
    public UltimateFollowStore(Context context){prefs=ProfileStore.data(context,"ultimate_follows");}
    private String id(FarsiPlexTitle series){return UltimateIdentity.sha256(UltimateIdentity.contentKey(series)).substring(0,24);}
    private String k(String prefix,FarsiPlexTitle series){return prefix+id(series);}
    public boolean following(FarsiPlexTitle series){return prefs.getBoolean(k("f_",series),false);}
    public void follow(FarsiPlexTitle series,boolean yes){
        SharedPreferences.Editor e=prefs.edit().putBoolean(k("f_",series),yes);
        if(yes){
            // Store public page only; Android profile data remains local. Do not store stream URLs.
            e.putString(k("url_",series),UltimateIdentity.safePage(series.url))
             .putString(k("name_",series),series.name)
             .putString(k("year_",series),series.year);
        }else e.remove(k("new_",series));
        e.apply();
    }
    public int observe(FarsiPlexTitle series,List<FarsiPlexTitle> episodes){
        if(!following(series)||episodes==null||episodes.isEmpty())return 0;
        Set<String> now=UltimateEpisodePolicy.known(episodes);
        if(now.isEmpty())return 0;
        String snap=k("seen_",series);
        Set<String> previous=prefs.getStringSet(snap,new HashSet<>());
        int number=UltimateEpisodePolicy.added(previous,now).size();
        Set<String> combined=new LinkedHashSet<>(previous);combined.addAll(now);
        // Changes of ordering cannot create alerts, and first scan establishes the baseline.
        prefs.edit().putStringSet(snap,combined).putInt(k("new_",series),
            Math.min(999,prefs.getInt(k("new_",series),0)+number)).apply();
        return number;
    }
    public int pending(FarsiPlexTitle series){return prefs.getInt(k("new_",series),0);}
    public void acknowledge(FarsiPlexTitle series){prefs.edit().putInt(k("new_",series),0).apply();}
    public static final class Followed {
        public final String url,name,year;
        Followed(String u,String n,String y){url=u;name=n;year=y;}
    }
    public java.util.List<Followed> all(){
        java.util.List<Followed> out=new java.util.ArrayList<>();
        for(java.util.Map.Entry<String,?> x:prefs.getAll().entrySet()){
            if(!x.getKey().startsWith("f_")||!Boolean.TRUE.equals(x.getValue()))continue;
            String id=x.getKey().substring(2),url=prefs.getString("url_"+id,"");
            if(!url.startsWith("https://")||url.length()>1000)continue;
            out.add(new Followed(url,prefs.getString("name_"+id,""),prefs.getString("year_"+id,"")));
            if(out.size()>=15)break;
        }
        return out;
    }
}
