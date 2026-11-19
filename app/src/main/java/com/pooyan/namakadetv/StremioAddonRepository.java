package com.pooyan.namakadetv;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Opt-in adapter for standard Stremio HTTPS catalog/meta/stream JSON endpoints.
 * No source-specific credentials, browser cookies, DRM bypass, arbitrary scripts or video proxies.
 * Third-party add-ons receive searches only after the user enters a manifest URL.
 */
public final class StremioAddonRepository {
    private static final String PREFS = "pooyan_stremio_addon_v1";
    private static final String KEY = "manifest";
    private static final int MAX_JSON = 950_000;
    private final Activity activity;
    private final ExecutorService io = Executors.newFixedThreadPool(2);

    public StremioAddonRepository(Activity activity) { this.activity = activity; }

    public static String configuredManifest(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").trim();
    }

    public static boolean configure(Context context, String manifest) {
        String next = manifest == null ? "" : manifest.trim();
        if (!next.isEmpty() && !AddonEndpointPolicy.validManifest(next)) return false;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (next.isEmpty()) editor.remove(KEY); else editor.putString(KEY, next);
        editor.apply();
        return true;
    }

    public boolean enabled() { return AddonEndpointPolicy.validManifest(configuredManifest(activity)); }

    public boolean supports(FarsiPlexTitle item) {
        return item != null && !AddonEndpointPolicy.typeFor(configuredManifest(activity), item.url).isEmpty();
    }

    public static String kindFor(Context context, String itemUrl) {
        return AddonEndpointPolicy.typeFor(configuredManifest(context), itemUrl);
    }

