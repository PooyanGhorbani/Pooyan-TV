package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pooyan TV: provider-neutral cinematic Android TV interface, remote-first. */
public final class MainActivity extends Activity {
    private static final int BG=0xFF08080B, PANEL=0xFF1B1B20, TEAL=0xFFE50914, WHITE=0xFFF6F6F6, MUTED=0xFFB3B3BB;
    private FrameLayout root;
    private LinearLayout content;
    private TextView status;
    private UnifiedRepository source;
    private FarsiPlexTitle featured;
    private TextView featuredTitle,featuredDescription;
    private ImageView featuredBackdrop;
    private CatalogImages images;
    private LocalStreams streams;
    private AiResolverClient aiResolver;
    private SharedPreferences preferences;
    private boolean destroyed;
    private int generation, page=1;
    private String screen="home", section="movie", searchTerm="";
    private FarsiPlexTitle selected;
    private FarsiPlexTitle parentSeries;
    private final List<FarsiPlexTitle> loadedMovies=new ArrayList<>(), loadedSeries=new ArrayList<>();
    private final Set<String> seen=new HashSet<>();
    private LinearLayout listingContainer;
    private TextView nextPage;
    private boolean fetching, hasMore=true;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root=new FrameLayout(this);root.setBackgroundColor(BG);setContentView(root);
        source=new UnifiedRepository(this);images=new CatalogImages(this);
        streams=new LocalStreams(this);aiResolver=new AiResolverClient(this);
        preferences=getSharedPreferences("pooyan_farsiplex_v04",MODE_PRIVATE);
        home();
    }
    private int dp(float v){return TvUi.dp(this,v);}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private GradientDrawable shape(int color,int radius,boolean focus){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        d.setStroke(dp(focus?2:0),focus?WHITE:color);return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=TvUi.text(this,s,size,color,bold);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        v.setMaxLines(5);return v;
    }
    private TextView button(String title,boolean selected,Runnable onClick){
        TextView v=text(title,15,WHITE,true);
        v.setGravity(Gravity.CENTER);v.setPadding(dp(14),dp(8),dp(14),dp(8));
        int normal=selected?TEAL:PANEL;
        v.setFocusable(true);v.setClickable(true);v.setBackground(shape(normal,7,false));
        v.setOnFocusChangeListener((view,focused)->{
            v.setBackground(shape(focused?WHITE:normal,7,focused));
            v.setTextColor(focused?BG:WHITE);
            v.setScaleX(focused?1.035f:1);v.setScaleY(focused?1.035f:1);
            v.setElevation(focused?dp(10):0);
        });
        v.setOnClickListener(view->onClick.run());return v;
    }
    private void gap(LinearLayout l,int width){l.addView(new View(this),lp(width,1));}
    private void begin(String destination){
        screen=destination;generation++;root.removeAllViews();
        LinearLayout outer=vertical();root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout navigation=horizontal();navigation.setPadding(dp(41),dp(13),dp(32),dp(8));
        navigation.setBackgroundColor(0xFF0B0B0F);
        TextView brand=text("POOYAN",24,TEAL,true);brand.setLetterSpacing(.07f);
        navigation.addView(brand,lp(179,47));
        navigation.addView(button("خانه",destination.equals("home"),this::home),lp(90,41));gap(navigation,7);
        navigation.addView(button("فیلم‌ها",destination.equals("movies"),()->browse("movie")),lp(104,41));gap(navigation,7);
        navigation.addView(button("سریال‌ها",destination.equals("series"),()->browse("series")),lp(113,41));gap(navigation,7);
        navigation.addView(button("لیست من",destination.equals("favorites"),this::favorites),lp(125,41));
        navigation.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        navigation.addView(button("⌕  جست‌وجو",destination.equals("search"),this::askSearch),lp(137,41));gap(navigation,9);
        navigation.addView(button("⚙",false,()->configureAI(null)),lp(53,41));
        outer.addView(navigation,lp(-1,65));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        content=vertical();content.setPadding(dp(40),dp(6),dp(36),dp(35));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=text("",12,MUTED,false);status.setPadding(dp(42),0,0,dp(4));
        status.setVisibility(View.GONE);outer.addView(status,lp(-1,24));
    }
    private void info(String value){
        if(status==null)return;
        String safe=value==null?"":value.replaceAll("(?i)FarsiPlex|Namakade|F2Media", "منبع")
           .replaceAll("https?://[^\\s]+", "");
        status.setText(safe);
        status.setVisibility(safe.trim().isEmpty()?View.GONE:View.VISIBLE);
    }
    private void title(String heading,String sub){
        LinearLayout section=vertical();
        TextView h=text(heading,23,WHITE,true);h.setLetterSpacing(.008f);
        section.addView(h,lp(-1,37));
        if(sub!=null&&!sub.isEmpty())section.addView(text(sub,13,MUTED,false),lp(-1,25));
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(21);p.bottomMargin=dp(7);
        content.addView(section,p);
    }
    private void home(){
        parentSeries=null;begin("home");
        FrameLayout hero=new FrameLayout(this);
        hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF15151B,0xFF242027,0xFF08080B}));
        featuredBackdrop=new ImageView(this);
        featuredBackdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);featuredBackdrop.setAlpha(.47f);
        hero.addView(featuredBackdrop,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF08080B,0xEC08080B,0xA008080B,0x1808080B}));
        hero.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout story=vertical();story.setGravity(Gravity.CENTER_VERTICAL);
        story.setPadding(dp(35),dp(15),dp(22),dp(20));
        FrameLayout.LayoutParams storyParams=new FrameLayout.LayoutParams(dp(680),-1,Gravity.START);
        hero.addView(story,storyParams);
        TextView eyebrow=text("POOYAN TV  •  ORIGINAL CINEMA EXPERIENCE",12,0xFFFF8087,true);
        eyebrow.setLetterSpacing(.06f);story.addView(eyebrow,lp(-1,32));
        featuredTitle=text("یک شب سینمایی، فقط برای تو",37,WHITE,true);
        featuredTitle.setMaxLines(2);story.addView(featuredTitle,lp(-1,100));
        featuredDescription=text("تازه‌ترین فیلم‌ها و سریال‌های ایرانی را کشف کن.",16,0xFFD7D7DD,false);
        featuredDescription.setMaxLines(3);story.addView(featuredDescription,lp(-1,77));
        LinearLayout actions=horizontal();
        actions.addView(button("▶  تماشا",true,()->{
            if(featured!=null)detail(featured);else browse("movie");
        }),lp(161,50));gap(actions,11);
        actions.addView(button("اطلاعات بیشتر",false,()->{
            if(featured!=null)detail(featured);else browse("series");
        }),lp(177,50));
        story.addView(actions,lp(-1,53));
        content.addView(hero,lp(-1,340));
        if(!loadedMovies.isEmpty())updateFeatured(loadedMovies.get(0));
        addContinueWatching();
        title("تازه‌ترین فیلم‌ها","");rail("movie");
        title("سریال‌ها برای تماشای پشت‌سرهم","");rail("series");
    }
    private void updateFeatured(FarsiPlexTitle item){
        if(item==null||featuredTitle==null)return;
        featured=item;featuredTitle.setText(item.name);
        featuredDescription.setText(item.description.isEmpty()?"یک انتخاب تازه از سینمای ایران":item.description);
        if(featuredBackdrop!=null&&!item.poster.isEmpty())images.load(featuredBackdrop,item.poster);
    }
    private void addContinueWatching(){
        List<LibraryStore.Entry> entries=new LibraryStore(this).resumeList();
        if(entries.isEmpty())return;
        title("ادامهٔ تماشا","از همان لحظه‌ای که توقف کردی");
        HorizontalScrollView slider=new HorizontalScrollView(this);slider.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);slider.addView(row);
        int count=0;
        for(LibraryStore.Entry entry:entries){
            if(entry.positionMs<=0 || !StreamDetector.isDirectMedia(entry.url))continue;
            LinearLayout tile=vertical();tile.setPadding(dp(9),dp(9),dp(9),dp(10));
            tile.setBackground(shape(PANEL,10,false));tile.setFocusable(true);
            TextView name=text(entry.title,16,WHITE,true);name.setMaxLines(2);
            tile.addView(name,lp(-1,64));
            long minutes=Math.max(1,entry.positionMs/60000);
            tile.addView(text("▶  ادامه از دقیقهٔ "+minutes,12,0xFFE5BBC0,false),lp(-1,37));
            tile.setOnFocusChangeListener((v,focus)->{
                tile.setBackground(shape(focus?0xFF4A1820:PANEL,10,focus));
                tile.setScaleX(focus?1.03f:1);tile.setScaleY(focus?1.03f:1);
            });
            tile.setOnClickListener(v->play(entry.title,entry.url));
            LinearLayout.LayoutParams params=lp(260,127);params.rightMargin=dp(14);
            row.addView(tile,params);if(++count==12)break;
        }
        if(count>0)content.addView(slider,lp(-1,143));
    }
    private void rail(String kind){
        HorizontalScrollView slider=new HorizontalScrollView(this);slider.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);slider.addView(row);
        slider.setFocusable(false);
        row.addView(text("در حال دریافت اطلاعات…",15,MUTED,false),lp(360,90));
        content.addView(slider,lp(-1,320));
        int token=generation;
        List<FarsiPlexTitle> cache="movie".equals(kind)?loadedMovies:loadedSeries;
        if(!cache.isEmpty()){
            row.removeAllViews();for(int i=0;i<Math.min(cache.size(),18);i++)card(row,cache.get(i));return;
        }
        source.list(kind,1,(list,error)->{
            if(destroyed||generation!=token)return;
            row.removeAllViews();
            if(!list.isEmpty()){
                cache.clear();cache.addAll(list);
                for(int i=0;i<Math.min(list.size(),18);i++)card(row,list.get(i));
                if("movie".equals(kind))updateFeatured(list.get(0));
            }else{
                row.addView(text("فهرست دریافت نشد. اینترنت، DNS یا محدودیت دسترسی سایت را بررسی کن.",15,MUTED,false),lp(750,85));
            }
            if(error!=null)info(error);
        });
    }
    private void browse(String kind){
        parentSeries=null;section=kind;page=1;fetching=false;hasMore=true;seen.clear();
        begin("movie".equals(kind)?"movies":"series");
        title("movie".equals(kind)?"فیلم‌ها":"سریال‌ها","بهترین انتخابت را پیدا کن");
        listingContainer=vertical();content.addView(listingContainer,lp(-1,-2));
        nextPage=button("نمایش عنوان‌های بیشتر",true,this::fetchNextPage);
        LinearLayout.LayoutParams np=lp(272,53);np.topMargin=dp(16);content.addView(nextPage,np);
        fetchNextPage();
    }
    private int columns(){
        float width=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;
        return Math.max(3,(int)((width-100)/200));
    }
    private void fetchNextPage(){
        if(fetching||!hasMore||page>10)return;
        fetching=true;final int p=page;final int token=generation;
        nextPage.setText("در حال بارگذاری…");
        source.list(section,p,(list,error)->{
            if(destroyed||generation!=token)return;
            fetching=false;
            if(error!=null){
                if(p>1 && error.contains("HTTP 404")){
                    hasMore=false;nextPage.setText("پایان فهرست");return;
                }
                nextPage.setText("خطای شبکه؛ دوباره تلاش کن");info(error);return;
            }
            int added=0;int perRow=columns();LinearLayout row=null,firstRow=null;
            for(FarsiPlexTitle item:list){
                if(!seen.add(item.url))continue;
                if(added%perRow==0){
                    row=horizontal();row.setGravity(Gravity.TOP);
                    LinearLayout.LayoutParams rp=lp(-1,316);rp.bottomMargin=dp(13);
                    listingContainer.addView(row,rp);
                    if(firstRow==null)firstRow=row;
                }
                card(row,item);added++;
            }
            page=p+1;hasMore=!list.isEmpty()&&page<=10;
            nextPage.setText(hasMore?"نمایش عنوان‌های بیشتر":"پایان فهرست");
            // Newly loaded posters are immediately navigable using the remote.
            if(firstRow!=null&&firstRow.getChildCount()>0){
                final View first=firstRow.getChildAt(0);
                if(p==1 || nextPage.isFocused())first.post(first::requestFocus);
            }
            info(seen.size()+" عنوان برای تماشا پیدا شد");
        });
    }
    private void card(LinearLayout row,FarsiPlexTitle item){
        LinearLayout cell=vertical();cell.setPadding(dp(6),dp(6),dp(6),dp(6));
        cell.setBackground(shape(0xFF141419,10,false));cell.setFocusable(true);cell.setClickable(true);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(shape(0xFF2A292F,8,false));cell.addView(poster,lp(160,217));
        if(!item.poster.isEmpty())images.load(poster,item.poster);
        TextView name=text(item.name,14,WHITE,true);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
        cell.addView(name,lp(-1,27));
        boolean playable=streams.get("farsiplex:"+item.url)!=null;
        cell.addView(text(playable?"▶  آمادهٔ تماشا":(item.year.isEmpty()?"کشف بیشتر":item.year),11,playable?0xFFFF7474:MUTED,false),lp(-1,24));
        cell.setOnFocusChangeListener((v,focused)->{
            cell.setBackground(shape(focused?0xFF492127:0xFF141419,10,focused));
            cell.setScaleX(focused?1.055f:1);cell.setScaleY(focused?1.055f:1);
            cell.setElevation(focused?dp(14):0);
            if(focused)cell.bringToFront();
        });
        cell.setOnClickListener(v->detail(item));
        // Do not depend on Android's geometry-based focus search through nested
        // ScrollViews: it often jumps from a poster to the top navigation bar.
        cell.setOnKeyListener((v,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN || event.getRepeatCount()>0)return false;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT){
                LinearLayout parent=(LinearLayout)v.getParent();
                int next=TvPosterFocusPolicy.neighbor(parent.indexOfChild(v),parent.getChildCount(),key==KeyEvent.KEYCODE_DPAD_RIGHT);
                if(next>=0&&next<parent.getChildCount())return focusPoster(parent.getChildAt(next));
                return true; // Do not send focus to unrelated top-menu controls.
            }
            if(key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN){
                return movePosterVertical(v,key==KeyEvent.KEYCODE_DPAD_DOWN);
            }
            return false;
        });
        LinearLayout.LayoutParams p=lp(186,302);p.rightMargin=dp(13);row.addView(cell,p);
    }
    /** Explicit TV focus traversal between poster rows, including lazy-loaded pages. */
    private boolean focusPoster(View target){
        if(target==null)return false;
        if(!target.requestFocus())return false;
        target.requestRectangleOnScreen(new android.graphics.Rect(0,0,target.getWidth(),target.getHeight()),false);
        return true;
    }
    private boolean movePosterVertical(View poster,boolean down){
        if(!(poster.getParent() instanceof LinearLayout))return false;
        LinearLayout row=(LinearLayout)poster.getParent();
        int column=row.indexOfChild(poster);
        ViewGroup container=(ViewGroup)row.getParent();
        // Home page: each strip is wrapped by its own HorizontalScrollView.
        if(container instanceof HorizontalScrollView){
            ViewGroup stripParent=(ViewGroup)container.getParent();
            if(stripParent!=null){
                int index=stripParent.indexOfChild(container);
                int step=down?1:-1;
                for(int i=index+step;i>=0&&i<stripParent.getChildCount();i+=step){
                    View candidate=stripParent.getChildAt(i);
                    if(!(candidate instanceof HorizontalScrollView))continue;
                    View view=((HorizontalScrollView)candidate).getChildAt(0);
                    if(view instanceof LinearLayout){
                        LinearLayout other=(LinearLayout)view;
                        if(other.getChildCount()>0 && other.getChildAt(0).isFocusable())
                            return focusPoster(other.getChildAt(TvPosterFocusPolicy.column(column,other.getChildCount())));
                    }
                }
            }
            return false; // Let the framework reach the hero buttons/navigation.
        }
        // Browse/favorites/search: posters are direct children of consecutive rows.
        int rowPosition=container.indexOfChild(row);
        int direction=down?1:-1;
        for(int i=rowPosition+direction;i>=0&&i<container.getChildCount();i+=direction){
            View candidate=container.getChildAt(i);
            if(!(candidate instanceof LinearLayout))continue;
            LinearLayout other=(LinearLayout)candidate;
            if(other.getChildCount()>0 && other.getChildAt(0).isFocusable()){
                return focusPoster(other.getChildAt(TvPosterFocusPolicy.column(column,other.getChildCount())));
            }
        }
        if(down && container==listingContainer && nextPage!=null && nextPage.isShown())
            return nextPage.requestFocus();
        return false;
    }

    private String returnEpisodeUrl="";
    private View returnView,returnFocus;
    private LinearLayout returnContent;
    private TextView returnStatus;
    private String returnScreen;
    private void detail(FarsiPlexTitle item){
        if(!"detail".equals(screen)){
            returnView=root.getChildAt(0);returnFocus=getCurrentFocus();
            returnContent=content;returnStatus=status;returnScreen=screen;
        }
        selected=item;showDetail(item,false);
        int token=generation;
        source.detail(item,(full,error)->{
            if(destroyed||!"detail".equals(screen)||selected==null||!selected.url.equals(item.url)||token!=generation)return;
            selected=full;showDetail(full,true);
            if(error!=null)info(error);
        });
    }
    private void showDetail(FarsiPlexTitle item,boolean complete){
        begin("detail");
        LinearLayout presentation=horizontal();presentation.setGravity(Gravity.TOP);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(shape(PANEL,9,false));presentation.addView(poster,lp(258,363));
        if(!item.poster.isEmpty())images.load(poster,item.poster);
        gap(presentation,31);
        LinearLayout info=vertical();presentation.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(text(("movie".equals(item.kind)?"فیلم":"series".equals(item.kind)?"سریال":"قسمت سریال")+"  ·  "+item.year,15,0xFFFF8288,true),lp(-1,39));
        info.addView(text(item.name,40,WHITE,true),lp(-1,-2));
        if(!item.genre.isEmpty())info.addView(text(item.genre,14,MUTED,false),lp(-1,34));
        TextView summary=text(item.description.isEmpty()?"اطلاعات تکمیلی در حال دریافت است…":item.description,16,0xFFE0E0E3,false);
        summary.setMaxLines(8);LinearLayout.LayoutParams sp=lp(-1,-2);sp.topMargin=dp(17);info.addView(summary,sp);
        TextView rights=text("اطلاعات و دسترسی به محتوا ممکن است با تغییر سرویس‌ها متفاوت باشد.",12,MUTED,false);
        LinearLayout.LayoutParams rp=lp(-1,31);rp.topMargin=dp(14);info.addView(rights,rp);
        LinearLayout controls=horizontal();LinearLayout.LayoutParams cp=lp(-1,62);cp.topMargin=dp(8);info.addView(controls,cp);
        String key="farsiplex:"+item.url;
        String custom=streams.get(key);
        String url=custom!=null?custom:item.publicStream;
        if(url!=null && !url.isEmpty())controls.addView(button("▶  پخش",true,()->play(item.name,url,item.url)),lp(145,53));
        else controls.addView(button("+  افزودن لینک مجاز",true,()->addStream(item)),lp(206,53));
        if(url==null || url.isEmpty()){
            TextView noMedia=text(complete?("series".equals(item.kind)?"یک قسمت را انتخاب کن.":item.playbackError.isEmpty()?"منبع پخش تأیید نشده است؛ بعداً دوباره امتحان کن.":item.playbackError):"در حال بررسی امکان پخش…",13,0xFFF2CA8F,false);
            LinearLayout.LayoutParams nm=lp(-1,-2);nm.topMargin=dp(6);info.addView(noMedia,nm);
        }
        // AI search is opt-in and uses a separately deployed Worker. No API key in Android.
        if((url==null||url.isEmpty())&&NamakadeCatalog.safeUrl(item.url).isEmpty()){
            LinearLayout aiActions=horizontal();
            LinearLayout.LayoutParams aiLayout=lp(-1,60);aiLayout.topMargin=dp(11);info.addView(aiActions,aiLayout);
            aiActions.addView(button("بررسی منبع جایگزین",false,()->resolveWithAI(item)),lp(247,50));
        }
        gap(controls,9);
        controls.addView(button(isFavorite(item)?"♥ حذف علاقه‌مندی":"♡ علاقه‌مندی",false,()->{
            toggleFavorite(item);showDetail(item,complete);
        }),lp(183,53));
        gap(controls,9);
        controls.addView(button("بازگشت",false,this::backToList),lp(112,53));
        content.addView(presentation,lp(-1,-2));
        if(controls.getChildCount()>0)controls.getChildAt(0).requestFocus();
        if("series".equals(item.kind)){
            title("قسمت‌ها",complete?"قسمت موردنظرت را انتخاب کن":"در حال دریافت قسمت‌ها…");
            if(complete && item.episodes.isEmpty())content.addView(text("فهرست قسمت‌ها در صفحهٔ عمومی دریافت نشد.",15,MUTED,false),lp(-1,50));
            for(FarsiPlexTitle ep:item.episodes){
                String epKey="farsiplex:"+ep.url;
                String stream=streams.get(epKey);
                LinearLayout episodeRow=horizontal();episodeRow.setPadding(dp(9),dp(5),dp(12),dp(5));
                episodeRow.setFocusable(true);episodeRow.setClickable(true);
                episodeRow.setBackground(shape(PANEL,8,false));
                ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cover.setBackground(shape(0xFF28282F,7,false));
                episodeRow.addView(cover,lp(105,73));
                if(!ep.poster.isEmpty())images.load(cover,ep.poster);
                gap(episodeRow,17);
                LinearLayout epText=vertical();
                epText.addView(text(ep.name,17,WHITE,true),lp(-1,43));
                epText.addView(text(stream==null?"قسمت را باز کن":"▶  آمادهٔ پخش",12,MUTED,false),lp(-1,27));
                episodeRow.addView(epText,new LinearLayout.LayoutParams(0,-2,1));
                episodeRow.setOnClickListener(v->{returnEpisodeUrl=ep.url;parentSeries=item;detail(ep);});
                episodeRow.setOnFocusChangeListener((v,focused)->{
                    episodeRow.setBackground(shape(focused?0xFF4A2029:PANEL,8,focused));
                    episodeRow.setScaleX(focused?1.012f:1);episodeRow.setScaleY(focused?1.012f:1);
                });
                LinearLayout.LayoutParams epLp=lp(-1,88);epLp.bottomMargin=dp(8);content.addView(episodeRow,epLp);
                if(ep.url.equals(returnEpisodeUrl))episodeRow.requestFocus();
            }
        }
        info(complete?"اطلاعات عنوان آماده است":"در حال دریافت جزئیات…");
    }
    private void backToList(){
        if(selected!=null&&"episode".equals(selected.kind)&&parentSeries!=null){
            FarsiPlexTitle series=parentSeries;parentSeries=null;selected=series;showDetail(series,true);return;
        }
        if(returnView!=null){
            generation++;root.removeAllViews();root.addView(returnView);
            screen=returnScreen;content=returnContent;status=returnStatus;
            View target=returnFocus;returnView=null;
            if(target!=null)target.post(()->focusPoster(target));
        }else if("series".equals(section))browse("series");else browse("movie");
    }
    private void configureAI(Runnable afterSave){
        LinearLayout fields=vertical();fields.setPadding(dp(17),dp(8),dp(17),dp(8));
        TextView explanation=text("آدرس Worker خصوصی خودت را وارد کن. کلید DeepSeek فقط در Cloudflare ذخیره می‌شود، نه در APK.",14,MUTED,false);
        fields.addView(explanation,lp(-1,-2));
        EditText endpoint=new EditText(this);endpoint.setSingleLine(true);
        endpoint.setTextColor(WHITE);endpoint.setHintTextColor(MUTED);
        endpoint.setHint("https://your-worker.workers.dev");
        endpoint.setText(preferences.getString("ai_worker_origin","https://pooyan-tv-resolver.pooyan-ghorbani-real.workers.dev"));
        fields.addView(endpoint,lp(-1,58));
        EditText code=new EditText(this);code.setSingleLine(true);
        code.setTextColor(WHITE);code.setHintTextColor(MUTED);
        code.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        code.setHint("کد اتصال Worker (حداقل ۱۲ کاراکتر)");
        code.setText(new ResolverSecretStore(preferences).get());
        fields.addView(code,lp(-1,58));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("اتصال موتور DeepSeek")
          .setMessage("برای حفظ امنیت، فقط URL سرویس Cloudflare Worker و کد دسترسی اختصاصی خودت را وارد کن.")
          .setView(fields).setNegativeButton("لغو",null).setPositiveButton("ذخیره",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            String origin=AiResolverUrlPolicy.normalize(endpoint.getText().toString());
            String token=code.getText().toString().trim();
            if(origin.isEmpty()||token.length()<12){
                Toast.makeText(this,"URL HTTPS معتبر و کد حداقل ۱۲ کاراکتری لازم است",Toast.LENGTH_LONG).show();return;
            }
            if(!new ResolverSecretStore(preferences).save(token)){
                Toast.makeText(this,"ذخیرهٔ امن کد انجام نشد",Toast.LENGTH_LONG).show();return;
            }
            preferences.edit().putString("ai_worker_origin",origin).apply();
            dialog.dismiss();
            if(afterSave!=null)afterSave.run();
            else Toast.makeText(this,"اتصال ذخیره شد؛ برای آزمون، یک فیلم را انتخاب کن.",Toast.LENGTH_LONG).show();
        }));
        dialog.show();
    }
    private void resolveWithAI(FarsiPlexTitle item){
        String worker=preferences.getString("ai_worker_origin","https://pooyan-tv-resolver.pooyan-ghorbani-real.workers.dev");
        String code=new ResolverSecretStore(preferences).get();
        if(AiResolverUrlPolicy.normalize(worker).isEmpty()||code.length()<12){
            configureAI(()->resolveWithAI(item));return;
        }
        info("در حال بررسی لینک‌های عمومی برای «"+item.name+"» …");
        String originalPage=item.url;
        aiResolver.resolve(worker,code,item.url,item.name,(candidate,message)->{
            if(destroyed||!"detail".equals(screen)||selected==null||!selected.url.equals(originalPage))return;
            if(!candidate.isEmpty()){
                // A server-side HTTP check is not a real playback test. Do not auto-play.
                selected.publicStream=candidate;
                showDetail(selected,true);
                info("منبع بررسی‌شده دریافت شد؛ برای آزمایش واقعی روی ▶ پخش بزن.");
            }else{
                info(message);
                new AlertDialog.Builder(this).setTitle("نتیجهٔ بررسی DeepSeek")
                  .setMessage(message+"\n\nهوش مصنوعی نمی‌تواند لینک قفل‌شده، DRM یا منبعی که در صفحهٔ عمومی وجود ندارد ایجاد کند.")
                  .setPositiveButton("متوجه شدم",null).show();
            }
        });
    }
    private void addStream(FarsiPlexTitle item){
        EditText field=new EditText(this);field.setTextColor(WHITE);field.setHintTextColor(MUTED);
        field.setHint("https://…/video.m3u8 یا video.mp4");
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle("منبع پخش مجاز برای «"+item.name+"»")
            .setMessage("تنها لینک HTTPS با اجازهٔ استفاده را وارد کن. برنامه به محتوای قفل‌شده یا اشتراکی دسترسی غیرمجاز نمی‌گیرد.")
            .setView(field).setNegativeButton("لغو",null).setPositiveButton("ذخیره",(d,w)->{
                if(!streams.put("farsiplex:"+item.url,item.name,field.getText().toString().trim())){
                    Toast.makeText(this,"لینک مستقیم معتبر MP4/HLS/DASH لازم است",Toast.LENGTH_LONG).show();return;
                }
                showDetail(item,true);
            }).show();
    }
    private void play(String title,String url){play(title,url,"");}
    private void play(String title,String url,String page){
        if(!StreamDetector.isDirectMedia(url)){Toast.makeText(this,"منبع پخش معتبر نیست",Toast.LENGTH_LONG).show();return;}
        Intent intent=new Intent(this,PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TITLE,title);intent.putExtra(PlayerActivity.EXTRA_URL,url);
        if(StreamDetector.isHttpsUrl(page))intent.putExtra(PlayerActivity.EXTRA_PAGE_URL,page);
        startActivity(intent);
    }
    private void askSearch(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);
        input.setText(searchTerm);input.setHint("نام فیلم یا سریال…");input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("جست‌وجوی فیلم و سریال")
            .setView(input).setNegativeButton("لغو",null).setPositiveButton("جست‌وجو",(d,w)->search(input.getText().toString())).create();
        input.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_SEARCH||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){
                String query=input.getText().toString();dialog.dismiss();search(query);return true;
            }return false;
        });
        dialog.show();input.requestFocus();
        input.postDelayed(()->((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .showSoftInput(input,InputMethodManager.SHOW_IMPLICIT),220);
    }
    private void search(String query){
        searchTerm=query==null?"":query.trim();if(searchTerm.isEmpty())return;
        if(searchTerm.length()>100)searchTerm=searchTerm.substring(0,100);
        begin("search");title("نتایج برای «"+searchTerm+"»","جست‌وجوی هم‌زمان بین منابع موجود");
        LinearLayout results=vertical();content.addView(results,lp(-1,-2));
        results.addView(text("در حال جست‌وجو…",15,MUTED,false),lp(-1,75));
        int token=generation;
        source.search(searchTerm,(items,error)->{
            if(destroyed||generation!=token)return;
            results.removeAllViews();
            if(items.isEmpty())results.addView(text(error==null?"عنوانی پیدا نشد.":error,15,MUTED,false),lp(-1,75));
            else showGrid(results,items);
            info(items.size()+" نتیجه پیدا شد");
        });
    }
    private void scanSearch(String kind,int p,int token,String q,List<FarsiPlexTitle> found,Set<String> urls,LinearLayout result,Runnable finished){
        if(destroyed||token!=generation)return;
        if(p>3){finished.run();return;} // bounded search to keep the site load modest; use browse for deeper pages
        source.list(kind,p,(items,error)->{
            if(destroyed||generation!=token)return;
            for(FarsiPlexTitle item:items){
                if(normalizeSearch(item.name).contains(q)&&urls.add(item.url))found.add(item);
            }
            if(items.isEmpty()||error!=null)finished.run();
            else scanSearch(kind,p+1,token,q,found,urls,result,finished);
        });
    }
    private static String normalizeSearch(String v){
        return v.toLowerCase(java.util.Locale.ROOT).replace('ي','ی').replace('ك','ک').trim();
    }
    private void showGrid(LinearLayout container,List<FarsiPlexTitle> items){
        LinearLayout row=null;int perRow=columns();
        for(int i=0;i<items.size();i++){
            if(i%perRow==0){row=horizontal();row.setGravity(Gravity.TOP);container.addView(row,lp(-1,318));}
            card(row,items.get(i));
        }
    }
    private List<FarsiPlexTitle> favoriteItems(){
        List<FarsiPlexTitle> result=new ArrayList<>();
        try{JSONArray json=new JSONArray(preferences.getString("favorites","[]"));
            for(int i=0;i<Math.min(json.length(),150);i++){
                JSONObject j=json.optJSONObject(i);if(j==null)continue;
                String raw=j.optString("url","");
                String url=NamakadeCatalog.safeUrl(raw);
                String kind=j.optString("kind","");
                if(url.isEmpty()){
                    url=FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,raw);
                    kind=FarsiPlexUrlPolicy.mediaType(url);
                }else if(kind.isEmpty())kind=url.contains("/movies/")?"movie":"series";
                if(url.isEmpty()||kind.isEmpty())continue;
                FarsiPlexTitle item=new FarsiPlexTitle(url,kind,j.optString("title"));
                String poster=j.optString("poster","");
                if(poster.startsWith("https://"))item.poster=poster;
                result.add(item);
            }
        }catch(Exception ignored){}
        return result;
    }
    private boolean isFavorite(FarsiPlexTitle item){for(FarsiPlexTitle f:favoriteItems())if(f.url.equals(item.url))return true;return false;}
    private void toggleFavorite(FarsiPlexTitle item){
        List<FarsiPlexTitle> f=favoriteItems();boolean removed=f.removeIf(e->e.url.equals(item.url));
        if(!removed)f.add(0,item);
        JSONArray json=new JSONArray();
        try{for(int i=0;i<Math.min(f.size(),100);i++){
            FarsiPlexTitle t=f.get(i);JSONObject value=new JSONObject();
            value.put("url",t.url);value.put("title",t.name);value.put("poster",t.poster);value.put("kind",t.kind);json.put(value);
        }}catch(Exception ignored){}
        preferences.edit().putString("favorites",json.toString()).apply();
    }
    private void favorites(){
        begin("favorites");title("علاقه‌مندی‌های من","ذخیره‌شده روی همین تلویزیون، بدون حساب کاربری");
        List<FarsiPlexTitle> list=favoriteItems();
        if(list.isEmpty())content.addView(text("هنوز عنوانی را ذخیره نکرده‌ای.",16,MUTED,false),lp(-1,73));
        else showGrid(content,list);
    }
    @Override public void onBackPressed(){
        if("detail".equals(screen))backToList();
        else if(!"home".equals(screen))home();
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        destroyed=true;generation++;source.close();images.close();aiResolver.close();super.onDestroy();
    }
}
