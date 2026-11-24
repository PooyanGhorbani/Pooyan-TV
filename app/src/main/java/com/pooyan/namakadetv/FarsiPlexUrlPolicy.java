package com.pooyan.namakadetv;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Limits metadata retrieval to public HTTPS pages on the chosen source. */
public final class FarsiPlexUrlPolicy {
    private FarsiPlexUrlPolicy() {}
    public static final String SITE = "https://farsiplex.com";
    public static String normalize(String base, String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) return "";
        try {
            URI relative = new URI(candidate.trim());
            URI resolved = new URI(base).resolve(relative).normalize();
            if (!"https".equalsIgnoreCase(resolved.getScheme())) return "";
            String host=resolved.getHost();
            if (host == null || !("farsiplex.com".equalsIgnoreCase(host) || "www.farsiplex.com".equalsIgnoreCase(host))) return "";
            if (resolved.getUserInfo() != null || resolved.getPort() != -1) return "";
            String path=resolved.getPath();
            if (path == null || path.isEmpty()) path="/";
            // No URL fragments/queries are needed to identify public catalogue entries.
            return new URI("https",null,"farsiplex.com",-1,path,null,null).toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException e) { return ""; }
    }
    public static String mediaType(String url) {
        String n=normalize(SITE,url);
        if(n.isEmpty())return "";
        try {
            String p=new URI(n).getPath().toLowerCase(Locale.ROOT);
            if(p.matches("/movie/[^/]+/?"))return "movie";
            if(p.matches("/tvshow/[^/]+/?"))return "series";
            if(p.matches("/episode/[^/]+/?"))return "episode";
        } catch(Exception ignored){}
        return "";
    }
    public static String publicList(String kind,int page) {
        if(!"movie".equals(kind)&&!"series".equals(kind))throw new IllegalArgumentException("kind");
        if(page<1||page>10)throw new IllegalArgumentException("page");
        String p="movie".equals(kind)?"movie":"tvshow";
        return SITE+"/"+p+"/"+(page==1?"":"page/"+page+"/");
    }
}
