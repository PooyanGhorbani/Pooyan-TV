package com.pooyan.namakadetv;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs only on CI Android TV emulator. Fixture pages do not assert media playback. */
@RunWith(AndroidJUnit4.class)
public class TvPremiumInstrumentedTest {
 @Before public void pickerOff(){InstrumentationRegistry.getInstrumentation().setInTouchMode(false);context().getSharedPreferences("family_profiles",0).edit().putBoolean("picker",false).commit();}
 private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 private Object field(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){throw new AssertionError(e);}}
 private void set(Object owner,String name,Object value){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}catch(Exception e){throw new AssertionError(e);}}
 private Object invoke(Object owner,String name,Class<?>[] types,Object...args){try{Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}catch(Exception e){throw new AssertionError(e);}}
 private void key(int code){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code);InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
 @Test public void posterOrderDirectionAndSelectAreStable(){
  final LinearLayout[] rows=new LinearLayout[2];AtomicInteger clicks=new AtomicInteger();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{
    set(a,"destroyed",true);invoke(a,"begin",new Class[]{String.class},"movies");LinearLayout content=(LinearLayout)field(a,"content");
    for(int n=0;n<2;n++){rows[n]=new LinearLayout(a);content.addView(rows[n]);}
    for(int i=0;i<5;i++)invoke(a,"card",new Class[]{LinearLayout.class,FarsiPlexTitle.class},rows[i<3?0:1],new FarsiPlexTitle("https://farsiplex.com/movie/fixture-"+i+"/","movie","Fixture "+i));
    rows[0].getChildAt(1).setOnClickListener(v->clicks.incrementAndGet());
   });
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->assertTrue("Initial poster must accept remote focus",rows[0].getChildAt(0).requestFocus()));
   key(KeyEvent.KEYCODE_DPAD_RIGHT);
   scenario.onActivity(a->{assertTrue("RIGHT selects second poster",rows[0].getChildAt(1).isFocused());assertEquals("https://farsiplex.com/movie/fixture-0/",rows[0].getChildAt(0).getTag());});
   key(KeyEvent.KEYCODE_DPAD_DOWN);scenario.onActivity(a->assertTrue("DOWN keeps column",rows[1].getChildAt(1).isFocused()));
   key(KeyEvent.KEYCODE_DPAD_UP);scenario.onActivity(a->assertTrue("UP returns to same column",rows[0].getChildAt(1).isFocused()));
   key(KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(1,clicks.get());
  }
 }
 @Test public void episodeBackRestoresChosenEpisode(){
  String episode="https://farsiplex.com/episode/fixture-02/";
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{set(a,"destroyed",true);FarsiPlexTitle series=new FarsiPlexTitle("https://farsiplex.com/tvshow/fixture/","series","Fixture");series.episodes.add(new FarsiPlexTitle(episode,"episode","1 - 2"));set(a,"selected",series);invoke(a,"showDetail",new Class[]{FarsiPlexTitle.class,boolean.class},series,true);});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->assertTrue("Episode row must accept focus",((FrameLayout)field(a,"root")).findViewWithTag(episode).requestFocus()));
   key(KeyEvent.KEYCODE_DPAD_CENTER);scenario.onActivity(a->assertEquals(episode,((FarsiPlexTitle)field(a,"selected")).url));
   key(KeyEvent.KEYCODE_BACK);scenario.onActivity(a->assertTrue("Back restores chosen episode",((FrameLayout)field(a,"root")).findViewWithTag(episode).isFocused()));
  }
 }
 @Test public void profileDataAndRealPreferenceRestartAreIsolated(){
  Context c=context();ProfileStore store=new ProfileStore(c);String first=store.active();LibraryStore original=new LibraryStore(c);original.savePlayback("First","https://example.invalid/profile.mp4","https://farsiplex.com/movie/profile/",15000,100000);
  assertTrue(store.edit(null,"Test profile",2));String child=store.list().optJSONObject(store.list().length()-1).optString("id");
  assertTrue(store.edit(child,"Renamed profile",1));assertEquals("Renamed profile",store.list().optJSONObject(store.list().length()-1).optString("name"));store.select(child);
  assertEquals(0,new LibraryStore(c).getResumePosition("","https://farsiplex.com/movie/profile/"));ProfileStore.data(c,"player_preferences").edit().putBoolean("autoplay",false).commit();
  store.select(first);assertEquals(15000,new LibraryStore(c).getResumePosition("","https://farsiplex.com/movie/profile/"));assertTrue(store.delete(child));
  // A process-restart fixture must be durable before the separate shell stops its process.
  assertTrue(ProfileStore.data(c,"namakade_local_library").edit().putBoolean("restart_fixture",true).commit());
  assertTrue(c.getSharedPreferences("family_profiles",0).edit().putBoolean("picker",false).commit());
  System.out.println("RESTART_SEED package="+c.getPackageName()+" data="+c.getApplicationInfo().dataDir+" profile="+store.active()+" position="+new LibraryStore(c).getResumePosition("","https://farsiplex.com/movie/profile/"));
 }
 @Test public void samePackageDefaultMigrationDoesNotEraseLegacyData(){
  Context c=context();ProfileStore store=new ProfileStore(c);store.select("default");c.getSharedPreferences("migration_fixture",0).edit().putString("value","preserved").commit();
  assertEquals("preserved",ProfileStore.data(c,"migration_fixture").getString("value",""));assertEquals("preserved",c.getSharedPreferences("migration_fixture",0).getString("value",""));
 }
 @Test public void historyPersistenceAndConfirmedRemovalArePerItem(){
  LibraryStore library=new LibraryStore(context());library.savePlayback("One","https://example.invalid/history.mp4","https://farsiplex.com/movie/history/",20000,100000);assertEquals(20000,new LibraryStore(context()).getResumePosition("","https://farsiplex.com/movie/history/"));library.forget("https://farsiplex.com/movie/history/","https://example.invalid/history.mp4");assertEquals(0,new LibraryStore(context()).getResumePosition("","https://farsiplex.com/movie/history/"));
 }
 @Test public void continueWatchingParticipatesInRemoteTraversal(){
  LibraryStore library=new LibraryStore(context());String page="https://farsiplex.com/movie/continue-fixture/";
  library.savePlayback("Continue fixture","https://example.invalid/continue.mp4",page,20000,100000);
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{set(a,"destroyed",true);LinearLayout content=(LinearLayout)field(a,"content");View slot=(View)field(a,"continueSlot");for(int i=content.getChildCount()-1;i>content.indexOfChild(slot);i--)content.removeViewAt(i);LinearLayout row=new LinearLayout(a);content.addView(row);invoke(a,"card",new Class[]{LinearLayout.class,FarsiPlexTitle.class},row,new FarsiPlexTitle("https://farsiplex.com/movie/after-resume/","movie","After resume"));});
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();key(KeyEvent.KEYCODE_DPAD_DOWN);key(KeyEvent.KEYCODE_DPAD_DOWN);
   scenario.onActivity(a->assertEquals("resume:"+PlaybackResumePolicy.identity("",page),a.getCurrentFocus().getTag()));
   key(KeyEvent.KEYCODE_DPAD_DOWN);scenario.onActivity(a->assertEquals("https://farsiplex.com/movie/after-resume/",a.getCurrentFocus().getTag()));
   key(KeyEvent.KEYCODE_DPAD_UP);scenario.onActivity(a->assertEquals("resume:"+PlaybackResumePolicy.identity("",page),a.getCurrentFocus().getTag()));
  }finally{library.forget(page,"https://example.invalid/continue.mp4");}
 }
 @Test @androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
 public void publicNativeEpisodeMedia3WhenAvailable()throws Exception{
  String page="https://farsiplex.com/episode/meymoone-mast-ep01/";org.jsoup.nodes.Document doc;
  try{doc=org.jsoup.Jsoup.connect(page).timeout(12000).maxBodySize(1800000).followRedirects(false).get();}catch(Exception e){org.junit.Assume.assumeTrue("Public live page unavailable; no device playback claim",false);return;}
  FarsiPlexPlayback.Result resolved=FarsiPlexPlayback.resolveDetailed(doc);
  org.junit.Assume.assumeTrue("No verified public stream available during emulator test",!resolved.media.isEmpty());
  LibraryStore library=new LibraryStore(context());library.removePlayback(resolved.media,page);
  android.content.SharedPreferences settings=ProfileStore.data(context(),"player_preferences");int oldDelay=settings.getInt("subtitle_delay",0);float oldSize=settings.getFloat("subtitle_size",1f);
  android.content.Intent intent=new android.content.Intent(context(),PlayerActivity.class).putExtra(PlayerActivity.EXTRA_URL,resolved.media).putExtra(PlayerActivity.EXTRA_PAGE_URL,page).putExtra(PlayerActivity.EXTRA_TITLE,"Live public episode check");
  intent.putExtra("candidates",CandidateCodec.encode(java.util.Arrays.asList(new PlaybackCandidate(resolved.media,page,0,true))));
  final boolean[] decoded={false};final int[] frames={0};final String[] error={""};
  try(ActivityScenario<PlayerActivity> scenario=ActivityScenario.launch(intent)){
   for(int n=0;n<60&&!decoded[0]&&error[0].isEmpty();n++){
    Thread.sleep(500);scenario.onActivity(a->{androidx.media3.exoplayer.ExoPlayer player=(androidx.media3.exoplayer.ExoPlayer)field(a,"player");if(player==null)return;if(player.getPlayerError()!=null)error[0]=player.getPlayerError().getErrorCodeName();androidx.media3.exoplayer.DecoderCounters counters=player.getVideoDecoderCounters();if(counters!=null){counters.ensureUpdated();frames[0]=counters.renderedOutputBufferCount;}decoded[0]=frames[0]>0&&player.getCurrentPosition()>1000;});
   }
   if(error[0].contains("IO_"))org.junit.Assume.assumeTrue("Live upstream unavailable during playback: "+error[0],false);
   assertTrue("Actual video output required; player error="+error[0],decoded[0]);
   scenario.onActivity(a->{
    androidx.media3.exoplayer.ExoPlayer player=(androidx.media3.exoplayer.ExoPlayer)field(a,"player");assertNotNull("Audio format must be detected",player.getAudioFormat());
    invoke(a,"togglePlayback",new Class[]{});assertFalse(player.getPlayWhenReady());invoke(a,"seekBy",new Class[]{long.class},10000L);assertTrue(player.getCurrentPosition()>=9000);
    android.app.AlertDialog delay=(android.app.AlertDialog)invoke(a,"subtitleDelay",new Class[]{});assertNotNull(delay.getListView());assertTrue("Delay choices must be visible",delay.getListView().isShown());assertEquals(6,delay.getListView().getCount());delay.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY));assertTrue(player.getPlayWhenReady());delay.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PAUSE));assertFalse(player.getPlayWhenReady());delay.getListView().performItemClick(null,2,2);assertEquals(1000,settings.getInt("subtitle_delay",0));
    android.app.AlertDialog size=(android.app.AlertDialog)invoke(a,"subtitleSize",new Class[]{});assertEquals(4,size.getListView().getCount());size.getListView().performItemClick(null,2,2);assertEquals(1.25f,settings.getFloat("subtitle_size",1f),.001f);
    android.app.AlertDialog audio=(android.app.AlertDialog)invoke(a,"chooseTrack",new Class[]{int.class},androidx.media3.common.C.TRACK_TYPE_AUDIO);assertNotNull(audio);assertTrue(audio.getListView().getCount()>1);audio.getListView().performItemClick(null,1,1);
    android.app.AlertDialog quality=(android.app.AlertDialog)invoke(a,"chooseTrack",new Class[]{int.class},androidx.media3.common.C.TRACK_TYPE_VIDEO);assertNotNull(quality);assertTrue(quality.getListView().getCount()>1);quality.getListView().performItemClick(null,1,1);assertTrue("Track selection must not reset progress",player.getCurrentPosition()>=9000);
    invoke(a,"togglePlayback",new Class[]{});assertTrue(player.getPlayWhenReady());
   });
   System.out.println("EMULATOR_MEDIA3: rendered video frames="+frames[0]+"; audio/video track menus, subtitle size/delay choices, pause/resume and seek exercised. No physical speaker or actual subtitle-track claim.");
  }finally{library.forget(page,resolved.media);settings.edit().putInt("subtitle_delay",oldDelay).putFloat("subtitle_size",oldSize).apply();}
 }
  /** InputDispatcher navigation from bottom rails back to the hero, plus manual slides. */
  @Test public void homeHeroCarouselAndRemoteUpNavigationWork(){
   final LinearLayout[] rails=new LinearLayout[2];
   android.content.Intent offlineUi=new android.content.Intent(context(),MainActivity.class)
       .putExtra(MainActivity.EXTRA_OFFLINE_UI_TEST,true);
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(offlineUi)){
    scenario.onActivity(a->{
     set(a,"destroyed",true); // Prevent network callbacks and timer rotation during this fixture.
     LinearLayout content=(LinearLayout)field(a,"content");
     if(content.getChildCount()>1)content.removeViews(1,content.getChildCount()-1);
     @SuppressWarnings("unchecked") java.util.List<FarsiPlexTitle> movies=(java.util.List<FarsiPlexTitle>)field(a,"loadedMovies");
     @SuppressWarnings("unchecked") java.util.List<FarsiPlexTitle> series=(java.util.List<FarsiPlexTitle>)field(a,"loadedSeries");
     movies.clear();series.clear();
      FarsiPlexTitle first=new FarsiPlexTitle("https://farsiplex.com/movie/fixture-a/","movie","Fixture A");first.poster="test-fixture://poster-a";
      FarsiPlexTitle second=new FarsiPlexTitle("https://farsiplex.com/tvshow/fixture-b/","series","Fixture B");second.poster="test-fixture://poster-b";
     movies.add(first);series.add(second);
     invoke(a,"refreshFeaturedCarousel",new Class[]{});
     for(int n=0;n<2;n++){
      rails[n]=new LinearLayout(a);content.addView(rails[n]);
      invoke(a,"card",new Class[]{LinearLayout.class,FarsiPlexTitle.class},rails[n],
          new FarsiPlexTitle("https://farsiplex.com/movie/rail-"+n+"/","movie","Rail"+n));
     }
     assertTrue(rails[1].getChildAt(0).requestFocus());
    });
    key(KeyEvent.KEYCODE_DPAD_UP);
    scenario.onActivity(a->assertTrue("UP moves to previous poster row",rails[0].getChildAt(0).isFocused()));
    key(KeyEvent.KEYCODE_DPAD_UP);
    scenario.onActivity(a->assertEquals("home-primary",a.getCurrentFocus().getTag()));
    key(KeyEvent.KEYCODE_DPAD_LEFT);
    scenario.onActivity(a->assertEquals("Fixture B",((FarsiPlexTitle)field(a,"featured")).name));
    key(KeyEvent.KEYCODE_DPAD_UP);
    scenario.onActivity(a->assertTrue("UP reaches top navigation",a.getCurrentFocus().getParent()==field(a,"navigationBar")));
   }
  }
  @Test public void homeStartsAndCapturesNativeLayout()throws Exception{
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(2500);
   scenario.onActivity(a->{assertFalse(a.isFinishing());FrameLayout root=(FrameLayout)field(a,"root");assertNotNull(root.findViewWithTag("home-primary"));});
   android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
   java.io.File directory=context().getExternalFilesDir(null);assertNotNull(directory);
   try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(directory,"emulator-home.png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}image.recycle();
  }
 }
}
