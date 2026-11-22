package com.pooyan.namakadetv;

import org.jsoup.nodes.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses public HTML metadata; the watch page payload is never used as a playback API. */
final class MovielandCatalog {
    static final String BASE = "https://www.movieland.af";
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");

    private MovielandCatalog() {}

    static String path(String value) {
        try {
            URI uri = URI.create(BASE).resolve(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || uri.getPort() != -1 || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !("www.movieland.af".equals(host) || "movieland.af".equals(host))) return "";
            String path = uri.getRawPath();
            return path.matches("/(?:movies|series)/[a-zA-Z0-9-]{1,150}|/watch/ep/[a-zA-Z0-9-]{1,100}") ? path : "";
        } catch (RuntimeException e) { return ""; }
    }

    static String kind(String url) {
        String path = path(url);
        if (path.startsWith("/movies/")) return "movie";
        if (path.startsWith("/series/")) return "series";
        return path.isEmpty() ? "" : "episode";
    }

    static boolean posterAllowed(URI uri) {
        return ("image.movieland.af".equals(uri.getHost()) || "cdn1.movieland.af".equals(uri.getHost()))
                && uri.getRawPath() != null && uri.getRawPath().matches("/images/\\d{4}/[a-zA-Z0-9._-]+\\.(?:jpg|jpeg|png|webp)")
                && uri.getRawQuery() == null;
    }

    private static String poster(String raw) {
        try {
            URI u = URI.create(raw);
            if (!"https".equals(u.getScheme()) || u.getPort() != -1 || u.getUserInfo() != null) return "";
            URI canonical = URI.create("https://" + u.getHost() + u.getRawPath());
            return posterAllowed(canonical) ? canonical.toString() : "";
        } catch (RuntimeException e) { return ""; }
    }

    static List<FarsiPlexTitle> listing(Document doc) {
        Map<String, FarsiPlexTitle> distinct = new LinkedHashMap<>();
        for (Element a : doc.select("a[href]")) {
            String route = path(a.attr("href"));
            if (route.isEmpty() || route.startsWith("/watch/")) continue;
            Element heading = a.selectFirst("h2,h3,h4,[data-title]");
            String name = heading == null ? a.attr("aria-label") : heading.text();
            if (name.isEmpty()) continue;
            FarsiPlexTitle item = new FarsiPlexTitle(BASE + route, kind(route), name);
            for (Element img : a.select("img[src]")) {
                item.poster = poster(img.attr("src"));
                if (!item.poster.isEmpty()) break;
            }
            Matcher year = YEAR.matcher(a.text());
            if (year.find()) item.year = year.group();
            distinct.putIfAbsent(item.url, item);
            if (distinct.size() >= 60) break;
        }
        return new ArrayList<>(distinct.values());
    }

    static FarsiPlexTitle details(Document doc, FarsiPlexTitle seed) {
        String name = meta(doc, "og:title");
        int separator = name.indexOf('|');
        if (separator >= 0) name = name.substring(separator + 1).trim();
        FarsiPlexTitle item = new FarsiPlexTitle(seed.url, seed.kind, name.isEmpty() ? seed.name : name);
        String cover = poster(meta(doc, "og:image"));
        item.poster = cover.isEmpty() ? seed.poster : cover;
        item.year = seed.year;
        item.season = seed.season;
        item.episode = seed.episode;
        Element description = doc.selectFirst("[data-sentry-component=Description], [itemprop=description]");
        if (description != null) item.description = description.text();
        JSONObject metadata = publicMetadata(doc, item.kind);
        item.description = metadata.optString("description", item.description);
        item.year = metadata.optString("release_year", item.year);
        String alias = metadata.optString("secondary_title");
        if (!alias.isEmpty()) item.aliases.add(alias);
        if ("series".equals(item.kind)) {
            JSONArray seasons = metadata.optJSONArray("seasonsWithEpisodes");
            if (seasons != null) for (int i = 0; i < seasons.length(); i++) {
                JSONObject season = seasons.optJSONObject(i);
                if (season == null) continue;
                int number = season.optInt("number", -1);
                if (number > 0 && !item.availableSeasons.contains(number)) item.availableSeasons.add(number);
            }
            Map<String, FarsiPlexTitle> episodes = new LinkedHashMap<>();
            for (Element a : doc.select("a[href][data-season][data-episode]")) {
                String route = path(a.attr("href"));
                if (!route.startsWith("/watch/ep/")) continue;
                try {
                    int season = Integer.parseInt(a.attr("data-season"));
                    int number = Integer.parseInt(a.attr("data-episode"));
                    if (season < 0 || number < 1) continue;
                    FarsiPlexTitle episode = new FarsiPlexTitle(BASE + route, "episode", a.text());
                    episode.season = season;
                    episode.episode = number;
                    episode.poster = item.poster;
                    episodes.putIfAbsent(episode.url, episode);
                } catch (NumberFormatException ignored) { }
            }
            item.episodes.addAll(episodes.values());
            if (item.episodes.isEmpty()) item.playbackError = "فهرست عمومی قسمت‌ها در دسترس نیست.";
        } else {
            item.playbackError = "پخش این عنوان در برنامه هنوز مجوز فعال ندارد.";
        }
        return item;
    }

    private static JSONObject publicMetadata(Document doc, String kind) {
        String entity = "series".equals(kind) ? "SeriesEntity" : "MoviesEntity";
        String marker = "\"item\":{\"__typename\":\"" + entity + "\"";
        String prefix = "self.__next_f.push([1,";
        for (Element script : doc.select("script")) {
            String data = script.data();
            if (!data.startsWith(prefix)) continue;
            try {
                Object chunk = new JSONTokener(data.substring(prefix.length())).nextValue();
                if (!(chunk instanceof String)) continue;
                String text = (String) chunk;
                int at = text.indexOf(marker);
                if (at >= 0) return new JSONObject(new JSONTokener(text.substring(at + 7)));
            } catch (org.json.JSONException ignored) { }
        }
        return new JSONObject();
    }

    private static String meta(Document doc, String key) {
        Element el = doc.selectFirst("meta[property=" + key + "]");
        return el == null ? "" : el.attr("content");
    }
}
