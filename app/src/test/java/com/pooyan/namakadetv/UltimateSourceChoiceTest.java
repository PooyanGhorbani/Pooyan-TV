package com.pooyan.namakadetv;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class UltimateSourceChoiceTest {
    private PlaybackCandidate c(String id,int height){return new PlaybackCandidate("https://cdn.test/"+id+".mp4","https://site.test/"+id,height,true);}
    @Test public void successful1080BeatsUnknown4kWithOtherwiseEqualMetadata(){
        PlaybackCandidate a=c("4k",2160),b=c("1080",1080);
        List<PlaybackCandidate> order=UltimateSourceChoice.rank(Arrays.asList(a,b),"",UltimateIdentity.sourceFingerprint(b.url),1080);
        assertEquals(b.url,order.get(0).url);
    }
    @Test public void explicitManualChoiceBeatsRememberedSource(){
        PlaybackCandidate a=c("a",720),b=c("b",1080);
        List<PlaybackCandidate> order=UltimateSourceChoice.rank(Arrays.asList(a,b),UltimateIdentity.sourceFingerprint(a.url),
            UltimateIdentity.sourceFingerprint(b.url),1080);
        assertEquals(a.url,order.get(0).url);
    }
    @Test public void duplicateLinksIgnoredAndMissingQualityNotInvented(){
        PlaybackCandidate a=c("a",0);
        assertEquals(1,UltimateSourceChoice.rank(Arrays.asList(a,a),"","",1080).size());
        assertEquals("کیفیت نامشخص",UltimateSourceChoice.quality(0));
    }
}
