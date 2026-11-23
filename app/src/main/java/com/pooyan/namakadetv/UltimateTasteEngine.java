package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Explainable local recommendation model. No external AI or profile data transmission. */
public final class UltimateTasteEngine {
    private UltimateTasteEngine(){}
    public static List<FarsiPlexTitle> rank(List<FarsiPlexTitle> catalog,
            List<FarsiPlexTitle> favorites,Set<String> watched,Set<String> disliked,int limit){
        List<FarsiPlexTitle> input=UltimateDiscovery.unique(catalog),result=new ArrayList<>();
        if(favorites==null)favorites=new ArrayList<>();
        final List<FarsiPlexTitle> likes=favorites;
        Set<String> completed=watched==null?new HashSet<>():watched;
        Set<String> negatives=disliked==null?new HashSet<>():disliked;
        for(FarsiPlexTitle t:input){String id=UltimateIdentity.contentKey(t);
            if(!completed.contains(id)&&!negatives.contains(id))result.add(t);}
        result.sort((a,b)->Integer.compare(score(b,likes),score(a,likes)));
        if(result.size()>Math.max(0,limit))return new ArrayList<>(result.subList(0,Math.max(0,limit)));
        return result;
    }
    static int score(FarsiPlexTitle title,List<FarsiPlexTitle> favorites){
        int score=DiscoveryPolicy.year(title.year)>0?Math.min(3,Math.max(0,DiscoveryPolicy.year(title.year)-2018)/3):0;
        score+=(int)Math.max(0,Math.min(10,DiscoveryPolicy.rating(title.rating)));
        for(FarsiPlexTitle f:favorites){if(f==null)continue;
            if(title.kind.equals(f.kind))score+=1;
            String a=CatalogIdentity.normalized(title.genre),b=CatalogIdentity.normalized(f.genre);
            if(!a.isEmpty()&&!b.isEmpty()){
                String[] aa=a.split("[,/|\\s]+");String[] bb=b.split("[,/|\\s]+");
                for(String x:aa)if(x.length()>2)for(String y:bb)if(x.equals(y)){score+=4;break;}
            }
        }
        return score;
    }
}
