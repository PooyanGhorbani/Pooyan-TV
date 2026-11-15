package com.pooyan.namakadetv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Draws a lightweight non-clickable cursor over the WebView. */
public final class RemoteCursorView extends View {
    private final Paint outer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float x = 200f;
    private float y = 200f;

    public RemoteCursorView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        shadow.setColor(0x99000000);
        shadow.setStrokeWidth(dp(5));
        shadow.setStyle(Paint.Style.STROKE);
        outer.setColor(Color.WHITE);
        outer.setStrokeWidth(dp(3));
        outer.setStyle(Paint.Style.STROKE);
        inner.setColor(0xFF13D2C0);
        inner.setStyle(Paint.Style.FILL);
    }

    private float dp(float number) {
        return number * getResources().getDisplayMetrics().density;
    }

    public void moveTo(float newX, float newY) {
        x = newX;
        y = newY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = dp(14);
        canvas.drawCircle(x + dp(2), y + dp(2), radius + dp(2), shadow);
        canvas.drawCircle(x, y, radius, outer);
        canvas.drawCircle(x, y, dp(5), inner);
        canvas.drawLine(x - dp(24), y, x - dp(18), y, outer);
        canvas.drawLine(x + dp(18), y, x + dp(24), y, outer);
        canvas.drawLine(x, y - dp(24), x, y - dp(18), outer);
        canvas.drawLine(x, y + dp(18), x, y + dp(24), outer);
    }
}
