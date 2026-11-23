package com.pooyan.namakadetv;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
/** Bounded process-local health; never logs URLs or persists tokens. */
public final class SourceHealth {
 public enum Failure{HTTP,TIMEOUT,PARSING,UNSUPPORTED,EXPIRED,ACCESS,DECODING,NETWORK,INCOMPATIBLE}
 private static final Map<String,Long> until=new LinkedHashMap<>();
 private static volatile int maxAttempts=3;
 private static volatile long httpCooldownMs=60000,decodeCooldownMs=600000;
 public static void configure(int max,long http,long decode){
  if(max<1||max>3||http<30000||http>180000||decode<180000||decode>900000)return;
  maxAttempts=max;httpCooldownMs=http;decodeCooldownMs=decode;
 }
 public static synchronized void failed(String url,Failure why,long now){if(until.size()>=256)until.remove(until.keySet().iterator().next());until.put(url,now+(why==Failure.DECODING||why==Failure.UNSUPPORTED?decodeCooldownMs:httpCooldownMs));}
 public static synchronized boolean healthy(String url,long now){Long expiry=until.get(url);return expiry==null||expiry<=now;}
 public static final class Session {
  private final Set<String> tried=new HashSet<>();private int attempts;
  public PlaybackCandidate next(List<PlaybackCandidate> candidates,long now){if(attempts>=maxAttempts)return null;List<PlaybackCandidate> sorted=new ArrayList<>(candidates);sorted.sort((a,b)->{int v=Boolean.compare(b.verified,a.verified);return v!=0?v:Integer.compare(b.height,a.height);});for(PlaybackCandidate c:sorted)if(!tried.contains(c.url)&&healthy(c.url,now)&&StreamDetector.isDirectMedia(c.url)){tried.add(c.url);attempts++;return c;}return null;}
  public PlaybackCandidate nextOrdered(List<PlaybackCandidate> ordered,long now){
    if(attempts>=maxAttempts)return null;
    for(PlaybackCandidate c:ordered)if(c!=null&&!tried.contains(c.url)&&healthy(c.url,now)&&StreamDetector.isDirectMedia(c.url)){
        tried.add(c.url);attempts++;return c;
    }
    return null;
   }
   public void markTried(String url){if(url!=null&&!url.isEmpty())tried.add(url);}
   public int attempts(){return attempts;}
 }
 public static String message(Failure f){switch(f){case TIMEOUT:return "مهلت ارتباط با منبع تمام شد.";case EXPIRED:return "لینک منبع دیگر معتبر نیست.";case ACCESS:return "دسترسی عمومی به منبع پذیرفته نشد.";case DECODING:return "تلویزیون نتوانست رسانه را رمزگشایی کند.";case UNSUPPORTED:return "قالب رسانه پشتیبانی نمی‌شود.";case PARSING:return "اطلاعات پلیر قابل خواندن نیست.";case INCOMPATIBLE:return "مدت منبع جایگزین با این عنوان سازگار نیست.";case NETWORK:return "ارتباط شبکه برقرار نشد.";default:return "سرور رسانه پاسخ معتبر نداد.";}}
}
