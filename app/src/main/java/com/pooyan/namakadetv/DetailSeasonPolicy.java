package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Stable season grouping for the remote-first series details screen. */
public final class DetailSeasonPolicy {
    private DetailSeasonPolicy() {}

    public static List<Integer> seasons(List<FarsiPlexTitle> episodes) {
        TreeSet<Integer> sorted = new TreeSet<>();
        if (episodes != null) for (FarsiPlexTitle episode : episodes) {
            if (episode != null) sorted.add(episode.season);
        }
        return new ArrayList<>(sorted);
    }

    public static List<FarsiPlexTitle> inSeason(List<FarsiPlexTitle> episodes, int season) {
        List<FarsiPlexTitle> result = new ArrayList<>();
        if (episodes != null) for (FarsiPlexTitle episode : episodes) {
            if (episode != null && episode.season == season) result.add(episode);
        }
        return result;
    }
}