    private static String req(String url, int cap) throws Exception {
        if (!StreamDetector.isHttpsUrl(url)) throw new java.io.IOException("Invalid HTTPS URL");
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(6500);c.setReadTimeout(7000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "PooyanTV/0.8 (Android TV; public JSON addon client)");
            if (c.getResponseCode() != 200) throw new java.io.IOException("Addon response unavailable");
            String ct = c.getContentType();
            if (ct == null || !(ct.toLowerCase(java.util.Locale.ROOT).contains("json")
                || ct.toLowerCase(java.util.Locale.ROOT).contains("text/plain"))) throw new java.io.IOException("Unexpected response type");
            if (c.getContentLength() > cap) throw new java.io.IOException("Response too large");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192];int n;
                while ((n = in.read(bytes)) != -1) {
                    if (out.size() + n > cap) throw new java.io.IOException("Response too large");
                    out.write(bytes,0,n);
                }
                return new String(out.toByteArray(),StandardCharsets.UTF_8);
            }
        } finally { if (c != null) c.disconnect(); }
    }

    private static JSONObject json(String url, int max) throws Exception { return new JSONObject(req(url,max)); }

    private static String first(String a, String b) {return a == null || a.trim().isEmpty() ? b : a;}

    private static String year(JSONObject obj) {
        String year = first(obj.optString("releaseInfo"), first(obj.optString("year"), obj.optString("released")));
        java.util.regex.Matcher match = java.util.regex.Pattern.compile("(?:19|20)\\d{2}").matcher(year);
        return match.find()?match.group():"";
    }

    private static String genres(JSONObject obj) {
        JSONArray genres = obj.optJSONArray("genres");
        if (genres == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i=0;i<Math.min(genres.length(),4);i++) {
            String v = genres.optString(i,"").trim();
            if (!v.isEmpty()) {if (out.length()>0) out.append(" · ");out.append(v);}
        }
        return out.toString();
    }

    private static FarsiPlexTitle title(String manifest, JSONObject obj, String fallbackType) {
        String id = obj.optString("id","").trim();
        String type = obj.optString("type",fallbackType);
        if (!"movie".equals(type) && !"series".equals(type)) type=fallbackType;
        if (!"movie".equals(type) && !"series".equals(type)) return null;
        String link = AddonEndpointPolicy.metaUrl(manifest, type, id);
        String name = obj.optString("name", "").trim();
        if (link.isEmpty() || name.isEmpty() || name.length()>250) return null;
        FarsiPlexTitle item = new FarsiPlexTitle(link,type,name);
        item.stableId=id.startsWith("tt") && id.matches("tt\\d{5,12}")?id:"";
        item.year=year(obj);
        item.genre=genres(obj);
        item.description=obj.optString("description","");
        if (item.description.length()>2000) item.description=item.description.substring(0,2000);
        String poster=first(obj.optString("poster"),obj.optString("background"));
        if (ImageUrlPolicy.allowed(poster)) item.poster=poster;
        return item;
    }

    static List<FarsiPlexTitle> metas(String manifest, JSONObject payload, String type, int cap) {
        JSONArray entries = payload.optJSONArray("metas");
        if (entries == null) return Collections.emptyList();
        List<FarsiPlexTitle> titles = new ArrayList<>();Set<String> seen = new HashSet<>();
        for(int i=0;i<entries.length() && titles.size()<cap;i++) {
            JSONObject item=entries.optJSONObject(i);
            if(item==null) continue;
            FarsiPlexTitle parsed=title(manifest,item,type);
            if(parsed!=null && seen.add(parsed.url))titles.add(parsed);
        }
        return titles;
    }

    private static boolean searchSupported(JSONObject catalog) {
        JSONArray extra=catalog.optJSONArray("extra");
        if(extra==null)return false;
        for(int i=0;i<extra.length();i++) {
            JSONObject field=extra.optJSONObject(i);
            if(field!=null && "search".equals(field.optString("name")))return true;
        }
        return false;
    }

    private static boolean requiresOtherExtras(JSONObject catalog) {
        JSONArray extra=catalog.optJSONArray("extra");
        if(extra==null)return false;
        for(int i=0;i<extra.length();i++){
            JSONObject field=extra.optJSONObject(i);
            if(field!=null && field.optBoolean("isRequired") && !"search".equals(field.optString("name")))return true;
        }
        return false;
    }

    private static List<JSONObject> catalogs(JSONObject manifest, String kind, boolean searching) {
        List<JSONObject> out = new ArrayList<>();JSONArray all=manifest.optJSONArray("catalogs");
        if(all==null)return out;
        for(int i=0;i<all.length() && out.size()<4;i++){
            JSONObject catalog=all.optJSONObject(i);
            if(catalog==null || !kind.equals(catalog.optString("type")))continue;
            if(catalog.optString("id","").trim().isEmpty() || requiresOtherExtras(catalog))continue;
            if(searching != searchSupported(catalog))continue;
            out.add(catalog);
        }
        return out;
    }

    private void runList(String kind,String query,FarsiPlexRepository.Result cb) {
        if (!enabled()) { cb.receive(Collections.emptyList(),null);return; }
        io.execute(()->{
            List<FarsiPlexTitle> found=new ArrayList<>();String error=null;
            try {
                String manifest=configuredManifest(activity),base=AddonEndpointPolicy.base(manifest);
                JSONObject meta=json(manifest,150_000);
                Set<String> seen=new HashSet<>();
                String[] types="search".equals(kind)?new String[]{"movie","series"}:new String[]{kind};
                int fetched=0;
                for(String type:types) {
                    for(JSONObject catalog:catalogs(meta,type,query!=null)) {
                        if(fetched++ >= 3)break;
                        String id=AddonEndpointPolicy.segment(catalog.optString("id"));
                        if(id.isEmpty())continue;
                        String path="catalog/"+type+"/"+id;
                        if(query!=null)path+="/search="+AddonEndpointPolicy.segment(query);
                        JSONObject response=json(base+path+".json",MAX_JSON);
                        for(FarsiPlexTitle item:metas(manifest,response,type,24))if(seen.add(item.url))found.add(item);
                        if(found.size()>=36)break;
                    }
                }
            } catch(Exception e){error="یکی از افزونه‌های انتخابی پاسخ نداد؛ نتایج دیگر حفظ شدند.";}
            List<FarsiPlexTitle> values=found;String msg=error;
            activity.runOnUiThread(()->cb.receive(values,msg));
        });
    }

    public void list(String kind,int page,FarsiPlexRepository.Result callback) {
        if(page!=1 || !("movie".equals(kind)||"series".equals(kind))) {callback.receive(Collections.emptyList(),null);return;}
        runList(kind,null,callback);
    }

    public void search(String query,FarsiPlexRepository.Result callback) {
        if(query==null||query.trim().isEmpty()){callback.receive(Collections.emptyList(),null);return;}
        runList("search",query.trim().substring(0,Math.min(query.trim().length(),90)),callback);
    }

    private static int resolution(String label) {
        String s=label.toLowerCase(java.util.Locale.ROOT);
        if(s.contains("2160")||s.contains("4k"))return 2160;
        if(s.contains("1440"))return 1440;
        if(s.contains("1080"))return 1080;
        if(s.contains("720"))return 720;
        if(s.contains("480"))return 480;
        return 0;
    }

    static void streams(FarsiPlexTitle title,JSONObject response) {
        JSONArray all=response.optJSONArray("streams");
        if(all==null)return;
        List<PlaybackCandidate> options=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int i=0;i<all.length()&&options.size()<16;i++){
            JSONObject stream=all.optJSONObject(i);if(stream==null)continue;
            JSONObject behavior=stream.optJSONObject("behaviorHints");
            if(behavior!=null && behavior.optBoolean("notWebReady"))continue;
            String url=stream.optString("url","").trim();
            if(!StreamDetector.isDirectMedia(url)||!seen.add(url))continue;
            String quality=stream.optString("title","")+" "+stream.optString("name","");
            options.add(new PlaybackCandidate(url,title.url,resolution(quality),false));
        }
        options.sort((a,b)->Integer.compare(b.height,a.height));
        title.candidates.clear();title.candidates.addAll(options);
        title.publicStream=options.isEmpty()?"":options.get(0).url;
        if(options.isEmpty())title.playbackError="افزونه لینک مستقیم HTTPS قابل‌پخش ارائه نکرد. دسترسی یا قالب رسانه را بررسی کن.";
    }

    public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb) {
        if(!supports(item)){cb.receive(item,"افزونهٔ این عنوان فعال نیست.");return;}
        io.execute(()->{
            FarsiPlexTitle full=item;String error=null;
            try {
                String manifest=configuredManifest(activity),base=AddonEndpointPolicy.base(manifest),type=AddonEndpointPolicy.typeFor(manifest,item.url);
                String id=AddonEndpointPolicy.idFrom(manifest,item.url);
                if(id.isEmpty())throw new java.io.IOException("Missing item ID");
                if("episode".equals(type))streams(full,json(item.url,MAX_JSON));
                else {
                    JSONObject response=json(item.url,MAX_JSON);
                    JSONObject metadata=response.optJSONObject("meta");
                    if(metadata!=null) {
                        FarsiPlexTitle details=title(manifest,metadata,type);
                        if(details!=null) {
                            details.name=first(details.name,item.name);
                            details.poster=first(details.poster,item.poster);
                            details.genre=first(details.genre,item.genre);
                            details.year=first(details.year,item.year);
                            details.description=first(details.description,item.description);
                            JSONArray videos=metadata.optJSONArray("videos");
                            if("series".equals(type) && videos!=null) {
                                for(int i=0;i<videos.length()&&details.episodes.size()<250;i++) {
                                    JSONObject episode=videos.optJSONObject(i);if(episode==null)continue;
                                    String videoId=episode.optString("id","");
                                    String epUrl=AddonEndpointPolicy.episodeUrl(manifest,videoId);
                                    if(epUrl.isEmpty())continue;
                                    FarsiPlexTitle ep=new FarsiPlexTitle(epUrl,"episode",first(episode.optString("title"),"قسمت "+episode.optInt("episode",i+1)));
                                    ep.season=episode.optInt("season",0);ep.episode=episode.optInt("episode",i+1);
                                    ep.poster=details.poster;
                                    details.episodes.add(ep);
                                }
                            }
                            full=details;
                        }
                    }
                    if("movie".equals(type))streams(full,json(base+"stream/movie/"+AddonEndpointPolicy.segment(id)+".json",MAX_JSON));
                }
            }catch(Exception e){error="دریافت اطلاعات این افزونه ممکن نشد.";}
            FarsiPlexTitle ready=full;String msg=error;
            activity.runOnUiThread(()->cb.receive(ready,msg));
        });
    }

    public void close() {io.shutdownNow();}
}
