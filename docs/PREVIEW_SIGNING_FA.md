# امضای ثابت APKهای پیش‌نمایش Pooyan TV

**این مرحله برای آپدیت مستقیم ضروری است.** Android فقط زمانی یک APK را روی نسخهٔ قبلی نصب می‌کند که `applicationId`، ترتیب افزایشی `versionCode` و گواهی/کلید امضای نسخه‌ها سازگار باشد. نسخهٔ `v0.8.6` شناسهٔ پیش‌نمایش قدیمی را حفظ می‌کند: `com.pooyan.namakadetv.dev083catalog` و `versionCode=51`.

## اگر کلید نسخهٔ نصب‌شده را داری
همان keystore قبلی را دوباره استفاده کن؛ **کلید جدید نساز**. یک کلید دیگر باعث ردشدن نصب درجا می‌شود. ابتدا گواهی APK نصب‌شده و خروجی CI را با ابزار `apksigner verify --print-certs` مقایسه کن.

## اگر کلید ثابت نداری

فقط **یک بار** یک کلید جدید در کامپیوتر مورد اعتماد خودت بساز، نه در چت و نه در مخزن:

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore pooyan-preview.jks \
  -alias pooyanpreview -keyalg RSA -keysize 4096 -validity 10000
```

از فایل JKS و همهٔ رمزها در محل امن پشتیبان بگیر. فایل کلید را **commit یا آپلود عمومی نکن**.

برای ثبت در GitHub Actions:

```bash
base64 -w0 pooyan-preview.jks > pooyan-preview.b64
# macOS: base64 -i pooyan-preview.jks -o pooyan-preview.b64
```

Repository → Settings → Secrets and variables → Actions:

| Secret | مقدار |
|---|---|
| `POOYAN_PREVIEW_KEYSTORE_BASE64` | محتوای فایل base64 |
| `POOYAN_PREVIEW_KEYSTORE_PASSWORD` | رمز keystore |
| `POOYAN_PREVIEW_KEY_ALIAS` | `pooyanpreview` |
| `POOYAN_PREVIEW_KEY_PASSWORD` | رمز کلید |

## هشدار حیاتی برای نصب موجود

اگر APK فعلی با **کلید موقت CI** امضا شده، ساخت کلید ثابت جدید به‌تنهایی نمی‌تواند آن APK قدیمی را درجا به‌روزرسانی کند. **هیچ‌چیز را حذف نکن** تا از همهٔ داده‌هایی که لازم داری پشتیبان گرفته و امکان بازیابی آن را واقعاً تست کرده باشی. ابزار پشتیبان موجود ممکن است همهٔ تنظیمات/کش/تاریخچه را پوشش ندهد؛ این را قبل از هر مهاجرت بررسی کن. بدون داشتن کلید قدیمی یا یک روش مهاجرت آزموده‌شده، تضمین حفظ داده وجود ندارد.

## ساخت و انتشار v0.8.6

1. روی شاخهٔ `ui/cinema-v086`، GitHub Actions → «Pooyan TV v0.8.6 Cinema Stability Preview».
2. برای ساخت آزمایشی، workflow را اجرا کن؛ APK به‌صورت Artifact ایجاد می‌شود.
3. برای انتشار رسمی قابل‌تشخیص توسط سیستم آپدیت، گزینهٔ `publish_prerelease` را فعال کن. فقط بعد از موفقیت **هر دو تست شبیه‌ساز Android TV** و ساخت APK با **کلید ثابت**، GitHub Pre-release با تگ `v0.8.6` و فایل `Pooyan-TV-v0.8.6-preview.apk` منتشر می‌شود.
4. فایل `SIGNING_CERT_SHA256.txt` و `SHA256SUMS.txt` را بررسی کن. برای نسخه‌های بعدی از همین keystore و شناسهٔ برنامه استفاده کن.
5. اگر GitHub Actions به Workflow permission نوشتن Release دسترسی نداشته باشد، تنظیم Repository Actions permissions باید توسط مالک بررسی شود؛ هیچ توکنی را داخل کد قرار نده.

**توجه:** Release پیش‌نمایش از Release پایدار جداست؛ فایل‌های داخل `Actions Artifacts` به‌تنهایی توسط سیستم آپدیت برنامه قابل کشف نیستند.

## قفل‌کردن اثرانگشت گواهی امضا (ضروری برای انتشار)

بعد از اولین Build که با کلید ثابت اجرا شده، از Artifact فایل `SIGNING_CERT_SHA256.txt` را بردار. سپس در **Settings → Secrets and variables → Actions → Variables** یک متغیر معمولی (غیرمحرمانه) بساز:

| Variable | مقدار |
|---|---|
| `POOYAN_PREVIEW_CERT_SHA256` | همان SHA-256 گواهی از فایل `SIGNING_CERT_SHA256.txt` |

در اجرای انتشار، GitHub اثرانگشت APK جدید را با این مقدار مقایسه می‌کند. اگر کلید به‌اشتباه تغییر کند یا Variable تنظیم نشده باشد، **انتشار متوقف می‌شود**. این اثرانگشت رمز نیست؛ انتشار عمومی آن ایرادی ندارد. اما keystore و رمزها را هرگز منتشر نکن.
