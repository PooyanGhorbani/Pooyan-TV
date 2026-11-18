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
import android.widget.SeekBar;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.content.Intent;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.ui.SubtitleView;

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
    private SharedPreferences profilePrefs;
    private final java.util.concurrent.ExecutorService verifier=java.util.concurrent.Executors.newSingleThreadExecutor();
    private List<PlaybackCandidate> candidates=new ArrayList<>();
    private final SourceHealth.Session failoverSession=new SourceHealth.Session();
    private PlaybackCandidate currentCandidate;private boolean switching,stopping,nextOffered,nextLoading;
    private long compatibleDuration;private int playbackGeneration;
    private DefaultHttpDataSource.Factory httpFactory;
    private SubtitleView captions;private final CueDelayQueue<List<Cue>> cueQueue=new CueDelayQueue<>();
    private final Runnable cueTicker=new Runnable(){public void run(){if(player!=null){List<Cue> cues=cueQueue.poll(player.getCurrentPosition());if(cues!=null)captions.setCues(cues);}handler.postDelayed(this,100);}};
    private List<FarsiPlexTitle> episodeContext=new ArrayList<>();private FarsiPlexTitle nextEpisode;
    private UnifiedRepository nextRepository;private CatalogImages nextImages;private AlertDialog nextDialog;
    private EpisodeOrder.Countdown nextCountdown;
    private final Runnable nextTicker=new Runnable(){public void run(){if(stopping||nextDialog==null||!nextDialog.isShowing()||nextCountdown==null)return;boolean advance=player!=null&&EpisodeOrder.advancing(player.isPlaying(),player.getPlaybackState()==Player.STATE_ENDED,player.getPlayWhenReady());if(nextCountdown.tick(advance,!stopping)){playNextEpisode();return;}nextDialog.setTitle("قسمت بعد · "+nextCountdown.remaining()+" ثانیه"+(advance?"":" · مکث"));handler.postDelayed(this,1000);}};
    private LibraryStore library;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView;
    private FrameLayout root;
    private LinearLayout controls, topPanel,timelinePanel;
    private SeekBar seekBar;
    private TextView timelineText;
    private boolean scrubbing,resumePrompt;
    private int revealKey=-1;
    private final Runnable timelineTick=new Runnable(){public void run(){updateTimeline();handler.postDelayed(this,500);}};
    private TextView titleView, stateView, playButton;
    private String streamUrl, pageUrl, name, userAgent, subtitleUrl;
    private float speed = 1f;
    private long pendingPosition = -1;
    private boolean zoomMode = false;
    private boolean playerErrorShown = false;
    private boolean resumePlayWhenReady = true;

    private final Runnable hideControls = () -> {
        if(player==null||!player.isPlaying()||scrubbing)return;
        if (timelinePanel != null) timelinePanel.setVisibility(View.GONE);
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
        profilePrefs=ProfileStore.data(this,"player_preferences");
        library = new LibraryStore(this);
        candidates=CandidateCodec.decode(getIntent().getStringExtra("candidates"));
        if(candidates.isEmpty())candidates.add(new PlaybackCandidate(streamUrl,pageUrl,0,false));
        currentCandidate=failoverSession.next(candidates,android.os.SystemClock.elapsedRealtime());
        if(currentCandidate!=null)streamUrl=currentCandidate.url;
        episodeContext=EpisodeOrder.decode(getIntent().getStringExtra("episodes"));
        nextEpisode=EpisodeOrder.next(episodeContext,pageUrl);
        nextRepository=new UnifiedRepository(this);nextImages=new CatalogImages(this);
        pendingPosition = state==null?library.getResumePosition(streamUrl,pageUrl):state.getLong("position",0);
        if(state!=null)resumePlayWhenReady=state.getBoolean("playing",true);
        resumePrompt=state==null?pendingPosition>0:state.getBoolean("prompt",false);
        if(resumePrompt)resumePlayWhenReady=false;
        initLayout();
        if(currentCandidate==null)exhausted(SourceHealth.Failure.NETWORK);else initPlayer();
        if(resumePrompt&&currentCandidate!=null)new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("ادامهٔ تماشا")
            .setMessage("ادامه از "+PlaybackResumePolicy.time(pendingPosition)+" یا تماشا از ابتدا؟")
            .setPositiveButton("ادامه",(d,w)->startChoice(false))
            .setNegativeButton("از ابتدا",(d,w)->startChoice(true))
            .setOnCancelListener(d->finish()).show();
    }
    private void startChoice(boolean restart){
        resumePrompt=false;resumePlayWhenReady=true;
        if(restart){pendingPosition=0;library.removePlayback(streamUrl,pageUrl);if(player!=null)player.seekTo(0);}
        if(player!=null)player.play();revealControls();
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
        captions=new SubtitleView(this);captions.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);captions.setBottomPaddingFraction(profilePrefs.getFloat("subtitle_position",.14f));
        captions.setFractionalTextSize(.0533f*profilePrefs.getFloat("subtitle_size",1f));applySubtitleStyle(profilePrefs.getInt("subtitle_style",0));root.addView(captions,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(28), dp(18), dp(28), dp(12));
        head.setBackground(TvUi.bg(0xCC101016, 18, this));
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
        controls.setBackground(TvUi.bg(0xE6101016, 24, this));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(true);
        scroller.addView(controls);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        barLp.setMargins(dp(25), 0, dp(25), dp(25));
        timelinePanel=new LinearLayout(this);timelinePanel.setOrientation(LinearLayout.VERTICAL);
        timelinePanel.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        timelinePanel.setPadding(dp(16),dp(10),dp(16),dp(8));
        timelinePanel.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xB308080B,0xFA08080B}));
        timelineText=TvUi.text(this,"00:00 / --:--    باقی‌مانده: --:--",14,Color.WHITE,false);
        timelineText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);timelinePanel.addView(timelineText);
        seekBar=new SeekBar(this);seekBar.setMax(10000);seekBar.setKeyProgressIncrement(100);
        seekBar.setProgressTintList(ColorStateList.valueOf(0xFFE50914));seekBar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF55555C));
        seekBar.setContentDescription("زمان پخش؛ چپ و راست برای جابه‌جایی ده ثانیه");
        timelinePanel.addView(seekBar,new LinearLayout.LayoutParams(-1,dp(42)));
        seekBar.setOnKeyListener((v,k,e)->{
            if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){if(e.getAction()==KeyEvent.ACTION_DOWN){seekBy(k==KeyEvent.KEYCODE_DPAD_LEFT?-10000:10000);revealControls();}return true;}
            if(k==KeyEvent.KEYCODE_DPAD_DOWN){if(e.getAction()==KeyEvent.ACTION_DOWN)playButton.requestFocus();return true;}
            if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_UP)togglePlayback();return true;}
            return false;
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){if(user&&player!=null&&canSeek()){player.seekTo((long)(player.getDuration()*(value/10000d)));updateTimeline();}}
            public void onStartTrackingTouch(SeekBar b){scrubbing=true;handler.removeCallbacks(hideControls);}
            public void onStopTrackingTouch(SeekBar b){scrubbing=false;saveProgress();revealControls();}
        });
        timelinePanel.addView(scroller);root.addView(timelinePanel,barLp);

        addControl("✕ برگشت", this::finish);
        playButton = addControl("Ⅱ مکث", this::togglePlayback);
        addControl("↶ ۱۰ ثانیه", () -> seekBy(-10000));
        addControl("↷ ۳۰ ثانیه", () -> seekBy(30000));
        addControl("⏭ پرش دستی ۹۰ ثانیه", () -> seekBy(90000));
        addControl("سرعت", this::chooseSpeed);
        addControl("کیفیت", () -> chooseTrack(C.TRACK_TYPE_VIDEO));
        addControl("صدا", () -> chooseTrack(C.TRACK_TYPE_AUDIO));
        addControl("زیرنویس", () -> chooseTrack(C.TRACK_TYPE_TEXT));
        addControl("+ زیرنویس", this::addSubtitle);
        addControl("اندازهٔ زیرنویس",this::subtitleSize);
        addControl("تأخیر زیرنویس",this::subtitleDelay);
        addControl("جای زیرنویس",this::subtitlePosition);
        addControl("سبک زیرنویس",this::subtitleStyle);
        addControl("قسمت بعد",()->showNext(true));
        addControl("اندازه تصویر", () -> {
            zoomMode = !zoomMode;
            playerView.setResizeMode(zoomMode ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    : AspectRatioFrameLayout.RESIZE_MODE_FIT);
            toast(zoomMode ? "بزرگنمایی (ممکن است تصویر برش بخورد)" : "نمایش کامل تصویر");
        });
        for(int i=0;i<controls.getChildCount();i++){
            final View v=controls.getChildAt(i);
            v.setOnKeyListener((view,key,event)->{
                if(TvUi.activate(view,key,event))return true;
                if(event.getAction()==KeyEvent.ACTION_DOWN){
                    revealControls();
                    if(key==KeyEvent.KEYCODE_DPAD_UP){if(seekBar.isEnabled())seekBar.requestFocus();return true;}
                    if(key==KeyEvent.KEYCODE_DPAD_DOWN)return true;
                }return false;
            });
        }
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
        String reference=currentCandidate!=null&&StreamDetector.isHttpsUrl(currentCandidate.page)?currentCandidate.page:pageUrl;
        if (StreamDetector.isHttpsUrl(reference)) {
            Uri p = Uri.parse(reference);
            String origin = p.getScheme() + "://" + p.getEncodedAuthority() + "/";
            headers.put("Referer", origin);
        }
        http.setDefaultRequestProperties(headers);
        httpFactory=http;
        DefaultDataSource.Factory source = new DefaultDataSource.Factory(this, http);
        DefaultMediaSourceFactory mediaSources = new DefaultMediaSourceFactory(source);
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredAudioLanguage(profilePrefs.getString("audio","fa")).setPreferredTextLanguage(profilePrefs.getString("text","fa")).setTrackTypeDisabled(C.TRACK_TYPE_TEXT,profilePrefs.getBoolean("subtitle_off",false)));
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSources)
                .build();
        playerView.setPlayer(player);
        if(playerView.getSubtitleView()!=null)playerView.getSubtitleView().setVisibility(View.GONE);
        player.setMediaItem(mediaItem());
        player.addListener(new Player.Listener() {
            @Override public void onCues(CueGroup group){if(player==null||stopping)return;int delay=profilePrefs.getInt("subtitle_delay",0);if(delay==0)captions.setCues(group.cues);else cueQueue.add(player.getCurrentPosition(),delay,group.cues);}
            @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition,Player.PositionInfo newPosition,int reason){cueQueue.clear();captions.setCues(java.util.Collections.emptyList());}
            @Override public void onEvents(Player p,Player.Events events){updateTimeline();maybeNext();}
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) stateView.setText("در حال بارگذاری ویدئو…");
                else if (state == Player.STATE_READY) {
                    long duration=player.getDuration();
                    if(compatibleDuration>0&&duration>0&&Math.abs(duration-compatibleDuration)>Math.max(60000,compatibleDuration/20)){failover(SourceHealth.Failure.INCOMPATIBLE);return;}
                    if(duration>0)compatibleDuration=duration;
                    stateView.setText("پخش آماده است · جهت چپ/راست: جابه‌جایی ۱۰ ثانیه");
                }
                else if (state == Player.STATE_ENDED) { stateView.setText("پخش تمام شد"); saveProgress(); }
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                if (playButton != null) playButton.setText(playing ? "Ⅱ مکث" : "▶ پخش");
            }
            @Override public void onPlayerError(PlaybackException error) {
                stateView.setText("خطای منبع: " + error.getErrorCodeName());
                String code=error.getErrorCodeName();
                SourceHealth.Failure why=code.contains("DECOD")?SourceHealth.Failure.DECODING:code.contains("TIMEOUT")?SourceHealth.Failure.TIMEOUT:code.contains("PARSING")?SourceHealth.Failure.PARSING:code.contains("FORMAT")?SourceHealth.Failure.UNSUPPORTED:SourceHealth.Failure.NETWORK;
                Throwable cause=error;
                while(cause!=null){if(cause instanceof androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException){int status=((androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)cause).responseCode;why=status==410?SourceHealth.Failure.EXPIRED:status==401||status==402||status==403?SourceHealth.Failure.ACCESS:SourceHealth.Failure.HTTP;break;}cause=cause.getCause();}
                failover(why);
            }
        });
        if (pendingPosition > 0) player.seekTo(pendingPosition);
        player.prepare();
        player.setPlaybackSpeed(speed);
        player.setPlayWhenReady(resumePlayWhenReady);
        handler.removeCallbacks(progressSaver);
        handler.postDelayed(progressSaver, 12000);
        handler.removeCallbacks(timelineTick);handler.post(timelineTick);
        handler.removeCallbacks(cueTicker);handler.post(cueTicker);
        revealControls();
    }
    private MediaItem mediaItem() {
        MediaItem.Builder b = new MediaItem.Builder().setUri(Uri.parse(streamUrl)).setMediaId(PlaybackResumePolicy.identity(streamUrl,pageUrl));
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
        if(player.getPlaybackState()==Player.STATE_ENDED){cancelNext();nextOffered=false;player.seekTo(0);player.play();return;}
        if (player.getPlayWhenReady()) player.pause(); else player.play();
    }
    private boolean canSeek(){return player!=null&&PlaybackResumePolicy.canSeek(player.getDuration(),player.isCurrentMediaItemLive(),player.isCurrentMediaItemSeekable());}
    private void updateTimeline(){
        if(player==null||seekBar==null)return;
        long position=Math.max(0,player.getCurrentPosition()),duration=player.getDuration();
        boolean finite=duration>0&&duration!=C.TIME_UNSET&&!player.isCurrentMediaItemLive();
        timelineText.setText(PlaybackResumePolicy.time(position)+" / "+(finite?PlaybackResumePolicy.time(duration):player.isCurrentMediaItemLive()?"زنده":"--:--")+"    باقی‌مانده: "+(finite?PlaybackResumePolicy.time(Math.max(0,duration-position)):"--:--"));
        boolean enabled=canSeek();
        if(!enabled&&seekBar.isFocused())playButton.requestFocus();
        seekBar.setEnabled(enabled);seekBar.setFocusable(enabled);
        if(!scrubbing)seekBar.setProgress(finite?(int)Math.min(10000,10000d*position/duration):0);
    }
    private void seekBy(long deltaMs){
        if(!canSeek())return;
        player.seekTo(PlaybackResumePolicy.seek(player.getCurrentPosition(),deltaMs,player.getDuration()));
        updateTimeline();saveProgress();
    }
    private void chooseSpeed() {
        final float[] speeds = {0.75f, 1f, 1.25f, 1.5f, 2f};
        String[] labels = {"0.75×", "1× (معمولی)", "1.25×", "1.5×", "2×"};
        new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("سرعت پخش")
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
    private AlertDialog chooseTrack(int type) {
        if (player == null || trackSelector == null) return null;
        String heading = type == C.TRACK_TYPE_VIDEO ? "کیفیت تصویر" :
                (type == C.TRACK_TYPE_AUDIO ? "زبان صدا" : "زیرنویس");
        List<String> labels = new ArrayList<>();
        List<TrackChoice> choices = new ArrayList<>();
        labels.add(type == C.TRACK_TYPE_TEXT ? "خاموش" : "خودکار");
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != type) continue;
            for (int i=0; i<group.length; i++) {
                if (!TrackPolicy.selectable(group.isTrackSupported(i),i,group.length)) continue;
                Format f = group.getTrackFormat(i);
                String label;
                if (type == C.TRACK_TYPE_VIDEO) {
                    label=TrackPolicy.video(f.height,f.bitrate);
                } else {
                    label=TrackPolicy.language(f.language,f.label);
                    if (f.channelCount > 0) label += " · " + f.channelCount + "ch";
                }
                if (group.isTrackSelected(i)) label += " ✓";
                labels.add(label);
                choices.add(new TrackChoice(group, i));
            }
        }
        if(labels.size()==1&&type==C.TRACK_TYPE_TEXT){toast("این رسانه هنوز زیرنویس قابل انتخاب ارائه نکرده است.");return null;}
        if (labels.size() == 1 && type != C.TRACK_TYPE_TEXT) {
            toast("این منبع فقط انتخاب خودکار دارد."); return null;
        }
        return new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle(heading)
                .setItems(labels.toArray(new String[0]), (d,n) -> {
                    DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                            .clearOverridesOfType(type);
                    if (n == 0) {
                        builder.setTrackTypeDisabled(type, type == C.TRACK_TYPE_TEXT);
                        if(type==C.TRACK_TYPE_TEXT){profilePrefs.edit().putBoolean("subtitle_off",true).apply();cueQueue.clear();captions.setCues(java.util.Collections.emptyList());}
                    } else {
                        TrackChoice choice = choices.get(n-1);
                        Format selected=choice.group.getTrackFormat(choice.index);
                        if(type==C.TRACK_TYPE_TEXT)profilePrefs.edit().putBoolean("subtitle_off",false).putString("text",selected.language==null?"fa":selected.language).apply();
                        if(type==C.TRACK_TYPE_AUDIO&&selected.language!=null)profilePrefs.edit().putString("audio",selected.language).apply();
                        if(type==C.TRACK_TYPE_AUDIO&&selected.language!=null)builder.setPreferredAudioLanguage(selected.language);
                        if(type==C.TRACK_TYPE_TEXT&&selected.language!=null)builder.setPreferredTextLanguage(selected.language);
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
        new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("زیرنویس مستقیم HTTPS (SRT / VTT)")
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
        boolean hidden=!timelinePanel.isShown();
        timelinePanel.setVisibility(View.VISIBLE);
        if(hidden&&playButton!=null)playButton.requestFocus();
        if (topPanel != null) topPanel.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideControls);
        handler.postDelayed(hideControls, 9000);
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private void saveProgress() {
        if (library == null || player == null || resumePrompt || player.isCurrentMediaItemLive()) return;
        long duration = player.getDuration();
        long position = player.getCurrentPosition();
        if(player.getPlaybackState()==Player.STATE_ENDED){library.complete(name,streamUrl,pageUrl,duration>0?duration:0);return;}
        if(position<=0&&player.getPlaybackState()==Player.STATE_READY){library.removePlayback(streamUrl,pageUrl);return;}
        library.savePlayback(name, streamUrl, pageUrl, Math.max(0, position),
                duration > 0 && duration != C.TIME_UNSET ? duration : 0);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if(e.getAction()==KeyEvent.ACTION_UP&&(e.getKeyCode()==revealKey||e.getKeyCode()==KeyEvent.KEYCODE_BACK)){revealKey=-1;return true;}
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            int key = e.getKeyCode();
            if(key==KeyEvent.KEYCODE_MEDIA_STOP){nextOffered=true;cancelNext();if(player!=null)player.pause();saveProgress();finish();return true;}
            if(key==KeyEvent.KEYCODE_BACK){
                if(e.getRepeatCount()>0)return true;
                if(RemoteInputPolicy.backHides(timelinePanel!=null&&timelinePanel.isShown(),player!=null&&player.isPlaying())){
                    handler.removeCallbacks(hideControls);hideControls.run();return true;
                }
                finish();return true;
            }
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
                    revealControls();seekBar.requestFocus();return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER ||
                        key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    revealKey=key;revealControls(); controls.getChildAt(1).requestFocus(); return true;
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }
    @Override protected void onStart() {
        super.onStart();stopping=false;
        if (player == null && StreamDetector.isHttpsUrl(streamUrl) && library != null && currentCandidate!=null) {
            // Recreate the native player at the position captured onStop, not the
            // stale resume position read once when the Activity was created.
            initPlayer();
        }
    }
    @Override protected void onStop() {
        stopping=true;playbackGeneration++;switching=false;nextLoading=false;cancelNext();handler.removeCallbacks(cueTicker);cueQueue.clear();
        handler.removeCallbacks(progressSaver);
        handler.removeCallbacks(timelineTick);
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

    private boolean dialogMediaKey(android.content.DialogInterface dialog,int key,KeyEvent event){
        boolean media=key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE||key==KeyEvent.KEYCODE_MEDIA_PLAY||key==KeyEvent.KEYCODE_MEDIA_PAUSE||key==KeyEvent.KEYCODE_MEDIA_STOP||key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD||key==KeyEvent.KEYCODE_MEDIA_REWIND;
        if(!media)return false;
        if(event.getAction()!=KeyEvent.ACTION_DOWN)return true;
        if((resumePrompt||playerErrorShown)&&key!=KeyEvent.KEYCODE_MEDIA_STOP)return true;
        if(key==KeyEvent.KEYCODE_MEDIA_STOP)dialog.dismiss();
        return dispatchKeyEvent(event);
    }
    private AlertDialog subtitleSize(){return new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("اندازهٔ زیرنویس این پروفایل").setItems(new String[]{"کوچک","معمولی","بزرگ","خیلی بزرگ"},(d,i)->{float value=new float[]{.8f,1f,1.25f,1.5f}[i];profilePrefs.edit().putFloat("subtitle_size",value).apply();captions.setFractionalTextSize(.0533f*value);}).show();}
    private AlertDialog subtitleDelay(){return new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("تأخیر مثبت؛ جلو انداختن پشتیبانی نمی‌شود").setItems(new String[]{"بدون تأخیر","نیم ثانیه","۱ ثانیه","۲ ثانیه","۳ ثانیه","۵ ثانیه"},(d,i)->{profilePrefs.edit().putInt("subtitle_delay",new int[]{0,500,1000,2000,3000,5000}[i]).apply();cueQueue.clear();captions.setCues(java.util.Collections.emptyList());}).show();}
    private AlertDialog subtitlePosition(){return new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey)
        .setTitle("موقعیت زیرنویس").setItems(new String[]{"پایین","معمولی","بالا"},(d,i)->{
            float value=new float[]{.05f,.14f,.26f}[i];profilePrefs.edit().putFloat("subtitle_position",value).apply();captions.setBottomPaddingFraction(value);
        }).show();}
    private void applySubtitleStyle(int style){
        if(captions==null)return;
        if(style==0){captions.setApplyEmbeddedStyles(true);captions.setStyle(androidx.media3.ui.CaptionStyleCompat.DEFAULT);return;}
        captions.setApplyEmbeddedStyles(false);
        int edge=style==1?androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE:androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_NONE;
        int back=style==2?0xB9000000:android.graphics.Color.TRANSPARENT;
        captions.setStyle(new androidx.media3.ui.CaptionStyleCompat(android.graphics.Color.WHITE,back,
            android.graphics.Color.TRANSPARENT,edge,android.graphics.Color.BLACK,null));
    }
    private AlertDialog subtitleStyle(){return new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey)
        .setTitle("شکل زیرنویس").setItems(new String[]{"حالت اصلی فیلم","سفید با خط دور مشکی","سفید با زمینهٔ تیره"},(d,i)->{
            profilePrefs.edit().putInt("subtitle_style",i).apply();applySubtitleStyle(i);
        }).show();}
    private void failover(SourceHealth.Failure reason){
        if(switching||stopping||isFinishing())return;
        cancelNext();if(currentCandidate!=null)SourceHealth.failed(currentCandidate.url,reason,android.os.SystemClock.elapsedRealtime());
        long position=player==null?Math.max(0,pendingPosition):Math.max(0,player.getCurrentPosition());
        boolean playing=player!=null?player.getPlayWhenReady():resumePlayWhenReady;
        PlaybackCandidate next=failoverSession.next(candidates,android.os.SystemClock.elapsedRealtime());
        if(next==null){exhausted(reason);return;}
        final int generation=playbackGeneration;
        switching=true;stateView.setText("در حال بررسی منبع جایگزین…");
        handler.postDelayed(()->{if(generation!=playbackGeneration)return;if(stopping||isFinishing()){switching=false;return;}verifier.execute(()->{
            MediaProbe.Result probe=MediaProbe.inspect(next.url);
            runOnUiThread(()->{if(generation!=playbackGeneration)return;switching=false;if(stopping||isFinishing())return;
                if(!probe.valid){currentCandidate=next;failover(probe.failure);return;}
                currentCandidate=next;streamUrl=next.url;pendingPosition=position;resumePlayWhenReady=playing;
                if(player==null){initPlayer();return;}
                Map<String,String> headers=new LinkedHashMap<>();if(StreamDetector.isHttpsUrl(next.page)){Uri u=Uri.parse(next.page);headers.put("Referer",u.getScheme()+"://"+u.getEncodedAuthority()+"/");}httpFactory.setDefaultRequestProperties(headers);
                cueQueue.clear();player.setMediaItem(mediaItem(),position);player.prepare();player.setPlayWhenReady(playing);revealControls();
            });
        });},500);
    }
    private void exhausted(SourceHealth.Failure failure){
        if(playerErrorShown||isFinishing())return;playerErrorShown=true;
        if(player!=null)player.pause();
        new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("منبع قابل‌پخش باقی نمانده است")
            .setMessage(SourceHealth.message(failure)+" همهٔ گزینه‌های مجاز این نوبت بررسی شدند (حداکثر ۳ تلاش). بعداً عنوان را دوباره باز کنید؛ ممکن است منبع نیازمند دسترسی رسمی باشد.")
            .setPositiveButton("بازگشت",(d,w)->finish()).setOnCancelListener(d->finish()).show();
    }
    private void maybeNext(){
        if(stopping||nextOffered||resumePrompt||nextEpisode==null||player==null||!profilePrefs.getBoolean("autoplay",true)||player.isCurrentMediaItemLive())return;
        long duration=player.getDuration(),position=player.getCurrentPosition();
        if(duration>90000&&duration-position<=45000&&EpisodeOrder.advancing(player.isPlaying(),player.getPlaybackState()==Player.STATE_ENDED,player.getPlayWhenReady()))showNext(false);
    }
    private void showNext(boolean manual){
        if(nextEpisode==null){if(manual)toast("قسمت بعدی با شمارهٔ متوالی و فصل معتبر در دسترس نیست.");return;}
        if(stopping||(nextDialog!=null&&nextDialog.isShowing()))return;nextOffered=true;
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(10),dp(20),dp(10));
        android.widget.ImageView poster=new android.widget.ImageView(this);poster.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);content.addView(poster,new LinearLayout.LayoutParams(dp(210),dp(118)));if(ImageUrlPolicy.allowed(nextEpisode.poster))nextImages.load(poster,nextEpisode.poster);
        content.addView(TvUi.text(this,nextEpisode.name,20,Color.WHITE,true));
        nextDialog=new AlertDialog.Builder(this).setOnKeyListener(this::dialogMediaKey).setTitle("قسمت بعد").setView(content).setPositiveButton("پخش بعدی",(d,w)->playNextEpisode()).setNegativeButton("لغو",(d,w)->cancelNext()).setOnCancelListener(d->cancelNext()).create();nextDialog.show();
        if(profilePrefs.getBoolean("autoplay",true)){nextCountdown=new EpisodeOrder.Countdown(profilePrefs.getInt("countdown",10));handler.postDelayed(nextTicker,1000);}
    }
    private void cancelNext(){if(nextCountdown!=null)nextCountdown.cancel();handler.removeCallbacks(nextTicker);if(nextDialog!=null){nextDialog.dismiss();nextDialog=null;}nextCountdown=null;}
    private void playNextEpisode(){
        if(nextLoading||stopping||nextEpisode==null)return;nextLoading=true;cancelNext();stateView.setText("در حال بررسی قسمت بعد…");
        nextRepository.detail(nextEpisode,(resolved,error)->{if(stopping||isFinishing()||!nextLoading)return;
            if(resolved==null||resolved.publicStream.isEmpty()){nextLoading=false;toast("قسمت بعد منبع عمومی قابل‌پخش ندارد؛ پخش خودکار متوقف شد.");return;}
            nextLoading=false;saveProgress();Intent next=new Intent(this,PlayerActivity.class);next.putExtra(EXTRA_URL,resolved.publicStream);next.putExtra(EXTRA_TITLE,resolved.name);next.putExtra(EXTRA_PAGE_URL,resolved.url);next.putExtra("candidates",CandidateCodec.encode(resolved.candidates));next.putExtra("episodes",EpisodeOrder.encode(episodeContext));startActivity(next);finish();
        });
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putLong("position",player==null?Math.max(0,pendingPosition):player.getCurrentPosition());
        state.putBoolean("playing",player==null?resumePlayWhenReady:player.getPlayWhenReady());
        state.putBoolean("prompt",resumePrompt);
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);verifier.shutdownNow();if(nextRepository!=null)nextRepository.close();if(nextImages!=null)nextImages.close();
        super.onDestroy();
    }
}
