package com.pooyan.namakadetv;
import org.json.*;
import java.util.*;
public final class CandidateCodec {
 public static String encode(List<PlaybackCandidate> values){JSONArray a=new JSONArray();for(PlaybackCandidate c:values)try{a.put(new JSONObject().put("url",c.url).put("page",c.page).put("height",c.height).put("verified",c.verified));}catch(Exception ignored){}return a.toString();}
 public static List<PlaybackCandidate> decode(String raw){List<PlaybackCandidate> out=new ArrayList<>();try{JSONArray a=new JSONArray(raw==null?"[]":raw);for(int i=0;i<Math.min(8,a.length());i++){JSONObject o=a.optJSONObject(i);if(o!=null&&StreamDetector.isDirectMedia(o.optString("url")))out.add(new PlaybackCandidate(o.optString("url"),o.optString("page"),o.optInt("height"),o.optBoolean("verified")));}}catch(Exception ignored){}return out;}
}
