package com.pooyan.namakadetv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public final class TvUi {
    public static final int BG = Color.rgb(9, 13, 22);
    public static final int BLUE = Color.rgb(72, 145, 252);
    public static final int TEXT = Color.WHITE;
    public static final int MUTED = Color.rgb(169, 182, 206);
    private TvUi() {}

    public static int dp(Context c, float n) { return Math.round(n * c.getResources().getDisplayMetrics().density); }

    public static GradientDrawable bg(int color, int radius, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(c, radius));
        return d;
    }

    public static TextView text(Context c, String t, int size, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(t); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    /** Consume paired OK events once, including gamepad Select, without key-repeat clicks. */
    public static boolean activate(View view,int key,android.view.KeyEvent event){
        if(key!=android.view.KeyEvent.KEYCODE_DPAD_CENTER&&key!=android.view.KeyEvent.KEYCODE_ENTER
            &&key!=android.view.KeyEvent.KEYCODE_NUMPAD_ENTER&&key!=android.view.KeyEvent.KEYCODE_BUTTON_A)return false;
        if(event.getAction()==android.view.KeyEvent.ACTION_UP&&!event.isCanceled())view.performClick();
        return true;
    }
    public static void remoteClick(View view){view.setOnKeyListener((v,k,e)->activate(v,k,e));}
    public static TextView focusButton(Context c, String label, boolean primary, Runnable onClick) {
        TextView v = text(c, label, 16, TEXT, true);
        v.setGravity(Gravity.CENTER);
        int hp = dp(c, 18), vp = dp(c, 10);
        v.setPadding(hp, vp, hp, vp);
        int normal = primary ? 0xFF2456AD : 0xFF242D40;
        v.setBackground(bg(normal, 12, c));
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(view -> onClick.run());
        remoteClick(v);
        v.setOnFocusChangeListener((view, focus) -> {
            v.setBackground(bg(focus ? BLUE : normal, 12, c));
            v.setScaleX(focus ? 1.05f : 1f);
            v.setScaleY(focus ? 1.05f : 1f);
        });
        return v;
    }
}
