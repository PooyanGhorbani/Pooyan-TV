package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.webkit.WebView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.Player;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/** Opt-in physical-device checks. Uses only public sample video and adds no production entry points. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class TvDeviceTest {
    private final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
    private static final String SAMPLE = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private Object get(Object target, String field) throws Exception {
        Field f = target.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(target);
    }
    private void call(Object target, String method) {
        try { Method m=target.getClass().getDeclaredMethod(method); m.setAccessible(true); m.invoke(target); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private Activity launch(Class<?> cls, Intent intent) {
        intent.setClass(inst.getTargetContext(),cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return inst.startActivitySync(intent);
    }
    private void ui(Runnable task) { inst.runOnMainSync(task); inst.waitForIdleSync(); }
    private String js(WebView web, String script) throws Exception {
        AtomicReference<String> result=new AtomicReference<>();
        ui(() -> web.evaluateJavascript(script,result::set));
        long until=SystemClock.uptimeMillis()+5000;
        while(result.get()==null && SystemClock.uptimeMillis()<until) SystemClock.sleep(100);
        assertNotNull("WebView JS response",result.get()); return result.get();
    }
    @Test public void homeRemoteFavoritesAndSearch() throws Exception {
        Activity a=launch(MainActivity.class,new Intent());
        try {
            AtomicReference<android.view.View> before=new AtomicReference<>();
            ui(() -> before.set(a.getCurrentFocus()));
            assertNotNull("Home has remote focus",before.get());
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
            ui(() -> assertNotSame("D-pad moves focus",before.get(),a.getCurrentFocus()));
            LibraryStore library=(LibraryStore)get(a,"library");
            String url="https://parsimondo.de/fa/?pooyan_device_test=1";
            boolean existed=library.hasBookmark(url);
            ui(() -> library.addBookmark("آزمون فیلم",url));
            assertTrue(library.hasBookmark(url));
            assertTrue(SearchEngine.matches("فيلم", "آزمون فیلم"));
            assertTrue(library.bookmarks().stream().anyMatch(e -> SearchEngine.matches("آزمون",e.title)));
            if(!existed) ui(() -> library.removeBookmark(url));
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            assertFalse(a.isFinishing());
        } finally { ui(a::finish); }
    }
    @Test public void nativePlaybackRemoteAndSavedProgress() throws Exception {
        Intent intent=new Intent().putExtra(PlayerActivity.EXTRA_URL,SAMPLE).putExtra(PlayerActivity.EXTRA_TITLE,"Big Buck Bunny · device test");
        Activity a=launch(PlayerActivity.class,intent);
        try {
            ExoPlayer p=(ExoPlayer)get(a,"player");
            long until=SystemClock.uptimeMillis()+60000;
            AtomicReference<Boolean> playing=new AtomicReference<>(false);
            while(!playing.get() && SystemClock.uptimeMillis()<until) {
                ui(() -> playing.set(p.isPlaying())); SystemClock.sleep(250);
            }
            ui(() -> assertNull("No Media3 source error",p.getPlayerError()));
            assertTrue("Public sample actually plays",playing.get());
            AtomicReference<Long> first=new AtomicReference<>(),later=new AtomicReference<>();
            ui(() -> first.set(p.getCurrentPosition())); SystemClock.sleep(2500);
            ui(() -> later.set(p.getCurrentPosition())); assertTrue(later.get()>first.get()+1000);
            ui(() -> assertTrue("Video frames rendered",p.getVideoDecoderCounters()!=null && p.getVideoDecoderCounters().renderedOutputBufferCount>0));
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PAUSE);
            ui(() -> assertFalse(p.getPlayWhenReady()));
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY);
            ui(() -> assertTrue(p.getPlayWhenReady()));
            ui(() -> p.seekTo(60000)); SystemClock.sleep(1500);
            ui(() -> call(a,"saveProgress"));
            LibraryStore store=new LibraryStore(inst.getTargetContext());
            assertTrue("Progress saved",store.getResumePosition(SAMPLE)>=59000);
            ui(a::finish);
            Activity resumed=launch(PlayerActivity.class,intent);
            try { ui(() -> { try { assertTrue(((ExoPlayer)get(resumed,"player")).getCurrentPosition()>=59000); } catch(Exception e){throw new RuntimeException(e);} }); }
            finally { ui(resumed::finish); }
        } finally { if(!a.isFinishing()) ui(a::finish); }
    }

    @Test public void sevenWebsitesOnTv() throws Exception {
        Activity a=launch(MainActivity.class,new Intent());
        try {
            WebView web=(WebView)get(a,"web");
            for(SiteCatalog.Site site:SiteCatalog.all()) {
                ui(() -> {try {Method m=a.getClass().getDeclaredMethod("openSite",SiteCatalog.Site.class);m.setAccessible(true);m.invoke(a,site);}catch(Exception e){throw new RuntimeException(e);} });
                SystemClock.sleep(1000);
                long until=SystemClock.uptimeMillis()+18000;
                String ready="";
                while(SystemClock.uptimeMillis()<until) {
                    ready=js(web,"document.readyState"); if(ready.equals("\"complete\""))break; SystemClock.sleep(500);
                }
                String status=js(web,"JSON.stringify({ready:document.readyState,title:document.title,error:document.body ? /ERR_|webpage.*not available|سایت.*دسترس نیست/i.test(document.body.innerText) : true})");
                android.os.Bundle result=new android.os.Bundle();
                result.putString("stream","SITE "+site.id+" "+status+"\n"); inst.sendStatus(0,result);
                assertFalse("Activity survives "+site.id,a.isFinishing());
            }
        } finally {ui(a::finish);}
    }

    @Test public void webVideoAndFullscreenFallback() throws Exception {
        Activity a=launch(MainActivity.class,new Intent());
        try {
            WebView web=(WebView)get(a,"web");
            ui(() -> { call(a,"openWeb"); web.loadDataWithBaseURL("https://storage.googleapis.com/", "<html><body style='margin:0;background:black'><video id='v' controls autoplay playsinline width='100%' src='"+SAMPLE+"'></video></body></html>","text/html","UTF-8",null); });
            long until=SystemClock.uptimeMillis()+60000;
            String time="0";
            while(SystemClock.uptimeMillis()<until){time=js(web,"document.getElementById('v') ? document.getElementById('v').currentTime : 0"); if(Double.parseDouble(time)>1)break; SystemClock.sleep(500);}
            assertTrue("HTML5 video actually advances",Double.parseDouble(time)>1);
            js(web,"document.getElementById('v').setAttribute('onclick',\"this.requestFullscreen()\"); true");
            ui(() -> { try {Method m=a.getClass().getDeclaredMethod("tapWeb",float.class,float.class);m.setAccessible(true);m.invoke(a,300f,200f);}catch(Exception e){throw new RuntimeException(e);} });
            SystemClock.sleep(1500);
            ui(() -> {try {assertNotNull("Web fullscreen custom view",get(a,"fullscreenVideo"));}catch(Exception e){throw new RuntimeException(e);} });
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            ui(() -> {try {assertNull("Back exits fullscreen",get(a,"fullscreenVideo"));}catch(Exception e){throw new RuntimeException(e);} });
        } finally {ui(a::finish);}
    }
}
