package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class CatalogOrderingTest {
    private FarsiPlexTitle title(String key, String name, String year, String rating) {
        FarsiPlexTitle item = new FarsiPlexTitle("https://example.org/"+key, "movie", name);
        item.year = year;
        item.rating = rating;
        return item;
    }

    @Test public void newReleasesComeBeforeOldReleasesRegardlessOfProvider() {
        FarsiPlexTitle older=title("old","Old","2021","9.0");
        FarsiPlexTitle recent=title("new","New","2026","6.0");
        FarsiPlexTitle unknown=title("unknown","Unknown","","9.9");
        List<FarsiPlexTitle> out=CatalogOrdering.order(Arrays.asList(older, unknown, recent),CatalogOrdering.NEWEST,null,"");
        assertEquals(Arrays.asList(recent,older,unknown),out);
    }

    @Test public void highestRatingUsesOnlyRealRatings() {
        FarsiPlexTitle unrated=title("none","Unrated","2026","");
        FarsiPlexTitle rated=title("rated","Rated","2020","8.4");
        assertEquals(Collections.singletonList(rated),CatalogOrdering.topRated(Arrays.asList(unrated,rated)));
    }

    @Test public void localFirstSeenOverridesReleaseYear() {
        FarsiPlexTitle old=title("old","Old","2010","");
        FarsiPlexTitle newer=title("new","New","2026","");
        Map<String,Long> times=new HashMap<>();times.put(old.url,200L);times.put(newer.url,100L);
        assertEquals(Arrays.asList(old,newer),CatalogOrdering.order(Arrays.asList(newer,old),CatalogOrdering.FIRST_SEEN,times,""));
    }

    @Test public void exactMatchesBeatUnrelatedTitles() {
        FarsiPlexTitle partial=title("p","Joyland story","2026","");
        FarsiPlexTitle exact=title("e","Joyland","2022","");
        assertEquals(exact,CatalogOrdering.order(Arrays.asList(partial,exact),CatalogOrdering.RELEVANCE,null,"Joyland").get(0));
    }

    @Test public void originalOrderNeverChanges() {
        FarsiPlexTitle a=title("a","A","2020","");
        FarsiPlexTitle b=title("b","B","2026","");
        assertEquals(Arrays.asList(a,b),CatalogOrdering.order(Arrays.asList(a,b),CatalogOrdering.ORIGINAL,null,""));
    }
}
