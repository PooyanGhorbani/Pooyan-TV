package com.pooyan.namakadetv;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONObject;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/** Follows only the exact public watch form and same-site JW player; no JS execution or guessed tokens. */
public final class FarsiPlexPlayback {
 private FarsiPlexPlayback(){}
 public static String resolve(Document episode){
  try{
   Element form=episode.selectFirst("form[id^=watch-][action][method=post]");
   if(form==null||!"https://farsiplex.com/play/".equals(form.absUrl("action")))return "";
   Element id=form.selectFirst("input[name=id]");if(id==null||!id.val().matches("[0-9]{1,9}"))return "";
   Map<String,String> cookies=new HashMap<>();
   Connection.Response r=Jsoup.connect("https://farsiplex.com/play/").data("id",id.val()).method(Connection.Method.POST)
    .followRedirects(false).ignoreHttpErrors(true).timeout(9000).maxBodySize(1000000).execute();
   cookies.putAll(r.cookies());
   for(int i=0;i<3&&r.statusCode()>=300&&r.statusCode()<400;i++){
    String next=new URI(r.url().toString()).resolve(r.header("Location")).toString();
    URI u=new URI(next);
    if(!"https".equals(u.getScheme())||!"farsiplex.com".equals(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1||!u.getPath().matches("/(play|player|play-[a-z-]+)/"))return "";
    r=Jsoup.connect(next).cookies(cookies).followRedirects(false).ignoreHttpErrors(true).timeout(9000).maxBodySize(1000000).execute();cookies.putAll(r.cookies());
   }
   if(r.statusCode()!=200)return "";
   Document player=r.parse();
   for(Element frame:player.select("iframe[src]")){
    String src=frame.absUrl("src");URI u=new URI(src);
    if(!"https".equals(u.getScheme())||!"farsiplex.com".equals(u.getHost())||!"/jwplayer/".equals(u.getPath())||u.getUserInfo()!=null||u.getPort()!=-1)continue;
    Connection.Response response=Jsoup.connect(src).cookies(cookies).followRedirects(false).ignoreHttpErrors(true).timeout(9000).maxBodySize(600000).execute();
    if(response.statusCode()!=200)continue;
    Matcher m=Pattern.compile("var\\s+jw\\s*=\\s*(\\{[^;]*\\})\\s*;",Pattern.DOTALL).matcher(response.body());
    if(!m.find())continue;
    JSONObject config=new JSONObject(m.group(1));
    String media=config.optString("file","");
    URI target=new URI(media);
    if("https".equals(target.getScheme())&&"pl.farsiplex.buzz".equals(target.getHost())&&target.getPort()==-1&&target.getUserInfo()==null&&MediaProbe.valid(media))return media;
   }
  }catch(Exception ignored){}
  return "";
 }
}
