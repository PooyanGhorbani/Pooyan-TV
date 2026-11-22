package com.pooyan.namakadetv;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HlsPlaybackInstrumentedTest {
    @Test public void media3RendersGeneratedHlsAndAdvancesAfterSeek() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        AtomicReference<ExoPlayer> player = new AtomicReference<>();
        AtomicReference<PlaybackException> error = new AtomicReference<>();
        AtomicReference<SurfaceTexture> texture = new AtomicReference<>();
        AtomicReference<Surface> surface = new AtomicReference<>();
        CountDownLatch rendered = new CountDownLatch(1);
        try {
            instrumentation.runOnMainSync(() -> {
                texture.set(new SurfaceTexture(false));
                surface.set(new Surface(texture.get()));
                ExoPlayer exo = new ExoPlayer.Builder(context).build();
                player.set(exo);
                exo.setVideoSurface(surface.get());
                exo.addListener(new Player.Listener() {
                    @Override public void onRenderedFirstFrame() { rendered.countDown(); }
                    @Override public void onPlayerError(PlaybackException failure) {
                        error.set(failure);
                        rendered.countDown();
                    }
                });
                exo.setMediaItem(new MediaItem.Builder().setUri("asset:///pooyan-test/index.m3u8")
                        .setMimeType(MimeTypes.APPLICATION_M3U8).build());
                exo.prepare();
                exo.play();
            });
            assertTrue("Media3 rendered a video frame", rendered.await(25, TimeUnit.SECONDS));
            assertNull(error.get());
            instrumentation.runOnMainSync(() -> player.get().seekTo(3000));
            long[] progress = {0};
            for (int i = 0; i < 30 && progress[0] <= 3300 && error.get() == null; i++) {
                Thread.sleep(200);
                instrumentation.runOnMainSync(() -> progress[0] = player.get().getCurrentPosition());
            }
            assertNull(error.get());
            assertTrue("Playback advanced after seeking", progress[0] > 3300);
        } finally {
            instrumentation.runOnMainSync(() -> {
                if (player.get() != null) player.get().release();
                if (surface.get() != null) surface.get().release();
                if (texture.get() != null) texture.get().release();
            });
        }
    }
}
