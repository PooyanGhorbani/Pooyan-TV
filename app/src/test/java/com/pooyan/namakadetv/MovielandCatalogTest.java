package com.pooyan.namakadetv;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Test;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.*;

public class MovielandCatalogTest {
    @Test public void extractsCoversAndMergesOnlySameTitleAndYear() {
        List<FarsiPlexTitle> items = MovielandCatalog.listing(Jsoup.parse(
                "<a href='/movies/example'><h2>Example</h2>2023"
                        + "<img src='https://image.movieland.af/images/2023/cover.jpg?width=20'></a>"
                        + "<a href='https://bad.example/movies/example'><h2>Bad</h2></a>"));
        assertEquals(1, items.size());
        FarsiPlexTitle item = items.get(0);
        assertTrue(ImageUrlPolicy.allowed(item.poster));
        assertTrue(SearchPolicy.matches(item, "example"));
        FarsiPlexTitle existing = new FarsiPlexTitle("https://farsiplex.com/movie/example/", "movie", "Example");
        existing.year = "2023";
        assertEquals(1, CatalogMerge.join(List.of(existing), items, new HashMap<>()).size());
        existing.year = "1982";
        assertEquals(2, CatalogMerge.join(List.of(existing), items, new HashMap<>()).size());
    }

    @Test public void parsesPublicSeasonMetadataWithoutCreatingMediaCandidates() {
        String data = "6:[{\"item\":{\"__typename\":\"SeriesEntity\",\"description\":\"A story\","
                + "\"release_year\":2023,\"seasonsWithEpisodes\":[{\"number\":2}]}}]";
        FarsiPlexTitle seed = new FarsiPlexTitle(MovielandCatalog.BASE + "/series/example", "series", "Example");
        FarsiPlexTitle parsed = MovielandCatalog.details(Jsoup.parse(
                "<script>self.__next_f.push([1," + JSONObject.quote(data) + "])</script>"
                        + "<a href='/watch/ep/episode2' data-season='2' data-episode='1'>Episode 1</a>"), seed);
        assertEquals("A story", parsed.description);
        assertEquals(List.of(2), parsed.availableSeasons);
        assertEquals(1, parsed.episodes.size());
        assertEquals(2, parsed.episodes.get(0).season);
        assertTrue(parsed.publicStream.isEmpty());
        assertTrue(parsed.candidates.isEmpty());
    }
}
