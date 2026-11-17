package com.pooyan.namakadetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Local-only watch history and bookmarks (no account or remote sync). */
public final class LibraryStore {
    public static final class Entry {
        public String title, url, pageUrl, posterUrl;
        public long positionMs, durationMs, updatedAt;
        Entry(String title, String url, String pageUrl, long positionMs, long durationMs, long updatedAt) {
            this(title, url, pageUrl, positionMs, durationMs, updatedAt, "");
        }
        Entry(String title, String url, String pageUrl, long positionMs, long durationMs, long updatedAt, String posterUrl) {
            this.title = title; this.url = url; this.pageUrl = pageUrl; this.posterUrl = posterUrl;
            this.positionMs = positionMs; this.durationMs = durationMs; this.updatedAt = updatedAt;
        }
    }

    private final SharedPreferences preferences;
    public LibraryStore(Context c) {
        preferences = ProfileStore.data(c,"namakade_local_library");
    }

    LibraryStore(SharedPreferences preferences){this.preferences=preferences;}

    private List<Entry> read(String key) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(preferences.getString(key, "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject j = data.optJSONObject(i);
                if (j == null) continue;
                String url = j.optString("url", "");
                if (!StreamDetector.isHttpsUrl(url)) continue;
                out.add(new Entry(j.optString("title", "بدون عنوان"), url,
                        j.optString("page", ""), Math.max(0, j.optLong("pos")),
                        Math.max(0, j.optLong("duration")), j.optLong("updated"), j.optString("poster", "")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void write(String key, List<Entry> entries, int limit) {
        JSONArray data = new JSONArray();
        for (int i = 0; i < Math.min(entries.size(), limit); i++) {
            Entry e = entries.get(i);
            JSONObject j = new JSONObject();
            try {
                j.put("title", e.title); j.put("url", e.url); j.put("page", e.pageUrl);
                j.put("pos", e.positionMs); j.put("poster", e.posterUrl == null ? "" : e.posterUrl); j.put("duration", e.durationMs);
                j.put("updated", e.updatedAt);
                data.put(j);
            } catch (Exception ignored) {}
        }
        preferences.edit().putString(key, data.toString()).apply();
    }

    private void upsert(String key, Entry e, int limit) {
        List<Entry> list = read(key);
        list.removeIf(item -> ("resume".equals(key)||"watched".equals(key)||"completed".equals(key)) ? PlaybackResumePolicy.identity(item.url,item.pageUrl).equals(PlaybackResumePolicy.identity(e.url,e.pageUrl)) : item.url.equals(e.url));
        list.add(0, e);
        write(key, list, limit);
    }

    public List<Entry> recentPages() { return read("pages"); }
    public List<Entry> bookmarks() { return read("favorites"); }
    public List<Entry> resumeList() { return read("resume"); }

    public void visit(String title, String url) { visit(title, url, ""); }
    public void visit(String title, String url, String posterUrl) {
        if (StreamDetector.isHttpsUrl(url)) {
            String existingPoster = "";
            for (Entry e : recentPages()) if (e.url.equals(url)) { existingPoster = e.posterUrl; break; }
            String poster = PosterPolicy.accept(url, posterUrl) ? posterUrl : existingPoster;
            upsert("pages", new Entry(title, url, url, 0, 0, System.currentTimeMillis(), poster), 60);
        }
    }
    public void addBookmark(String title, String url) {
        if (StreamDetector.isHttpsUrl(url)) {
            String poster = "";
            for (Entry e : recentPages()) if (e.url.equals(url)) { poster = e.posterUrl; break; }
            upsert("favorites", new Entry(title, url, url, 0, 0, System.currentTimeMillis(), poster), 50);
        }
    }
    public void removeBookmark(String url) {
        List<Entry> list = read("favorites");
        list.removeIf(e -> e.url.equals(url));
        write("favorites", list, 50);
    }
    public boolean hasBookmark(String url) {
        for (Entry e : bookmarks()) if (e.url.equals(url)) return true;
        return false;
    }
    public long getResumePosition(String videoUrl) {for(Entry e:resumeList())if(e.url.equals(videoUrl))return e.positionMs;return 0;}
    public long getResumePosition(String videoUrl,String pageUrl) {
        String id=PlaybackResumePolicy.identity(videoUrl,pageUrl);
        for(Entry e:resumeList())if(PlaybackResumePolicy.identity(e.url,e.pageUrl).equals(id))return e.positionMs;
        return 0;
    }
    public void removePlayback(String stream,String page){
        String id=PlaybackResumePolicy.identity(stream,page);
        List<Entry> entries=read("resume");
        entries.removeIf(e->PlaybackResumePolicy.identity(e.url,e.pageUrl).equals(id));
        write("resume",entries,40);
    }
    public void savePlayback(String title,String streamUrl,String pageUrl,long position,long duration){
        if(!StreamDetector.isHttpsUrl(streamUrl))return;
        if(position<=0)return;
        boolean complete=duration>0&&position>=duration*(preferences.getInt("completion",97)/100d);
        Entry history=new Entry(title,streamUrl,pageUrl,position,Math.max(0,duration),System.currentTimeMillis());
        upsert("watched",history,200);
        if(complete){upsert("completed",history,200);removePlayback(streamUrl,pageUrl);return;}
        String poster="";
        for(Entry e:recentPages())if(e.url.equals(pageUrl)){poster=e.posterUrl;break;}
        upsert("resume",new Entry(title,streamUrl,pageUrl,Math.max(0,position),Math.max(0,duration),System.currentTimeMillis(),poster),40);
    }
    public List<Entry> watched(){return read("watched");}
    public List<Entry> completed(){return read("completed");}
    public void complete(String title,String stream,String page,long duration){
        Entry entry=new Entry(title,stream,page,Math.max(0,duration),Math.max(0,duration),System.currentTimeMillis());
        upsert("watched",entry,200);upsert("completed",entry,200);removePlayback(stream,page);
    }
    public void forget(String page,String stream){
        String id=PlaybackResumePolicy.identity(stream,page);
        for(String key:new String[]{"watched","completed","resume"}){List<Entry> list=read(key);list.removeIf(e->PlaybackResumePolicy.identity(e.url,e.pageUrl).equals(id));write(key,list,200);}
    }
    public void clearHistory(){preferences.edit().remove("watched").remove("completed").remove("resume").apply();}
    public void completion(int percent){preferences.edit().putInt("completion",Math.max(90,Math.min(99,percent))).apply();}
    public void clear() {
        preferences.edit().clear().apply();
    }
}
