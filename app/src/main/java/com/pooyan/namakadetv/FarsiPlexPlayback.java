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
import java.util.function.Predicate;
/** Follows only the exact public watch form and same-site JW player; no JS execution or guessed tokens. */
public final class FarsiPlexPlayback {
 private FarsiPlexPlayback(){}
 public static final class Result {
  public final String media,error;
  Result(String media,String error){this.media=media;this.error=error;}
 }
 interface Fetcher { Reply fetch(String url,boolean post,String id,Map<String,String> cookies)throws Exception; }
 static final class Reply {
  final int status;final String url,location,html;final Map<String,String> cookies;
  Reply(int status,String url,String location,String html,Map<String,String> cookies){this.status=status;this.url=url;this.location=location;this.html=html;this.cookies=cookies;}
 }
 private static Result unavailable(String error){return new Result("",error);}
 public static String resolve(Document episode){
  return resolveDetailed(episode).media;
 }
 public static Result resolveDetailed(Document episode){
  return resolveDetailed(episode,(url,post,id,cookies)->{
   Connection c=Jsoup.connect(url).cookies(cookies).followRedirects(false).ignoreHttpErrors(true).timeout(9000).maxBodySize(1000000);
   if(post)c.data("id",id).method(Connection.Method.POST);
   Connection.Response r=c.execute();
   return new Reply(r.statusCode(),r.url().toString(),r.header("Location"),r.body(),r.cookies());
  },MediaProbe::valid);
 }
 static Result resolveDetailed(Document episode,Fetcher fetcher,Predicate<String> probe){
  try{
   Element form=episode.selectFirst("form[id^=watch-][action][method=post]");
   if(form==null||!"https://farsiplex.com/play/".equals(form.absUrl("action")))return unavailable("برای این عنوان منبع پخش عمومی پیدا نشد؛ نمایش پوستر به معنی امکان پخش نیست.");
   Element id=form.selectFirst("input[name=id]");if(id==null||!id.val().matches("[0-9]{1,9}"))return unavailable("فرم پخش سایت معتبر نیست؛ ساختار سایت ممکن است تغییر کرده باشد.");
   Map<String,String> cookies=new HashMap<>();
   Reply r=fetcher.fetch("https://farsiplex.com/play/",true,id.val(),cookies);
   cookies.putAll(r.cookies);
   for(int i=0;i<3&&r.status>=300&&r.status<400;i++){
    String next=new URI(r.url).resolve(r.location).toString();
    URI u=new URI(next);
    if(!"https".equals(u.getScheme())||!"farsiplex.com".equals(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1||!u.getPath().matches("/(play|player|play-[a-z-]+)/"))return unavailable("مسیر پخش خارج از مسیر عمومی مجاز است و دنبال نشد.");
    r=fetcher.fetch(next,false,"",cookies);cookies.putAll(r.cookies);
   }
   if(r.status!=200)return unavailable(httpError(r.status));
   Document player=Jsoup.parse(r.html,r.url);
   for(Element frame:player.select("iframe[src]")){
    String src=frame.absUrl("src");URI u=new URI(src);
    if(!"https".equals(u.getScheme())||!"farsiplex.com".equals(u.getHost())||!"/jwplayer/".equals(u.getPath())||u.getUserInfo()!=null||u.getPort()!=-1)continue;
    Reply response=fetcher.fetch(src,false,"",cookies);
    if(response.status!=200)return unavailable(httpError(response.status));
    String html=response.html;Matcher marker=Pattern.compile("var\\s+jw\\s*=\\s*\\{").matcher(html);
    if(!marker.find())continue;
    int begin=marker.end()-1,end=-1,depth=0;boolean quoted=false,escaped=false;
    for(int i=begin;i<html.length();i++){
     char ch=html.charAt(i);
     if(quoted){if(escaped)escaped=false;else if(ch=='\\')escaped=true;else if(ch=='"')quoted=false;}
     else if(ch=='"')quoted=true;else if(ch=='{')depth++;else if(ch=='}'&&--depth==0){end=i+1;break;}
    }
    if(end<0)continue;
    JSONObject config=new JSONObject(html.substring(begin,end));
    String media=config.optString("file","");
    URI target=new URI(media);
    if("https".equals(target.getScheme())&&"pl.farsiplex.buzz".equals(target.getHost())&&target.getPort()==-1&&target.getUserInfo()==null&&probe.test(media))return new Result(media,"");
   }
  }catch(java.net.SocketTimeoutException e){return unavailable("مهلت دریافت منبع پخش تمام شد؛ دوباره تلاش کنید.");}
  catch(java.io.IOException e){return unavailable("ارتباط با سرور پخش برقرار نشد؛ دوباره تلاش کنید.");}
  catch(Exception e){return unavailable("اطلاعات پلیر سایت قابل خواندن نیست؛ لینک پخش ساخته یا حدس زده نمی‌شود.");}
  return unavailable("منبع عمومی قابل‌پخش تأیید نشد؛ ممکن است لینک منقضی شده یا نیازمند دسترسی رسمی باشد.");
 }
 private static String httpError(int code){
  if(code==401||code==402||code==403)return "سایت دسترسی عمومی به پخش را نپذیرفت (HTTP "+code+")؛ ورود یا اشتراک را در سایت رسمی بررسی کنید.";
  return "سرور پخش پاسخ معتبر نداد (HTTP "+code+")؛ بعداً دوباره تلاش کنید.";
 }
}

