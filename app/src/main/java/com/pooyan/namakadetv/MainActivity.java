package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** v0.2: Native, D-pad driven, catalog-first cinema. There is deliberately no browser/WebView. */
public final class MainActivity extends Activity {
    private static final int BG=0xFF070C16, PANEL=0xFF131F30, ACCENT=0xFF15B8A6;
    private static final int WHITE=0xFFF9FAFC, DIM=0xFFA2B4C8;
    private FrameLayout root;
    private LinearLayout body;
    private TextView status;
    private MediaCatalog catalog;
    private CatalogImages images;
    private LocalStreams streams;
    private LibraryStore playback;
    private SharedPreferences prefs;
    private String mode="home", returnMode="home", searchTerm="";
    private int generation=0;
    private CatalogMovie activeMovie;
    private int selectedSeason = -1;
    private CatalogMovie.Episode activeEpisode;
    private boolean destroyed=false;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        catalog=new MediaCatalog(this);images=new CatalogImages(this);
        streams=new LocalStreams(this);playback=new LibraryStore(this);
        prefs=getSharedPreferences("pooyan_v02_ui",MODE_PRIVATE);
        root=new FrameLayout(this);root.setBackgroundColor(BG);setContentView(root);
        home();
    }
    private int dp(float v){return TvUi.dp(this,v);}
    private GradientDrawable panel(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView label(String txt,int size,int color,boolean bold){
        TextView v=TvUi.text(this,txt,size,color,bold);v.setIncludeFontPadding(true);v.setMaxLines(3);return v;
    }
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private TextView action(String txt,boolean primary,Runnable click){
        TextView v=label(txt,15,WHITE,true);v.setGravity(Gravity.CENTER);
        v.setPadding(dp(17),dp(11),dp(17),dp(11));
        v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);
        v.setBackground(panel(primary?0xFF087A77:PANEL,12));
        v.setOnFocusChangeListener((view,focused)->{
            v.setBackground(panel(focused?ACCENT:(primary?0xFF087A77:PANEL),12));
            v.setTextColor(focused?BG:WHITE);
            v.setScaleX(focused?1.045f:1);v.setScaleY(focused?1.045f:1);
        });
        v.setOnClickListener(vw->click.run());return v;
    }
    private void spacer(LinearLayout container,int size){View v=new View(this);container.addView(v,lp(size,1));}
    private void enter(String newMode){mode=newMode;generation++;root.removeAllViews();
        LinearLayout whole=vertical();root.addView(whole,new FrameLayout.LayoutParams(-1,-1));
        addHeader(whole);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(false);sc.setVerticalScrollBarEnabled(false);
        body=vertical();body.setPadding(dp(45),dp(15),dp(38),dp(40));sc.addView(body,new ScrollView.LayoutParams(-1,-2));
        whole.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        status=label("",13,DIM,false);
        status.setPadding(dp(46),dp(9),dp(46),dp(8));
        whole.addView(status,lp(-1,35));
    }
    private void addHeader(LinearLayout whole){
        LinearLayout header=horizontal();header.setPadding(dp(38),dp(16),dp(38),dp(8));
        TextView brand=label("◈  POOYAN TV",23,WHITE,true);
        brand.setLetterSpacing(0.075f);header.addView(brand,lp(250,52));
        TextView home=action("خانه",mode.equals("home"),this::home);
        header.addView(home,lp(94,45));spacer(header,10);
        TextView movies=action("فیلم‌ها",mode.equals("movies"),()->category("movies"));
        header.addView(movies,lp(105,45));spacer(header,10);
        TextView series=action("سریال‌ها",mode.equals("series"),()->category("series"));
        header.addView(series,lp(104,45));spacer(header,10);
        TextView free=action("تماشای رایگان",mode.equals("open"),()->category("open"));
        header.addView(free,lp(150,45));spacer(header,10);
        TextView library=action("کتابخانه من",mode.equals("library"),()->category("library"));
        header.addView(library,lp(135,45));
        View grow=new View(this);header.addView(grow,new LinearLayout.LayoutParams(0,1,1));
        TextView search=action("⌕  جست‌وجو",mode.equals("search"),this::searchDialog);
        header.addView(search,lp(130,45));
        whole.addView(header,lp(-1,82));
        View line=new View(this);line.setBackgroundColor(0xFF1F3041);whole.addView(line,lp(-1,1));
    }
    private void setStatus(String msg){if(status!=null)status.setText(msg);}
    private void hero(){
        LinearLayout banner=vertical(); banner.setPadding(dp(34),dp(26),dp(24),dp(25));
        GradientDrawable grad=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF114047,0xFF1B2645,0xFF0F1725});grad.setCornerRadius(dp(24));banner.setBackground(grad);
        TextView chip=label("POOYAN TV  ·  0.2",13,0xFF72F0DF,true);banner.addView(chip,lp(-1,25));
        TextView title=label("سینمای جهان، روی تلویزیون تو",31,WHITE,true);banner.addView(title,lp(-1,60));
        TextView desc=label("فیلم، سریال، انیمیشن و آثار آزاد؛ با پوستر، جست‌وجو و پخش‌کنندهٔ اختصاصی.",16,0xFFCEDFE9,false);
        banner.addView(desc,lp(-1,44));
        LinearLayout buttons=horizontal();
        buttons.addView(action("▶  فیلم‌های قابل پخش",true,()->category("open")),lp(224,48));spacer(buttons,12);
        buttons.addView(action("⌕  جست‌وجوی جهانی",false,this::searchDialog),lp(226,48));
        banner.addView(buttons,lp(-1,58));
        body.addView(banner,lp(-1,255));
    }
    private TextView sectionTitle(String title,String caption){
        LinearLayout wrapper=vertical();
        TextView t=label(title,22,WHITE,true);wrapper.addView(t,lp(-1,34));
        if(caption!=null&&!caption.isEmpty()){TextView c=label(caption,12,DIM,false);wrapper.addView(c,lp(-1,20));}
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(27);p.bottomMargin=dp(10);
        body.addView(wrapper,p);return t;
    }
    private void home(){
        enter("home");hero();
        openRail();
        loadRail("محبوب‌ترین فیلم‌ها","نمایش اطلاعات؛ برای تماشا باید منبع داشته باشد", "movie", "");
        loadRail("سریال‌های محبوب","فهرست جهانی با اطلاعات قسمت‌ها", "series", "");
        loadRail("انیمیشن و خانواده","آثار انیمیشن از کشورهای مختلف", "movie", "Animation");
        loadRail("اکشن و ماجراجویی","", "movie", "Action");
        loadRail("کمدی","", "movie", "Comedy");
        setStatus("کاتالوگ: افزونهٔ رسمی Cinemeta · پخش: Media3 · بدون مرورگر");
    }
    private void category(String next){
        enter(next);
        if("movies".equals(next)){
            sectionTitle("فیلم‌های جهان","فهرست‌های عمومی Cinemeta · اطلاعات فیلم با لینک پخش فرق دارد");
            loadRail("پرطرفدار","","movie","");
            loadRail("درام","","movie","Drama");
            loadRail("علمی‌تخیلی","","movie","Sci-Fi");
            loadRail("وحشت","","movie","Horror");
        }else if("series".equals(next)){
            sectionTitle("سریال‌های جهان","برای هر قسمت می‌توان منبع قانونی مجزا اضافه کرد");
            loadRail("محبوب","","series","");
            loadRail("جنایی","","series","Crime");
            loadRail("کمدی","","series","Comedy");
        }else if("open".equals(next)){
            sectionTitle("فیلم‌های آزاد و قابل پخش","آثار Blender با مجوز Creative Commons؛ دسترسی اینترنت چین ممکن است محدود باشد");
            openRail();
        }else if("library".equals(next)){
            sectionTitle("کتابخانهٔ من","منابع مستقیمی که خودت اضافه کرده‌ای؛ فقط روی همین دستگاه ذخیره می‌شوند");
            savedSection();favoritesSection();
            TextView add=action("+  افزودن فیلم MP4 / HLS / DASH",true,()->askAddCustom());
            LinearLayout.LayoutParams p=lp(380,55);p.topMargin=dp(25);body.addView(add,p);
            sectionTitle("ادامهٔ تماشا","ویدئوهای اخیر در پخش‌کنندهٔ اختصاصی");
            LinearLayout rows=vertical();body.addView(rows);
            for(LibraryStore.Entry e:playback.resumeList()){
                TextView button=action("▶  "+e.title + (e.positionMs>0?" · از "+(e.positionMs/60000)+" دقیقه":""),false,()->play(e.title,e.url,e.pageUrl));
                LinearLayout.LayoutParams rp=lp(-1,53);rp.bottomMargin=dp(8);rows.addView(button,rp);
            }
        }
        setStatus("Pooyan TV · نسخهٔ آزمایشی 0.2");
    }
    private void loadRail(String title,String caption,String type,String genre){
        sectionTitle(title,caption);
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);scroll.addView(row,new HorizontalScrollView.LayoutParams(-2,-2));
        TextView loading=label("در حال دریافت پوسترها…",14,DIM,false);row.addView(loading,lp(320,85));
        body.addView(scroll,lp(-1,288));
        final int current=generation;
        catalog.list(CatalogPolicy.catalog(type,genre),type,(items,error)->{
            if(destroyed||current!=generation)return;
            row.removeAllViews();
            if(items.isEmpty()){
                TextView empty=label("فعلاً به کاتالوگ دسترسی نداریم؛ اینترنت یا DNS را بررسی کن.",15,DIM,false);
                row.addView(empty,lp(780,110));
            }else{
                for(CatalogMovie m:items)addCard(row,m);
            }
            if(error!=null)setStatus(error);
        });
    }
    private void addCard(LinearLayout row,CatalogMovie m){
        LinearLayout card=vertical();card.setFocusable(true);card.setClickable(true);
        card.setPadding(dp(6),dp(6),dp(6),dp(6));card.setBackground(outline(false));
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setBackground(panel(0xFF24334A,10));
        card.addView(art,lp(148,199));
        if(CatalogPolicy.isImageUrl(m.poster))images.load(art,m.poster);
        TextView title=label(m.title,14,WHITE,true);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title,lp(-1,28));
        TextView year=label(m.year+(m.type.equals("series")?" · سریال":""),11,DIM,false);card.addView(year,lp(-1,20));
        card.setOnFocusChangeListener((v,has)->{
            card.setBackground(outline(has));card.setScaleX(has?1.045f:1);card.setScaleY(has?1.045f:1);
            if(has)card.bringToFront();
        });
        card.setOnClickListener(v->details(m));
        LinearLayout.LayoutParams p=lp(170,276);p.rightMargin=dp(12);row.addView(card,p);
    }
    private GradientDrawable outline(boolean active){
        GradientDrawable d=panel(active?0xFF1A3846:0xFF101827,13);
        d.setStroke(dp(active?3:1),active?ACCENT:0xFF223040);return d;
    }
    private void openRail(){
        sectionTitle("▶  همین حالا تماشا کن","فیلم‌های آزاد Blender؛ پخش مستقیم، بدون اشتراک");
        HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);
        for(OpenFilms.Film f:OpenFilms.all()){
            LinearLayout tile=vertical();tile.setPadding(dp(12),dp(11),dp(12),dp(10));
            tile.setBackground(outline(false));tile.setFocusable(true);tile.setClickable(true);
            TextView symbol=label("▶",45,0xFF67E5D1,true);symbol.setGravity(Gravity.CENTER);
            GradientDrawable image=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF164D51,0xFF202743,0xFF111A2D});image.setCornerRadius(dp(9));
            symbol.setBackground(image);tile.addView(symbol,lp(-1,153));
            TextView name=label(f.title,14,WHITE,true);name.setMaxLines(2);tile.addView(name,lp(-1,43));
            TextView sub=label(f.year+" · "+f.genre,11,DIM,false);sub.setMaxLines(2);tile.addView(sub,lp(-1,33));
            tile.setOnFocusChangeListener((v,focus)->tile.setBackground(outline(focus)));
            tile.setOnClickListener(v->openFilm(f));
            LinearLayout.LayoutParams p=lp(200,264);p.rightMargin=dp(15);row.addView(tile,p);
        }
        sc.addView(row);body.addView(sc,lp(-1,268));
    }
    private void openFilm(OpenFilms.Film f){
        String prev=mode;returnMode=prev;
        enter("detail");
        LinearLayout box=vertical();box.setPadding(dp(32),dp(35),dp(32),dp(35));box.setBackground(panel(PANEL,20));
        body.addView(box,lp(-1,-2));
        box.addView(label("فیلم آزاد · "+f.year,16,ACCENT,true),lp(-1,38));
        box.addView(label(f.title,36,WHITE,true),lp(-1,65));
        box.addView(label(f.summary,18,DIM,false),lp(-1,78));
        box.addView(label(f.license+" · فیلم اصلی روی سرور سازنده میزبانی می‌شود",13,0xFFACCCCB,false),lp(-1,53));
        LinearLayout controls=horizontal();box.addView(controls,lp(-1,66));
        controls.addView(action("▶  پخش فیلم",true,()->play(f.title,f.url,"")),lp(180,55));spacer(controls,12);
        controls.addView(action("بازگشت",false,()->goBackTo(prev)),lp(130,55));
        controls.getChildAt(0).post(() -> controls.getChildAt(0).requestFocus());
        setStatus("پخش وابسته به دسترسی شبکه و قابلیت پخش دستگاه است");
    }
    private void details(CatalogMovie movie){
        if(!"detail".equals(mode))returnMode=mode;
        activeMovie=movie; activeEpisode=null;selectedSeason=-1;renderDetails(movie,false);
        catalog.detail(movie,(full,error)->{
            if(destroyed||!"detail".equals(mode)||activeMovie==null||!activeMovie.key().equals(movie.key()))return;
            activeMovie=full;renderDetails(full,true);
            if(error!=null)setStatus(error);
        });
    }
    private void renderDetails(CatalogMovie m,boolean resolved){
        enter("detail");
        LinearLayout view=horizontal();view.setGravity(Gravity.TOP);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(panel(PANEL,16));view.addView(poster,lp(270,380));
        if(CatalogPolicy.isImageUrl(m.poster))images.load(poster,m.poster);
        spacer(view,42);
        LinearLayout info=vertical();view.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=label("◈  "+("series".equals(m.type)?"سریال":"فیلم")+"  ·  "+m.year,15,ACCENT,true);info.addView(badge,lp(-1,38));
        TextView title=label(m.title,34,WHITE,true);info.addView(title,lp(-1,-2));
        TextView genres=label(m.genres,15,DIM,false);info.addView(genres,lp(-1,45));
        String description=m.description.isEmpty()?"اطلاعات تکمیلی هنوز در دسترس نیست.":m.description;
        TextView summary=label(description,16,0xFFD9E4EC,false);summary.setMaxLines(6);
        info.addView(summary,lp(-1,-2));
        TextView note=label("اطلاعات فیلم به معنی موجودبودن لینک پخش نیست.",13,0xFFFFD59A,false);
        LinearLayout.LayoutParams np=lp(-1,40);np.topMargin=dp(10);info.addView(note,np);
        LinearLayout actions=horizontal();LinearLayout.LayoutParams ap=lp(-1,65);ap.topMargin=dp(10);info.addView(actions,ap);
        String assigned=streams.get(m.key());
        if(assigned!=null)actions.addView(action("▶  پخش",true,()->play(m.title,assigned,"")),lp(160,52));
        else actions.addView(action("+  افزودن منبع پخش",true,()->addStream(m.key(),m.title,()->renderDetails(m,resolved))),lp(225,52));
        spacer(actions,12);
        actions.addView(action(isFavorite(m)?"♥  حذف علاقه‌مندی":"♡  علاقه‌مندی",false,()->{toggleFavorite(m);renderDetails(m,resolved);}),lp(192,52));
        spacer(actions,12);
        actions.addView(action("بازگشت",false,()->goBackTo(returnMode)),lp(125,52));
        body.addView(view,lp(-1,-2));
        if(actions.getChildCount()>0)actions.getChildAt(0).post(() -> actions.getChildAt(0).requestFocus());
        if("series".equals(m.type)){
            sectionTitle("قسمت‌ها",resolved?"قسمت‌ها را انتخاب کن؛ منبع پخش هر قسمت جداست":"در حال دریافت اطلاعات قسمت‌ها…");
            LinearLayout container=vertical();body.addView(container);
            if(m.episodes.isEmpty())container.addView(label("اطلاعات قسمت‌ها موجود نیست.",15,DIM,false),lp(-1,65));
            else{
                java.util.SortedSet<Integer> seasons = new java.util.TreeSet<>();
                for (CatalogMovie.Episode episode : m.episodes) if (episode.season > 0) seasons.add(episode.season);
                if (selectedSeason < 0 && !seasons.isEmpty()) selectedSeason = seasons.first();
                HorizontalScrollView seasonScroll = new HorizontalScrollView(this);
                LinearLayout seasonButtons = horizontal();
                seasonScroll.addView(seasonButtons);
                container.addView(seasonScroll, lp(-1, 65));
                for (int season : seasons) {
                    seasonButtons.addView(action("فصل " + season, selectedSeason == season, () -> {
                        selectedSeason = season;
                        renderDetails(m, resolved);
                    }), lp(115, 52));
                    spacer(seasonButtons, 10);
                }
                for(CatalogMovie.Episode ep:m.episodes){
                    if(ep.season!=selectedSeason)continue;
                    String key=m.key()+":"+ep.id;
                    String stream=streams.get(key);
                    String n="S"+ep.season+" E"+ep.episode+" · "+ep.title;
                    TextView row=action((stream!=null?"▶  ":"+  ")+n,false,()->{
                        if(stream!=null)play(m.title+" — "+n,stream,"");
                        else addStream(key,m.title+" — "+n,()->renderDetails(m,resolved));
                    });
                    LinearLayout.LayoutParams p=lp(-1,57);p.bottomMargin=dp(7);container.addView(row,p);
                }
            }
        }
        setStatus("شناسه: "+m.id+" · کاتالوگ رسمی Cinemeta");
    }
    private void searchDialog(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);
        input.setHintTextColor(DIM);input.setHint("نام فیلم، بازیگر یا سریال…");input.setText(searchTerm);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        AlertDialog dialog=new AlertDialog.Builder(this)
                .setTitle("جست‌وجوی جهانی فیلم و سریال")
                .setView(input).setPositiveButton("جست‌وجو",(d,w)->search(input.getText().toString()))
                .setNegativeButton("انصراف",null).create();
        input.setOnEditorActionListener((v,id,e)->{
            if(id==EditorInfo.IME_ACTION_SEARCH || (e!=null&&e.getKeyCode()==KeyEvent.KEYCODE_ENTER)){
                String q=input.getText().toString();dialog.dismiss();search(q);return true;
            }return false;
        });
        dialog.show();input.requestFocus();
        input.postDelayed(()->((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT),200);
    }
    private void search(String query){
        String q=query==null?"":query.trim();if(q.isEmpty()){toast("یک عنوان وارد کن");return;}
        if(q.length()>100)q=q.substring(0,100);
        searchTerm=q;enter("search");
        sectionTitle("نتایج: "+q,"جست‌وجوی عنوان در کاتالوگ جهانی · زبان فارسی ممکن است نتایج محدودی داشته باشد");
        searchRail("فیلم‌ها","movie",q);
        searchRail("سریال‌ها","series",q);
        TextView another=action("⌕  جست‌وجوی دوباره",false,this::searchDialog);body.addView(another,lp(250,54));
    }
    private void searchRail(String label,String type,String q){
        sectionTitle(label,"");
        HorizontalScrollView s=new HorizontalScrollView(this);s.setHorizontalScrollBarEnabled(false);
        LinearLayout r=horizontal();r.setGravity(Gravity.TOP);s.addView(r);body.addView(s,lp(-1,288));
        r.addView(label("در حال جست‌وجو…",14,DIM,false),lp(240,74));
        final int current=generation;
        catalog.list(CatalogPolicy.search(type,q),type,(movies,error)->{
            if(destroyed||current!=generation)return;
            r.removeAllViews();
            if(movies.isEmpty())r.addView(label("نتیجه‌ای پیدا نشد / اتصال در دسترس نیست",14,DIM,false),lp(520,80));
            else for(CatalogMovie item:movies)addCard(r,item);
            if(error!=null)setStatus(error);
        });
    }
    private void addStream(String key,String title,Runnable after){
        EditText field=new EditText(this);field.setSingleLine(false);field.setLines(2);
        field.setTextColor(WHITE);field.setHintTextColor(DIM);
        field.setHint("https://example.com/film.mp4 یا playlist.m3u8");
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle("لینک مستقیمِ مجاز برای «"+title+"»")
                .setMessage("لینک مستقیم HTTPS از نوع MP4 / WebM / HLS / DASH وارد کن. این برنامه وارد سرویس‌های اشتراکی نمی‌شود و DRM را دور نمی‌زند.")
                .setView(field).setNegativeButton("لغو",null)
                .setPositiveButton("ذخیره",(d,w)->{
                    String url=field.getText().toString().trim();
                    if(!streams.put(key,title,url))toast("لینک باید HTTPS و فرمت ویدئویی پشتیبانی‌شده داشته باشد.");
                    else{toast("منبع ذخیره شد");after.run();}
                }).show();
    }
    private void askAddCustom(){
        EditText title=new EditText(this);title.setSingleLine(true);title.setHint("نام فیلم");
        new AlertDialog.Builder(this).setTitle("فیلم شخصی").setView(title)
                .setNegativeButton("لغو",null).setPositiveButton("بعدی",(d,w)->{
                    String label=title.getText().toString().trim();if(label.isEmpty()){toast("نام لازم است");return;}
                    addStream("custom:"+System.currentTimeMillis(),label,()->category("library"));
                }).show();
    }
    private void savedSection(){
        List<LocalStreams.Saved> items=streams.all();
        if(items.isEmpty())body.addView(label("هنوز منبع شخصی اضافه نکرده‌ای.",15,DIM,false),lp(-1,45));
        for(LocalStreams.Saved s:items){
            LinearLayout row=horizontal();
            row.addView(action("▶  "+s.title,true,()->play(s.title,s.url,"")),new LinearLayout.LayoutParams(0,dp(56),1));spacer(row,12);
            row.addView(action("حذف",false,()->new AlertDialog.Builder(this).setTitle("حذف لینک «"+s.title+"»؟")
                    .setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{streams.delete(s.key);category("library");}).show()),lp(90,56));
            LinearLayout.LayoutParams p=lp(-1,57);p.bottomMargin=dp(10);body.addView(row,p);
        }
    }
    private void favoritesSection(){
        sectionTitle("علاقه‌مندی‌ها","فهرست محلی عنوان‌هایی که ذخیره کرده‌ای");
        HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);
        for(CatalogMovie m:favorites())addCard(row,m);
        if(row.getChildCount()==0)row.addView(label("هنوز فیلمی به علاقه‌مندی‌ها اضافه نشده.",14,DIM,false),lp(500,80));
        sc.addView(row);body.addView(sc,lp(-1,285));
    }
    private List<CatalogMovie> favorites(){
        List<CatalogMovie> list=new ArrayList<>();
        try{JSONArray arr=new JSONArray(prefs.getString("faves","[]"));
            for(int i=0;i<Math.min(arr.length(),150);i++){
                JSONObject j=arr.optJSONObject(i);if(j==null)continue;
                CatalogMovie m=CatalogMovie.parse(j,j.optString("type","movie"));if(m!=null)list.add(m);
            }
        }catch(Exception ignored){}
        return list;
    }
    private boolean isFavorite(CatalogMovie movie){for(CatalogMovie m:favorites())if(m.key().equals(movie.key()))return true;return false;}
    private void toggleFavorite(CatalogMovie movie){
        List<CatalogMovie> list=favorites();boolean exists=false;
        for(CatalogMovie m:list)if(m.key().equals(movie.key())){exists=true;break;}
        list.removeIf(m->m.key().equals(movie.key()));if(!exists)list.add(0,movie);
        JSONArray arr=new JSONArray();
        for(int i=0;i<Math.min(100,list.size());i++){
            CatalogMovie m=list.get(i);JSONObject j=new JSONObject();
            try{j.put("id",m.id);j.put("type",m.type);j.put("name",m.title);
                j.put("poster",m.poster);j.put("background",m.background);
                j.put("description",m.description);j.put("releaseInfo",m.year);
                JSONArray g=new JSONArray();
                if(m.genres!=null&&!m.genres.isEmpty())for(String part:m.genres.split(" · "))g.put(part);
                j.put("genres",g);
                arr.put(j);
            }catch(Exception ignored){}
        }
        prefs.edit().putString("faves",arr.toString()).apply();
    }
    private void play(String title,String url,String source){
        if(!StreamDetector.isDirectMedia(url)){toast("لینک ویدئویی مستقیم معتبر نیست");return;}
        Intent intent=new Intent(this,PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_URL,url);
        intent.putExtra(PlayerActivity.EXTRA_TITLE,title);
        intent.putExtra(PlayerActivity.EXTRA_PAGE_URL,source);
        startActivity(intent);
    }
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private void goBackTo(String destination){
        if("home".equals(destination)) home();
        else if("search".equals(destination)) search(searchTerm);
        else category(destination);
    }
    @Override public void onBackPressed(){
        if("detail".equals(mode)){
            goBackTo(returnMode);
        }else if(!"home".equals(mode))home();else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        destroyed=true;generation++;
        catalog.close();images.close();super.onDestroy();
    }
}
