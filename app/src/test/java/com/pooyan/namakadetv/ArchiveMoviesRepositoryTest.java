package com.pooyan.namakadetv;

import org.junit.Test;
import org.json.JSONObject;
import java.util.List;
import static org.junit.Assert.*;

public final class ArchiveMoviesRepositoryTest {
    @Test public void curatedOpenMoviesAreVisibleWithoutConfiguration() {
        List<FarsiPlexTitle> titles=ArchiveMoviesRepository.openFilms();
        assertTrue(titles.size()>=4);
        assertTrue(titles.stream().anyMatch(t->t.name.equals("Sintel")));
        for(FarsiPlexTitle t:titles){
            assertEquals("movie",t.kind);
            assertTrue(StreamDetector.isDirectMedia(t.publicStream));
            assertEquals(t.url,t.publicStream);
            assertEquals(t.url,ArchiveMoviesRepository.safePage(t.url));
        }
    }
    @Test public void licenseAndUrlRulesFailClosed() {
        assertTrue(ArchiveMoviesRepository.declaredLicense("https://creativecommons.org/licenses/by/3.0/"));
        assertTrue(ArchiveMoviesRepository.declaredLicense("https://creativecommons.org/publicdomain/mark/1.0/"));
        assertFalse(ArchiveMoviesRepository.declaredLicense("https://example.org/licenses/by/3.0/"));
        assertFalse(ArchiveMoviesRepository.declaredLicense("https://creativecommons.org.evil.net/licenses/by/3.0/"));
        assertTrue(ImageUrlPolicy.allowed("https://archive.org/services/img/film_1927"));
        assertTrue(ImageUrlPolicy.allowed("https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg"));
        assertFalse(ImageUrlPolicy.allowed("https://archive.org/services/img/film_1927?api_key=123"));
        assertEquals("https://archive.org/details/known-film_1927",ArchiveMoviesRepository.safePage("https://archive.org/details/known-film_1927"));
        assertEquals("",ArchiveMoviesRepository.safePage("https://archive.org/details/a/b"));
        assertEquals("",ArchiveMoviesRepository.safePage("https://archive.org/details/a?token=b"));
        assertEquals("",ArchiveMoviesRepository.safePage("https://evil.org/details/a"));
        assertEquals("",ArchiveMoviesRepository.safePage("http://archive.org/details/a"));
    }
    @Test public void licensedSearchResultsOnly() throws Exception {
        JSONObject payload=new JSONObject("{\"response\":{\"docs\":["
            +"{\"identifier\":\"film-1925\",\"title\":\"Example Movie\",\"year\":\"1925\",\"licenseurl\":\"https://creativecommons.org/publicdomain/mark/1.0/\"},"
            +"{\"identifier\":\"copyrighted\",\"title\":\"Unknown rights\"},"
            +"{\"identifier\":\"bad/id\",\"title\":\"Bad ID\",\"licenseurl\":\"https://creativecommons.org/licenses/by/4.0/\"}]}}");
        List<FarsiPlexTitle> found=ArchiveMoviesRepository.catalog(payload,20);
        assertEquals(1,found.size());assertEquals("Example Movie",found.get(0).name);
        assertEquals("",found.get(0).publicStream);
    }
    @Test public void detailOnlyUsesListedVideoFiles() throws Exception {
        JSONObject payload=new JSONObject("{\"files\":[{\"name\":\"Movie_1080p.mp4\"},"
            +"{\"name\":\"Movie_720p.webm\"},{\"name\":\"private.mp4\",\"private\":true},"
            +"{\"name\":\"../evil.mp4\"},{\"name\":\"poster.jpg\"}]}");
        List<PlaybackCandidate> found=ArchiveMoviesRepository.mediaFiles("test-film",payload);
        assertEquals(2,found.size());assertEquals(1080,found.get(0).height);
        assertTrue(StreamDetector.isDirectMedia(found.get(0).url));
        assertEquals("https://archive.org/download/test-film/Movie_1080p.mp4",found.get(0).url);
    }
}
