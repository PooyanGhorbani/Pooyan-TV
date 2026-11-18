package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local, deterministic discovery. No profile history or watch data is uploaded. */
public final class DiscoveryPolicy {
    private DiscoveryPolicy() {}
    public static final class Filter {
        public final String kind,genre;
        public final int yearFrom,ratingFrom;
        public Filter(String kind,String genre,int yearFrom,int ratingFrom){
            this.kind=kind==null?"":kind;this.genre=genre==null?"":genre;
            this.yearFrom=Math.max(0,yearFrom);this.ratingFrom=Math.max(0,ratingFrom);
        }
        public boolean active(){return !kind.isEmpty()||!genre.isEmpty()||yearFrom>0||ratingFrom>0;}
    }
    static String norm(String text){return text==null?"":text.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').trim();}
    public static List<FarsiPlexTitle> filter(List<FarsiPlexTitle> items,Filter filter){
        List<FarsiPlexTitle> result=new ArrayList<>();if(items==null)return result;
        for(FarsiPlexTitle item:items){
            if(item==null)continue;
            if(!filter.kind.isEmpty()&&!filter.kind.equals(item.kind))continue;
            if(!filter.genre.isEmpty()&&!norm(item.genre).contains(norm(filter.genre)))continue;
            if(filter.yearFrom>0&&year(item.year)<filter.yearFrom)continue;
            if(filter.ratingFrom>0&&rating(item.rating)<filter.ratingFrom)continue;
            result.add(item);
        }
        return result;
    }
    static int year(String raw){if(raw==null)return 0;java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:19|20)\\d{2}").matcher(raw);return m.find()?Integer.parseInt(m.group()):0;}
    static double rating(String raw){if(raw==null)return 0;java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\d+(?:[.,]\\d+)?").matcher(raw);if(!m.find())return 0;try{return Double.parseDouble(m.group().replace(',','.'));}catch(Exception ignored){return 0;}}
    public static List<FarsiPlexTitle> similar(FarsiPlexTitle selected,List<FarsiPlexTitle> catalog,int limit){
        List<FarsiPlexTitle> result=new ArrayList<>();if(selected==null||catalog==null||selected.genre.isEmpty())return result;
        Set<String> seen=new HashSet<>();
        for(FarsiPlexTitle item:catalog){if(item==null||item.url.equals(selected.url)||!seen.add(item.url))continue;
            if(!item.genre.isEmpty() && (norm(item.genre).contains(norm(selected.genre))
                    || norm(selected.genre).contains(norm(item.genre))))result.add(item);
        }
        result.sort((a,b)->Integer.compare(similarScore(selected,b),similarScore(selected,a)));
        return new ArrayList<>(result.subList(0,Math.min(Math.max(0,limit),result.size())));
    }
    private static int similarScore(FarsiPlexTitle selected,FarsiPlexTitle item){
        int score=item.kind.equals(selected.kind)?3:0;
        if(year(selected.year)!=0&&Math.abs(year(item.year)-year(selected.year))<=3)score+=2;
        score+=(int)rating(item.rating);return score;
    }
    public static FarsiPlexTitle surprise(List<FarsiPlexTitle> catalog,List<FarsiPlexTitle> ranked,List<LibraryStore.Entry> watched,long seed){
        if(catalog==null||catalog.isEmpty())return null;
        Set<String> seen=new HashSet<>();if(watched!=null)for(LibraryStore.Entry e:watched)seen.add(e.pageUrl);
        List<FarsiPlexTitle> candidates=new ArrayList<>();
        if(ranked!=null)for(FarsiPlexTitle t:ranked)if(t!=null&&!seen.contains(t.url))candidates.add(t);
        if(candidates.isEmpty())for(FarsiPlexTitle t:catalog)if(t!=null&&!seen.contains(t.url))candidates.add(t);
        if(candidates.isEmpty())candidates.addAll(catalog);
        return candidates.get((int)Math.floorMod(seed,candidates.size()));
    }
}
