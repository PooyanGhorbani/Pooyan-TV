package com.pooyan.namakadetv;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UltimateIdentityTest {
    private FarsiPlexTitle t(String url,String kind,String name,String year){
        FarsiPlexTitle t=new FarsiPlexTitle(url,kind,name);t.year=year;return t;
    }
    @Test public void identicalNameYearAcrossProvidersMerges(){
        assertTrue(UltimateIdentity.sameTitle(t("https://site-a.test/movie/1","movie","Inception","2010"),
            t("https://site-b.test/movie/2","movie","Inception","2010")));
    }
    @Test public void remakesAndMoviesVersusShowsStaySeparate(){
        assertFalse(UltimateIdentity.sameTitle(t("https://a.test/a","movie","Dune","1984"),
            t("https://b.test/b","movie","Dune","2021")));
        assertFalse(UltimateIdentity.sameTitle(t("https://a.test/a","movie","Dune","2021"),
            t("https://b.test/b","series","Dune","2021")));
    }
    @Test public void unknownYearMustNotGuessCrossSourceIdentity(){
        assertFalse(UltimateIdentity.sameTitle(t("https://a.test/a","movie","Film", ""),
            t("https://b.test/b","movie","Film","")));
    }
    @Test public void sharedStrongImdbIdOverridesDifferentLanguage(){
        FarsiPlexTitle a=t("https://a.test/a","movie","اسم فارسی",""),b=t("https://b.test/b","movie","English Name","");
        a.stableId="tt1234567";b.stableId="tt1234567";
        assertTrue(UltimateIdentity.sameTitle(a,b));
    }
    @Test public void differentKnownIdsMustNeverMerge(){
        FarsiPlexTitle a=t("https://a.test/a","movie","Film","2020"),b=t("https://b.test/b","movie","Film","2020");
        a.stableId="tt1234567";b.stableId="tt7654321";
        assertFalse(UltimateIdentity.sameTitle(a,b));
    }
    @Test public void episodeNumbersAloneNeverMixDifferentSeries(){
        FarsiPlexTitle a=t("https://a.test/s1/e1","episode","Ep 1",""),b=t("https://b.test/s2/e1","episode","Ep 1","");
        a.season=b.season=1;a.episode=b.episode=1;
        assertFalse(UltimateIdentity.sameTitle(a,b));
    }
    @Test public void temporaryQueryTokensDoNotEnterContentKey(){
        FarsiPlexTitle a=t("https://a.test/movie?id=1&token=secret","movie","No Year",""),
            b=t("https://a.test/movie?token=new","movie","No Year","");
        assertEquals(UltimateIdentity.contentKey(a),UltimateIdentity.contentKey(b));
    }
}
