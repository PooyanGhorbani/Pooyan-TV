# Pooyan TV v0.4.2 — نتایج واقعی

APK: https://github.com/PooyanGhorbani/Pooyan-TV/releases/download/v0.4.2/Pooyan-TV-v0.4.2-preview.apk
Branch: dev/v0.4.2-deepseek
CI: https://github.com/PooyanGhorbani/Pooyan-TV/actions/runs/44044231151
SHA256: d9d5c7327a147dd5ba988dde0aefa64bb6ddaaef53a910f5e1e2b017a6c771eb

نصب مجزای preview042 روی Xiaomi Android14 موفق؛ اجرای سرد 883ms. نسخه‌های قدیمی حذف نشدند.

اصلاحات: حذف bringToFront که ترتیب پوسترها را خراب می‌کرد؛ بازگرداندن همان صفحه و فوکوس پس از جزئیات؛ deadline برای Worker؛ بررسی مستقل امضای بایت رسانه در Android برای لینک‌های خودکار.

ساخت: JDK17، SDK35، Gradle8.9؛ assembleDebug، unit tests و lintDebug موفق. 55 بررسی سیاست Java و شش آزمون mock Worker موفق؛ سینتکس 18 فایل Java بررسی شد.

روی دستگاه و با تأیید package preview042: راست Harchi To Begi → Ghomarbaz → Balit؛ چپ به Ghomarbaz؛ OK جزئیات؛ Back به همان پوستر و موقعیت. پایین به Taxi در ردیف سریال و بالا به همان ستون فیلم. حرکت شبکه به Mahoor و بازگشت نیز مشاهده شد. دکمهٔ نمایش بیشتر، صفحهٔ بعد را افزود و فوکوس به Aghaye Zaloo رفت.

جست‌وجوی Taxi یک نتیجه داد؛ جزئیات سریال باز شد و افزودن و نمایش آن در علاقه‌مندی موفق بود. پوسترها بارگذاری شدند. Logcat در بازهٔ بررسی Fatal نشان نداد؛ تضمین همهٔ شرایط نیست.

نمونهٔ عمومی رسمی AndroidX با Media3 واقعاً تصویر و رندر فریم داشت. این نمونه حدود یک ثانیه و غیرمرتبط با فیلم FarsiPlex بود؛ نگاشت آزمایشی بعداً پاک شد. HLS/DASH واقعی در این نوبت آزمایش نشد. در ابتدا نسخهٔ قدیمی گاهی پیش‌زمینه بود؛ آن مشاهدات در نتایج 0.4.2 حساب نشده‌اند.

DeepSeek: درخواست Search با مستندات رسمی https://api-docs.deepseek.com/api/search/ تطبیق داده شد. Worker فقط صفحات عمومی همان عنوان را بررسی می‌کند و URL تخیلی را نمی‌پذیرد. تست‌های موفق mock هستند. دسترسی مجاز انتشار Cloudflare و آدرس Worker در دسترس نبود؛ Worker منتشر نشده، Android به سرویس زنده متصل نیست و آزمون زنده DeepSeek انجام نشده است. اطلاعات محرمانه در سورس یا APK ذخیره نشد.

FarsiPlex: صفحات عمومی Tars Az Ashk Haye Vaghei و Meymoone Mast تگ video HTML5 نداشتند. API مستند و مجاز پخش برای این برنامه تأیید نشده است. هیچ احراز هویت، DRM یا محدودیت دسترسی دور زده نشد. پخش فیلم‌های واقعی FarsiPlex تأیید نشده و هدف تماشای مستقیم آن‌ها هنوز محقق نیست.

APK Debug است؛ امضای پایدار آینده هنوز تنظیم نشده. جست‌وجو حداکثر سه صفحه از هر نوع را می‌خواند؛ پوشش تمام عناوین، قسمت‌ها و شرایط شبکه کامل نیست.
