package com.pooyan.namakadetv;
import java.net.URI;
import java.util.Locale;
/** Tight host allowlist for cover art, not a general web proxy. */
public final class ImageUrlPolicy {
    private ImageUrlPolicy(){}
    public static boolean allowed(String url){
        if(url==null||url.isEmpty()||url.length()>1800)return false;
        try{
            URI u=new URI(url);
            if(!"https".equalsIgnoreCase(u.getScheme())||u.getPort()!=-1||u.getUserInfo()!=null||u.getFragment()!=null)return false;
            String host=u.getHost();if(host==null)return false;
            host=host.toLowerCase(Locale.ROOT);
            if(!host.equals("farsiplex.com")&&!host.equals("www.farsiplex.com")
                &&!host.equals("namakadeh.com")&&!host.equals("namakade.com")
                &&!host.equals("media.negahestan.com"))return false;
            String path=u.getPath();
            return path!=null&&path.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|webp|png)");
        }catch(Exception e){return false;}
    }
    public static boolean allowedFor(String item,String poster){
        if(!allowed(poster))return false;
        try{
            String h=new URI(poster).getHost().toLowerCase(Locale.ROOT);
            URI source=new URI(item);
            if(!"https".equalsIgnoreCase(source.getScheme())||source.getUserInfo()!=null||source.getPort()!=-1)return false;
            String itemHost=source.getHost()==null?"":source.getHost().toLowerCase(Locale.ROOT);
            String itemPath=source.getPath()==null?"":source.getPath();
            if(itemHost.equals("namakadeh.com")||itemHost.equals("namakade.com")){
                if(!itemPath.startsWith("/movies/")&&!itemPath.startsWith("/series/")&&!itemPath.startsWith("/serieses/")&&!itemPath.startsWith("/iran-1-movies/"))return false;
                return h.equals("namakadeh.com")||h.equals("namakade.com")||h.equals("media.negahestan.com");
            }
            return !FarsiPlexUrlPolicy.mediaType(item).isEmpty()&&(h.equals("farsiplex.com")||h.equals("www.farsiplex.com"));
        }catch(Exception e){return false;}
    }
}
