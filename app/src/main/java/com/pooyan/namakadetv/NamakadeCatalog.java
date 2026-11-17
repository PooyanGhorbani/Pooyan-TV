package com.pooyan.namakadetv;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict metadata-only adapter for the public movie and series pages. No embedded player scraping. */
public final class NamakadeCatalog {
    public static final String BASE="https://namakadeh.com";
    private NamakadeCatalog(){}
    public static String safeUrl(String url){
        try{
            URI u=new URI(url).normalize();
            if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1)return "";
            String host=u.getHost();
            if(!"namakadeh.com".equalsIgnoreCase(host)&&!"namakade.com".equalsIgnoreCase(host))return "";
            String path=u.getPath();
            if(path==null||path.length()>300||path.contains(".."))return "";
            // Catalog and metadata only; no dynamic player or arbitrary redirect URLs.
            if(!path.equals("/movies")&&!path.equals("/serial")&&!path.startsWith("/movies/")
                    &&!path.startsWith("/iran-1-movies/")&&!path.startsWith("/serieses/")
                    &&!path.startsWith("/series/"))return "";
            return new URI("https",null,host.toLowerCase(java.util.Locale.ROOT),-1,path,null,null).toString();
        }catch(Exception ex){return "";}
    }
    public static String start(String kind){return BASE+("series".equals(kind)?"/serial":"/movies");}
    public static List<FarsiPlexTitle> listing(String html,String page,String kind){
        Document doc=Jsoup.parse(html,page);
        List<FarsiPlexTitle> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Element a:doc.select("a[href]")){
            String url=safeUrl(a.absUrl("href"));
            if(url.isEmpty()||!seen.add(url)||url.equals(start(kind)))continue;
            String path=URI.create(url).getPath();
            if("movie".equals(kind)&&!(path.startsWith("/movies/")||path.startsWith("/iran-1-movies/")))continue;
            if("series".equals(kind)&&!(path.startsWith("/serieses/")||path.startsWith("/series/")))continue;
            String name="";
            for(String selector:new String[]{"h3","h4","h2",".title","strong"}){
                Element n=a.selectFirst(selector);
                if(n!=null&&!n.text().trim().isEmpty()){name=n.text().trim();break;}
            }
            if(name.isEmpty()){
                Element img=a.selectFirst("img[alt]");if(img!=null)name=img.attr("alt").trim();
            }
            if(name.isEmpty())name=a.text().replaceAll("(?i)\\b(Watch Now|Episode[s]?:?\\s*\\d+|Director:.*|Genre:.*)\\b", " ").trim();
            Element card=a.closest("li,.item,.owl-item,.movie-item,.series-item,.col-md-3");
            String label=(card==null?a.text():card.text()).toLowerCase(java.util.Locale.ROOT);
            if(label.matches("(?s).*\\bforeign\\b.*")||label.contains("خارجی")||label.contains("ترکی")||label.contains("turkish"))continue;
            if(name.length()<2||name.length()>110)continue;
            FarsiPlexTitle item=new FarsiPlexTitle(url,kind,name);
            Element img=a.selectFirst("img");
            if(img!=null){
                for(String attr:new String[]{"data-src","src"}){
                    String raw=img.absUrl(attr);
                    if(raw.startsWith("https://") && (raw.matches("(?i).*\\.(png|jpeg|jpg|webp)(\\?.*)?"))){item.poster=raw;break;}
                }
            }
            out.add(item);if(out.size()>=70)break;
        }
        return out;
    }
    public static FarsiPlexTitle details(String html,String url,String kind){
        if(safeUrl(url).isEmpty())throw new IllegalArgumentException("unsafe url");
        Document doc=Jsoup.parse(html,url);
        Element h=doc.selectFirst("h1");String title=h==null?"":h.text().trim();
        if(title.isEmpty())title=url.substring(url.lastIndexOf('/')+1).replace('-',' ');
        FarsiPlexTitle item=new FarsiPlexTitle(url,kind,title);
        Element img=doc.selectFirst("meta[property=og:image]");
        if(img!=null&&img.hasAttr("content")){
            String poster=img.attr("content");if(poster.startsWith("https://"))item.poster=poster;
        }
        Element desc=doc.selectFirst("meta[name=description]");
        if(desc!=null)item.description=desc.attr("content");
        // Do not guess video URLs, bypass access controls, or claim playback from page metadata.
        item.playbackError="منبع پخش عمومی معتبر برای این عنوان تأیید نشده است.";
        return item;
    }
}
