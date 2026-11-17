package com.pooyan.namakadetv;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses public catalog metadata and directly embedded public HTML5 video URLs only. */
public final class FarsiPlexParser {
    private static final Pattern YEAR=Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private FarsiPlexParser(){}
    public static List<FarsiPlexTitle> listing(String html,String page,String kind){
        Document document=Jsoup.parse(html,page);
        List<FarsiPlexTitle> results=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(Element link:document.select("a[href]")) {
            String url=FarsiPlexUrlPolicy.normalize(page,link.attr("href"));
            if(url.isEmpty() || !kind.equals(FarsiPlexUrlPolicy.mediaType(url)) || !seen.add(url))continue;
            Element parent=link.parent();
            String name=bestText(link, parent);
            if(name.isEmpty())name=slugLabel(url);
            if(name.isEmpty())continue;
            FarsiPlexTitle title=new FarsiPlexTitle(url,kind,name);
            Element img=link.selectFirst("img");
            if(img==null && parent!=null)img=parent.selectFirst("img");
            if(img!=null)title.poster=imageUrl(page,img);
            if(parent!=null){
                Matcher match=YEAR.matcher(parent.text());if(match.find())title.year=match.group();
                Element p=parent.selectFirst("p");if(p!=null)title.description=truncate(p.text(),290);
            }
            results.add(title);
            if(results.size()>=90)break;
        }
        return results;
    }
    public static FarsiPlexTitle details(String html,String url,String kind) {
        String trusted=FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,url);
        if(trusted.isEmpty()||FarsiPlexUrlPolicy.mediaType(trusted).isEmpty())throw new IllegalArgumentException("URL");
        Document d=Jsoup.parse(html,trusted);
        String title="";
        Element heading=d.selectFirst("h1");if(heading!=null)title=heading.text();
        if(title.isEmpty()){
            Element og=d.selectFirst("meta[property=og:title]");
            if(og!=null)title=og.attr("content").replace("– FarsiPlex","").trim();
        }
        if(title.isEmpty())title=slugLabel(trusted);
        FarsiPlexTitle t=new FarsiPlexTitle(trusted,kind,title);
        t.publicStream=PublicHtml5Media.find(d,trusted);
        Element ogImg=d.selectFirst("meta[property=og:image]");
        if(ogImg!=null)t.poster=FarsiPlexUrlPolicy.normalize(trusted,ogImg.attr("content"));
        if(t.poster.isEmpty()){
            for(Element img:d.select("img")){
                String alt=img.attr("alt");
                if(!alt.isEmpty() && (alt.contains(title)||title.contains(alt))){t.poster=imageUrl(trusted,img);if(!t.poster.isEmpty())break;}
            }
        }
        Element summary=d.selectFirst("h2:matchesOwn((?i)Synopsis)");
        if(summary!=null){
            Element next=summary.nextElementSibling();
            if(next!=null)t.description=truncate(next.text(),780);
        }
        if(t.description.isEmpty()){
            Element meta=d.selectFirst("meta[name=description]");if(meta!=null)t.description=truncate(meta.attr("content"),780);
        }
        if(t.description.isEmpty()){
            Element first=d.selectFirst("article p, .entry-content p, .description p");
            if(first!=null)t.description=truncate(first.text(),780);
        }
        Matcher y=YEAR.matcher(d.text());if(y.find())t.year=y.group();
        Element genre=d.selectFirst("a[href*=genres/]");if(genre!=null)t.genre=genre.text();
        if("series".equals(kind)) {
            Set<String> seen=new HashSet<>();
            for(Element a:d.select("a[href]")) {
                String target=FarsiPlexUrlPolicy.normalize(trusted,a.attr("href"));
                if(!"episode".equals(FarsiPlexUrlPolicy.mediaType(target))||!seen.add(target))continue;
                String label=a.text().trim();
                if(label.isEmpty()||label.equalsIgnoreCase("Image"))label="قسمت "+(t.episodes.size()+1);
                FarsiPlexTitle ep=new FarsiPlexTitle(target,"episode",label);
                Element img=a.selectFirst("img");if(img!=null)ep.poster=imageUrl(trusted,img);
                t.episodes.add(ep);
                if(t.episodes.size()>=250)break;
            }
        }
        return t;
    }
    private static String imageUrl(String base,Element img){
        for(String key:new String[]{"data-src","data-lazy-src","src"}){
            String n=FarsiPlexUrlPolicy.normalize(base,img.attr(key));
            if(!n.isEmpty()&&n.matches("(?i).*\\.(jpg|jpeg|webp|png)/?"))return n;
        }
        return "";
    }
    private static String bestText(Element link,Element parent){
        for(Element target:new Element[]{link, parent}){
            if(target==null)continue;
            for(String selector:new String[]{"h3","h2","h4",".title",".name"}){
                Element h=target.selectFirst(selector);
                if(h!=null&&useful(h.text()))return truncate(h.text().trim(),115);
            }
            if(target==link&&useful(link.text()))return truncate(link.text().trim(),115);
        }
        Element img=link.selectFirst("img");
        if(img!=null&&useful(img.attr("alt")))return truncate(img.attr("alt"),115);
        if(parent!=null){img=parent.selectFirst("img");if(img!=null&&useful(img.attr("alt")))return truncate(img.attr("alt"),115);}
        return "";
    }
    private static boolean useful(String s){return s!=null&&!s.trim().isEmpty()&&s.trim().length()>2&&!s.trim().equalsIgnoreCase("image")&&!s.trim().equalsIgnoreCase("read more");}
    private static String slugLabel(String url){
        String[] parts=url.replaceAll("/+$","").split("/");
        String slug=parts[parts.length-1];return slug.replace('-',' ').trim();
    }
    private static String truncate(String s,int max){return s==null?"":s.trim().substring(0,Math.min(max,s.trim().length()));}
}
