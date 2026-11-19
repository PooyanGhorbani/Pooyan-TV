package com.pooyan.namakadetv;

import org.junit.Test;
import org.json.JSONObject;
import java.util.List;
import static org.junit.Assert.*;

public final class StremioAddonRepositoryTest {
    private static final String MANIFEST = "https://test.example.org/manifest.json";

    @Test public void catalogsMapMovieMetadataWithoutInventingSources() throws Exception {
        JSONObject response = new JSONObject("{\"metas\":[{\"id\":\"tt1234567\",\"type\":\"movie\",\"name\":\"Example Movie\",\"releaseInfo\":\"2021\",\"genres\":[\"Action\"],\"description\":\"A short summary\"},{\"type\":\"movie\",\"name\":\"No ID\"}]}");
        List<FarsiPlexTitle> items=StremioAddonRepository.metas(MANIFEST,response,"movie",10);
        assertEquals(1,items.size());
        assertEquals("Example Movie",items.get(0).name);
        assertEquals("2021",items.get(0).year);
        assertEquals("tt1234567",items.get(0).stableId);
        assertEquals("Action",items.get(0).genre);
        assertEquals("",items.get(0).publicStream);
    }

    @Test public void onlyDirectHttpsStreamsPassAndBestResolutionWins() throws Exception {
        JSONObject response = new JSONObject("{\"streams\":[{\"url\":\"https://cdn.example.org/video-720.mp4\",\"title\":\"720p\"},{\"url\":\"https://cdn.example.org/video-1080.m3u8\",\"title\":\"1080p\"},{\"url\":\"http://bad.example.org/video.mp4\",\"title\":\"4K\"},{\"url\":\"https://cdn.example.org/video.torrent\",\"title\":\"4K\"}]}");
        FarsiPlexTitle title = new FarsiPlexTitle(AddonEndpointPolicy.metaUrl(MANIFEST,"movie","tt1234567"),"movie","Example Movie");
        StremioAddonRepository.streams(title,response);
        assertEquals(2,title.candidates.size());
        assertEquals(1080,title.candidates.get(0).height);
        assertEquals("https://cdn.example.org/video-1080.m3u8",title.publicStream);
    }

    @Test public void sourceWithoutDirectMediaDoesNotInventPlayback() throws Exception {
        FarsiPlexTitle title = new FarsiPlexTitle(AddonEndpointPolicy.metaUrl(MANIFEST,"movie","tt1234567"),"movie","Example Movie");
        StremioAddonRepository.streams(title,new JSONObject("{\"streams\":[{\"ytId\":\"abcd\"}] }"));
        assertEquals("",title.publicStream);
        assertTrue(title.playbackError.length()>0);
    }
}
