package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public final class UltimateTasteEngineTest {
  @Test public void favoritesGuideRecommendationsWithoutExternalAi(){
    FarsiPlexTitle preferred=new FarsiPlexTitle("https://a.test/1","movie","Favorite");preferred.genre="comedy";
    FarsiPlexTitle similar=new FarsiPlexTitle("https://a.test/2","movie","Similar");similar.genre="comedy";
    FarsiPlexTitle other=new FarsiPlexTitle("https://a.test/3","series","Other");other.genre="horror";
    List<FarsiPlexTitle> ranked=UltimateTasteEngine.rank(Arrays.asList(other,similar),Arrays.asList(preferred),
       Collections.emptySet(),Collections.emptySet(),12);
    assertEquals(similar.url,ranked.get(0).url);
  }
  @Test public void hiddenAndDislikedContentNeverRecommended(){
    FarsiPlexTitle f=new FarsiPlexTitle("https://a.test/1","movie","Unwanted");f.year="2020";
    assertTrue(UltimateTasteEngine.rank(Arrays.asList(f),Collections.emptyList(),Collections.emptySet(),
       new HashSet<>(Arrays.asList(UltimateIdentity.contentKey(f))),12).isEmpty());
  }
}
