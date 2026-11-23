package com.pooyan.namakadetv;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Detect episodes using known (season, episode) pairs; source ordering/count is not an identity. */
public final class UltimateEpisodePolicy {
    private UltimateEpisodePolicy(){}
    public static String key(int season,int episode){return season>0&&episode>0?season+":"+episode:"";}
    public static Set<String> known(List<FarsiPlexTitle> episodes){
        Set<String> keys=new LinkedHashSet<>();if(episodes==null)return keys;
        for(FarsiPlexTitle e:episodes)if(e!=null){String k=key(e.season,e.episode);if(!k.isEmpty())keys.add(k);}
        return keys;
    }
    public static Set<String> added(Set<String> previous,Set<String> current){
        Set<String> out=new LinkedHashSet<>();if(previous==null||previous.isEmpty()||current==null)return out;
        out.addAll(current);out.removeAll(previous);return out;
    }
}
