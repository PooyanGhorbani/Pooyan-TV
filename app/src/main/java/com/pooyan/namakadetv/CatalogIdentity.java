package com.pooyan.namakadetv;
import java.text.Normalizer;
import java.util.Locale;
/** Stable, source-independent matching for visible movie/series entries. */
public final class CatalogIdentity {
    private CatalogIdentity(){}
    public static String normalized(String text){
        if(text==null)return "";
        String t=Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
            .replace('ي','ی').replace('ك','ک').replace('ة','ه').replace('ۀ','ه');
        t=t.replaceAll("(?i)\\b(20[0-9]{2}|19[0-9]{2}|episode|ep|season|dubbed|hd|4k|1080p|720p)\\b", " ");
        t=t.replaceAll("[\\p{P}\\p{S}\\u200c\\u200f\\u200e]", " ").replaceAll("\\s+", " ").trim();
        return t;
    }
    public static String key(FarsiPlexTitle item){
        if(item==null)return "";
        String text=normalized(item.name);
        String year=item.year==null?"":item.year.trim();
        return item.kind+"|"+text+"|"+(year.matches("(?:19|20)\\d{2}")?year:"");
    }
}
