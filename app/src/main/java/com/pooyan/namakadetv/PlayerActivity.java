package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dedicated Media3 player: progressive/HLS/DASH, quality/audio/captions/resume/remote. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlayerActivity extends Activity {
    public static final String EXTRA_URL = "video_url";
    public static final String EXTRA_TITLE = "video_title";
    public static final String EXTRA_PAGE_URL = "source_page";
    public static final String EXTRA_USER_AGENT = "user_agent";
    public static final String EXTRA_COOKIE = "stream_cookie"; // Reserved; deliberately not forwarded across redirects.

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressSaver = new Runnable() {
        @Override public void run() { saveProgress(); handler.postDelayed(this, 12000); }
    };
    private LibraryStore library;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView;
    private FrameLayout root;
    private LinearLayout controls, topPanel;
    private TextView titleView, stateView, playButton;
    private String streamUrl, pageUrl, name, userAgent, subtitleUrl;
    private float speed = 1f;
    private long pendingPosition = -1;
    private boolean zoomMode = false;
    private boolean playerErrorShown = false;
    private boolean resumePlayWhenReady = true;

    private final Runnable hideControls = () -> {
        if (controls != null) controls.setVisibility(View.GONE);
        if (topPanel != null) topPanel.setVisibility(View.GONE);
        if (playerView != null) { playerView.setFocusable(true); playerView.requestFocus(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        streamUrl = getIntent().getStringExtra(EXTRA_URL);
        name = getIntent().getStringExtra(EXTRA_TITLE);
        pageUrl = getIntent().getStringExtra(EXTRA_PAGE_URL);
        userAgent = getIntent().getStringExtra(EXTRA_USER_AGENT);
        if (name == null || name.trim().isEmpty()) name = "Pooyan TV Video";
        if (!StreamDetector.isHttpsUrl(streamUrl)) {
            Toast.makeText(this, "آدرس ویدئو باید HTTPS باشد.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        library = new LibraryStore(this);
        pendingPosition = library.getResumePosition(streamUrl);
        initLayout();
        initPlayer();
    }

    private int dp(float value) { return TvUi.dp(this, value); }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void initLayout() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);
        playerView.setShutterBackgroundColor(Color.BLACK);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(28), dp(18), dp(28), dp(12));
        head.setBackground(TvUi.bg(0xE3172235, 12, this));
        topPanel = head;
        titleView = TvUi.text(this, name, 19, TvUi.TEXT, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        head.addView(titleView);
        stateView = TvUi.text(this, "در حال اتصال به منبع ویدئو…", 13, TvUi.MUTED, false);
        head.addView(stateView);
        FrameLayout.LayoutParams headLp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        headLp.setMargins(dp(24), dp(15), dp(24), 0);
        root.addView(head, headLp);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(dp(20), dp(14), dp(20), dp(14));
        controls.setBackground(TvUi.bg(0xED132137, 14, this));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        scroller.addView(controls);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        barLp.setMargins(dp(25), 0, dp(25), dp(25));
        root.addView(scroller, barLp);

        addControl("✕ برگشت", this::finish);
        playButton = addControl("Ⅱ مکث", this::togglePlayback);
        addControl("↶ ۱۰ ثانیه", () -> seekBy(-10000));
        addControl("↷ ۳۰ ثانیه", () -> seekBy(30000));
        addControl("سرعت", this::chooseSpeed);
        addControl("کیفیت", () -> chooseTrack(C.TRACK_TYPE_VIDEO));
        addControl("صدا", () -> chooseTrack(C.TRACK_TYPE_AUDIO));
        addControl("زیرنویس", () -> chooseTrack(C.TRACK_TYPE_TEXT));
        addControl("+ زیرنویس", this::addSubtitle);
        addControl("اندازه تصویر", () -> {
            zoomMode = !zoomMode;
            playerView.setResizeMode(zoomMode ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    : AspectRatioFrameLayout.RESIZE_MODE_FIT);
            toast(zoomMode ? "بزرگنمایی (ممکن است تصویر برش بخورد)" : "نمایش کامل تصویر");
        });
        controls.getChildAt(1).requestFocus();
    }
    private TextView addControl(String label, Runnable run) {
        TextView v = TvUi.focusButton(this, label, false, () -> { run.run(); revealControls(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(9);
        controls.addView(v, lp);
        return v;
    }
    private void initPlayer() {
        if (player != null) return;
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15000).setReadTimeoutMs(25000)
                .setAllowCrossProtocolRedirects(false);
        if (userAgent != null && !userAgent.trim().isEmpty()) http.setUserAgent(userAgent);
        // Only send the page's ORIGIN as Referer; never transmit its private query token.
        // Deliberately omit browser Cookies, which might leak on cross-host redirects.
        Map<String, String> headers = new LinkedHashMap<>();
        if (StreamDetector.isHttpsUrl(pageUrl)) {
            Uri p = Uri.parse(pageUrl);
            String origin = p.getScheme() + "://" + p.getEncodedAuthority() + "/";
            headers.put("Referer", origin);
        }
        http.setDefaultRequestProperties(headers);
        DefaultDataSource.Factory source = new DefaultDataSource.Factory(this, http);
        DefaultMediaSourceFactory mediaSources = new DefaultMediaSourceFactory(source);
        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSources)
                .build();
        playerView.setPlayer(player);
        player.setMediaItem(mediaItem());
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) stateView.setText("در حال بارگذاری ویدئو…");
                else if (state == Player.STATE_READY) stateView.setText("پخش آماده است · جهت چپ/راست: جابه‌جایی ۱۰ ثانیه");
                else if (state == Player.STATE_ENDED) { stateView.setText("پخش تمام شد"); saveProgress(); }
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                if (playButton != null) playButton.setText(playing ? "Ⅱ مکث" : "▶ پخش");
            }
            @Override public void onPlayerError(PlaybackException error) {
                stateView.setText("خطای منبع: " + error.getErrorCodeName());
                if (!playerErrorShown && !isFinishing()) {
                    playerErrorShown = true;
                    new AlertDialog.Builder(PlayerActivity.this)
                            .setTitle("ویدئو با پلیر اختصاصی باز نشد")
                            .setMessage("ممکن است لینک منقضی شده باشد، منبع به کوکی یا DRM نیاز داشته باشد، " +
                                    "یا کدک روی تلویزیون پشتیبانی نشود. در حالت مرورگر می‌توانی پخش را ادامه بدهی.\n" +
                                    error.getErrorCodeName())
                            .setNegativeButton("برگشت به سایت", (d,w) -> finish())
                            .setPositiveButton("تلاش دوباره", (d,w) -> {
                                playerErrorShown = false;
                                player.prepare(); player.play();
                            }).show();
                }
            }
        });
        if (pendingPosition > 0) player.seekTo(pendingPosition);
        player.prepare();
        player.setPlaybackSpeed(speed);
        player.setPlayWhenReady(resumePlayWhenReady);
        handler.removeCallbacks(progressSaver);
        handler.postDelayed(progressSaver, 12000);
        revealControls();
    }
    private MediaItem mediaItem() {
        MediaItem.Builder b = new MediaItem.Builder().setUri(Uri.parse(streamUrl)).setMediaId(streamUrl);
        if (StreamDetector.isHttpsUrl(subtitleUrl)) {
            String path = Uri.parse(subtitleUrl).getPath();
            String mime = path != null && path.toLowerCase(Locale.ROOT).endsWith(".srt")
                    ? MimeTypes.APPLICATION_SUBRIP : MimeTypes.TEXT_VTT;
            MediaItem.SubtitleConfiguration cfg = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(mime).setLanguage("fa").setLabel("زیرنویس انتخابی")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
            b.setSubtitleConfigurations(java.util.Collections.singletonList(cfg));
        }
        return b.build();
    }
    private void togglePlayback() {
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
    }
    private void seekBy(long deltaMs) {
        if (player == null) return;
        long next = Math.max(0, player.getCurrentPosition() + deltaMs);
        long duration = player.getDuration();
        if (duration > 0 && duration != C.TIME_UNSET) next = Math.min(next, duration);
        player.seekTo(next);
        stateView.setText((next / 60000) + ":" + String.format(Locale.US,"%02d",(next/1000)%60));
    }
    private void chooseSpeed() {
        final float[] speeds = {0.75f, 1f, 1.25f, 1.5f, 2f};
        String[] labels = {"0.75×", "1× (معمولی)", "1.25×", "1.5×", "2×"};
        new AlertDialog.Builder(this).setTitle("سرعت پخش")
                .setItems(labels, (d,n) -> {
                    speed = speeds[n]; if (player != null) player.setPlaybackSpeed(speed);
                    toast("سرعت " + labels[n]);
                }).show();
    }
    private static final class TrackChoice {
        final Tracks.Group group;
        final int index;
        TrackChoice(Tracks.Group g, int i) { group = g; index = i; }
    }
    private void chooseTrack(int type) {
        if (player == null || trackSelector == null) return;
        String heading = type == C.TRACK_TYPE_VIDEO ? "کیفیت تصویر" :
                (type == C.TRACK_TYPE_AUDIO ? "زبان صدا" : "زیرنویس");
        List<String> labels = new ArrayList<>();
        List<TrackChoice> choices = new ArrayList<>();
        labels.add(type == C.TRACK_TYPE_TEXT ? "خاموش" : "خودکار");
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != type) continue;
            for (int i=0; i<group.length; i++) {
                if (!group.isTrackSupported(i)) continue;
                Format f = group.getTrackFormat(i);
                String label;
                if (type == C.TRACK_TYPE_VIDEO) {
                    label = (f.height > 0 ? f.height + "p" : "Video") +
                            (f.bitrate > 0 ? " · " + f.bitrate/1000000f + " Mbps" : "");
                } else {
                    label = (f.label != null ? f.label : (f.language != null ? f.language :
                            (type == C.TRACK_TYPE_AUDIO ? "Audio" : "Subtitles")));
                    if (f.channelCount > 0) label += " · " + f.channelCount + "ch";
                }
                if (group.isTrackSelected(i)) label += " ✓";
                labels.add(label);
                choices.add(new TrackChoice(group, i));
            }
        }
        if (labels.size() == 1 && type != C.TRACK_TYPE_TEXT) {
            toast("این منبع فقط انتخاب خودکار دارد."); return;
        }
        new AlertDialog.Builder(this).setTitle(heading)
                .setItems(labels.toArray(new String[0]), (d,n) -> {
                    DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                            .clearOverridesOfType(type);
                    if (n == 0) {
                        builder.setTrackTypeDisabled(type, type == C.TRACK_TYPE_TEXT);
                    } else {
                        TrackChoice choice = choices.get(n-1);
                        builder.setTrackTypeDisabled(type, false)
                                .setOverrideForType(new TrackSelectionOverride(
                                        choice.group.getMediaTrackGroup(), choice.index));
                    }
                    trackSelector.setParameters(builder);
                }).show();
    }
    private void addSubtitle() {
        EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setHint("https://example.com/subtitles.vtt");
        url.setPadding(dp(16), dp(16), dp(16), dp(16));
        new AlertDialog.Builder(this).setTitle("زیرنویس مستقیم HTTPS (SRT / VTT)")
                .setView(url).setNegativeButton("انصراف", null)
                .setPositiveButton("افزودن", (d,n) -> {
                    String value = url.getText().toString().trim();
                    if (!StreamDetector.isHttpsUrl(value)) {
                        toast("آدرس HTTPS معتبر وارد کن."); return;
                    }
                    String path = Uri.parse(value).getPath();
                    if (path == null || !(path.toLowerCase(Locale.ROOT).endsWith(".srt") ||
                            path.toLowerCase(Locale.ROOT).endsWith(".vtt"))) {
                        toast("فقط SRT یا VTT پذیرفته می‌شود."); return;
                    }
                    subtitleUrl = value;
                    long position = player != null ? player.getCurrentPosition() : 0;
                    if (player != null) {
                        player.setMediaItem(mediaItem());
                        player.seekTo(position);
                        player.prepare(); player.play();
                    }
                    toast("زیرنویس اضافه شد؛ از منوی زیرنویس انتخاب کن.");
                }).show();
    }
    private void revealControls() {
        if (controls == null) return;
        controls.setVisibility(View.VISIBLE);
        if (topPanel != null) topPanel.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideControls);
        handler.postDelayed(hideControls, 9000);
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private void saveProgress() {
        if (library == null || player == null) return;
        long duration = player.getDuration();
        long position = player.getCurrentPosition();
        library.savePlayback(name, streamUrl, pageUrl, Math.max(0, position),
                duration > 0 && duration != C.TIME_UNSET ? duration : 0);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            int key = e.getKeyCode();
            if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || key == KeyEvent.KEYCODE_MEDIA_PLAY ||
                    key == KeyEvent.KEYCODE_MEDIA_PAUSE || key == KeyEvent.KEYCODE_SPACE) {
                if (e.getRepeatCount() == 0 && player != null) {
                    if (key == KeyEvent.KEYCODE_MEDIA_PLAY) player.play();
                    else if (key == KeyEvent.KEYCODE_MEDIA_PAUSE) player.pause();
                    else togglePlayback();
                }
                revealControls(); return true;
            }
            if (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || key == KeyEvent.KEYCODE_MEDIA_REWIND) {
                seekBy(key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ? 30000 : -10000);
                revealControls(); return true;
            }
            if (key == KeyEvent.KEYCODE_MENU || key == KeyEvent.KEYCODE_SETTINGS ||
                    key == KeyEvent.KEYCODE_PROG_YELLOW) {
                revealControls();
                if (controls != null && controls.getChildCount() > 0) controls.getChildAt(1).requestFocus();
                return true;
            }
            if (controls != null && controls.isShown() &&
                    (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT ||
                     key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN)) {
                handler.removeCallbacks(hideControls);
                handler.postDelayed(hideControls, 9000);
            }
            if (controls != null && !controls.isShown()) {
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    seekBy(key == KeyEvent.KEYCODE_DPAD_LEFT ? -10000 : 10000);
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                    revealControls(); controls.getChildAt(1).requestFocus(); return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }
    @Override protected void onStart() {
        super.onStart();
        if (player == null && StreamDetector.isHttpsUrl(streamUrl) && library != null) {
            // Recreate the native player at the position captured onStop, not the
            // stale resume position read once when the Activity was created.
            initPlayer();
        }
    }
    @Override protected void onStop() {
        handler.removeCallbacks(progressSaver);
        handler.removeCallbacks(hideControls);
        if (player != null) {
            resumePlayWhenReady = player.getPlayWhenReady();
            long duration = player.getDuration();
            pendingPosition = PlaybackResumePolicy.resumeAfterBackground(
                    player.getCurrentPosition(), duration,
                    player.getPlaybackState() == Player.STATE_ENDED);
            saveProgress();
            player.release();
            player = null;
        }
        if (playerView != null) playerView.setPlayer(null);
        trackSelector = null;
        super.onStop();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
