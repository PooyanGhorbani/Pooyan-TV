# Pooyan TV — FarsiPlex Edition v0.4

**Android TV native catalog prototype** — اپ مستقل، با نام نمایشی «Pooyan TV» و منبع اطلاعات عمومی **فقط https://farsiplex.com/**.

## این نسخه چه کار می‌کند؟

- رابط کاملاً بومی Android TV بدون WebView، با کارت‌های پوستر و ناوبری ریموت D-pad
- خانه، فیلم‌ها، سریال‌ها، صفحهٔ جزئیات، فهرست قسمت‌های سریال و جست‌وجو در عناوین عمومی
- خواندن عنوان، پوستر و اطلاعات عمومی از صفحات HTML عمومی `/movie/`، `/tvshow/` و `/episode/` با Jsoup؛ **API رسمی یا توافق دسترسی نداریم**
- ذخیرهٔ محلی علاقه‌مندی‌ها
- پخش Media3 برای لینک مستقیم HTTPS با فرمت‌های MP4/WebM/HLS/DASH که **کاربر مجاز است استفاده کند**؛ ادامهٔ پخش توسط PlayerActivity ذخیره می‌شود
- عدم استفاده از API پولی، ورود، کوکی، اسکریپت پلیر، DRM یا لینک خصوصی
- فایل‌ساز GitHub Actions برای APK آزمایشی، با امکان نصب موازی کنار v0.1

**مهم:** ویدئوهای FarsiPlex هنوز از طریق اپ قابل پخش اعلام نشده‌اند؛ منبع مستقیم معتبر و مجوز آن جداست. تعداد پوستر ≠ تعداد فیلم قابل‌تماشا. نمایش اطلاعات و پوسترهای سایت ممکن است با تغییر HTML یا سیاست‌های دسترسی از کار بیفتد. پیش از انتشار عمومی اپ متصل به FarsiPlex، شرایط و اجازهٔ صاحب سایت را بررسی کنید.

## ساخت APK

پیش‌نیاز: JDK 17، Android SDK API 35 و Build Tools 35، Gradle 8.9 و دسترسی به Maven Central/Google Maven.

```
sh scripts/verify-offline.sh
gradle --no-daemon assembleDebug testDebugUnitTest lintDebug
```

خروجی پس از **ساخت موفق**: `app/build/outputs/apk/debug/app-debug.apk`.

در GitHub، فایل‌ها را در شاخه `dev/v0.4-farsiplex` قرار دهید، سپس **Actions → Build Pooyan TV FarsiPlex v0.4 Preview → Run workflow**؛ فایل `Pooyan-TV-FarsiPlex-v0.4-Preview-APK` را از بخش Artifacts همان اجرا بردارید. APK تا زمان اجرای Workflow آماده نیست.

نصب آزمایشی با ADB مجاز روی تلویزیون:

```
# Windows PowerShell (در فولدر سورس)
./scripts/Install-PooyanTV-v0.4.ps1 -TvAddress "TV_IP:5555" -ApkPath "C:\\path\\to\\app-debug.apk"
```

نسخهٔ Debug شناسهٔ `com.pooyan.namakadetv.preview` دارد و نباید نسخهٔ اصلی v0.1 را حذف کند. احتمال دارد نسخهٔ Debug قبلی v0.2/v0.3 با همان شناسه و امضای متفاوت نصب شده باشد؛ در این حالت **بدون تأیید کاربر حذف نکنید**. سازگاری نصب به امضای APK بستگی دارد.

## بررسی‌ها و محدودیت‌ها

در محیط آماده‌سازی، اسکریپت آفلاین (تست URL، پخش، ساختار Java و مانیفست) اجرا شد. آزمون‌های JUnit پارسر نمونهٔ HTML در `app/src/test` اضافه شده‌اند اما **فقط با Gradle / Android SDK** اجرا می‌شوند. اتصال مستقیم از این محیط به farsiplex.com با DNS failure مواجه شد؛ HTML واقعی در اپ و تصاویر از چین/تلویزیون هنوز تست نشده‌اند. هیچ APK از این محیط کامپایل یا روی TV نصب نشده است.

قواعد شبکه: فقط HTTPS دامنهٔ farsiplex.com برای **متادیتا**، بدون ریدایرکت به دامنه‌های متفرقه، حداکثر ۱٫۸ مگابایت HTML و محدودیت تعداد صفحات در جست‌وجو. لینک پخش فقط در صورت افزودن توسط خود کاربر استفاده می‌شود. به‌دلیل احتمال محدودیت کشور، در صورت خطای DNS یا HTTP، پیام مناسب نمایش داده می‌شود.

## ساختار

- `MainActivity.java`: رابط بومی و ناوبری تلویزیون
- `FarsiPlexRepository.java`: دریافت محدود HTML عمومی
- `FarsiPlexParser.java`: استخراج عنوان، تصویر و قسمت، بدون استخراج ویدئو
- `FarsiPlexUrlPolicy.java`: محدودسازی URLهای دامنهٔ موردنظر
- `CatalogImages.java`: دریافت محدود پوستر
- `LocalStreams.java`، `PlayerActivity.java`: منبع شخصی و پخش Media3
- `.github/workflows/build-apk.yml`: ساخت و تست ابری

این پروژه رسمی FarsiPlex نیست و توسط صاحبان آن تأیید یا پشتیبانی نشده است.
