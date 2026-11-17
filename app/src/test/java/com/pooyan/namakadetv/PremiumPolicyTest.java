package com.pooyan.namakadetv;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import android.content.SharedPreferences;
import org.json.*;
public class PremiumPolicyTest {
 private FarsiPlexTitle title(String path,String name,String year){FarsiPlexTitle t=new FarsiPlexTitle("https://farsiplex.com/movie/"+path+"/","movie",name);t.year=year;return t;}
 @Test public void verifiedFirstAndNoRetriesOfTheSameCandidate(){
  List<PlaybackCandidate> options=Arrays.asList(new PlaybackCandidate("https://example.invalid/a.mp4","",2160,false),new PlaybackCandidate("https://example.invalid/b.mp4","",720,true));
  SourceHealth.Session s=new SourceHealth.Session();assertTrue(s.next(options,1000).url.endsWith("b.mp4"));assertTrue(s.next(options,1000).url.endsWith("a.mp4"));assertNull(s.next(options,1000));
 }
 @Test public void healthCooldownAndThreeAttemptBound(){
  List<PlaybackCandidate> options=new ArrayList<>();for(int i=0;i<5;i++)options.add(new PlaybackCandidate("https://example.invalid/bound"+i+".mp4","",0,true));
  SourceHealth.failed(options.get(0).url,SourceHealth.Failure.ACCESS,1000);SourceHealth.Session s=new SourceHealth.Session();assertEquals(options.get(1).url,s.next(options,2000).url);assertNotNull(s.next(options,2000));assertNotNull(s.next(options,2000));assertNull(s.next(options,2000));assertTrue(SourceHealth.healthy(options.get(0).url,62000));
 }
 @Test public void decodingCooldownAndFriendlyReasonAreDistinct(){SourceHealth.failed("decode",SourceHealth.Failure.DECODING,0);assertFalse(SourceHealth.healthy("decode",60000));assertTrue(SourceHealth.healthy("decode",600001));assertNotEquals(SourceHealth.message(SourceHealth.Failure.ACCESS),SourceHealth.message(SourceHealth.Failure.TIMEOUT));}
 @Test public void conservativeMergeRetainsCandidatesAndSeparatesRemakes(){
  FarsiPlexTitle a=title("a","Example","2020"),b=title("b","example","2020"),remake=title("c","Example","2025"),unknown=title("d","Example","");Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
  List<FarsiPlexTitle> result=CatalogMerge.join(Arrays.asList(a),Arrays.asList(b,remake,unknown),variants);assertEquals(3,result.size());assertEquals(b.url,variants.get(a.url).get(0).url);
 }
 @Test public void unknownYearsAndEpisodeNumbersNeverFuzzyMerge(){
  FarsiPlexTitle a=title("a","Example",""),b=title("b","Example","");assertNotEquals(CatalogMerge.matchKey(a),CatalogMerge.matchKey(b));
  FarsiPlexTitle e1=new FarsiPlexTitle("https://farsiplex.com/episode/e1/","episode","Show"),e2=new FarsiPlexTitle("https://farsiplex.com/episode/e2/","episode","Show");assertNotEquals(CatalogMerge.matchKey(e1),CatalogMerge.matchKey(e2));
 }
 @Test public void persianEnglishAndAvailableAliases(){
  assertEquals("میمون مست 12",SearchPolicy.normalize("ميمون‌ مست ۱۲"));assertEquals("season 2 2025",SearchPolicy.normalize("Season ٢ 2025"));
  FarsiPlexTitle t=title("a","Meymoone Mast","2026");t.aliases.add("میمون مست");assertTrue(SearchPolicy.matches(t,"ميمون مست"));assertTrue(SearchPolicy.matches(t,"meymoone"));assertFalse(SearchPolicy.matches(t,"other"));
 }
 @Test public void trackLabelsNeverInventResolutionAndUnsupportedTracksHidden(){assertEquals("کیفیت اعلام نشده",TrackPolicy.video(-1,-1));assertTrue(TrackPolicy.video(720,1000000).startsWith("720p"));assertFalse(TrackPolicy.selectable(false,0,2));assertEquals("fa · فارسی",TrackPolicy.language("fa","فارسی"));}
 @Test public void positiveSubtitleDelayUsesMediaTimeAndClearsAfterSeek(){CueDelayQueue<String> q=new CueDelayQueue<>();q.add(1000,500,"متن فارسی");assertNull(q.poll(1000));assertNull(q.poll(1499));assertEquals("متن فارسی",q.poll(1500));q.add(2000,500,"next");q.clear();assertNull(q.poll(3000));}
 private FarsiPlexTitle episode(int season,int number){FarsiPlexTitle e=new FarsiPlexTitle("https://farsiplex.com/episode/s"+season+"e"+number+"/","episode","قسمت "+number);e.season=season;e.episode=number;return e;}
 @Test public void nextEpisodeUsesNumbersAndStopsAtMissingOrUnknown(){FarsiPlexTitle one=episode(1,1),two=episode(1,2),four=episode(1,4),other=episode(2,1);List<FarsiPlexTitle> list=Arrays.asList(four,other,two,one);assertEquals(two,EpisodeOrder.next(list,one.url));assertNull(EpisodeOrder.next(list,two.url));assertNull(EpisodeOrder.next(list,"absent"));}
 @Test public void autoplayPausesCancelsAndNeverTicksAfterExit(){EpisodeOrder.Countdown c=new EpisodeOrder.Countdown(3);assertFalse(c.tick(false,true));assertEquals(3,c.remaining());assertFalse(c.tick(true,false));assertFalse(c.tick(true,true));c.cancel();assertFalse(c.tick(true,true));EpisodeOrder.Countdown d=new EpisodeOrder.Countdown(3);assertFalse(d.tick(true,true));assertFalse(d.tick(true,true));assertTrue(d.tick(true,true));}
 @Test public void episodeContextRoundTripDoesNotAddMedia(){FarsiPlexTitle a=episode(1,1),b=episode(1,2);List<FarsiPlexTitle> decoded=EpisodeOrder.decode(EpisodeOrder.encode(Arrays.asList(a,b)));assertEquals(2,decoded.size());assertEquals(b.url,EpisodeOrder.next(decoded,a.url).url);assertEquals("",decoded.get(1).publicStream);}
 @Test public void historySeparateFromBrowsingAndCompletionIsConfigurable(){SharedPreferences p=LibraryStoreTest.memory();LibraryStore lib=new LibraryStore(p);lib.visit("Browse","https://farsiplex.com/movie/a/");assertTrue(lib.watched().isEmpty());lib.completion(95);lib.savePlayback("A","https://example.invalid/a.mp4","https://farsiplex.com/movie/a/",96000,100000);assertTrue(lib.resumeList().isEmpty());assertEquals(1,lib.completed().size());lib.clearHistory();assertTrue(lib.watched().isEmpty());assertEquals(1,lib.recentPages().size());}
 @Test public void explicitImportExcludesSecretsAndPreservesCurrentEntries()throws Exception{SharedPreferences p=LibraryStoreTest.memory();LibraryStore l=new LibraryStore(p);l.savePlayback("A","https://example.invalid/a.mp4?token=private","https://farsiplex.com/movie/a/?secret=private",10000,100000);String exported=DataTransfer.exportData(p,l);assertFalse(exported.contains("private"));assertFalse(exported.contains("token="));SharedPreferences other=LibraryStoreTest.memory();LibraryStore imported=new LibraryStore(other);DataTransfer.importData(exported,other,imported);assertEquals(10000,imported.getResumePosition("","https://farsiplex.com/movie/a/"));}
 @Test public void importRejectsUnknownFormat()throws Exception{try{DataTransfer.importData("{\"format\":\"not-pooyan\"}",LibraryStoreTest.memory(),new LibraryStore(LibraryStoreTest.memory()));fail();}catch(IllegalArgumentException expected){}}
 @Test public void localRecommendationsPreferFavoriteGenres(){FarsiPlexTitle a=title("a","A","2026"),b=title("b","B","2026"),fav=title("f","F","2026");a.genre="Drama";b.genre="Comedy";fav.genre="Comedy";assertEquals(b,Recommendations.rank(Arrays.asList(a,b),Arrays.asList(fav),Collections.emptyList()).get(0));}
 @Test public void voiceMissingDeniedAndEmptyFallBack(){assertEquals("",VoiceInputPolicy.result(null));assertEquals("",VoiceInputPolicy.result(Arrays.asList(" ")));assertEquals("میمون مست",VoiceInputPolicy.result(Arrays.asList(" میمون مست ")));assertNotEquals(VoiceInputPolicy.error(false,false),VoiceInputPolicy.error(true,true));}
 @Test public void oneFailingSourceCannotBlockPartialResultsAndCacheExpires(){assertTrue(ResultPolicy.emit(4,0,true,false));assertTrue(ResultPolicy.emit(0,3,false,true));assertFalse(ResultPolicy.emit(0,0,true,false));assertTrue(ResultPolicy.emit(0,0,true,true));assertTrue(ResultPolicy.fresh(119999,0,120000));assertFalse(ResultPolicy.fresh(120000,0,120000));assertFalse(ResultPolicy.fresh(10,20,120000));}
 @Test public void observedPublicSearchMarkupAndDefaultHttpsPort(){String html="<div class='searchResultHold'><div><a href='https://namakade.com:443/series/example'><img src='https://media.negahestan.com/example.jpg'></a></div><div class='searchTxtHold'>Example</div></div>";List<FarsiPlexTitle> values=NamakadeCatalog.searchListing(html);assertEquals(1,values.size());assertEquals("https://namakade.com/series/example",values.get(0).url);assertEquals("Example",values.get(0).name);assertEquals("series",values.get(0).kind);assertTrue(values.get(0).poster.endsWith("example.jpg"));}
 @Test public void stableIdsDoNotMergeDifferentKnownSeasons(){FarsiPlexTitle a=new FarsiPlexTitle("a","series","Show"),b=new FarsiPlexTitle("b","series","Show");a.stableId=b.stableId="catalog:1";a.season=1;b.season=2;assertNotEquals(CatalogMerge.matchKey(a),CatalogMerge.matchKey(b));}
 @Test public void exportedArtworkNeverContainsSignedQuery(){assertEquals("",DataTransfer.safePoster("https://media.negahestan.com/poster.jpg?token=private"));assertEquals("https://media.negahestan.com/poster.jpg",DataTransfer.safePoster("https://media.negahestan.com/poster.jpg"));assertFalse(DataTransfer.page(null));}
}
