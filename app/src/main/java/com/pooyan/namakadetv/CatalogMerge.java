package com.pooyan.namakadetv;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** Keeps first provider as canonical while retaining different-source variants for optional fallback. */
public final class CatalogMerge {
    private CatalogMerge(){}
    public static String matchKey(FarsiPlexTitle title){
        if(title==null)return "";
        String scope=title.kind+("series".equals(title.kind)&&title.season>0?"|season:"+title.season:"");
        if("episode".equals(title.kind))return "episode|"+title.url;
        if(!title.stableId.isEmpty())return scope+"|id:"+title.stableId;
        String name=SearchPolicy.normalize(title.name);
        // Unknown years cannot reliably distinguish remakes across catalogs.
        if(title.year==null||!title.year.matches("(?:19|20)\\d{2}"))return scope+"|url:"+title.url;
        return scope+"|"+name+"|"+title.year;
    }
    public static List<FarsiPlexTitle> join(List<FarsiPlexTitle> primary,List<FarsiPlexTitle> secondary,
                                                 Map<String,List<FarsiPlexTitle>> variants){
        Map<String,FarsiPlexTitle> distinct=new LinkedHashMap<>();
        variants.clear();
        for(List<FarsiPlexTitle> list:java.util.Arrays.asList(primary,secondary)){
            if(list==null)continue;
            for(FarsiPlexTitle item:list){
                if(item==null||item.url==null||item.url.isEmpty())continue;
                String key=matchKey(item);
                FarsiPlexTitle first=distinct.get(key);
                if(first!=null && !first.year.isEmpty() && !item.year.isEmpty() && !first.year.equals(item.year))
                    key=key+"|"+item.year;
                // Merge strong cross-provider matches, not merely provider-specific page URLs.
                FarsiPlexTitle chosen=null;
                for(FarsiPlexTitle candidate:distinct.values())
                    if(UltimateIdentity.sameTitle(candidate,item)){chosen=candidate;break;}
                if(chosen==null){
                    // Different trusted IDs can share a visible name: NEVER overwrite a distinct entry.
                    if(distinct.containsKey(key))key=key+"|page:"+UltimateIdentity.sha256(item.url);
                    distinct.put(key,item);
                }else if(!chosen.url.equals(item.url))
                    variants.computeIfAbsent(chosen.url,k->new ArrayList<>()).add(item);
            }
        }
        return new ArrayList<>(distinct.values());
    }
}
