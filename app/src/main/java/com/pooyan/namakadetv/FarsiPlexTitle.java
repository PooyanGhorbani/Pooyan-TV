package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.List;

/** Public title metadata; source page is NOT a playable media URL. */
public final class FarsiPlexTitle {
    public final String url,kind;
    public String name,poster="",description="",year="",genre="",publicStream="",playbackError="",rating="",cast="",runtime="",director="",country="",ageRating="",trailerUrl="";
    public int season,episode;
    public String stableId="";
    public final List<String> aliases=new ArrayList<>();
    public final List<PlaybackCandidate> candidates=new ArrayList<>();
    public final List<Integer> availableSeasons=new ArrayList<>();
    public final List<FarsiPlexTitle> episodes=new ArrayList<>();
    public FarsiPlexTitle(String url,String kind,String name) {
        this.url=url;this.kind=kind;this.name=name;
    }
}
