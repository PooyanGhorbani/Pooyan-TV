package com.pooyan.namakadetv;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The curated first-party website directory. Sites are opened on their own domains. */
public final class SiteCatalog {
    public static final String VERIFIED = "verified";
    public static final String PUBLIC = "public";
    public static final String REVIEW = "review";
    public static final String PERSONAL = "personal";

    public static final class Site {
        public final String id, title, url, host, subtitle, detail, group, status;
        public final int accent;
        private Site(String id, String title, String url, String host,
                     String subtitle, String detail, String group, String status, int accent) {
            this.id=id; this.title=title; this.url=url; this.host=host;
            this.subtitle=subtitle; this.detail=detail; this.group=group;
            this.status=status; this.accent=accent;
        }
    }

    // No access tokens, API keys, personal login links or third-party video files are bundled.
    private static final List<Site> SITES = Collections.unmodifiableList(Arrays.asList(
        new Site("parsimondo", "پارسی‌موندو", "https://parsimondo.de/fa/", "parsimondo.de",
                "فیلم، سریال و کلاسیک‌های ایرانی", "رایگان بدون حساب؛ بعضی ویدئوها با YouTube رسمی پخش می‌شوند",
                VERIFIED, "بدون ثبت‌نام", 0xFF285D8C),
        new Site("babak", "بابک فیلم", "https://babakfilm.com/", "babakfilm.com",
                "آرشیو فارسی و فیلم‌های متنوع", "طبق راهنمای سایت، رایگان و بدون ثبت‌نام؛ مجوز همه آثار تأیید نشده",
                VERIFIED, "بدون ثبت‌نام", 0xFF704C90),
        new Site("telewebion", "تلوبیون", "https://telewebion.ir/", "telewebion.ir",
                "پخش زنده، آرشیو و فیلم و سریال", "بیشتر محتوای عمومی بدون ورود؛ بعضی خدمات حساب می‌خواهند",
                PUBLIC, "دسترسی عمومی", 0xFF176D78),
        new Site("tmk", "تماشاخونه", "https://tmk.ir/", "tmk.ir",
                "فیلم و سریال ایرانی و خارجی", "پخش رایگان برای بسیاری از عناوین؛ ورود ممکن است برای بعضی موارد لازم شود",
                PUBLIC, "رایگانِ محدود", 0xFF8B622D),
        new Site("pardeh", "Pardeh TV", "https://pardehtv.com/movies", "pardehtv.com",
                "فهرست فیلم‌های ایرانی و گزینه‌های تماشا", "گزینه‌های تماشای بدون هزینه؛ پخش هر عنوان و ورود بررسی نشده",
                PUBLIC, "بررسی هر عنوان", 0xFF6656A1),
        new Site("sytaso", "سیتاسو", "https://sytaso.com/", "sytaso.com",
                "فیلم و سریال با دوبله یا زیرنویس", "خود سایت رایگان معرفی شده؛ شرط ورود و دسترسی هنوز تأیید نشده",
                REVIEW, "آزمایشی", 0xFF8A4F62),
        new Site("namakade", "Namakade", "https://namakade.com/", "namakade.com",
                "سایت قبلی تو؛ لینک شخصی قابل تنظیم", "لینک شخصی را یک بار از تنظیمات وارد کن؛ توکن داخل سورس ذخیره نمی‌شود",
                PERSONAL, "لینک شخصی", 0xFF4B6079)
    ));

    private SiteCatalog() {}
    public static List<Site> all() { return SITES; }
    public static Site byId(String id) {
        if (id != null) for (Site s : SITES) if (id.equals(s.id)) return s;
        return SITES.get(0);
    }
    public static List<Site> group(String group) {
        List<Site> found = new ArrayList<>();
        for (Site site : SITES) if (site.group.equals(group)) found.add(site);
        return found;
    }
    public static boolean matches(Site site, String url) {
        if (site == null || !StreamDetector.isHttpsUrl(url)) return false;
        try {
            String host = new URI(url).getHost().toLowerCase(Locale.ROOT);
            return host.equals(site.host) || host.endsWith("." + site.host);
        } catch (Exception e) { return false; }
    }
    public static Site fromUrl(String url) {
        for (Site s : SITES) if (matches(s, url)) return s;
        return null;
    }
}
