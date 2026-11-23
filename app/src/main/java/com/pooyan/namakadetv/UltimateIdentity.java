package com.pooyan.namakadetv;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Conservative cross-provider identity. Ambiguity means NO merge, never a guessed match. */
public final class UltimateIdentity {
    private static final Pattern YEAR=Pattern.compile("(?:19|20)\\d{2}");
    private static final Pattern IMDB=Pattern.compile("tt[0-9]{7,12}");
    private static final Pattern TMDB=Pattern.compile("tmdb:(movie|tv):[0-9]+");
    private UltimateIdentity(){}
    public static String norm(String text){return CatalogIdentity.normalized(text);}
    public static String trustedId(FarsiPlexTitle a){
        if(a==null||a.stableId==null)return "";
        String id=a.stableId.trim().toLowerCase(Locale.ROOT);
        return IMDB.matcher(id).matches()||TMDB.matcher(id).matches()?id:"";
    }
    static Set<String> names(FarsiPlexTitle item){
        Set<String> out=new LinkedHashSet<>();
        if(item==null)return out;
        String n=norm(item.name);if(n.length()>=3)out.add(n);
        for(String alias:item.aliases){n=norm(alias);if(n.length()>=3)out.add(n);}
        return out;
    }
    public static boolean sameTitle(FarsiPlexTitle a,FarsiPlexTitle b){
        if(a==null||b==null||!a.kind.equals(b.kind))return false;
        if("episode".equals(a.kind)){
            if(a.season<1||b.season<1||a.episode<1||b.episode<1)return a.url.equals(b.url);
            if(a.season!=b.season||a.episode!=b.episode)return false;
            // Episode numbers alone are never globally unique; only same original page is safe.
            return a.url.equals(b.url);
        }
        if("series".equals(a.kind) && a.season>0&&b.season>0&&a.season!=b.season)return false;
        String ai=trustedId(a),bi=trustedId(b);
        if(!ai.isEmpty()&&!bi.isEmpty())return ai.equals(bi);
        if(a.year==null||b.year==null||!YEAR.matcher(a.year.trim()).matches()||!YEAR.matcher(b.year.trim()).matches())
            return a.url.equals(b.url); // No risky merge on missing dates.
        if(!a.year.trim().equals(b.year.trim()))return false;
        Set<String> aa=names(a),bb=names(b);
        for(String name:aa)if(bb.contains(name))return true;
        return a.url.equals(b.url);
    }
    public static String contentKey(FarsiPlexTitle item){
        if(item==null)return "";
        String trusted=trustedId(item);
        if(!trusted.isEmpty())return item.kind+"|"+trusted;
        String name=norm(item.name);
        String year=item.year==null?"":item.year.trim();
        if(!"episode".equals(item.kind)&&YEAR.matcher(year).matches()&&!name.isEmpty())
            return item.kind+"|"+year+"|"+name;
        return item.kind+"|page:"+sha256(safePage(item.url));
    }
    public static String safePage(String url){
        try{URI u=new URI(url);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null)return "";
            return "https://"+u.getHost().toLowerCase(Locale.ROOT)+(u.getRawPath()==null?"/":u.getRawPath());
        }catch(Exception e){return "";}
    }
    public static String sourceFingerprint(String url){return sha256(safePage(url));}
    public static String sha256(String input){
        try{byte[] data=MessageDigest.getInstance("SHA-256").digest((input==null?"":input).getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();for(byte v:data)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
