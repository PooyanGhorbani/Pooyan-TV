package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public final class UltimateCatalogMergeTest {
    private FarsiPlexTitle make(String url,String name,String year){FarsiPlexTitle t=new FarsiPlexTitle(url,"movie",name);t.year=year;return t;}
    @Test public void threeSitesBecomeOneCanonicalWithTwoAlternatives(){
        FarsiPlexTitle a=make("https://a.test/1","Sintel","2010"),b=make("https://b.test/1","Sintel","2010"),
                       c=make("https://c.test/1","Sintel","2010");
        Map<String,List<FarsiPlexTitle>> vars=new HashMap<>();
        List<FarsiPlexTitle> x=CatalogMerge.join(Arrays.asList(a,b),Arrays.asList(c),vars);
        assertEquals(1,x.size());assertEquals(2,vars.get(a.url).size());
    }
    @Test public void twoDifferentImdbIdsMustRemainTwoVisibleTiles(){
        FarsiPlexTitle a=make("https://a.test/1","Film","2020"),b=make("https://b.test/1","Film","2020");
        a.stableId="tt1234567";b.stableId="tt7654321";
        assertEquals(2,CatalogMerge.join(Arrays.asList(a),Arrays.asList(b),new HashMap<>()).size());
    }
}
