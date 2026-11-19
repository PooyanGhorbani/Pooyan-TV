package com.pooyan.namakadetv;

import android.app.Activity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreign-language open cinema: explicitly credited CC-BY sample films plus
 * licensed, curated Internet Archive movies. No account, torrent, cookie or DRM bypass.
 * Browse/search records are only metadata; playable URLs are selected from actual files
 * on the Archive metadata endpoint and never fabricated from the film's page URL.
 */
public final class ArchiveMoviesRepository {
    private static final String ARCHIVE = "https://archive.org/";
    private static final String SAMPLE = "https://storage.googleapis.com/gtv-videos-bucket/sample/";
    private static final int MAX_JSON = 2_500_000;
    private static final String QUERY = "mediatype:movies AND collection:(feature_films OR prelinger OR moviesandfilms OR short_films) "
        + "AND (licenseurl:https://creativecommons.org* OR licenseurl:http://creativecommons.org*) "
        + "AND -title:(episode* OR trailer* OR teaser*)";
    private final Activity activity;
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private volatile boolean closed;

    public ArchiveMoviesRepository(Activity activity) { this.activity=activity; }

    private static final class OpenFilm {
        final String file,name,year,about;
        OpenFilm(String file,String name,String year,String about) {
            this.file=file;this.name=name;this.year=year;this.about=about;
        }
    }
    private static final OpenFilm[] FILMS = {
        new OpenFilm("BigBuckBunny.mp4","Big Buck Bunny","2008","Animated short film · Blender Foundation · CC BY 3.0"),
        new OpenFilm("Sintel.mp4","Sintel","2010","Fantasy animated short film · Blender Foundation · CC BY 3.0"),
        new OpenFilm("ElephantsDream.mp4","Elephants Dream","2006","Animated short film · Blender Foundation · CC BY 2.5"),
        new OpenFilm("TearsOfSteel.mp4","Tears of Steel","2012","Science-fiction short film · Blender Foundation · CC BY 3.0")
    };
    private static String segment(String text) {
        try{return URLEncoder.encode(text,"UTF-8").replace("+","%20");}
        catch(Exception ignored){return "";}
    }
    private static boolean id(String value) {return value!=null && value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}") && !value.contains("..");}
    private static boolean sampleUrl(String raw) {
        if(raw==null)return false;
        for(OpenFilm film:FILMS)if((SAMPLE+film.file).equals(raw))return true;
        return false;
    }
    public static String safePage(String raw) {
        if(sampleUrl(raw))return raw;
        if(raw==null||raw.length()>250)return "";
        try {
            URI u=new URI(raw);
            if(!"https".equalsIgnoreCase(u.getScheme()) || !"archive.org".equalsIgnoreCase(u.getHost())
                    || u.getPort()!=-1 || u.getRawUserInfo()!=null || u.getRawQuery()!=null || u.getRawFragment()!=null)return "";
            String path=u.getRawPath();
            if(path==null||!path.startsWith("/details/") || !id(path.substring(9)))return "";
            return ARCHIVE+"details/"+path.substring(9);
        }catch(Exception ignored){return "";}
    }
    public boolean supports(FarsiPlexTitle title){return title!=null && !safePage(title.url).isEmpty();}

    public static List<FarsiPlexTitle> openFilms() {
        List<FarsiPlexTitle> items=new ArrayList<>();
        for(OpenFilm film:FILMS){
            String url=SAMPLE+film.file;
            FarsiPlexTitle title=new FarsiPlexTitle(url,"movie",film.name);
            title.year=film.year;title.genre="Animation · Short Film";
            title.description=film.about;
            title.poster=SAMPLE+"images/"+film.file.replace(".mp4",".jpg");
            title.publicStream=url;
            title.candidates.add(new PlaybackCandidate(url,url,0,false));
            items.add(title);
        }
        return items;
    }

