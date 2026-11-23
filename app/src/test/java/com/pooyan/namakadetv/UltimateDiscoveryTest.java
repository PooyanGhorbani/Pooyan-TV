package com.pooyan.namakadetv;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;
public final class UltimateDiscoveryTest {
    private FarsiPlexTitle t(String name,String year,String genre,String rating){FarsiPlexTitle o=new FarsiPlexTitle("https://site.test/"+name,"movie",name);o.year=year;o.genre=genre;o.rating=rating;return o;}
    @Test public void filteringUsesActualKnownMetadata(){
        FarsiPlexTitle a=t("new","2024","comedy","8.0"),b=t("old","1990","drama","9.0");
        List<FarsiPlexTitle> r=UltimateDiscovery.filter(Arrays.asList(a,b),"movie","comedy",2020,0,7,null,false);
        assertEquals(1,r.size());assertEquals(a.url,r.get(0).url);
    }
    @Test public void emptyOrFullyWatchedListHasNoSurpriseCandidate(){
        FarsiPlexTitle t=t("seen","2024","comedy","8");
        assertNull(UltimateDiscovery.surprise(Arrays.asList(t),new HashSet<>(),
            new HashSet<>(Arrays.asList(UltimateIdentity.contentKey(t))),1));
    }
    @Test public void curatedCollectionsDoNotInventGenreOrRatings(){
        assertTrue(UltimateDiscovery.collection(Arrays.asList(t("x","2024","","")),"comedy").isEmpty());
    }
    @Test public void persianGenreIsRecognizedByEnglishFilter(){
        FarsiPlexTitle comedy=t("iranian","2024","کمدی خانوادگی","7.5");
        assertEquals(1,UltimateDiscovery.filter(Arrays.asList(comedy),"movie","comedy",0,0,0,null,false).size());
    }
}
