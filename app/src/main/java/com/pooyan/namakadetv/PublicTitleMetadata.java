package com.pooyan.namakadetv;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Genuine published JSON-LD metadata only; ratings/cast never invented. */
public final class PublicTitleMetadata {
    private PublicTitleMetadata(){}
    public static final class Details {public String rating="",cast="",runtime="";}
    public static Details from(Document page){
        Details out=new Details();if(page==null)return out;
        for(Element script:page.select("script[type=application/ld+json]")){
            if(script.data().length()>100000)continue;
            try{
                Object root=new org.json.JSONTokener(script.data()).nextValue();
                parse(root,out,0);
                if(!out.rating.isEmpty()||!out.cast.isEmpty()||!out.runtime.isEmpty())break;
            }catch(Exception ignored){}
        }
        return out;
    }
    private static void parse(Object node,Details result,int depth){
        if(depth>3||node==null)return;
        if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<Math.min(a.length(),12);i++)parse(a.opt(i),result,depth+1);return;}
        if(!(node instanceof JSONObject))return;JSONObject o=(JSONObject)node;
        Object type=o.opt("@type");String typeName=type==null?"":type.toString();
        if(typeName.contains("Movie")||typeName.contains("TVSeries")||typeName.contains("TVEpisode")){
            JSONObject ratings=o.optJSONObject("aggregateRating");
            if(ratings!=null){double score=ratings.optDouble("ratingValue",-1);double maximum=ratings.optDouble("bestRating",10);
                if(score>=0&&score<=maximum&&maximum>=1&&maximum<=10)result.rating=String.format(java.util.Locale.US,"%.1f / %.0f",score,maximum);
            }
            String duration=o.optString("duration","");if(duration.matches("PT[0-9]{1,3}H[0-9]{1,2}M")||duration.matches("PT[0-9]{1,3}M"))result.runtime=duration;
            Object actor=o.opt("actor");List<String> names=new ArrayList<>();
            if(actor instanceof JSONArray){JSONArray a=(JSONArray)actor;for(int i=0;i<Math.min(5,a.length());i++)addName(a.opt(i),names);}
            else addName(actor,names);
            if(!names.isEmpty())result.cast=String.join("، ",names);
        }
        parse(o.opt("@graph"),result,depth+1);
    }
    private static void addName(Object actor,List<String> out){if(actor instanceof JSONObject){String n=((JSONObject)actor).optString("name","").trim();if(!n.isEmpty()&&n.length()<=80)out.add(n);}}
}
