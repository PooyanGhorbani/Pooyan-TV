package com.pooyan.namakadetv;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
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
/** Partial-result aggregation with bounded caches and conservative per-title candidate resolution. */
public final class UnifiedRepository {
 private boolean closed;private int searchGeneration;private final Handler handler=new Handler(Looper.getMainLooper());
 private final FarsiPlexRepository main;private final NamakadeRepository alternate;private final StremioAddonRepository addon;private final ArchiveMoviesRepository archive;
 private final Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
 private final Map<String,Cache> cache=new LinkedHashMap<>();
 private final Map<String,Cache> detailIndex=new LinkedHashMap<>();
 private static final class Cache {long at=android.os.SystemClock.elapsedRealtime();List<FarsiPlexTitle> items;Cache(List<FarsiPlexTitle> items){this.items=new ArrayList<>(items);}}
 public UnifiedRepository(Activity activity){main=new FarsiPlexRepository(activity);alternate=new NamakadeRepository(activity);addon=new StremioAddonRepository(activity);archive=new ArchiveMoviesRepository(activity);}
 private boolean secondary(FarsiPlexTitle item){return !NamakadeCatalog.safeUrl(item.url).isEmpty();}
 private static final class Pending {List<FarsiPlexTitle> first=new ArrayList<>(),second=new ArrayList<>(),third=new ArrayList<>(),foreign=new ArrayList<>();boolean one,two,three,four,finished;String e1,e2,e3,e4;}
 private void deliver(Pending p,String key,FarsiPlexRepository.Result cb){
  if(closed||p.finished)return;
  if(p.first.isEmpty()&&p.second.isEmpty()&&p.third.isEmpty()&&p.foreign.isEmpty()&&!(p.one&&p.two&&p.three&&p.four))return;
  Map<String,List<FarsiPlexTitle>> found=new HashMap<>();
  List<FarsiPlexTitle> base=CatalogMerge.join(p.first,p.second,found);
  Map<String,List<FarsiPlexTitle>> extra=new HashMap<>();
  List<FarsiPlexTitle> out=CatalogMerge.join(base,p.third,extra);
  for(Map.Entry<String,List<FarsiPlexTitle>> pair:extra.entrySet())
   found.computeIfAbsent(pair.getKey(),k->new ArrayList<>()).addAll(pair.getValue());
  Map<String,List<FarsiPlexTitle>> foreignVariants=new HashMap<>();
  List<FarsiPlexTitle> all=CatalogMerge.join(out,p.foreign,foreignVariants);
  for(Map.Entry<String,List<FarsiPlexTitle>> pair:foreignVariants.entrySet())
   found.computeIfAbsent(pair.getKey(),k->new ArrayList<>()).addAll(pair.getValue());
  variants.putAll(found);
  if(p.one&&p.two&&p.three&&p.four){p.finished=true;if(!all.isEmpty()){
   if(cache.size()>=8)cache.remove(cache.keySet().iterator().next());cache.put(key,new Cache(all));}}
  cb.receive(all,all.isEmpty()?(p.e1!=null?p.e1:p.e2!=null?p.e2:p.e3!=null?p.e3:p.e4):null);
 }
 private boolean cached(String key,FarsiPlexRepository.Result cb){Cache c=cache.get(key);if(c!=null&&ResultPolicy.fresh(android.os.SystemClock.elapsedRealtime(),c.at,120000)){cb.receive(new ArrayList<>(c.items),null);return true;}cache.remove(key);return false;}
 public void list(String kind,int page,FarsiPlexRepository.Result cb){
  String key=kind+":"+page;if(cached(key,cb))return;if(page!=1){main.list(kind,page,cb);return;}
  Pending p=new Pending();p.three=!addon.enabled();
  main.list(kind,page,(v,e)->{p.first=v;p.e1=e;p.one=true;deliver(p,key,cb);});
  alternate.list(kind,page,(v,e)->{p.second=v;p.e2=e;p.two=true;deliver(p,key,cb);});
  if(!p.three)addon.list(kind,page,(v,e)->{p.third=v;p.e3=e;p.three=true;deliver(p,key,cb);});
  archive.list(kind,page,(v,e)->{p.foreign=v;p.e4=e;p.four=true;deliver(p,key,cb);});
  handler.postDelayed(()->{p.one=p.two=p.three=p.four=true;
   if(p.e1==null)p.e1="مهلت دریافت بعضی فهرست‌ها تمام شد؛ دوباره تلاش کنید.";
   deliver(p,key,cb);},20000);
 }
 public void search(String text,FarsiPlexRepository.Result cb){
  int token=++searchGeneration;String key="search:"+SearchPolicy.normalize(text);if(cached(key,cb))return;
  Pending p=new Pending();p.three=!addon.enabled();
  List<FarsiPlexTitle> local=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(Cache indexed:detailIndex.values())if(ResultPolicy.fresh(android.os.SystemClock.elapsedRealtime(),indexed.at,120000))
   for(FarsiPlexTitle title:indexed.items)if(SearchPolicy.matches(title,text)&&seen.add(title.url))local.add(title);
  p.first=local;if(!local.isEmpty())cb.receive(new ArrayList<>(local),null);
  FarsiPlexRepository.Result guarded=(v,e)->{if(token==searchGeneration&&!closed)cb.receive(v,e);};
  main.search(text,(v,e)->{List<FarsiPlexTitle> combined=new ArrayList<>(v);
   for(FarsiPlexTitle title:local){boolean exists=false;for(FarsiPlexTitle remote:v)if(remote.url.equals(title.url))exists=true;
    if(!exists)combined.add(title);}
   p.first=combined;p.e1=e;p.one=true;deliver(p,key,guarded);});
  alternate.search(text,(v,e)->{p.second=v;p.e2=e;p.two=true;deliver(p,key,guarded);});
  if(!p.three)addon.search(text,(v,e)->{p.third=v;p.e3=e;p.three=true;deliver(p,key,guarded);});
  archive.search(text,(v,e)->{p.foreign=v;p.e4=e;p.four=true;deliver(p,key,guarded);});
  handler.postDelayed(()->{p.one=p.two=p.three=p.four=true;
   if(p.e1==null)p.e1="جست‌وجوی بعضی منابع پاسخ نداد؛ نتیجه‌های موجود نمایش داده می‌شوند.";
   deliver(p,key,guarded);},20000);
 }
 private void resolve(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){if(archive.supports(item))archive.detail(item,cb);else if(addon.supports(item))addon.detail(item,cb);else if(secondary(item))alternate.detail(item,cb);else main.detail(item,cb);}
 public void foreignMovies(int page,FarsiPlexRepository.Result cb){archive.list("movie",page,cb);}
 public void detail(FarsiPlexTitle item,FarsiPlexRepository.DetailResult cb){
  final boolean[] done={false};final FarsiPlexTitle[] result={item};
  handler.postDelayed(()->{if(!done[0]&&!closed){done[0]=true;if(result[0].publicStream.isEmpty())result[0].playbackError="مهلت بررسی منابع تمام شد؛ بعداً دوباره تلاش کنید.";cb.receive(result[0],null);}},22000);
  resolve(item,(value,error)->{
   if(closed||done[0])return;result[0]=value==null?item:value;remember(result[0]);addCandidate(result[0],result[0]);
   List<FarsiPlexTitle> options=variants.get(item.url);
   if("series".equals(item.kind)||options==null||options.isEmpty()){done[0]=true;cb.receive(result[0],result[0].publicStream.isEmpty()?error:null);return;}
   collect(result[0],options,0,done,cb);
  });
 }
 private void remember(FarsiPlexTitle value){
  FarsiPlexTitle meta=new FarsiPlexTitle(value.url,value.kind,value.name);meta.year=value.year;meta.poster=value.poster;meta.genre=value.genre;meta.aliases.addAll(value.aliases);
  if(detailIndex.size()>=200)detailIndex.remove(detailIndex.keySet().iterator().next());detailIndex.put(meta.url,new Cache(Arrays.asList(meta)));
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
 public void invalidate(){cache.clear();detailIndex.clear();variants.clear();searchGeneration++;}
 public void close(){closed=true;handler.removeCallbacksAndMessages(null);main.close();alternate.close();addon.close();archive.close();cache.clear();detailIndex.clear();}
}
