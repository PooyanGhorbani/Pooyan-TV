package com.pooyan.namakadetv;
import java.net.URI;
import java.util.Locale;
/** Stable per-title identity and bounded VOD timeline calculations, independent of Android. */
public final class PlaybackResumePolicy {
 private PlaybackResumePolicy(){}
 public static String identity(String stream,String page){
  if(StreamDetector.isHttpsUrl(page))try{
   URI u=new URI(page);String path=u.getRawPath();
   return "https://"+u.getHost().toLowerCase(Locale.ROOT)+(path==null?"/":path.replaceAll("/+$", ""));
  }catch(Exception ignored){}
  return stream==null?"":stream;
 }
 public static boolean completed(long position,long duration){
  return duration>0&&position>0&&(position>=duration*0.97||(duration>=120000&&position>=duration-30000));
 }
 public static long resumeAfterBackground(long position,long duration,boolean ended){
  if(ended)return 0;long safe=Math.max(0,position);
  return duration>0&&duration<Long.MAX_VALUE?Math.min(safe,duration):safe;
 }
 public static boolean canSeek(long duration,boolean live,boolean seekable){return duration>0&&duration<Long.MAX_VALUE&&!live&&seekable;}
 public static long seek(long position,long delta,long duration){
  if(duration<=0)return Math.max(0,position);
  double target=(double)Math.max(0,position)+delta;
  return (long)Math.max(0,Math.min(target,duration));
 }
 public static String time(long ms){
  if(ms<0)return "--:--";long sec=ms/1000;
  return sec>=3600?String.format(Locale.US,"%d:%02d:%02d",sec/3600,(sec/60)%60,sec%60):String.format(Locale.US,"%02d:%02d",sec/60,sec%60);
 }
}
