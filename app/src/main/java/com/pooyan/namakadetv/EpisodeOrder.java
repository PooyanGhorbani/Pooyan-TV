package com.pooyan.namakadetv;
import java.util.*;
import org.json.*;
/** Never infer episode identity from a fuzzy movie title or guess across missing seasons. */
public final class EpisodeOrder {
 public static FarsiPlexTitle next(List<FarsiPlexTitle> episodes,String page){FarsiPlexTitle current=null;for(FarsiPlexTitle e:episodes)if(e.url.equals(page))current=e;if(current==null||current.season<1||current.episode<1)return null;FarsiPlexTitle result=null;for(FarsiPlexTitle e:episodes)if(e.season==current.season&&e.episode==current.episode+1){if(result!=null&&!result.url.equals(e.url))return null;result=e;}return result;}
 public static String encode(List<FarsiPlexTitle> episodes){JSONArray a=new JSONArray();for(FarsiPlexTitle e:episodes)try{a.put(new JSONObject().put("page",e.url).put("title",e.name).put("poster",e.poster).put("season",e.season).put("episode",e.episode));}catch(Exception ignored){}return a.toString();}
 public static List<FarsiPlexTitle> decode(String raw){List<FarsiPlexTitle> out=new ArrayList<>();try{JSONArray a=new JSONArray(raw==null?"[]":raw);for(int i=0;i<Math.min(250,a.length());i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String page=o.optString("page");if(!"episode".equals(FarsiPlexUrlPolicy.mediaType(page))&&NamakadeCatalog.safeUrl(page).isEmpty())continue;FarsiPlexTitle e=new FarsiPlexTitle(page,"episode",o.optString("title"));e.poster=o.optString("poster");e.season=o.optInt("season");e.episode=o.optInt("episode");out.add(e);}}catch(Exception ignored){}return out;}
 public static final class Countdown {private int remaining;private boolean cancelled;public Countdown(int seconds){remaining=Math.max(3,Math.min(seconds,30));}public void cancel(){cancelled=true;}public boolean tick(boolean playing,boolean foreground){if(cancelled||!foreground||!playing)return false;return --remaining<=0;}public int remaining(){return Math.max(0,remaining);}public boolean cancelled(){return cancelled;}}
}
