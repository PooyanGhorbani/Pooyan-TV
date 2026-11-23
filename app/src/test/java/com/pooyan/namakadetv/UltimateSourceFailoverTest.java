package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public final class UltimateSourceFailoverTest {
  @Test public void orderedSourcesAreTriedWithoutRepeatsOrOverThreeAttempts(){
    List<PlaybackCandidate> sources=Arrays.asList(
      new PlaybackCandidate("https://cdn.test/one.mp4","https://a.test/one",1080,true),
      new PlaybackCandidate("https://cdn.test/two.mp4","https://b.test/two",720,true),
      new PlaybackCandidate("https://cdn.test/three.mp4","https://c.test/three",480,true));
    SourceHealth.configure(3,60000,600000);
    SourceHealth.Session session=new SourceHealth.Session();
    for(int i=0;i<3;i++)assertEquals(sources.get(i).url,session.nextOrdered(sources,0).url);
    assertNull(session.nextOrdered(sources,0));
  }
  @Test public void manuallyChosenCandidateCannotLoopForever(){
    PlaybackCandidate c=new PlaybackCandidate("https://cdn.test/one.mp4","https://a.test/one",1080,true);
    SourceHealth.Session session=new SourceHealth.Session();
    session.markTried(c.url);
    assertNull(session.nextOrdered(Arrays.asList(c),0));
  }
}
