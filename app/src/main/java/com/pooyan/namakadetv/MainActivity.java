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

/** Pooyan TV FarsiPlex Edition: a true, remote-driven Android TV UI (not a WebView). */
public final class MainActivity extends Activity {
    private static final int BG=0xFF080C15, PANEL=0xFF152133, TEAL=0xFF26D0C0, WHITE=0xFFFAFAFC, MUTED=0xFFA6B8CC;
    private FrameLayout root;
    private LinearLayout content;
    private TextView status;
    private FarsiPlexRepository source;
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
        source=new FarsiPlexRepository(this);images=new CatalogImages(this);
        streams=new LocalStreams(this);aiResolver=new AiResolverClient(this);
        preferences=getSharedPreferences("pooyan_farsiplex_v04",MODE_PRIVATE);
        home();
    }
    private int dp(float v){return TvUi.dp(this,v);}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private GradientDrawable shape(int color,int radius,boolean focus){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(focus?3:1),focus?TEAL:0xFF2A4054);return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=TvUi.text(this,s,size,color,bold);v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        v.setMaxLines(5);return v;
    }
    private TextView button(String title,boolean selected,Runnable onClick){
        TextView v=text(title,15,WHITE,true);
        v.setGravity(Gravity.CENTER);v.setPadding(dp(11),dp(8),dp(11),dp(8));
        v.setFocusable(true);v.setClickable(true);v.setBackground(shape(selected?0xFF117D79:PANEL,12,false));
        v.setOnFocusChangeListener((view,focused)->{
            v.setBackground(shape(focused?TEAL:(selected?0xFF117D79:PANEL),12,focused));
            v.setTextColor(focused?BG:WHITE);
            v.setScaleX(focused?1.04f:1);v.setScaleY(focused?1.04f:1);
        });
        v.setOnClickListener(view->onClick.run());return v;
    }
    private void gap(LinearLayout l,int width){l.addView(new View(this),lp(width,1));}
    private void begin(String destination){
        screen=destination;generation++;root.removeAllViews();
        LinearLayout outer=vertical();root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout navigation=horizontal();navigation.setPadding(dp(32),dp(12),dp(32),dp(10));
        TextView brand=text("◈  POOYAN TV",23,WHITE,true);brand.setLetterSpacing(.055f);
        navigation.addView(brand,lp(251,47));
        navigation.addView(button("خانه",destination.equals("home"),this::home),lp(95,45));gap(navigation,9);
        navigation.addView(button("فیلم‌ها",destination.equals("movies"),()->browse("movie")),lp(107,45));gap(navigation,9);
        navigation.addView(button("سریال‌ها",destination.equals("series"),()->browse("series")),lp(111,45));gap(navigation,9);
        navigation.addView(button("علاقه‌مندی",destination.equals("favorites"),this::favorites),lp(142,45));
        navigation.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        navigation.addView(button("⚙  AI",false,()->configureAI(null)),lp(95,45));gap(navigation,9);
        navigation.addView(button("⌕  جست‌وجو",destination.equals("search"),this::askSearch),lp(146,45));
        outer.addView(navigation,lp(-1,73));
        View divider=new View(this);divider.setBackgroundColor(0xFF243246);outer.addView(divider,lp(-1,1));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        content=vertical();content.setPadding(dp(40),dp(14),dp(36),dp(40));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=text("Pooyan TV · FarsiPlex Edition · آرشیو عمومی؛ پخش نیازمند منبع مجاز است",12,MUTED,false);
        status.setPadding(dp(42),dp(10),0,dp(7));outer.addView(status,lp(-1,39));
    }
    private void info(String value){if(status!=null)status.setText(value);}
    private void title(String heading,String sub){
        LinearLayout section=vertical();
        section.addView(text(heading,23,WHITE,true),lp(-1,38));
        if(sub!=null&&!sub.isEmpty())section.addView(text(sub,13,MUTED,false),lp(-1,28));
        LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(23);p.bottomMargin=dp(8);
        content.addView(section,p);
    }
    private void home(){
        parentSeries=null;begin("home");
        LinearLayout hero=vertical();hero.setPadding(dp(30),dp(24),dp(30),dp(23));
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF123F44,0xFF16233B,0xFF101620});
        background.setCornerRadius(dp(23));hero.setBackground(background);
        hero.addView(text("POOYAN TV  ·  FARSIPLEX EDITION  ·  0.4.2",13,0xFF7FEEE1,true),lp(-1,27));
        hero.addView(text("سینمای ایران، روی تلویزیون تو",32,WHITE,true),lp(-1,60));
        hero.addView(text("فیلم‌ها و سریال‌های FarsiPlex در رابط بومی تلویزیون؛ بدون WebView و بدون تبلیغات داخل برنامه.",16,0xFFCCDFE6,false),lp(-1,57));
        LinearLayout actions=horizontal();actions.addView(button("◈  تماشای فیلم‌ها",true,()->browse("movie")),lp(201,52));gap(actions,12);
        actions.addView(button("▣  سریال‌ها و قسمت‌ها",false,()->browse("series")),lp(235,52));
        hero.addView(actions,lp(-1,57));
        content.addView(hero,lp(-1,248));
        title("تازه‌ترین فیلم‌ها","کاتالوگ زندهٔ FarsiPlex · بدون تضمین لینک پخش");
        rail("movie");
        title("تازه‌ترین سریال‌ها","فصل‌ها و قسمت‌ها پس از انتخاب عنوان دریافت می‌شوند");
        rail("series");
        title("شفافیت پخش","لینک مستقیم شخصی یا لینک عمومی بررسی‌شده با موتور اختیاری DeepSeek.");
        TextView note=text("وب‌سایت FarsiPlex فعلاً API رسمی پخش قابل تأیید برای Pooyan TV ارائه نکرده است. موتور DeepSeek اختیاری است و فقط منابع عمومی را جست‌وجو می‌کند؛ لینک‌هایی که پیدا نشوند همچنان قابل پخش نخواهند بود.",15,MUTED,false);
        content.addView(note,lp(-1,-2));
    }
    private void rail(String kind){
        HorizontalScrollView slider=new HorizontalScrollView(this);slider.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);slider.addView(row);
        slider.setFocusable(false);
        row.addView(text("در حال دریافت اطلاعات…",15,MUTED,false),lp(360,90));
        content.addView(slider,lp(-1,296));
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
            }else{
                row.addView(text("فهرست دریافت نشد. اینترنت، DNS یا محدودیت دسترسی سایت را بررسی کن.",15,MUTED,false),lp(750,85));
            }
            if(error!=null)info(error);
        });
    }
    private void browse(String kind){
        parentSeries=null;section=kind;page=1;fetching=false;hasMore=true;seen.clear();
        begin("movie".equals(kind)?"movies":"series");
        title("movie".equals(kind)?"فیلم‌های FarsiPlex":"سریال‌های FarsiPlex","فقط اطلاعات عمومی سایت، بدون وب‌گردی و با کنترل ریموت");
        listingContainer=vertical();content.addView(listingContainer,lp(-1,-2));
        nextPage=button("نمایش عنوان‌های بیشتر",true,this::fetchNextPage);
        LinearLayout.LayoutParams np=lp(272,53);np.topMargin=dp(16);content.addView(nextPage,np);
        fetchNextPage();
    }
    private int columns(){
        float width=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;
        return Math.max(3,(int)((width-100)/184));
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
                    LinearLayout.LayoutParams rp=lp(-1,292);rp.bottomMargin=dp(14);
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
            info("FarsiPlex · "+seen.size()+" عنوان · قابل پخش بودن مستقل از کاتالوگ است");
        });
    }
    private void card(LinearLayout row,FarsiPlexTitle item){
        LinearLayout cell=vertical();cell.setPadding(dp(6),dp(6),dp(6),dp(6));
        cell.setBackground(shape(0xFF111C2D,11,false));cell.setFocusable(true);cell.setClickable(true);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(shape(0xFF203349,9,false));cell.addView(poster,lp(146,197));
        if(!item.poster.isEmpty())images.load(poster,item.poster);
        TextView name=text(item.name,14,WHITE,true);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
        cell.addView(name,lp(-1,27));
        boolean playable=streams.get("farsiplex:"+item.url)!=null;
        cell.addView(text(playable?"▶  منبع شخصی آماده":(item.year.isEmpty()?"اطلاعات فیلم":item.year),11,playable?TEAL:MUTED,false),lp(-1,24));
        cell.setOnFocusChangeListener((v,focused)->{
            cell.setBackground(shape(focused?0xFF17434B:0xFF111C2D,11,focused));
            cell.setScaleX(focused?1.04f:1);cell.setScaleY(focused?1.04f:1);
            // Never reorder poster siblings on focus: D-pad indices must remain stable.
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
        LinearLayout.LayoutParams p=lp(172,274);p.rightMargin=dp(13);row.addView(cell,p);
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

    private View returnView,returnFocus;
    private LinearLayout returnContent;
    private TextView returnStatus;
    private String returnScreen;
    private void detail(FarsiPlexTitle item){
        if(!"detail".equals(screen)){
            returnView=root.getChildAt(0); returnFocus=getCurrentFocus();
            returnContent=content; returnStatus=status; returnScreen=screen;
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
        poster.setBackground(shape(PANEL,13,false));presentation.addView(poster,lp(260,365));
        if(!item.poster.isEmpty())images.load(poster,item.poster);
        gap(presentation,31);
        LinearLayout info=vertical();presentation.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(text(("movie".equals(item.kind)?"فیلم":"series".equals(item.kind)?"سریال":"قسمت سریال")+"  ·  "+item.year,15,TEAL,true),lp(-1,39));
        info.addView(text(item.name,34,WHITE,true),lp(-1,-2));
        if(!item.genre.isEmpty())info.addView(text(item.genre,14,MUTED,false),lp(-1,34));
        TextView summary=text(item.description.isEmpty()?"اطلاعات تکمیلی در حال دریافت است…":item.description,16,0xFFDAE6EF,false);
        summary.setMaxLines(8);LinearLayout.LayoutParams sp=lp(-1,-2);sp.topMargin=dp(17);info.addView(summary,sp);
        TextView rights=text("داده‌های عمومی: FarsiPlex · صرفاً فهرست، نه مجوز پخش محتوا",12,0xFFF2CA8F,false);
        LinearLayout.LayoutParams rp=lp(-1,31);rp.topMargin=dp(14);info.addView(rights,rp);
        LinearLayout controls=horizontal();LinearLayout.LayoutParams cp=lp(-1,62);cp.topMargin=dp(8);info.addView(controls,cp);
        String key="farsiplex:"+item.url;
        String custom=streams.get(key);
        String url=custom!=null?custom:item.publicStream;
        if(url!=null && !url.isEmpty())controls.addView(button("▶  پخش",true,()->play(item.name,url)),lp(145,53));
        else controls.addView(button("+  افزودن لینک مجاز",true,()->addStream(item)),lp(206,53));
        if(url==null || url.isEmpty()){
            TextView noMedia=text(complete?"در صفحهٔ عمومی این عنوان لینک مستقیم قابل‌پخش پیدا نشد؛ اتصال رسمی به منبع پخش هنوز فراهم نیست.":"در حال بررسی امکان پخش…",13,0xFFF2CA8F,false);
            LinearLayout.LayoutParams nm=lp(-1,-2);nm.topMargin=dp(6);info.addView(noMedia,nm);
        }
        // AI search is opt-in and uses a separately deployed Worker. No API key in Android.
        LinearLayout aiActions=horizontal();
        LinearLayout.LayoutParams aiLayout=lp(-1,60);aiLayout.topMargin=dp(11);info.addView(aiActions,aiLayout);
        aiActions.addView(button("✨  بررسی پخش با DeepSeek",false,()->resolveWithAI(item)),lp(280,50));
        gap(controls,9);
        controls.addView(button(isFavorite(item)?"♥ حذف علاقه‌مندی":"♡ علاقه‌مندی",false,()->{
            toggleFavorite(item);showDetail(item,complete);
        }),lp(183,53));
        gap(controls,9);
        controls.addView(button("بازگشت",false,this::backToList),lp(112,53));
        content.addView(presentation,lp(-1,-2));
        if(controls.getChildCount()>0)controls.getChildAt(0).requestFocus();
        if("series".equals(item.kind)){
            title("قسمت‌های سریال",complete?"هر قسمت منبع پخش جداگانه دارد":"در حال دریافت فهرست قسمت‌ها…");
            if(complete && item.episodes.isEmpty())content.addView(text("فهرست قسمت‌ها در صفحهٔ عمومی دریافت نشد.",15,MUTED,false),lp(-1,50));
            for(FarsiPlexTitle ep:item.episodes){
                String epKey="farsiplex:"+ep.url;
                String stream=streams.get(epKey);
                TextView b=button((stream==null?"+  ":"▶  ")+ep.name,false,()->{
                    parentSeries=item;detail(ep);
                });
                LinearLayout.LayoutParams epLp=lp(-1,54);epLp.bottomMargin=dp(8);content.addView(b,epLp);
            }
        }
        info("FarsiPlex · "+item.url+" · "+(complete?"اطلاعات دریافت شد":"در حال دریافت جزئیات…"));
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
            if(!new ResolverSecretStore(preferences).save(token)){Toast.makeText(this,"ذخیرهٔ امن توکن انجام نشد",Toast.LENGTH_LONG).show();return;}
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
            .setMessage("تنها لینک مستقیم HTTPS متعلق به خودت یا دارای اجازهٔ استفاده را وارد کن. برنامه محتوای FarsiPlex را استخراج یا قفل اشتراک را دور نمی‌زند.")
            .setView(field).setNegativeButton("لغو",null).setPositiveButton("ذخیره",(d,w)->{
                if(!streams.put("farsiplex:"+item.url,item.name,field.getText().toString().trim())){
                    Toast.makeText(this,"لینک مستقیم معتبر MP4/HLS/DASH لازم است",Toast.LENGTH_LONG).show();return;
                }
                showDetail(item,true);
            }).show();
    }
    private void play(String title,String url){
        if(!StreamDetector.isDirectMedia(url)){Toast.makeText(this,"منبع پخش معتبر نیست",Toast.LENGTH_LONG).show();return;}
        Intent intent=new Intent(this,PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_TITLE,title);intent.putExtra(PlayerActivity.EXTRA_URL,url);
        startActivity(intent);
    }
    private void askSearch(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(WHITE);input.setHintTextColor(MUTED);
        input.setText(searchTerm);input.setHint("نام فیلم یا سریال…");input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("جست‌وجو در FarsiPlex")
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
        begin("search");title("جست‌وجو: "+searchTerm,"جست‌وجوی عمومی خود FarsiPlex");
        LinearLayout results=vertical();content.addView(results,lp(-1,-2));
        TextView wait=text("در حال جست‌وجوی فیلم و سریال…",15,MUTED,false);results.addView(wait,lp(-1,70));
        String q=normalizeSearch(searchTerm);int token=generation;
        List<FarsiPlexTitle> found=new ArrayList<>();Set<String> urls=new HashSet<>();
        source.search(searchTerm,(items,error)->{
            if(destroyed||generation!=token)return;
            results.removeAllViews();
            if(error!=null){results.addView(text(error,15,MUTED,false),lp(-1,76));info(error);return;}
            if(items.isEmpty())results.addView(text("عنوانی در جست‌وجوی سایت پیدا نشد.",15,MUTED,false),lp(-1,76));
            else showGrid(results,items);
            info("نتایج جست‌وجوی FarsiPlex: "+items.size());
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
            if(i%perRow==0){row=horizontal();row.setGravity(Gravity.TOP);container.addView(row,lp(-1,289));}
            card(row,items.get(i));
        }
    }
    private List<FarsiPlexTitle> favoriteItems(){
        List<FarsiPlexTitle> result=new ArrayList<>();
        try{JSONArray json=new JSONArray(preferences.getString("favorites","[]"));
            for(int i=0;i<Math.min(json.length(),150);i++){
                JSONObject j=json.optJSONObject(i);if(j==null)continue;
                String url=FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,j.optString("url"));
                String kind=FarsiPlexUrlPolicy.mediaType(url);if(kind.isEmpty())continue;
                FarsiPlexTitle item=new FarsiPlexTitle(url,kind,j.optString("title"));
                item.poster=FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,j.optString("poster"));
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
            value.put("url",t.url);value.put("title",t.name);value.put("poster",t.poster);json.put(value);
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




