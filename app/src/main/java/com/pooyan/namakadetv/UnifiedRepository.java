package com.pooyan.namakadetv;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.util.*;
/** Partial-result aggregation with bounded caches and conservative per-title candidate resolution. */
public final class UnifiedRepository {
 private boolean closed;private int searchGeneration;private final Handler handler=new Handler(Looper.getMainLooper());
 private final FarsiPlexRepository main;private final NamakadeRepository alternate;
 private final Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
 private final Map<String,Cache> cache=new LinkedHashMap<>();
 private static final class Cache {long at=System.currentTimeMillis();List<FarsiPlexTitle> items;Cache(List<FarsiPlexTitle> items){this.items=new ArrayList<>(items);}}
 public UnifiedRepository(Activity activity){main=new FarsiPlexRepository(activity);alternate=new NamakadeRepository(activity);}
 private boolean secondary(FarsiPlexTitle item){return !NamakadeCatalog.safeUrl(item.url).isEmpty();}
 private static final class Pending {List<FarsiPlexTitle> first=new ArrayList<>(),second=new ArrayList<>();boolean one,two,finished;String e1,e2;}
 private void deliver(Pending p,String key,FarsiPlexRepository.Result cb){
  if(closed||p.finished)return;
  if(p.first.isEmpty()&&p.second.isEmpty()&&!(p.one&&p.two))return;
  Map<String,List<FarsiPlexTitle>> found=new HashMap<>();List<FarsiPlexTitle> out=CatalogMerge.join(p.first,p.second,found);variants.putAll(found);
  if(p.one&&p.two){p.finished=true;if(!out.isEmpty()){if(cache.size()>=8)cache.remove(cache.keySet().iterator().next());cache.put(key,new Cache(out));}}
  cb.receive(out,out.isEmpty()?(p.e1!=null?p.e1:p.e2):null);
 }
 private boolean cached(String key,FarsiPlexRepository.Result cb){Cache c=cache.get(key);if(c!=null&&System.currentTimeMillis()-c.at<120000){cb.receive(new ArrayList<>(c.items),null);return true;}cache.remove(key);return false;}
 public void list(String kind,int page,FarsiPlexRepository.Result cb){
  String key=kind+":"+page;if(cached(key,cb))return;if(page!=1){main.list(kind,page,cb);return;}Pending p=new Pending();
  main.list(kind,page,(v,e)->{p.first=v;p.e1=e;p.one=true;deliver(p,key,cb);});
  alternate.list(kind,page,(v,e)->{p.second=v;p.e2=e;p.two=true;deliver(p,key,cb);});
  handler.postDelayed(()->{p.one=p.two=true;if(p.e1==null)p.e1="مهلت دریافت بعضی فهرست‌ها تمام شد؛ دوباره تلاش کنید.";deliver(p,key,cb);},14000);
 }
 public void search(String text,FarsiPlexRepository.Result cb){
  int token=++searchGeneration;String key="search:"+SearchPolicy.normalize(text);if(cached(key,cb))return;Pending p=new Pending();
  FarsiPlexRepository.Result guarded=(v,e)->{if(token==searchGeneration&&!closed)cb.receive(v,e);};
  main.search(text,(v,e)->{p.first=v;p.e1=e;p.one=true;deliver(p,key,guarded);});
  alternate.search(text,(v,e)->{p.second=v;p.e2=e;p.two=true;deliver(p,key,guarded);});
  handler.postDelayed(()->{p.one=p.two=true;if(p.e1==null)p.e1="جست‌وجوی بعضی منابع پاسخ نداد؛ نتیجه‌های موجود نمایش داده می‌شوند.";deliver(p,key,guarded);},14000);
 }
 private void resolve(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){if(secondary(item))alternate.detail(item,cb);else main.detail(item,cb);}
 public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){
  final boolean[] done={false};final FarsiPlexTitle[] result={item};
  handler.postDelayed(()->{if(!done[0]&&!closed){done[0]=true;if(result[0].publicStream.isEmpty())result[0].playbackError="مهلت بررسی منابع تمام شد؛ بعداً دوباره تلاش کنید.";cb.receive(result[0],null);}},22000);
  resolve(item,(value,error)->{
   if(closed||done[0])return;result[0]=value==null?item:value;addCandidate(result[0],result[0]);
   List<FarsiPlexTitle> options=variants.get(item.url);
   if("series".equals(item.kind)||options==null||options.isEmpty()){done[0]=true;cb.receive(result[0],result[0].publicStream.isEmpty()?error:null);return;}
   collect(result[0],options,0,done,cb);
  });
 }
 private void addCandidate(FarsiPlexTitle owner,FarsiPlexTitle resolved){
  if(resolved==null||resolved.publicStream.isEmpty())return;
  for(PlaybackCandidate c:owner.candidates)if(c.url.equals(resolved.publicStream))return;
  owner.candidates.add(new PlaybackCandidate(resolved.publicStream,resolved.url,0,true));
  if(owner.publicStream.isEmpty())owner.publicStream=resolved.publicStream;
 }
 private void collect(FarsiPlexTitle owner,List<FarsiPlexTitle> options,int n,boolean[] done,FarsiPlexRepository.DetailResult cb){
  if(done[0]||closed)return;if(n>=Math.min(options.size(),2)){done[0]=true;cb.receive(owner,null);return;}
  resolve(options.get(n),(value,error)->{if(done[0]||closed)return;addCandidate(owner,value);if(owner.poster.isEmpty()&&value!=null)owner.poster=value.poster;if(!owner.publicStream.isEmpty())owner.playbackError="";collect(owner,options,n+1,done,cb);});
 }
 public void close(){closed=true;handler.removeCallbacksAndMessages(null);main.close();alternate.close();cache.clear();}
}