    /** Require a visible Creative Commons or public-domain declaration, never infer one. */
    static boolean declaredLicense(String raw){
        if(raw==null)return false;
        try{
            URI u=new URI(raw.trim());String scheme=u.getScheme();String host=u.getHost();String path=u.getPath();
            return ("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))
                && "creativecommons.org".equalsIgnoreCase(host)&&path!=null
                && (path.startsWith("/licenses/")||path.startsWith("/publicdomain/"));
        }catch(Exception ignored){return false;}
    }
    private static String year(JSONObject item){
        String[] fields={"year","date","publicdate"};
        for(String field:fields){String value=item.optString(field,"");
            java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?:18|19|20)\\d{2}").matcher(value);
            if(match.find())return match.group();
        }return "";
    }
    private static String text(String raw,int limit){
        if(raw==null)return "";
        String safe=raw.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();
        return safe.substring(0,Math.min(safe.length(),limit));
    }
    private static String title(JSONObject obj){
        Object raw=obj.opt("title");
        return raw instanceof String?text((String)raw,170):"";
    }
    private static boolean looksLikeFilm(String name){
        return !name.toLowerCase(Locale.ROOT).matches(".*(?:\\btrailer\\b|\\bteaser\\b|\\bepisode\\s*\\d+).*");
    }
    static List<FarsiPlexTitle> catalog(JSONObject payload,int limit){
        JSONObject response=payload.optJSONObject("response");
        JSONArray docs=response==null?null:response.optJSONArray("docs");
        if(docs==null)return Collections.emptyList();
        List<FarsiPlexTitle> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<docs.length()&&out.size()<limit;i++){
            JSONObject obj=docs.optJSONObject(i);if(obj==null)continue;
            String identifier=obj.optString("identifier","");String name=title(obj);
            if(!id(identifier)||name.isEmpty()||!looksLikeFilm(name)||!declaredLicense(obj.optString("licenseurl", ""))||!seen.add(identifier))continue;
            FarsiPlexTitle film=new FarsiPlexTitle(ARCHIVE+"details/"+identifier,"movie",name);
            film.year=year(obj);film.description=text(obj.optString("description",""),1200);
            film.poster=ARCHIVE+"services/img/"+identifier;
            out.add(film);
        }
        return out;
    }

    private static String readJson(String url)throws Exception {
        if(!url.startsWith(ARCHIVE) || !StreamDetector.isHttpsUrl(url))throw new java.io.IOException("Invalid archive URL");
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(6500);c.setReadTimeout(9000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","PooyanTV/0.8.2 (Android TV; open-film metadata)");
            c.setRequestProperty("Accept","application/json");
            if(c.getResponseCode()!=200)throw new java.io.IOException("Archive API unavailable");
            String ct=c.getContentType();
            if(ct==null||!ct.toLowerCase(Locale.ROOT).contains("json")||c.getContentLength()>MAX_JSON)throw new java.io.IOException("Invalid metadata");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] part=new byte[8192];int n;
                while((n=in.read(part))!=-1){if(out.size()+n>MAX_JSON)throw new java.io.IOException("Metadata too large");out.write(part,0,n);}
                return new String(out.toByteArray(),StandardCharsets.UTF_8);
            }
        }finally{if(c!=null)c.disconnect();}
    }
    private static JSONObject query(String q,int page)throws Exception {
        String address=ARCHIVE+"advancedsearch.php?q="+segment(q)
            +"&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=year&fl%5B%5D=date&fl%5B%5D=licenseurl"
            +"&sort%5B%5D=downloads+desc&rows=30&page="+page+"&output=json";
        return new JSONObject(readJson(address));
    }
    private void delivery(List<FarsiPlexTitle> films,String error,FarsiPlexRepository.Result cb){
        if(closed)return;
        activity.runOnUiThread(()->{if(!closed)cb.receive(films,error);});
    }
    public void list(String kind,int page,FarsiPlexRepository.Result cb){
        if(!"movie".equals(kind)||page<1||page>10){cb.receive(Collections.emptyList(),null);return;}
        worker.execute(()->{
            List<FarsiPlexTitle> items=page==1?openFilms():new ArrayList<>();String error=null;
            try{for(FarsiPlexTitle film:catalog(query(QUERY,page),24))items.add(film);}
            catch(Exception ignored){if(items.isEmpty())error="دریافت فیلم‌های آزاد از آرشیو ممکن نشد؛ اتصال شبکه را بررسی کن.";}
            delivery(items,error,cb);
        });
    }
    public void search(String input,FarsiPlexRepository.Result cb){
        String term=input==null?"":input.trim();if(term.isEmpty()){cb.receive(Collections.emptyList(),null);return;}
        if(term.length()>75)term=term.substring(0,75);
        final String cleaned=term.replaceAll("[^\\p{L}\\p{N}\\s-]"," ").replaceAll("\\s+"," ").trim();
        if(cleaned.isEmpty()){cb.receive(Collections.emptyList(),null);return;}
        worker.execute(()->{
            List<FarsiPlexTitle> out=new ArrayList<>();String error=null;
            for(FarsiPlexTitle film:openFilms())if(SearchPolicy.matches(film,cleaned))out.add(film);
            try{
                String escaped=cleaned.replace("\\","\\\\").replace("\"","\\\"");
                out.addAll(catalog(query(QUERY+" AND (title:\""+escaped+"\" OR description:\""+escaped+"\")",1),24));
            }catch(Exception ignored){if(out.isEmpty())error="جست‌وجوی فیلم‌های آزاد در حال حاضر در دسترس نیست.";}
            delivery(out,error,cb);
        });
    }
    static List<PlaybackCandidate> mediaFiles(String identifier,JSONObject response){
        JSONArray files=response.optJSONArray("files");if(!id(identifier)||files==null)return Collections.emptyList();
        List<PlaybackCandidate> list=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<files.length()&&list.size()<16;i++){
            JSONObject file=files.optJSONObject(i);if(file==null||file.optBoolean("private",false))continue;
            String name=file.optString("name","");
            if(name.isEmpty()||name.length()>180||name.contains("/")||name.contains("\\")||name.contains(".."))continue;
            String lower=name.toLowerCase(Locale.ROOT);
            if(!(lower.endsWith(".mp4")||lower.endsWith(".m4v")||lower.endsWith(".webm")))continue;
            if(!seen.add(name))continue;
            String url=ARCHIVE+"download/"+identifier+"/"+segment(name);
            if(!StreamDetector.isDirectMedia(url))continue;
            int height=0;
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("(2160|1440|1080|720|480|360)[pP]?").matcher(lower);
            if(m.find())height=Integer.parseInt(m.group(1));
            list.add(new PlaybackCandidate(url,ARCHIVE+"details/"+identifier,height,false));
        }
        list.sort((a,b)->Integer.compare(b.height,a.height));
        return list;
    }
    public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){
        String page=safePage(item.url);
        if(page.isEmpty()){cb.receive(item,"آدرس فیلم معتبر نیست.");return;}
        if(sampleUrl(page)){
            for(FarsiPlexTitle film:openFilms())if(film.url.equals(page)){
                cb.receive(film,null);return;
            }
        }
        worker.execute(()->{
            FarsiPlexTitle full=item;String error=null;
            try{
                String identifier=page.substring((ARCHIVE+"details/").length());
                JSONObject response=new JSONObject(readJson(ARCHIVE+"metadata/"+identifier));
                JSONObject metadata=response.optJSONObject("metadata");
                if(metadata==null || response.optBoolean("is_dark") || !"movies".equals(metadata.optString("mediatype"))
                        || !declaredLicense(metadata.optString("licenseurl", "")))throw new java.io.IOException("Unavailable or unlicensed film");
                List<PlaybackCandidate> playable=mediaFiles(identifier,response);
                full.candidates.clear();full.candidates.addAll(playable);
                full.publicStream=playable.isEmpty()?"":playable.get(0).url;
                if(full.name==null||full.name.isEmpty())full.name=title(metadata);
                if(full.year.isEmpty())full.year=year(metadata);
                if(full.description.isEmpty())full.description=text(metadata.optString("description",""),1200);
                if(full.publicStream.isEmpty())full.playbackError="این عنوان فایل مستقیم قابل‌پخش ندارد.";
            }catch(Exception ignored){
                full.publicStream="";full.candidates.clear();full.playbackError="لینک قابل‌پخش از آرشیو دریافت نشد.";
                error="ممکن است فیلم حذف شده باشد یا اینترنت به آرشیو دسترسی نداشته باشد.";
            }
            FarsiPlexTitle ready=full;String message=error;
            if(!closed)activity.runOnUiThread(()->{if(!closed)cb.receive(ready,message);});
        });
    }
    public void close(){closed=true;worker.shutdownNow();}
}
