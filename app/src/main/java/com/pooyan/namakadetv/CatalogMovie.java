package com.pooyan.namakadetv;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

/** A catalog listing is metadata, NOT proof of a playable stream. */
public final class CatalogMovie {
    public String id, type, title, poster, background, description, year, genres;
    public final List<Episode> episodes = new ArrayList<>();
    public static final class Episode {
        public String id, title;
        public int season, episode;
    }
    public CatalogMovie(String id, String type, String title, String poster, String background, String description, String year, String genres) {
        this.id=id; this.type=type; this.title=title; this.poster=poster;
        this.background=background; this.description=description; this.year=year; this.genres=genres;
    }
    public String key() { return type + ":" + id; }
    public static CatalogMovie parse(JSONObject j, String fallbackType) {
        String id=j.optString("imdb_id", j.optString("id", ""));
        if (!id.matches("tt[0-9]{5,12}")) return null;
        String t=j.optString("type", fallbackType);
        if (!"series".equals(t)) t="movie";
        String title=j.optString("name", j.optString("title", ""));
        if (title.isEmpty()) return null;
        String y=j.optString("releaseInfo", j.optString("year", ""));
        String genre="";
        JSONArray genres=j.optJSONArray("genres");
        if (genres!=null) { for(int i=0;i<Math.min(3, genres.length());i++) { if(i>0) genre += " · "; genre += genres.optString(i); } }
        CatalogMovie m=new CatalogMovie(id,t,title,j.optString("poster", ""),j.optString("background", ""),j.optString("description", ""),y,genre);
        JSONArray videos=j.optJSONArray("videos");
        if (videos!=null) for(int i=0;i<Math.min(500,videos.length());i++) {
            JSONObject e=videos.optJSONObject(i);if(e==null)continue;
            Episode ep=new Episode();
            ep.id=e.optString("id", ""); ep.title=e.optString("title", e.optString("name", "Episode " + (i+1)));
            ep.season=e.optInt("season", 0);ep.episode=e.optInt("episode", e.optInt("number", i+1));
            if(!ep.id.isEmpty())m.episodes.add(ep);
        }
        return m;
    }
}
