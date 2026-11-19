package com.pooyan.namakadetv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
/** Transparent local ranking: shared genres and type from favorites; never a remote AI call. */
public final class Recommendations {
 public static List<FarsiPlexTitle> rank(List<FarsiPlexTitle> catalog,List<FarsiPlexTitle> favorites,List<LibraryStore.Entry> history){
  List<FarsiPlexTitle> out=new ArrayList<>(catalog);Set<String> watched=new HashSet<>();for(LibraryStore.Entry h:history)watched.add(h.pageUrl);
  out.sort((a,b)->Integer.compare(score(b,favorites,watched),score(a,favorites,watched)));return out.subList(0,Math.min(12,out.size()));
 }
 private static int score(FarsiPlexTitle item,List<FarsiPlexTitle> favorites,Set<String> watched){int score=watched.contains(item.url)?-4:0;for(FarsiPlexTitle f:favorites){if(f.kind.equals(item.kind))score++;if(!item.genre.isEmpty()&&!f.genre.isEmpty()&&SearchPolicy.normalize(item.genre).equals(SearchPolicy.normalize(f.genre)))score+=3;}return score;}
}
