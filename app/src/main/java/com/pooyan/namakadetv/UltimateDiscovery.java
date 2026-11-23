package com.pooyan.namakadetv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extended local filters, collections and recommendation candidates over actually fetched metadata. */
public final class UltimateDiscovery {
    private UltimateDiscovery(){}
    public static List<FarsiPlexTitle> filter(List<FarsiPlexTitle> input,String kind,String genre,
            int since,int until,int ratingAtLeast,Set<String> excluded,boolean unwatchedOnly){
        List<FarsiPlexTitle> out=new ArrayList<>();if(input==null)return out;
        for(FarsiPlexTitle t:input){
            if(t==null||kind!=null&&!kind.isEmpty()&&!kind.equals(t.kind))continue;
            if(genre!=null&&!genre.trim().isEmpty()){
                String have=CatalogIdentity.normalized(t.genre),want=CatalogIdentity.normalized(genre);
                boolean match=have.contains(want);
                if(want.equals("comedy"))match=match||have.contains("کمدی");
                if(want.equals("drama"))match=match||have.contains("درام");
                if(!match)continue;
            }
            int year=DiscoveryPolicy.year(t.year);
            if(since>0&&(year==0||year<since)||until>0&&(year==0||year>until))continue;
            if(ratingAtLeast>0&&DiscoveryPolicy.rating(t.rating)<ratingAtLeast)continue;
            if(unwatchedOnly&&excluded!=null&&excluded.contains(UltimateIdentity.contentKey(t)))continue;
            out.add(t);
        }
        return out;
    }
    public static List<FarsiPlexTitle> unique(List<FarsiPlexTitle> input){
        List<FarsiPlexTitle> out=new ArrayList<>();if(input==null)return out;
        for(FarsiPlexTitle t:input){if(t==null)continue;boolean existing=false;
            for(FarsiPlexTitle seen:out)if(UltimateIdentity.sameTitle(t,seen)){existing=true;break;}
            if(!existing)out.add(t);}
        return out;
    }
    public static List<FarsiPlexTitle> collection(List<FarsiPlexTitle> input,String preset){
        if("comedy".equals(preset))return filter(input,"","comedy",0,0,0,null,false);
        if("new".equals(preset))return filter(input,"","",2020,0,0,null,false);
        if("classic".equals(preset))return filter(input,"","",1900,1999,0,null,false);
        if("top".equals(preset))return filter(input,"","",0,0,7,null,false);
        return input==null?new ArrayList<>():new ArrayList<>(input);
    }
    public static FarsiPlexTitle surprise(List<FarsiPlexTitle> input,Set<String> hidden,Set<String> watched,long seed){
        List<FarsiPlexTitle> possible=new ArrayList<>();if(input==null)return null;
        for(FarsiPlexTitle t:unique(input)){
            String key=UltimateIdentity.contentKey(t);
            if(hidden!=null&&hidden.contains(key)||watched!=null&&watched.contains(key))continue;
            possible.add(t);
        }
        return possible.isEmpty()?null:possible.get((int)Math.floorMod(seed,possible.size()));
    }
}
