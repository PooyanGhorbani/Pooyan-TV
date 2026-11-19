package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class HomeHeroPolicyTest {
    private static FarsiPlexTitle item(String id, String kind, boolean hasPoster) {
        FarsiPlexTitle t = new FarsiPlexTitle("https://catalog.example/"+id,kind,id);
        t.poster = hasPoster ? "https://image.example/"+id+".jpg" : "";
        return t;
    }
    @Test public void interleavesMovieAndSeriesWithoutDuplicates() {
        FarsiPlexTitle m=item("same","movie",true);
        List<FarsiPlexTitle> result=HomeHeroPolicy.select(
            Arrays.asList(m,item("m2","movie",true)),
            Arrays.asList(item("s1","series",true),m,item("s2","series",true)));
        assertEquals(4,result.size());
        assertEquals("same",result.get(0).name);
        assertEquals("s1",result.get(1).name);
        assertEquals("m2",result.get(2).name);
        assertEquals("s2",result.get(3).name);
    }
    @Test public void skipsBlankPosterAndCapsAtEight() {
        List<FarsiPlexTitle> movies=new ArrayList<>();
        movies.add(item("blank","movie",false));
        for(int i=0;i<20;i++)movies.add(item("m"+i,"movie",true));
        List<FarsiPlexTitle> selected=HomeHeroPolicy.select(movies,Collections.emptyList());
        assertEquals(8,selected.size());
        assertEquals("m0",selected.get(0).name);
    }
    @Test public void emptyAndWrapAroundSafe() {
        assertTrue(HomeHeroPolicy.select(null,null).isEmpty());
        assertEquals(0,HomeHeroPolicy.indexAfter(2,1,0));
        assertEquals(0,HomeHeroPolicy.indexAfter(2,1,3));
        assertEquals(2,HomeHeroPolicy.indexAfter(0,-1,3));
        assertEquals("پیشنهادها در حال دریافت‌اند",HomeHeroPolicy.label(0,0));
    }
}
