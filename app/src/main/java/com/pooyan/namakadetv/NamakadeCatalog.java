package com.pooyan.namakadetv;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Source-specific public catalog, episode markup and explicit HTML5 media adapter. No JS execution or private APIs. */
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
                    &&!path.startsWith("/iran-1-movies/")&&!path.startsWith("/best-1-movies/")&&!path.startsWith("/serieses/")
                    &&!path.startsWith("/series/"))return "";
            return "https://"+host.toLowerCase(java.util.Locale.ROOT)+u.getRawPath()+(u.getRawQuery()==null?"":"?"+u.getRawQuery());
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
            if("movie".equals(kind)&&!(path.startsWith("/movies/")||path.startsWith("/iran-1-movies/")||path.startsWith("/best-1-movies/")))continue;
            if("series".equals(kind)&&!(path.startsWith("/serieses/")||path.startsWith("/series/")))continue;
            String name="";
            for(String selector:new String[]{".SSh3","h3","h4","h2",".title","strong"}){
                Element n=a.selectFirst(selector);
                if(n!=null&&!n.text().trim().isEmpty()){name=n.text().trim();break;}
            }
            if(name.isEmpty()){
                Element img=a.selectFirst("img[alt]");if(img!=null)name=img.attr("alt").trim();
            }
            if(name.isEmpty())name=a.text().replaceAll("(?i)\\b(Watch Now|Episode[s]?:?\\s*\\d+|Director:.*|Genre:.*)\\b", " ").trim();
            Element card=a.closest(".divPicMask2,li,.item,.owl-item,.movie-item,.series-item,.col-md-3");
            String label=(card==null?a.text():card.text()).toLowerCase(java.util.Locale.ROOT);
            if(label.matches("(?s).*\\bforeign\\b.*")||label.contains("خارجی")||label.contains("ترکی")||label.contains("turkish"))continue;
            if(name.length()<2||name.length()>110)continue;
            FarsiPlexTitle item=new FarsiPlexTitle(url,kind,name);
            Element img=a.selectFirst("img");
            if(img==null&&card!=null)img=card.selectFirst("img");
            if(img!=null){
                for(String attr:new String[]{"data-src","src"}){
                    String raw=img.absUrl(attr);
                    if(ImageUrlPolicy.allowedFor(url,raw)){item.poster=raw;break;}
                }
            }
            out.add(item);if(out.size()>=70)break;
        }
        return out;
    }
    private static boolean sameSeries(String page,String episode){
        String[] a=URI.create(page).getPath().split("/"),b=URI.create(episode).getPath().split("/");
        return a.length>=3&&b.length>=5&&a[2].equals(b[2]);
    }
    public static String publishedMedia(Document doc,String page){
        if(safeUrl(page).isEmpty())return "";
        for(Element tag:doc.select("video[src],video source[src]")){
            String media=tag.absUrl("src");
            try{URI u=URI.create(media);
                if(StreamDetector.isDirectMedia(media)&&"media.negahestan.com".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null)return media;
            }catch(Exception ignored){}
        }
        return "";
    }
    public static FarsiPlexTitle details(String html,String url,String kind){
        if(safeUrl(url).isEmpty())throw new IllegalArgumentException("unsafe url");
        Document doc=Jsoup.parse(html,url);
        Element h=doc.selectFirst("h1,#divTitrGrid");String title=h==null?"":h.text().trim();
        Element ogTitle=doc.selectFirst("meta[property=og:title],meta[name=og:title]");
        if(title.isEmpty()&&ogTitle!=null)title=ogTitle.attr("content").split("\\|")[0].trim();
        if(title.isEmpty())title=url.substring(url.lastIndexOf('/')+1).replace('-',' ');
        FarsiPlexTitle item=new FarsiPlexTitle(url,kind,title);
        Element img=doc.selectFirst("meta[property=og:image],meta[name=og:image]");
        if(img!=null&&img.hasAttr("content")){
            String poster=doc.location().isEmpty()?img.attr("content"):img.absUrl("content");if(ImageUrlPolicy.allowedFor(url,poster))item.poster=poster;
        }
        Element desc=doc.selectFirst("meta[name=description]");
        if(desc!=null)item.description=desc.attr("content");
        if(item.poster.isEmpty()){
            Element video=doc.selectFirst("video[poster]");
            if(video!=null&&ImageUrlPolicy.allowedFor(url,video.absUrl("poster")))item.poster=video.absUrl("poster");
        }
        if("series".equals(kind)){
            Set<String> seen=new HashSet<>();
            for(Element a:doc.select("a[href*=/episodes/]")){
                String target=safeUrl(a.absUrl("href"));
                if(target.isEmpty()||!sameSeries(url,target)||!seen.add(target))continue;
                Element row=a.closest("li");Element number=row==null?null:row.selectFirst("[id^=divEpiNo]");
                String label=number==null?a.text().trim():"قسمت "+number.text().trim();
                if(label.isEmpty())label="قسمت "+(item.episodes.size()+1);
                FarsiPlexTitle ep=new FarsiPlexTitle(target,"episode",label);
                Element art=row==null?a.selectFirst("img"):row.selectFirst("img");
                if(art!=null&&ImageUrlPolicy.allowedFor(target,art.absUrl("src")))ep.poster=art.absUrl("src");
                if(ep.poster.isEmpty())ep.poster=item.poster;
                item.episodes.add(ep);if(item.episodes.size()>=250)break;
            }
        }
        // Direct media is only a candidate until the repository probes its bytes.
        item.publicStream=publishedMedia(doc,url);
        item.playbackError=item.publicStream.isEmpty()?"منبع پخش عمومی معتبر برای این عنوان تأیید نشده است؛ امکان نیاز به دسترسی رسمی یا تغییر ساختار سایت وجود دارد.":"";
        return item;
    }
}
