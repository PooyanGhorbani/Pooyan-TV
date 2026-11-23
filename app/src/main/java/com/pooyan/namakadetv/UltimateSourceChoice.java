package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic source ranking: trusted user preference, recent successes, verified playback, then known quality. */
public final class UltimateSourceChoice {
    private UltimateSourceChoice(){}
    public static List<PlaybackCandidate> rank(List<PlaybackCandidate> input,String manual,String lastGood,int preferredHeight){
        List<PlaybackCandidate> out=new ArrayList<>();if(input==null)return out;
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(PlaybackCandidate c:input)if(c!=null&&c.url!=null&&StreamDetector.isDirectMedia(c.url)&&seen.add(c.url))out.add(c);
        // Java sort is stable; ties keep provider order, rather than fluctuating every launch.
        out.sort((a,b)->Integer.compare(score(b,manual,lastGood,preferredHeight),score(a,manual,lastGood,preferredHeight)));
        return out;
    }
    public static int score(PlaybackCandidate c,String manual,String lastGood,int preferredHeight){
        if(c==null)return Integer.MIN_VALUE;
        int s=c.verified?180:0;
        String fingerprint=UltimateIdentity.sourceFingerprint(c.url);
        if(manual!=null&&!manual.isEmpty()&&fingerprint.equals(manual))s+=20000;
        if(lastGood!=null&&!lastGood.isEmpty()&&fingerprint.equals(lastGood))s+=300;
        if(c.height>0){
            // A supported 1080p source can be a better default than an untested 4K one.
            s+=Math.min(c.height,preferredHeight>0?preferredHeight:1080)/6;
            if(preferredHeight>0&&c.height>preferredHeight)s-=80;
        }
        return s;
    }
    public static String quality(int height){return height>=2160?"4K":height>=1440?"1440p":height>=1080?"1080p":height>=720?"720p":height>0?height+"p":"کیفیت نامشخص";}
}
