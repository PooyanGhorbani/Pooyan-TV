package com.pooyan.namakadetv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public final class TvUi {
    public static final int BG = 0xFF0B0D14;
    public static final int BLUE = 0xFF5B8CFF;
    public static final int TEXT = Color.WHITE;
    public static final int MUTED = 0xFF9AA4BA;
    /** Visual-only design tokens shared by Home, Details and Player. */
    public static final int SURFACE = 0xFF141824;
    public static final int SURFACE_RAISED = 0xFF1E2433;
    public static final int ACCENT = 0xFF7C5CFF;
    public static final int ACCENT_SOFT = 0xFF2A2250;
    public static final int FOCUS_RING = 0xFFFFFFFF;
    public static final int SCRIM = 0xCC0B0D14;
    public static final int ACCENT_LIGHT = 0xFFB9A8FF;
    public static final int BORDER = 0xFF2A3248;
    public static final int AMBER = 0xFFFFC857;
    public static final int SUCCESS = 0xFF7EE0B0;
    public static final int DANGER = 0xFFFF6B7A;
    public static final int RADIUS_PILL = 100;
    public static final int RADIUS_CARD = 16;
    private TvUi() {}
    private static final java.util.Map<View,RemoteInputPolicy> selectStates=new java.util.WeakHashMap<>();

    /** Reduced-motion: honour the system "remove animations" switch (animator scale 0). */
    public static boolean reducedMotion(Context c) {
        try {
            return android.provider.Settings.Global.getFloat(c.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) { return false; }
    }

    /** Scale used for focused items; 1 when reduced motion is on. */
    public static float focusScale(Context c, float scale) { return reducedMotion(c) ? 1f : scale; }

    /** Focus ring colour that stays visible on the given fill: white on dark, accent on light. */
    public static int ringColorFor(int fill) {
        int r = Color.red(fill), g = Color.green(fill), b = Color.blue(fill);
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255d;
        return luminance > 0.6 ? ACCENT : FOCUS_RING;
    }

    /** Rounded surface with an optional high-visibility focus ring (not colour change alone). */
    public static GradientDrawable ring(int fill, int radius, boolean focused, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp(c, radius));
        if (focused) d.setStroke(dp(c, 3), ringColorFor(fill));
        return d;
    }

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
        RemoteInputPolicy state=selectStates.get(view);
        if(state==null){state=new RemoteInputPolicy();selectStates.put(view,state);}
        if(state.select(event.getAction()==android.view.KeyEvent.ACTION_DOWN,event.getAction()==android.view.KeyEvent.ACTION_UP,event.isCanceled(),event.getRepeatCount()))view.performClick();
        return true;
    }
    public static void remoteClick(View view){view.setOnKeyListener((v,k,e)->activate(v,k,e));}
    public static TextView focusButton(Context c, String label, boolean primary, Runnable onClick) {
        TextView v = text(c, label, 16, TEXT, true);
        v.setGravity(Gravity.CENTER);
        int hp = dp(c, 20), vp = dp(c, 10);
        v.setPadding(hp, vp, hp, vp);
        int normal = primary ? ACCENT : 0xB31E2433;
        v.setBackground(ring(normal, RADIUS_PILL, false, c));
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(view -> onClick.run());
        remoteClick(v);
        v.setOnFocusChangeListener((view, focus) -> {
            v.setBackground(ring(focus ? FOCUS_RING : normal, RADIUS_PILL, focus, c));
            v.setTextColor(focus ? BG : TEXT);
            float scale = focusScale(c, focus ? 1.06f : 1f);
            v.setScaleX(scale);
            v.setScaleY(scale);
        });
        return v;
    }
}
