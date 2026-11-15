package com.pooyan.namakadetv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Pooyan TV: multi-site home + compatible browser + optional native player. */
public final class MainActivity extends Activity {
    private static final String PREFS = "tv_settings_v2";
    private static final String START_URL = "start_url"; // legacy preference from v2
    private static final String ACTIVE_SITE = "active_site_v3";
    private static final String CUSTOM_NAMAKADE = "namakade_custom_url_v3";
    private static final String POINTER_MODE = "pointer_mode";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService diagnosticsPool = Executors.newFixedThreadPool(3);
    private final List<String> observedMedia = new ArrayList<>();
    private LibraryStore library;
    private PosterLoader posterLoader;
    private SourceUpdater sourceUpdater;
    private FrameLayout root, browserLayer, homeLayer;
    private WebView web;
    private RemoteCursorView cursor;
    private TextView hint;
    private View fullscreenVideo;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private boolean showHome = true, pointerMode = true;
    private boolean cursorInitialized;
    private float cursorX, cursorY;
    private String homeUrl;
    private String activeSiteId = "parsimondo";
    private String detectedPageUrl;
    private final Runnable hideHint = () -> { if (hint != null) hint.setVisibility(View.GONE); };

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        library = new LibraryStore(this);
        posterLoader = new PosterLoader();
        sourceUpdater = new SourceUpdater(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Migrate an existing personal Namakade start URL without exposing it in source.
        String legacy = prefs.getString(START_URL, "");
        if (SiteCatalog.matches(SiteCatalog.byId("namakade"), legacy)) {
            if (!prefs.contains(CUSTOM_NAMAKADE)) {
                prefs.edit().putString(CUSTOM_NAMAKADE, legacy).remove(START_URL).apply();
            } else {
                prefs.edit().remove(START_URL).apply();
            }
        }
        activeSiteId = SiteCatalog.byId(prefs.getString(ACTIVE_SITE, "parsimondo")).id;
        homeUrl = siteStartUrl(SiteCatalog.byId(activeSiteId));
        pointerMode = prefs.getBoolean(POINTER_MODE, true);
        root = new FrameLayout(this);
        root.setBackgroundColor(TvUi.BG);
        setContentView(root);

        browserLayer = new FrameLayout(this);
        root.addView(browserLayer, fill());
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        WebView.setWebContentsDebuggingEnabled(false);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                SiteCatalog.Site current = SiteCatalog.fromUrl(url);
                if (current != null && !activeSiteId.equals(current.id)) activeSiteId = current.id;
                if (detectedPageUrl == null || !detectedPageUrl.equals(url)) {
                    detectedPageUrl = url;
                    observedMedia.clear();
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                // Only curate metadata from known source top-level pages, never search queries.
                if (SiteCatalog.fromUrl(url) != null) {
                    library.visit(titleOrUrl(view.getTitle(), url), url);
                    collectArtwork(url);
                }
                if (!showHome) showHint("جهت‌ها: نشانگر | OK: انتخاب | Menu: امکانات", 4000);
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                // Observe eligible URLs only. Do not read, modify, cache or replay responses.
                String url = req.getUrl().toString();
                if (StreamDetector.isDirectMedia(url)) {
                    handler.post(() -> rememberObserved(url));
                }
                return super.shouldInterceptRequest(view, req);
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError e) {
                if (req.isForMainFrame()) showHint("سایت در دسترس نیست؛ شبکه، DNS یا محدودیت منطقه‌ای را بررسی کن. از منو می‌توانی سایت دیگری انتخاب کنی.", 10000);
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler h, SslError error) {
                h.cancel();
                showHint("خطای گواهی SSL سایت؛ اتصال ناامن تأیید نمی‌شود.", 10000);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                if (!"https".equalsIgnoreCase(scheme)) {
                    showHint("فقط لینک‌های HTTPS مجازند.", 4500);
                    return true;
                }
                return false;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback cb) {
                if (fullscreenVideo != null) { cb.onCustomViewHidden(); return; }
                fullscreenVideo = view;
                fullscreenCallback = cb;
                root.addView(view, fill());
                cursor.setVisibility(View.GONE);
                hint.setVisibility(View.GONE);
                immersive();
            }
            @Override public void onHideCustomView() { exitWebFullscreen(); }
            @Override public void onPermissionRequest(PermissionRequest req) {
                // Video viewing doesn't require granting camera/microphone to arbitrary sites.
                req.deny();
            }
        });
        browserLayer.addView(web, fill());
        cursor = new RemoteCursorView(this);
        browserLayer.addView(cursor, fill());
        cursor.setVisibility(pointerMode ? View.VISIBLE : View.GONE);
        hint = TvUi.text(this, "", 14, TvUi.TEXT, true);
        hint.setGravity(Gravity.CENTER);
        hint.setBackground(TvUi.bg(0xE91D2B3C, 12, this));
        hint.setPadding(dp(18), dp(12), dp(18), dp(12));
        hint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hp.bottomMargin = dp(24);
        browserLayer.addView(hint, hp);
        root.post(() -> {
            cursorX = root.getWidth() / 2f;
            cursorY = root.getHeight() / 2f;
            cursorInitialized = true;
            cursor.moveTo(cursorX, cursorY);
        });
        homeLayer = new FrameLayout(this);
        root.addView(homeLayer, fill());
        // Do not load any site until the user selects it, reducing needless connections.
        openHome();
        sourceUpdater.check(false, result -> runOnUiThread(() -> {
            if (!isFinishing() && showHome) buildHome();
        }));
    }

    private FrameLayout.LayoutParams fill() { return new FrameLayout.LayoutParams(-1, -1); }
    private int dp(float p) { return TvUi.dp(this, p); }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private String titleOrUrl(String title, String url) {
        if (url == null || url.isEmpty()) return "Pooyan TV";
        if (title != null && !title.trim().isEmpty() && !title.startsWith("http")) return title.trim();
        String p = Uri.parse(url).getLastPathSegment();
        return p != null && !p.isEmpty() ? p : "Pooyan TV";
    }
    private void showHint(String message, long milliseconds) {
        if (fullscreenVideo != null || hint == null) return;
        hint.setText(message); hint.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideHint);
        handler.postDelayed(hideHint, milliseconds);
    }
    private void rememberObserved(String url) {
        if (!StreamDetector.isDirectMedia(url)) return;
        observedMedia.remove(url);
        observedMedia.add(0, url);
        if (observedMedia.size() > 12) observedMedia.remove(observedMedia.size() - 1);
    }

    private String siteStartUrl(SiteCatalog.Site site) {
        String key = "namakade".equals(site.id) ? CUSTOM_NAMAKADE : "site_custom_" + site.id;
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, "");
        if (SiteCatalog.matches(site, saved)) return saved;
        String updated = sourceUpdater.overrideFor(site);
        return !updated.isEmpty() ? updated : site.url;
    }
    private void openSite(SiteCatalog.Site site) {
        activeSiteId = site.id;
        homeUrl = siteStartUrl(site);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(ACTIVE_SITE, activeSiteId).apply();
        // Clear candidates from the previous page/source before switching.
        observedMedia.clear();
        detectedPageUrl = null;
        web.loadUrl(homeUrl);
        openWeb();
    }
    private void openSavedPage(String url) {
        if (!StreamDetector.isHttpsUrl(url)) { toast("لینک معتبر نیست."); return; }
        SiteCatalog.Site site = SiteCatalog.fromUrl(url);
        if (site != null) {
            activeSiteId = site.id;
            homeUrl = siteStartUrl(site);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(ACTIVE_SITE, activeSiteId).apply();
        }
        observedMedia.clear();
        detectedPageUrl = null;
        web.loadUrl(url);
        openWeb();
    }
    private void showSitePicker() {
        List<SiteCatalog.Site> sites = SiteCatalog.all();
        String[] choices = new String[sites.size()];
        for (int i = 0; i < sites.size(); i++) {
            SiteCatalog.Site site = sites.get(i);
            choices[i] = site.title + " · " + site.status;
        }
        new AlertDialog.Builder(this).setTitle("انتخاب سایت در Pooyan TV")
                .setItems(choices, (d, n) -> openSite(sites.get(n)))
                .setNegativeButton("انصراف", null).show();
    }

    private void openHome() {
        if (fullscreenVideo != null) exitWebFullscreen();
        if (web != null && !showHome) {
            // Prevent a hidden WebView from continuing an HTML5 video behind Home.
            web.evaluateJavascript("(function(){document.querySelectorAll('video').forEach(function(v){v.pause();})})()", null);
            web.onPause();
        }
        showHome = true;
        browserLayer.setVisibility(View.GONE);
        homeLayer.setVisibility(View.VISIBLE);
        buildHome();
    }
    private void openWeb() {
        showHome = false;
        if (web != null) web.onResume();
        homeLayer.setVisibility(View.GONE);
        browserLayer.setVisibility(View.VISIBLE);
        if (web.getUrl() == null || "about:blank".equals(web.getUrl())) web.loadUrl(homeUrl);
        web.requestFocus();
        showHint("Menu: تغییر سایت، پخش‌کننده، علاقه‌مندی و تنظیمات", 5500);
    }

    private void buildHome() {
        homeLayer.removeAllViews();
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF102139, TvUi.BG, 0xFF070A12});
        homeLayer.setBackground(gradient);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        homeLayer.addView(scroll, fill());
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(46), dp(26), dp(46), dp(38));
        scroll.addView(body);

        TextView brand = TvUi.text(this, "POOYAN  /  TV", 26, TvUi.TEXT, true);
        body.addView(brand);
        topSpace(body, TvUi.text(this, "سینمای فارسی در یک جا · مخصوص Android TV", 14, TvUi.MUTED, false), 4);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(28), dp(22), dp(28), dp(25));
        GradientDrawable banner = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1B4870, 0xFF14223C});
        banner.setCornerRadius(dp(20));
        hero.setBackground(banner);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.topMargin = dp(22);
        body.addView(hero, heroLp);
        hero.addView(TvUi.text(this, "Pooyan TV  v0.1  |  سینمای فارسی", 30, TvUi.TEXT, true));
        topSpace(hero, TvUi.text(this,
                "۷ منبع، جست‌وجوی عناوین و پوسترهای صفحات بازدیدشده؛ بدون حساب در خود اپ.",
                16, 0xFFD2DEEF, false), 5);
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-1, -2);
        actionLp.topMargin = dp(18);
        hero.addView(actionRow, actionLp);
        addAction(actionRow, "▶  باز کردن: " + SiteCatalog.byId(activeSiteId).title,
                true, () -> openSite(SiteCatalog.byId(activeSiteId)));
        addAction(actionRow, "⌕  جست‌وجوی فیلم", false, this::showSearch);
        addAction(actionRow, "▦  منابع", false, this::showSitePicker);
        addAction(actionRow, "+  لینک مستقیم", false, this::promptStreamUrl);

        siteSection(body, "بدون ثبت‌نام اعلام‌شده", "این سایت‌ها صریحاً عدم نیاز به ثبت‌نام را اعلام کرده‌اند.",
                SiteCatalog.VERIFIED);
        siteSection(body, "سایت‌های عمومی", "ممکن است بعضی محتواها یا قابلیت‌ها ورود بخواهند.",
                SiteCatalog.PUBLIC);
        siteSection(body, "آزمایشی و سایت شخصی", "دسترسی این منابع هنوز کامل آزمایش نشده است.",
                SiteCatalog.REVIEW);
        siteSection(body, null, null, SiteCatalog.PERSONAL);

        List<LibraryStore.Entry> watched = library.resumeList();
        if (!watched.isEmpty()) section(body, "ادامه تماشا با پلیر اختصاصی", watched, 8, true);
        List<LibraryStore.Entry> faves = library.bookmarks();
        if (!faves.isEmpty()) posterSection(body, "علاقه‌مندی‌ها", faves, 12);
        List<LibraryStore.Entry> suggestions = suggestions();
        if (!suggestions.isEmpty()) posterSection(body, "پیشنهاد محلی بر اساس سابقهٔ تو", suggestions, 12);
        List<LibraryStore.Entry> recent = library.recentPages();
        if (!recent.isEmpty()) posterSection(body, "صفحات دیده‌شده و پوسترهایشان", recent, 15);

        LinearLayout secondary = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(26);
        body.addView(secondary, lp);
        addAction(secondary, "⌕  جست‌وجو", false, this::showSearch);
        addAction(secondary, "☆  ذخیره صفحه", false, this::bookmarkCurrent);
        addAction(secondary, "◎  بررسی اتصال سایت‌ها", false, this::showSourceDiagnostics);
        addAction(secondary, "⚙  تنظیمات", false, this::showSettings);
        addAction(secondary, "ⓘ  راهنما", false, this::showHelp);
        topSpace(body, TvUi.text(this,
                "پوسترها و پیشنهادها فقط از صفحاتی می‌آیند که بازدید کرده‌ای. جست‌وجوی سراسری از Bing استفاده می‌کند؛ بعضی سرویس‌ها به ورود یا پروکسی نیاز دارند.",
                13, TvUi.MUTED, false), 25);
        hero.post(() -> { if (actionRow.getChildCount() > 0) actionRow.getChildAt(0).requestFocus(); });
    }

    /** Extract just page-level OpenGraph artwork. No JS bridge, scraping service or token sharing. */
    private void collectArtwork(String pageUrl) {
        if (web == null || SiteCatalog.fromUrl(pageUrl) == null) return;
        String js = "(function(){var x=document.querySelector('meta[property=\"og:image\"],meta[name=\"twitter:image\"]');" +
                "var u=x&&(x.content||x.getAttribute('content'));if(!u)return '';" +
                "try{return new URL(u,location.href).href;}catch(e){return '';}})()";
        web.evaluateJavascript(js, result -> {
            if (web == null || !pageUrl.equals(web.getUrl())) return;
            try {
                String poster = new JSONArray("[" + result + "]").optString(0, "");
                if (PosterPolicy.accept(pageUrl, poster)) {
                    library.visit(titleOrUrl(web.getTitle(), pageUrl), pageUrl, poster);
                }
            } catch (Exception ignored) { }
        });
    }

    private List<LibraryStore.Entry> suggestions() {
        List<LibraryStore.Entry> recent = library.recentPages();
        List<LibraryStore.Entry> saved = library.bookmarks();
        List<LibraryStore.Entry> ranked = new ArrayList<>();
        if (recent.isEmpty()) return ranked;
        // On-device, deterministic similarity to saved titles / last visited provider.
        SiteCatalog.Site favoriteProvider = SiteCatalog.fromUrl(recent.get(0).url);
        for (LibraryStore.Entry e : recent) {
            if (ranked.size() >= 12) break;
            if (e.url.equals(recent.get(0).url)) continue;
            boolean related = favoriteProvider != null && SiteCatalog.matches(favoriteProvider, e.url);
            for (LibraryStore.Entry f : saved) {
                if (e.url.equals(f.url)) related = true;
                String[] words = SearchEngine.normalize(f.title).split(" ");
                for (String w : words) if (w.length() > 3 && SearchEngine.normalize(e.title).contains(w)) related = true;
            }
            if (related) ranked.add(e);
        }
        return ranked;
    }

    private void posterSection(LinearLayout body, String title, List<LibraryStore.Entry> entries, int limit) {
        topSpace(body, TvUi.text(this, title, 21, TvUi.TEXT, true), 24);
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setPadding(dp(6), dp(5), dp(6), dp(12));
        scroller.addView(cards);
        topSpace(body, scroller, 8);
        for (int i=0; i<Math.min(limit, entries.size()); i++) {
            LibraryStore.Entry entry = entries.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8), dp(8), dp(8), dp(10));
            card.setBackground(TvUi.bg(0xFF202E43, 12, this));
            card.setFocusable(true); card.setClickable(true);
            card.setContentDescription(entry.title + ", باز کردن صفحه");
            card.setOnClickListener(v -> openSavedPage(entry.url));
            card.setOnFocusChangeListener((v, focused) -> {
                card.setBackground(TvUi.bg(focused ? TvUi.BLUE : 0xFF202E43, 12, this));
                card.setScaleX(focused ? 1.04f : 1f); card.setScaleY(focused ? 1.04f : 1f);
            });
            FrameLayout artwork = new FrameLayout(this);
            artwork.setBackground(TvUi.bg(0xFF283C58, 10, this));
            TextView fallback = TvUi.text(this, "▶", 36, 0xFF8BA9D1, true);
            fallback.setGravity(Gravity.CENTER);
            artwork.addView(fallback, fill());
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            artwork.addView(image, fill());
            posterLoader.show(this, image, entry.url, entry.posterUrl);
            card.addView(artwork, new LinearLayout.LayoutParams(-1, dp(126)));
            TextView label = TvUi.text(this, entry.title, 16, TvUi.TEXT, true);
            label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            topSpace(card, label, 9);
            SiteCatalog.Site site = SiteCatalog.fromUrl(entry.url);
            topSpace(card, TvUi.text(this, site == null ? "صفحه ذخیره‌شده" : site.title,
                    12, TvUi.MUTED, false), 5);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(210), dp(213));
            cp.rightMargin = dp(12);
            cards.addView(card, cp);
        }
    }

    private void showSearch() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true); edit.setTextSize(19);
        edit.setHint("نام فیلم یا سریال به فارسی یا انگلیسی");
        edit.setPadding(dp(17), dp(14), dp(17), dp(14));
        new AlertDialog.Builder(this).setTitle("جست‌وجو در Pooyan TV")
                .setMessage("در تاریخچهٔ خودت جست‌وجو کن؛ برای کل منابع از جست‌وجوی وب استفاده می‌شود.")
                .setView(edit).setNegativeButton("انصراف", null)
                .setPositiveButton("جست‌وجو", (d,w) -> {
                    String query = edit.getText().toString().trim();
                    if (query.isEmpty()) { toast("عنوان را وارد کن."); return; }
                    showSearchResults(query);
                }).show();
    }

    private void showSearchResults(String query) {
        Map<String, LibraryStore.Entry> unique = new LinkedHashMap<>();
        for (LibraryStore.Entry e : library.bookmarks()) unique.put(e.url, e);
        for (LibraryStore.Entry e : library.recentPages()) unique.putIfAbsent(e.url, e);
        List<LibraryStore.Entry> matches = new ArrayList<>();
        for (LibraryStore.Entry e : unique.values()) if (SearchEngine.matches(query, e.title)) matches.add(e);
        int shown = Math.min(12, matches.size());
        String[] labels = new String[shown + 2];
        for (int i = 0; i < shown; i++) {
            SiteCatalog.Site s = SiteCatalog.fromUrl(matches.get(i).url);
            labels[i] = "▶ " + matches.get(i).title + (s == null ? "" : " · " + s.title);
        }
        labels[shown] = "🌐 جست‌وجوی وب در همهٔ منابع (Bing)";
        labels[shown + 1] = "▦ انتخاب یک سایت برای جست‌وجو";
        new AlertDialog.Builder(this)
                .setTitle("نتایج محلی: " + matches.size() + " · " + query)
                .setItems(labels, (d, n) -> {
                    if (n < shown) openSavedPage(matches.get(n).url);
                    else if (n == shown) openWebSearch(query, null);
                    else showSearchSourcePicker(query);
                }).setNeutralButton("تغییر جست‌وجو", (d,w) -> showSearch())
                .setNegativeButton("بستن", null).show();
    }

    private void showSearchSourcePicker(String query) {
        List<SiteCatalog.Site> choices = new ArrayList<>();
        for (SiteCatalog.Site site : SiteCatalog.all()) {
            if (!SiteCatalog.PERSONAL.equals(site.group)) choices.add(site);
        }
        String[] names = new String[choices.size()];
        for (int i=0;i<choices.size();i++) names[i] = choices.get(i).title;
        new AlertDialog.Builder(this).setTitle("جست‌وجوی «" + query + "» در سایت")
                .setItems(names, (d,n) -> openWebSearch(query, choices.get(n)))
                .setNegativeButton("انصراف", null).show();
    }
    private void openWebSearch(String query, SiteCatalog.Site site) {
        String url = SearchEngine.webSearchUrl(query, site);
        if (url.isEmpty()) { toast("عنوان جست‌وجو خالی است."); return; }
        // Avoid loading the previous site before launching the user's chosen search.
        showHome = false;
        homeLayer.setVisibility(View.GONE);
        browserLayer.setVisibility(View.VISIBLE);
        web.onResume(); // Home pauses WebView, so a search launched from Home must resume it.
        web.loadUrl(url);
        web.requestFocus();
        showHint("جست‌وجو از Bing است؛ نمایهٔ وب ممکن است کامل یا به‌روز نباشد.", 8500);
    }

    private void siteSection(LinearLayout body, String title, String description, String group) {
        List<SiteCatalog.Site> sites = SiteCatalog.group(group);
        if (sites.isEmpty()) return;
        if (title != null) {
            topSpace(body, TvUi.text(this, title, 21, TvUi.TEXT, true), 27);
            if (description != null) topSpace(body, TvUi.text(this, description, 13, TvUi.MUTED, false), 3);
        }
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(this);
        cards.setPadding(dp(6), dp(6), dp(6), dp(8));
        cards.setOrientation(LinearLayout.HORIZONTAL);
        scroller.addView(cards);
        topSpace(body, scroller, title == null ? 5 : 9);
        for (SiteCatalog.Site site : sites) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(19), dp(17), dp(18), dp(14));
            int base = site.accent;
            GradientDrawable panel = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{base, 0xFF1B2A43});
            panel.setCornerRadius(dp(16));
            card.setBackground(panel);
            card.setFocusable(true);
            card.setClickable(true);
            card.setContentDescription(site.title + "، " + site.detail);
            card.setOnFocusChangeListener((v, focus) -> {
                if (focus) {
                    card.setBackground(TvUi.bg(TvUi.BLUE, 16, this));
                    card.setScaleX(1.04f); card.setScaleY(1.04f);
                } else {
                    card.setBackground(panel);
                    card.setScaleX(1f); card.setScaleY(1f);
                }
            });
            card.setOnClickListener(v -> openSite(site));
            TextView name = TvUi.text(this, site.title, 22, TvUi.TEXT, true);
            card.addView(name);
            topSpace(card, TvUi.text(this, site.subtitle, 14, 0xFFECF2FC, false), 6);
            TextView status = TvUi.text(this, "●  " + site.status + "   ·   ورود با OK", 12, 0xFFE0EBFA, true);
            topSpace(card, status, 20);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(296), dp(142));
            p.rightMargin = dp(14);
            cards.addView(card, p);
        }
    }

    private void addAction(LinearLayout target, String label, boolean primary, Runnable run) {
        TextView b = TvUi.focusButton(this, label, primary, run);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.rightMargin = dp(12);
        target.addView(b, p);
    }
    private void topSpace(LinearLayout target, View v, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(margin);
        target.addView(v, p);
    }
    private void section(LinearLayout body, String title, List<LibraryStore.Entry> entries, int max, boolean video) {
        TextView label = TvUi.text(this, title, 21, TvUi.TEXT, true);
        topSpace(body, label, 28);
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(this);
        cards.setPadding(dp(6), dp(6), dp(6), dp(8));
        cards.setOrientation(LinearLayout.HORIZONTAL);
        scroller.addView(cards);
        topSpace(body, scroller, 8);
        for (int i=0; i<Math.min(max, entries.size()); i++) {
            LibraryStore.Entry e = entries.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(TvUi.bg(0xFF1F2C43, 14, this));
            card.setFocusable(true);
            card.setClickable(true);
            card.setOnFocusChangeListener((v, focused) -> {
                card.setBackground(TvUi.bg(focused ? TvUi.BLUE : 0xFF1F2C43, 14, this));
                card.setScaleX(focused ? 1.03f : 1f);
                card.setScaleY(focused ? 1.03f : 1f);
            });
            card.setOnClickListener(v -> {
                if (video) launchPlayer(e.url, e.title, e.pageUrl);
                else openSavedPage(e.url);
            });
            TextView name = TvUi.text(this, e.title, 17, TvUi.TEXT, true);
            name.setMaxLines(2);
            card.addView(name);
            String sub = video && e.durationMs > 0
                    ? (e.positionMs / 60000) + " / " + (e.durationMs / 60000) + " دقیقه"
                    : (video ? "پخش اختصاصی" : "باز کردن در مرورگر");
            topSpace(card, TvUi.text(this, sub, 13, 0xFFCBD7E8, false), 10);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(245), dp(108));
            p.rightMargin = dp(12);
            cards.addView(card, p);
        }
    }

    private void bookmarkCurrent() {
        String url = web.getUrl();
        if (!StreamDetector.isHttpsUrl(url)) { toast("صفحه‌ای برای ذخیره وجود ندارد."); return; }
        if (library.hasBookmark(url)) { toast("این صفحه قبلاً ذخیره شده است."); return; }
        library.addBookmark(titleOrUrl(web.getTitle(), url), url);
        toast("به علاقه‌مندی‌ها اضافه شد.");
        if (showHome) buildHome();
    }

    private void probeMediaAndChoose() {
        if (web == null) return;
        // DOM access is intentionally restricted to visible same-frame elements.
        // Cross-origin iframes, blob URLs and DRM sessions cannot be extracted here.
        String probe = "(function(){var list=[];var els=document.querySelectorAll('video,source,a[href]');"
                + "for(var i=0;i<els.length&&i<300;i++){var e=els[i],src=e.currentSrc||e.src||e.href||e.getAttribute('src');"
                + "if(!src)continue;try{src=new URL(src,location.href).href;}catch(z){continue;}"
                + "if(/^https:\\/\\//i.test(src)&&/\\.(m3u8|mpd|mp4|m4v|webm)(?:[?#]|$)/i.test(src)&&list.indexOf(src)<0)list.push(src);"
                + "if(list.length>=12)break;}return list;})()";
        web.evaluateJavascript(probe, response -> {
            List<String> options = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(response);
                for (int i=0;i<array.length();i++) {
                    String candidate = array.optString(i);
                    if (StreamDetector.isDirectMedia(candidate) && !options.contains(candidate)) options.add(candidate);
                }
            } catch (Exception ignored) {}
            for (String captured : observedMedia) {
                if (StreamDetector.isDirectMedia(captured) && !options.contains(captured)) options.add(captured);
            }
            if (options.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("لینک مستقیم پیدا نشد")
                        .setMessage("این صفحه هنوز لینک MP4 / HLS / DASH قابل شناسایی ارائه نکرده است. " +
                                "ویدئو را در سایت پخش کن و دوباره Menu > پخش‌کننده اختصاصی را بزن. " +
                                "در صورت استفاده از DRM، iframe محدود یا blob، حالت سایت لازم است.")
                        .setPositiveButton("ادامه در سایت", null)
                        .setNeutralButton("ورود لینک مستقیم", (d,w) -> promptStreamUrl())
                        .show();
                return;
            }
            String[] labels = new String[options.size()];
            for (int i=0;i<options.size();i++) {
                Uri u = Uri.parse(options.get(i));
                labels[i] = StreamDetector.safeName(options.get(i)) + " · " + u.getHost();
            }
            new AlertDialog.Builder(this).setTitle("پخش با Media3 · " + options.size() + " لینک")
                    .setItems(labels, (d, index) -> launchPlayer(options.get(index),
                            titleOrUrl(web.getTitle(), web.getUrl()), web.getUrl()))
                    .setNegativeButton("انصراف", null).show();
        });
    }

    private void launchPlayer(String url, String title, String pageUrl) {
        if (!StreamDetector.isHttpsUrl(url)) { toast("فقط پخش HTTPS مجاز است."); return; }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL, url);
        intent.putExtra(PlayerActivity.EXTRA_TITLE, title);
        if (StreamDetector.isHttpsUrl(pageUrl)) intent.putExtra(PlayerActivity.EXTRA_PAGE_URL, pageUrl);
        if (web != null) intent.putExtra(PlayerActivity.EXTRA_USER_AGENT, web.getSettings().getUserAgentString());
        if (web != null) {
            // Pause an HTML5 video before switching to Media3, preventing double audio.
            web.evaluateJavascript("(function(){document.querySelectorAll('video').forEach(function(v){v.pause();})})()", null);
        }
        startActivity(intent);
    }

    private void promptStreamUrl() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(17);
        edit.setHint("https://example.com/video.m3u8");
        edit.setPadding(dp(18), dp(16), dp(18), dp(16));
        new AlertDialog.Builder(this).setTitle("لینک مستقیم MP4 / HLS / DASH")
                .setView(edit).setNegativeButton("انصراف", null)
                .setPositiveButton("پخش", (d,w) -> {
                    String value = edit.getText().toString().trim();
                    if (!StreamDetector.isDirectMedia(value)) {
                        toast("لینک مستقیم HTTPS با پسوند mp4 / m3u8 / mpd وارد کن.");
                    } else launchPlayer(value, StreamDetector.safeName(value), web != null ? web.getUrl() : "");
                }).show();
    }

    private void showSettings() {
        String[] items = {"انتخاب سایت", "باز کردن سایت فعلی", "بازگشت به صفحه شروع سایت",
                "تنظیم لینک شخصی Namakade", "تغییر آدرس شروع سایت فعلی",
                pointerMode ? "خاموش کردن نشانگر" : "روشن کردن نشانگر",
                "بارگذاری مجدد", "پاک کردن تاریخچه و علاقه‌مندی‌ها",
                "پاک کردن کوکی و داده‌های همه سایت‌ها", "جست‌وجوی فیلم",
                "تنظیم منبع به‌روزرسانی (JSON)", "بررسی به‌روزرسانی منابع",
                "بررسی اتصال سایت‌ها", "راهنما"};
        new AlertDialog.Builder(this).setTitle("تنظیمات Pooyan TV")
                .setItems(items, (d,n) -> {
                    switch (n) {
                        case 0: showSitePicker(); break;
                        case 1: openWeb(); break;
                        case 2: openSite(SiteCatalog.byId(activeSiteId)); break;
                        case 3: changeSiteUrl(SiteCatalog.byId("namakade")); break;
                        case 4: changeSiteUrl(SiteCatalog.byId(activeSiteId)); break;
                        case 5: togglePointer(); break;
                        case 6: web.reload(); openWeb(); break;
                        case 7: new AlertDialog.Builder(this)
                                .setMessage("تاریخچه تماشا و علاقه‌مندی‌ها پاک شوند؟")
                                .setNegativeButton("انصراف", null)
                                .setPositiveButton("پاک کردن", (x,y) -> {
                                    library.clear(); if (showHome) buildHome();
                                }).show(); break;
                        case 8: new AlertDialog.Builder(this)
                                .setMessage("کوکی‌ها و داده‌های همه سایت‌ها پاک شوند؟ این کار ممکن است از حساب سایت‌ها خارجت کند.")
                                .setNegativeButton("انصراف", null)
                                .setPositiveButton("پاک کردن", (x,y) -> {
                                    CookieManager.getInstance().removeAllCookies(null);
                                    android.webkit.WebStorage.getInstance().deleteAllData();
                                    web.clearCache(true);
                                    if (!showHome) web.reload();
                                    toast("کوکی و داده‌های وب پاک شدند.");
                                }).show(); break;
                        case 9: showSearch(); break;
                        case 10: configureSourceUpdates(); break;
                        case 11: checkSourceUpdates(); break;
                        case 12: showSourceDiagnostics(); break;
                        case 13: showHelp(); break;
                        default: break;
                    }
                }).show();
    }
    private void showBrowserMenu() {
        String[] items = {"▦ تغییر سایت", "◉ پخش‌کننده اختصاصی (تشخیص لینک)",
                "☆ ذخیره صفحه", "⌂ صفحه اصلی", "↻ بارگذاری دوباره",
                pointerMode ? "خاموش کردن نشانگر" : "روشن کردن نشانگر",
                "🔗 لینک مستقیم", "⌕ جست‌وجوی عنوان در منابع", "⇄ همین عنوان در سایت‌های دیگر", "⚙ تنظیمات"};
        new AlertDialog.Builder(this)
                .setTitle("Pooyan TV · " + SiteCatalog.byId(activeSiteId).title)
                .setItems(items, (d,i) -> {
                    switch(i) {
                        case 0: showSitePicker(); break;
                        case 1: probeMediaAndChoose(); break;
                        case 2: bookmarkCurrent(); break;
                        case 3: openHome(); break;
                        case 4: web.reload(); break;
                        case 5: togglePointer(); break;
                        case 6: promptStreamUrl(); break;
                        case 7: showSearch(); break;
                        case 8: showSearchSourcePicker(titleOrUrl(web.getTitle(), web.getUrl())); break;
                        case 9: showSettings(); break;
                        default: break;
                    }
                }).show();
    }
    private void changeSiteUrl(SiteCatalog.Site site) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(siteStartUrl(site));
        edit.setPadding(dp(18), dp(15), dp(18), dp(15));
        new AlertDialog.Builder(this).setTitle("آدرس HTTPS برای " + site.title)
                .setMessage("فقط آدرس دامنهٔ همین سایت را بگذار. برای Namakade می‌توانی لینک شخصی خودت را اینجا ذخیره کنی.")
                .setView(edit).setNegativeButton("لغو", null)
                .setNeutralButton("آدرس اصلی", (d,w) -> {
                    String key = "namakade".equals(site.id) ? CUSTOM_NAMAKADE : "site_custom_" + site.id;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(key).apply();
                    if (site.id.equals(activeSiteId)) openSite(site);
                    else toast("آدرس سایت بازنشانی شد.");
                })
                .setPositiveButton("ذخیره", (d,w) -> {
                    String value = edit.getText().toString().trim();
                    if (!SiteCatalog.matches(site, value)) {
                        toast("آدرس باید HTTPS و متعلق به دامنهٔ " + site.host + " باشد.");
                        return;
                    }
                    String key = site.id.equals("namakade") ? CUSTOM_NAMAKADE : "site_custom_" + site.id;
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(key, value).apply();
                    if (site.id.equals(activeSiteId)) openSite(site);
                    else toast("آدرس ذخیره شد.");
                }).show();
    }
    private void togglePointer() {
        pointerMode = !pointerMode;
        cursor.setVisibility(pointerMode ? View.VISIBLE : View.GONE);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(POINTER_MODE, pointerMode).apply();
        toast(pointerMode ? "نشانگر روشن است" : "کنترل مستقیم WebView روشن است");
        web.requestFocus();
    }
    private void configureSourceUpdates() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(sourceUpdater.getManifestUrl());
        edit.setHint("https://your-domain.example/sources.json");
        edit.setPadding(dp(17), dp(16), dp(17), dp(16));
        new AlertDialog.Builder(this).setTitle("به‌روزرسانی آدرس منابع")
                .setMessage("اختیاری: آدرس یک فایل JSON مورداعتماد را وارد کن. در هر ۲۴ ساعت یک بار بررسی می‌شود و فقط مسیر سایت‌های شناخته‌شده را تغییر می‌دهد؛ هرگز دامنهٔ جدید یا لینک خصوصی Namakade را نمی‌پذیرد. برای غیرفعال‌سازی، این فیلد را خالی کن.")
                .setView(edit).setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (d,w) -> {
                    if (sourceUpdater.setManifestUrl(edit.getText().toString()))
                        toast("تنظیمات به‌روزرسانی ذخیره شد.");
                    else toast("نشانی باید HTTPS معتبر باشد.");
                }).show();
    }
    private void checkSourceUpdates() {
        toast("در حال بررسی فهرست منابع…");
        sourceUpdater.check(true, result -> runOnUiThread(() -> {
            if (!isFinishing()) { toast(result); if (showHome) buildHome(); }
        }));
    }

    private void showSourceDiagnostics() {
        // Tests each public base URL without credentials; this is not a video test.
        List<SiteCatalog.Site> sources = SiteCatalog.all();
        final String[] statuses = new String[sources.size()];
        java.util.Arrays.fill(statuses, "در انتظار…");
        TextView results = TvUi.text(this, "", 16, TvUi.TEXT, false);
        results.setPadding(dp(24), dp(15), dp(24), dp(15));
        results.setText(buildDiagnosticText(sources, statuses));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(results);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("بررسی اتصال ۷ منبع")
                .setMessage("فقط پاسخ صفحهٔ اصلی سایت بررسی می‌شود؛ نتیجهٔ موفق به معنای پخش فیلم، نبود ثبت‌نام یا وجود زیرنویس نیست.")
                .setPositiveButton("بستن", null)
                .create();
        // AlertDialog.setView must be called before show().
        dialog.setView(scroll);
        dialog.show();
        for (int i = 0; i < sources.size(); i++) {
            final int index = i;
            SiteCatalog.Site site = sources.get(i);
            diagnosticsPool.execute(() -> {
                String result = SourceDiagnostics.check(site);
                runOnUiThread(() -> {
                    if (isFinishing() || !dialog.isShowing()) return;
                    statuses[index] = result;
                    results.setText(buildDiagnosticText(sources, statuses));
                });
            });
        }
    }
    private String buildDiagnosticText(List<SiteCatalog.Site> sources, String[] results) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) text.append("\n\n");
            text.append(sources.get(i).title).append("\n").append(results[i]);
        }
        return text.toString();
    }

    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("راهنمای Pooyan TV")
                .setMessage("از صفحهٔ اصلی یکی از سایت‌ها را انتخاب کن؛ همهٔ سایت‌ها در یک اپ هستند. " +
                        "بدون ثبت‌نام داخل Pooyan TV، اما بعضی سایت‌ها ممکن است ورود بخواهند. " +
                        "برای انتخاب سایت دیگر Menu > تغییر سایت را بزن.\n\n" +
                        "در سایت: جهت‌ها برای نشانگر، OK برای کلیک، Menu یا کلید زرد برای امکانات. " +
                        "در پلیر اختصاصی: چپ/راست برای عقب و جلو، OK برای نمایش کنترل‌ها، Play/Pause برای توقف.\n\n" +
                        "تشخیص لینک پس از شروع ویدئو در وب‌سایت بهتر عمل می‌کند. " +
                        "فقط MP4، HLS و DASH مستقیم با HTTPS پشتیبانی می‌شوند. " +
                        "کیفیت و زیرنویس به منبع بستگی دارند. پخش DRM و iframe محدود از مسیر سایت انجام می‌شود.\n\n" +
                        "جست‌وجوی وب عبارت موردنظر را به Bing می‌فرستد؛ تا خودت آن را انتخاب نکنی اجرا نمی‌شود. " +
                "پوسترها فقط از سایت اصلی صفحه و بدون ارسال کوکی خوانده می‌شوند. " +
                "پیشنهادهای محلی فقط از صفحات دیده‌شده یا ذخیره‌شده ساخته می‌شوند. " +
                "فهرست اختیاری به‌روزرسانی صرفاً نشانی شروع سایت‌های شناخته‌شده را تغییر می‌دهد. " +
                "این اپ غیررسمی است و فایل‌های ویدئویی را میزبانی نمی‌کند.")
                .setPositiveButton("متوجه شدم", null).show();
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_MENU || key == KeyEvent.KEYCODE_SETTINGS || key == KeyEvent.KEYCODE_PROG_YELLOW) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (fullscreenVideo == null) {
                    if (showHome) showSettings(); else showBrowserMenu();
                }
            }
            return true;
        }
        if (showHome || fullscreenVideo != null || !pointerMode) return super.dispatchKeyEvent(event);
        boolean arrow = key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT ||
                key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean select = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER ||
                key == KeyEvent.KEYCODE_NUMPAD_ENTER || key == KeyEvent.KEYCODE_BUTTON_A;
        if (!arrow && !select) return super.dispatchKeyEvent(event);
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (arrow) moveCursor(key, event.getRepeatCount());
        else if (event.getRepeatCount() == 0) tapWeb(cursorX, cursorY);
        return true;
    }
    private void moveCursor(int dir, int repeats) {
        if (root.getWidth() <= 0 || root.getHeight() <= 0) return;
        if (!cursorInitialized) {
            cursorX = root.getWidth()/2f; cursorY = root.getHeight()/2f; cursorInitialized=true;
        }
        int step = dp(repeats >= 4 ? 76 : 35);
        switch (dir) {
            case KeyEvent.KEYCODE_DPAD_LEFT: cursorX -= step; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: cursorX += step; break;
            case KeyEvent.KEYCODE_DPAD_UP: cursorY -= step; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: cursorY += step; break;
            default: break;
        }
        cursorX = Math.max(dp(10), Math.min(root.getWidth()-dp(10), cursorX));
        cursorY = Math.max(dp(10), Math.min(root.getHeight()-dp(10), cursorY));
        cursor.moveTo(cursorX, cursorY);
    }
    private void tapWeb(float x, float y) {
        long t = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0);
        MotionEvent up = MotionEvent.obtain(t,t+65,MotionEvent.ACTION_UP,x,y,0);
        try { web.dispatchTouchEvent(down); web.dispatchTouchEvent(up); }
        finally { down.recycle(); up.recycle(); }
    }
    private void exitWebFullscreen() {
        if (fullscreenVideo == null) return;
        View v = fullscreenVideo;
        fullscreenVideo = null;
        root.removeView(v);
        if (fullscreenCallback != null) {
            WebChromeClient.CustomViewCallback cb=fullscreenCallback;
            fullscreenCallback=null;
            cb.onCustomViewHidden();
        }
        cursor.setVisibility(pointerMode ? View.VISIBLE : View.GONE);
        immersive();
        web.requestFocus();
    }
    @Override public void onBackPressed() {
        if (fullscreenVideo != null) { exitWebFullscreen(); return; }
        if (!showHome) {
            if (web.canGoBack()) web.goBack(); else openHome();
            return;
        }
        new AlertDialog.Builder(this).setMessage("خروج از Pooyan TV؟")
                .setNegativeButton("نه", null).setPositiveButton("خروج", (d,w)->finish()).show();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null && !showHome) web.onResume();
        if (showHome && homeLayer != null) buildHome();
        immersive();
    }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        diagnosticsPool.shutdownNow();
        if (posterLoader != null) posterLoader.close();
        if (sourceUpdater != null) sourceUpdater.close();
        if (fullscreenVideo != null) exitWebFullscreen();
        if (web != null) { browserLayer.removeView(web); web.destroy(); web=null; }
        super.onDestroy();
    }
}
