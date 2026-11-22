package com.pooyan.namakadetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit family-wide document export/import; no automatic cloud upload. */
public final class BackupActivity extends Activity {
    private static final int EXPORT=410,IMPORT=411;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private TextView status;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(28,28,28,20);layout.setBackgroundColor(0xff0B0D14);
        TextView title=new TextView(this);title.setText("Pooyan TV · پشتیبان‌گیری خانوادگی");title.setTextColor(-1);title.setTextSize(24);layout.addView(title);
        status=new TextView(this);status.setText("پروفایل‌ها، علاقه‌مندی‌ها، پیشرفت تماشا و تنظیمات مجاز ذخیره می‌شوند. کلیدها، توکن‌ها و لینک‌های ویدئویی ذخیره نمی‌شوند.");status.setTextSize(16);status.setTextColor(0xffdddddd);layout.addView(status);
        add(layout,"ذخیرهٔ پشتیبان همهٔ پروفایل‌ها",()->pick(false));
        add(layout,"بازیابی پشتیبان روی نصب تازه",()->new AlertDialog.Builder(this).setTitle("تأیید بازیابی")
            .setMessage("بازیابی فقط روی نصب تازهٔ بدون سابقه انجام می‌شود و اطلاعات موجود را جایگزین نمی‌کند. فایل را از منبع مطمئن انتخاب کن.")
            .setNegativeButton("لغو",null).setPositiveButton("انتخاب فایل",(d,w)->pick(true)).show());
        add(layout,"بازگشت",this::finish);setContentView(layout);
    }
    private void add(LinearLayout layout,String text,Runnable click){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->click.run());layout.addView(b);}
    private void info(String text){runOnUiThread(()->{if(!isFinishing())status.setText(text);});}
    private void pick(boolean importFile){
        Intent i=new Intent(importFile?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");
        if(!importFile)i.putExtra(Intent.EXTRA_TITLE,"Pooyan-TV-Family-Backup.json");
        try{startActivityForResult(i,importFile?IMPORT:EXPORT);}catch(Exception ex){info("فایل‌منیجر سیستم روی دستگاه قابل استفاده نیست.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null||request!=EXPORT&&request!=IMPORT)return;
        Uri uri=data.getData();info("در حال پردازش فایل…");
        io.execute(()->{
            try{
                if(request==EXPORT){byte[] json=FamilyBackup.exportAll(this).getBytes(StandardCharsets.UTF_8);
                    try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IllegalStateException("فایل قابل نوشتن نیست");out.write(json);}
                    info("پشتیبان خانواده با موفقیت ذخیره شد.");
                }else{
                    ByteArrayOutputStream buf=new ByteArrayOutputStream();
                    try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IllegalStateException("فایل قابل خواندن نیست");
                        byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(buf.size()+n>600000)throw new IllegalStateException("فایل بیش از حد بزرگ است");buf.write(b,0,n);}}
                    int count=FamilyBackup.importAll(this,buf.toString("UTF-8"));info(count+" پروفایل بازیابی شد. برنامه را دوباره باز کن.");
                }
            }catch(Exception ex){info("عملیات انجام نشد: "+ex.getMessage());}
        });
    }
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
