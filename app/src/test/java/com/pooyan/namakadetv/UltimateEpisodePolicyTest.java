package com.pooyan.namakadetv;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public final class UltimateEpisodePolicyTest {
    @Test public void sourceDuplicatesCannotFakeNewEpisode(){
        FarsiPlexTitle a=new FarsiPlexTitle("https://one.test/s1/e2","episode","قسمت ۲");a.season=1;a.episode=2;
        FarsiPlexTitle b=new FarsiPlexTitle("https://two.test/s1/e2","episode","Episode 2");b.season=1;b.episode=2;
        assertEquals(1,UltimateEpisodePolicy.known(Arrays.asList(a,b)).size());
    }
    @Test public void newEpisodeDetectionRequiresBaseline(){
        Set<String> previous=new HashSet<>(Arrays.asList("1:1","1:2"));
        Set<String> current=new HashSet<>(Arrays.asList("1:1","1:2","1:3"));
        assertEquals(1,UltimateEpisodePolicy.added(previous,current).size());
        assertTrue(UltimateEpisodePolicy.added(new HashSet<>(),current).isEmpty());
    }
}
