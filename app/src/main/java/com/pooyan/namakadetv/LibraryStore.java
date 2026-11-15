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
        preferences = c.getSharedPreferences("namakade_local_library", Context.MODE_PRIVATE);
    }

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
        list.removeIf(item -> item.url.equals(e.url));
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
    public long getResumePosition(String videoUrl) {
        for (Entry e : resumeList()) if (e.url.equals(videoUrl)) return e.positionMs;
        return 0;
    }
    public void savePlayback(String title, String streamUrl, String pageUrl, long position, long duration) {
        if (!StreamDetector.isHttpsUrl(streamUrl)) return;
        long saved = position;
        if (duration > 0 && (position >= duration - 30000 || position >= duration * 0.97)) saved = 0;
        upsert("resume", new Entry(title, streamUrl, pageUrl,
                saved, duration, System.currentTimeMillis()), 40);
    }
    public void clear() {
        preferences.edit().clear().apply();
    }
}
