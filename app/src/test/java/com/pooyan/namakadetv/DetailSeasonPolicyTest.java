package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DetailSeasonPolicyTest {
    @Test public void seasonsAreSortedAndUnknownSeasonIsKeptExplicit() {
        FarsiPlexTitle s2 = episode("s2", 2), unknown = episode("unknown", 0), s1 = episode("s1", 1);
        assertEquals(Arrays.asList(0, 1, 2), DetailSeasonPolicy.seasons(Arrays.asList(s2, unknown, s1, s1)));
        assertTrue(DetailSeasonPolicy.seasons(null).isEmpty());
    }

    @Test public void episodeGroupingPreservesCatalogOrderAndDoesNotInventEpisodes() {
        FarsiPlexTitle s2a = episode("s2a", 2), s1a = episode("s1a", 1), s2b = episode("s2b", 2);
        assertEquals(Arrays.asList(s2a, s2b), DetailSeasonPolicy.inSeason(Arrays.asList(s2a, s1a, s2b), 2));
        assertTrue(DetailSeasonPolicy.inSeason(Arrays.asList(s1a), 9).isEmpty());
    }

    private FarsiPlexTitle episode(String name, int season) {
        FarsiPlexTitle episode = new FarsiPlexTitle("https://farsiplex.com/episode/" + name + "/", "episode", name);
        episode.season = season;
        return episode;
    }
}
