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
 @Before public void pickerOff(){context().getSharedPreferences("family_profiles",0).edit().putBoolean("picker",false).commit();}
 private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 private Object field(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){throw new AssertionError(e);}}
 private void set(Object owner,String name,Object value){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}catch(Exception e){throw new AssertionError(e);}}
 private Object invoke(Object owner,String name,Class<?>[] types,Object...args){try{Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}catch(Exception e){throw new AssertionError(e);}}
 private void key(MainActivity a,int code){a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
 @Test public void posterOrderDirectionAndSelectAreStable(){
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{set(a,"destroyed",true);invoke(a,"begin",new Class[]{String.class},"movies");LinearLayout content=(LinearLayout)field(a,"content");LinearLayout first=new LinearLayout(a),second=new LinearLayout(a);content.addView(first);content.addView(second);
    for(int i=0;i<5;i++)invoke(a,"card",new Class[]{LinearLayout.class,FarsiPlexTitle.class},i<3?first:second,new FarsiPlexTitle("https://farsiplex.com/movie/fixture-"+i+"/","movie","Fixture "+i));
    View initial=first.getChildAt(0);initial.requestFocus();key(a,KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue(first.getChildAt(1).isFocused());assertSame(initial,first.getChildAt(0));
    key(a,KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(second.getChildAt(1).isFocused());key(a,KeyEvent.KEYCODE_DPAD_UP);assertTrue(first.getChildAt(1).isFocused());
    AtomicInteger clicks=new AtomicInteger();first.getChildAt(1).setOnClickListener(v->clicks.incrementAndGet());key(a,KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(1,clicks.get());
   });
  }
 }
 @Test public void episodeBackRestoresChosenEpisode(){
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{set(a,"destroyed",true);FarsiPlexTitle series=new FarsiPlexTitle("https://farsiplex.com/tvshow/fixture/","series","Fixture");FarsiPlexTitle ep=new FarsiPlexTitle("https://farsiplex.com/episode/fixture-02/","episode","1 - 2");series.episodes.add(ep);set(a,"selected",series);invoke(a,"showDetail",new Class[]{FarsiPlexTitle.class,boolean.class},series,true);FrameLayout root=(FrameLayout)field(a,"root");View row=root.findViewWithTag(ep.url);row.requestFocus();key(a,KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(ep.url,((FarsiPlexTitle)field(a,"selected")).url);a.onBackPressed();assertTrue(root.findViewWithTag(ep.url).isFocused());});
  }
 }
 @Test public void profileDataAndRealPreferenceRestartAreIsolated(){
  Context c=context();ProfileStore store=new ProfileStore(c);String first=store.active();LibraryStore original=new LibraryStore(c);original.savePlayback("First","https://example.invalid/profile.mp4","https://farsiplex.com/movie/profile/",15000,100000);
  assertTrue(store.edit(null,"Test profile",2));String child=store.list().optJSONObject(store.list().length()-1).optString("id");store.select(child);
  assertEquals(0,new LibraryStore(c).getResumePosition("","https://farsiplex.com/movie/profile/"));ProfileStore.data(c,"player_preferences").edit().putBoolean("autoplay",false).commit();
  store.select(first);assertEquals(15000,new LibraryStore(c).getResumePosition("","https://farsiplex.com/movie/profile/"));assertTrue(store.delete(child));
 }
 @Test public void samePackageDefaultMigrationDoesNotEraseLegacyData(){
  Context c=context();ProfileStore store=new ProfileStore(c);store.select("default");c.getSharedPreferences("migration_fixture",0).edit().putString("value","preserved").commit();
  assertEquals("preserved",ProfileStore.data(c,"migration_fixture").getString("value",""));assertEquals("preserved",c.getSharedPreferences("migration_fixture",0).getString("value",""));
 }
 @Test public void historyPersistenceAndConfirmedRemovalArePerItem(){
  LibraryStore library=new LibraryStore(context());library.savePlayback("One","https://example.invalid/history.mp4","https://farsiplex.com/movie/history/",20000,100000);assertEquals(20000,new LibraryStore(context()).getResumePosition("","https://farsiplex.com/movie/history/"));library.forget("https://farsiplex.com/movie/history/","https://example.invalid/history.mp4");assertEquals(0,new LibraryStore(context()).getResumePosition("","https://farsiplex.com/movie/history/"));
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
