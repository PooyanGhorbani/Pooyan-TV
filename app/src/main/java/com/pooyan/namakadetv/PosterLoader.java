package com.pooyan.namakadetv;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small bounded in-memory poster cache. Never forwards credentials or follows redirects. */
public final class PosterLoader {
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(12 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount() / 1024; }
    };
    public void show(Activity activity, ImageView view, String pageUrl, String imageUrl) {
        if (!PosterPolicy.accept(pageUrl, imageUrl)) return;
        view.setTag(imageUrl);
        Bitmap hit = cache.get(imageUrl);
        if (hit != null) { view.setImageBitmap(hit); return; }
        io.execute(() -> {
            Bitmap bmp = fetch(imageUrl);
            if (bmp == null) return;
            cache.put(imageUrl, bmp);
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing() && imageUrl.equals(view.getTag())) view.setImageBitmap(bmp);
            });
        });
    }
    private Bitmap fetch(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(6000); connection.setReadTimeout(6000);
            connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/avif");
            if (connection.getResponseCode() != 200) return null;
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) return null;
            if (connection.getContentLength() > 2_000_000) return null;
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) >= 0) {
                    if (out.size() + n > 2_000_000) return null;
                    out.write(buffer, 0, n);
                }
                byte[] bytes = out.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 640 || options.outHeight / options.inSampleSize > 640)
                    options.inSampleSize *= 2;
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            }
        } catch (Exception e) { return null; }
        finally { if (connection != null) connection.disconnect(); }
    }
    public void close() { io.shutdownNow(); }
}
