# Pooyan TV v0.4.2 — FarsiPlex + DeepSeek

نمونهٔ **آزمایشی Android TV** با نام نمایشی **Pooyan TV**، کاتالوگ عمومی **فقط FarsiPlex** و موتور اختیاری بررسی لینک با API جست‌وجوی DeepSeek. این پروژه رسمی FarsiPlex نیست.

## قابلیت‌ها

- رابط بومی تلویزیون بدون WebView، خانه، پوستر فیلم و سریال، جست‌وجو و علاقه‌مندی‌ها
- صفحهٔ جزئیات و لیست قسمت‌های سریال، کنترل D-pad و حرکت بین ردیف پوسترها (اصلاحات v0.4.1 حفظ شده)
- پخش Media3 برای لینک‌های مستقیم HTTPS سازگار و مجاز که در HTML5 عمومی موجودند یا کاربر با اجازه وارد می‌کند
- **جدید:** دکمهٔ «✨ بررسی پخش با DeepSeek» و بخش تنظیمات «⚙ AI» در اپ
- **جدید:** Cloudflare Worker مجزا (`resolver-worker/`) که با DeepSeek Search به دنبال صفحه‌های عمومی **همان عنوان** در FarsiPlex می‌گردد؛ لینک مستقیم فقط وقتی پذیرفته می‌شود که در HTML5 آن صفحه واقعاً منتشر شده باشد، دامنهٔ رسانه در allowlist باشد و پاسخ HTTP نوع رسانهٔ قابل‌قبول داشته باشد
- بدون نیاز به جاسازی **DEEPSEEK_API_KEY** در APK یا مخزن GitHub

## محدودیت اصلی

هوش مصنوعی نمی‌تواند یک لینک رسانهٔ پنهان یا قفل‌شده ایجاد کند. اگر سایت فقط iframe، DRM، کوکی نشست، توکن خصوصی یا پلیر جاوااسکریپتی داشته باشد، اپ ممکن است فقط **کاتالوگ** را نشان دهد. این نسخه به API رسمی FarsiPlex یا حق پخش آثار دسترسی ندارد. بررسی HTTP لینک، تضمین پخش کامل در تلویزیون نیست.

## راه‌اندازی Worker با کلید شخصی DeepSeek

در فولدر `resolver-worker/`:

```bash
npm install
node --test resolver.test.mjs
npx wrangler login
npx wrangler secret put DEEPSEEK_API_KEY
npx wrangler secret put TV_ACCESS_TOKEN
npx wrangler deploy
```

**DEEPSEEK_API_KEY** همان کلید پولی DeepSeek است؛ **فقط در Secretهای Cloudflare وارد شود**. برای **TV_ACCESS_TOKEN** یک کد تصادفی قوی (حداقل ۲۴ کاراکتر) انتخاب کن؛ این کد با کلید API متفاوت است.

در `wrangler.toml` فهرست `MEDIA_ALLOWED_HOSTS` را فقط با دامنه‌های CDN موردتأیید و مجاز پر کن. به‌صورت پیش‌فرض فقط `farsiplex.com,www.farsiplex.com` مجازند. بدون اجازه یا اطمینان از منبع، دامنهٔ ناشناس وارد نکن. همچنین بهتر است برای Worker محدودیت نرخ مصرف (Rate Limiting) در Cloudflare فعال کنی.

پس از Deploy، در اپ Pooyan TV روی «⚙ AI» بزن و تنها **آدرس HTTPS Worker** و کد **TV_ACCESS_TOKEN** را وارد کن. **هرگز کلید پولی DeepSeek را در تلویزیون وارد نکن.** سپس صفحهٔ فیلم را باز کن و روی «✨ بررسی پخش با DeepSeek» بزن.

## تست‌ها و ساخت APK

پیش‌نیاز بیلد: JDK 17، Gradle 8.9، Android SDK API 35 و Build Tools 35، اینترنت برای وابستگی‌ها.

```bash
bash scripts/verify-offline.sh
node --test resolver-worker/resolver.test.mjs
gradle --no-daemon --stacktrace assembleDebug testDebugUnitTest lintDebug
```

در GitHub مخزن `https://github.com/PooyanGhorbani/Pooyan-TV`، سورس را روی شاخهٔ `dev/v0.4.2-deepseek` بارگذاری کن و از **Actions → Build Pooyan TV v0.4.2 DeepSeek Preview → Run workflow** بیلد بگیر. خروجی Artifacts با نام `Pooyan-TV-FarsiPlex-v0.4.2-Preview-APK` است.

نصب بعد از ساخت موفق روی تلویزیون متصل و مجاز:

```powershell
./scripts/Install-PooyanTV-v0.4.2.ps1 -TvAddress "TV_IP:5555" -ApkPath "C:\\path\\to\\app-debug.apk"
```

نسخهٔ Debug با شناسهٔ `com.pooyan.namakadetv.preview042` **جدا** از نسخه‌های v0.1 و preview041 نصب می‌شود؛ چیزی را Uninstall نکن. از وجود امضای معتبر APK و مجوز ADB اطمینان حاصل کن.

## آنچه واقعاً آزمایش شده

- تست‌های منطقی آفلاین Java و بررسی نمادها، سورس ۱۷ فایل و اعتبار XML مانیفست: **موفق**
- شش تست خودکار Node برای Worker: **موفق**، شامل امنیت token، دامنه، لینک‌های HTML5، رد لینک خیالی DeepSeek و HTTP check
- **انجام نشده:** کامپایل واقعی Android APK (SDK/Gradle در محیط فعلی موجود نیست)، دیپلوی Worker روی حساب تو، درخواست واقعی به DeepSeek (کلید در دسترس نیست)، اتصال مستقیم به FarsiPlex (DNS این محیط خطا داد)، تست واقعی روی شیائومی و آزمون کامل پخش فیلم

**توصیه:** پیش از انتشار عمومی یا استفاده از پخش محتوا، شرایط استفاده و مجوز اتصال به منابع FarsiPlex را با صاحب سرویس بررسی کن. تعداد پوستر به معنی تعداد فیلم قابل‌پخش نیست.
