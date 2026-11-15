package com.pooyan.namakadetv;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import javax.net.ssl.SSLException;

/** Checks response headers only: reachability is NOT evidence that films will play. */
public final class SourceDiagnostics {
    private SourceDiagnostics() {}

    public static String category(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "پاسخ HTTP " + statusCode + " (دسترسی اولیه)";
        if (statusCode >= 300 && statusCode < 400) return "انتقال HTTP " + statusCode + " (نیازمند بررسی)";
        if (statusCode == 401 || statusCode == 403) return "محدودیت دسترسی HTTP " + statusCode;
        if (statusCode == 404) return "صفحه پیدا نشد (404)";
        if (statusCode >= 500) return "خطای سمت سایت HTTP " + statusCode;
        return "پاسخ HTTP " + statusCode + " (بررسی بیشتر)";
    }

    public static String check(SiteCatalog.Site site) {
        // Never probe a private bookmarked page or a URL containing access tokens.
        if (site == null || !SiteCatalog.matches(site, site.url)) return "نشانی امن نیست";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(site.url).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "text/html");
            return category(connection.getResponseCode());
        } catch (UnknownHostException e) {
            return "DNS / نام دامنه در دسترس نیست";
        } catch (SocketTimeoutException e) {
            return "مهلت اتصال به پایان رسید";
        } catch (SSLException e) {
            return "مشکل گواهی SSL / TLS";
        } catch (Exception e) {
            return "خطای شبکه (" + e.getClass().getSimpleName() + ")";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
