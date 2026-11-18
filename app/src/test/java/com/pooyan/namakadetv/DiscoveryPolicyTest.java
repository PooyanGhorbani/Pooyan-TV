package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class DiscoveryPolicyTest {
    private FarsiPlexTitle t(String id,String kind,String genre,String year,String rating){
        FarsiPlexTitle t=new FarsiPlexTitle("https://example.org/"+id,kind,id);
        t.genre=genre;t.year=year;t.rating=rating;return t;
    }
    @Test public void filtersOnlyGroundedFields(){
        FarsiPlexTitle a=t("a","movie","Drama","2024","8.4"),b=t("b","series","Comedy","2019","9.1"),c=t("c","movie","Drama","","?");
        List<FarsiPlexTitle> result=DiscoveryPolicy.filter(Arrays.asList(a,b,c),new DiscoveryPolicy.Filter("movie","Drama",2020,7));
        assertEquals(1,result.size());assertSame(a,result.get(0));
    }
    @Test public void similarExcludesSelectedAndUnrelated(){
        FarsiPlexTitle selected=t("a","movie","Drama","2024","8.0"),same=t("b","movie","Drama","2023","7.7"),unrelated=t("c","movie","Comedy","2022","8.0");
        List<FarsiPlexTitle> result=DiscoveryPolicy.similar(selected,Arrays.asList(selected,same,unrelated),10);
        assertEquals(1,result.size());assertSame(same,result.get(0));
        FarsiPlexTitle noGenre=t("d","series","","2024","");
        assertFalse(DiscoveryPolicy.similar(selected,Arrays.asList(noGenre),10).contains(noGenre));
    }
    @Test public void missingMetadataIsNotInvented(){
        assertEquals(0,DiscoveryPolicy.year("unknown"));assertEquals(0,DiscoveryPolicy.rating("--"),.01);
        assertTrue(DiscoveryPolicy.similar(t("a","movie","","", ""),Collections.emptyList(),8).isEmpty());
    }
    @Test public void episodeDifferenceRequiresBaseline(){
        assertEquals(0,SeriesTracker.difference(0,9));assertEquals(0,SeriesTracker.difference(10,9));assertEquals(2,SeriesTracker.difference(9,11));
    }
}
