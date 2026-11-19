package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/** Representative public markup structure, not a claim of a live network integration test. */
public final class FarsiPlexParserTest {
    @Test public void movieListingDeduplicatesAndGetsPoster(){
        String html="<div class='movie'><a href='/movie/harchi-to-begi/'>"+
            "<img alt='Harchi To Begi' src='/wp-content/uploads/2026/10/harchi-to-begi-185x278.jpg'></a>"+
            "<h3><a href='/movie/harchi-to-begi/'>Harchi To Begi</a></h3><p>2026 comedy</p></div>"+
            "<a href='https://evil.example/movie/fake/'>Injected</a>";
        List<FarsiPlexTitle> movies=FarsiPlexParser.listing(html,"https://farsiplex.com/movie/","movie");
        assertEquals(1,movies.size());
        assertEquals("Harchi To Begi",movies.get(0).name);
        assertTrue(movies.get(0).poster.endsWith("harchi-to-begi-185x278.jpg"));
        assertEquals("2026",movies.get(0).year);
    }
    @Test public void seriesDetailsExtractEpisodes(){
        String url="https://farsiplex.com/tvshow/taxi-series-7d33ee0b/";
        String html="<html><head><meta property='og:image' content='https://farsiplex.com/wp-content/uploads/taxi.jpg'></head>"+
            "<body><h1>Taxi</h1><h2>Synopsis</h2><p>Taxi is a comedy series.</p>"+
            "<a href='/episode/taxi-ep-01/'>Episode 01</a>"+
            "<a href='/episode/taxi-ep-01/'>Episode 01</a>"+
            "<a href='https://evil.example/episode/other/'>Evil</a>"+
            "</body></html>";
        FarsiPlexTitle t=FarsiPlexParser.details(html,url,"series");
        assertEquals("Taxi",t.name);assertEquals(1,t.episodes.size());
        assertEquals("Episode 01",t.episodes.get(0).name);
        assertEquals("Taxi is a comedy series.",t.description);
        assertTrue(t.poster.endsWith("taxi.jpg"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void blocksExternalDetails(){FarsiPlexParser.details("<h1>fake</h1>","https://not-farsiplex.example/movie/a/","movie");}
}
