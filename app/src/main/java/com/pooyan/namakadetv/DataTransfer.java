package com.pooyan.namakadetv;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
/** Explicit schema-limited profile import/export. No media tokens, credentials or private app access. */
public final class DataTransfer {
 public static String safePoster(String url){if(!ImageUrlPolicy.allowed(url))return "";try{return new java.net.URI(url).getRawQuery()==null?url:"";}catch(Exception e){return "";}}
 public static boolean page(String url){return url!=null&&url.length()<=1800&&( !FarsiPlexUrlPolicy.mediaType(url).isEmpty()||!NamakadeCatalog.safeUrl(url).isEmpty()||!ArchiveMoviesRepository.safePage(url).isEmpty());}
 public static String exportData(SharedPreferences prefs,LibraryStore library)throws Exception{
  JSONArray favorites=new JSONArray(prefs.getString("favorites","[]")),safe=new JSONArray(),progress=new JSONArray();
  for(int i=0;i<Math.min(150,favorites.length());i++){JSONObject o=favorites.optJSONObject(i);if(o!=null&&page(o.optString("url")))safe.put(new JSONObject().put("url",PlaybackResumePolicy.identity("",o.optString("url"))).put("title",o.optString("title")).put("kind",o.optString("kind")).put("poster",safePoster(o.optString("poster"))));}
  java.util.List<LibraryStore.Entry> history=new java.util.ArrayList<>(library.watched());java.util.Set<String> pages=new java.util.HashSet<>();for(LibraryStore.Entry e:history)pages.add(e.pageUrl);for(LibraryStore.Entry e:library.resumeList())if(pages.add(e.pageUrl))history.add(e);
  for(LibraryStore.Entry e:history)if(page(e.pageUrl))progress.put(new JSONObject().put("page",PlaybackResumePolicy.identity("",e.pageUrl)).put("title",e.title).put("pos",e.positionMs).put("duration",e.durationMs));
  return new JSONObject().put("format","pooyan-profile").put("version",1).put("favorites",safe).put("progress",progress).toString();
 }
 public static void importData(String raw,SharedPreferences prefs,LibraryStore library)throws Exception{
  if(raw==null||raw.length()>262144)throw new IllegalArgumentException("size");JSONObject data=new JSONObject(raw);
  if(!"pooyan-profile".equals(data.optString("format"))||data.optInt("version")!=1)throw new IllegalArgumentException("format");
  JSONArray existing=new JSONArray(prefs.getString("favorites","[]")),incoming=data.optJSONArray("favorites");java.util.Set<String> seen=new java.util.HashSet<>();for(int i=0;i<existing.length();i++)seen.add(existing.optJSONObject(i).optString("url"));
  if(incoming!=null)for(int i=0;i<Math.min(150,incoming.length())&&existing.length()<150;i++){JSONObject o=incoming.optJSONObject(i);if(o==null||!page(o.optString("url"))||!seen.add(o.optString("url")))continue;String url=PlaybackResumePolicy.identity("",o.optString("url"));String kind=FarsiPlexUrlPolicy.mediaType(url);if(kind.isEmpty())kind=url.contains("/episodes/")?"episode":url.contains("/series")?"series":"movie";existing.put(new JSONObject().put("url",url).put("title",o.optString("title").substring(0,Math.min(115,o.optString("title").length()))).put("kind",kind).put("poster",safePoster(o.optString("poster"))));}
  JSONArray progress=data.optJSONArray("progress");if(progress!=null)for(int i=0;i<Math.min(200,progress.length());i++){JSONObject o=progress.optJSONObject(i);if(o==null||!page(o.optString("page")))continue;String page=PlaybackResumePolicy.identity("",o.optString("page"));if(library.getResumePosition("",page)==0)library.savePlayback(o.optString("title"),page,page,Math.max(0,o.optLong("pos")),Math.max(0,o.optLong("duration")));}
  prefs.edit().putString("favorites",existing.toString()).apply();
 }
}
