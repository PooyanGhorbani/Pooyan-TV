# Pooyan TV 

اپ Android TV با هفت منبع وب، کنترل ریموت، WebView، پلیر Media3 برای MP4/HLS/DASH، جست‌وجو، علاقه‌مندی و ادامهٔ تماشا.

## دریافت APK

فایل نصب و SHA256 در [Release v0.1](https://github.com/PooyanGhorbani/Pooyan-TV/releases/tag/v0.1) قرار می‌گیرند. خروجی هر اجرای ساخت در بخش Actions نیز نگهداری می‌شود.

## ساخت

JDK 17، Gradle 8.9، Android Gradle Plugin 8.7.3، SDK 35 و Media3 1.6.1 استفاده می‌شوند.

```sh
bash scripts/verify-offline.sh
gradle --no-daemon assembleDebug assembleDebugAndroidTest lintDebug
```

Workflow در `.github/workflows/build-apk.yml` با push به main یا اجرای دستی فعال می‌شود.

## نصب با حفظ داده

در ویندوز از `scripts/Install-PooyanTV.ps1 -TvAddress IP:PORT -ApkPath PATH` استفاده کنید. نصب با `adb install -r` انجام می‌شود و اسکریپت هرگز برنامه‌ای را حذف نمی‌کند. امضای Debug ممکن است بین اجراهای CI تغییر کند؛ در صورت ناسازگاری امضا، نصب متوقف می‌شود. برای انتشار پایدار بعدی کلید امضای خصوصی ثابت لازم است. versionName برابر 0.1 و versionCode برابر 5 است.

## آزمون روی دستگاه

APK آزمایش در artifact ساخت وجود دارد. پس از نصب آن، دستور زیر تست ریموت، علاقه‌مندی، پخش نمونهٔ عمومی، ذخیرهٔ پیشرفت، WebView، تمام‌صفحه و بررسی صفحات هفت منبع را اجرا می‌کند:

```sh
adb shell am instrument -w com.pooyan.namakadetv.test/androidx.test.runner.AndroidJUnitRunner
```

بررسی سایت‌ها، نتیجهٔ شبکه و صفحه را گزارش می‌کند؛ به معنای تأیید پخش تمام فیلم‌های آن‌ها نیست. گزارش نهایی دستگاه در `docs/DEVICE_RESULTS_FA.md` ثبت می‌شود.

## حریم خصوصی

نشانی‌های شخصی، کوکی‌ها و کلیدها در مخزن عمومی قرار نمی‌گیرند. لینک شخصی Namakade فقط در تنظیمات خود تلویزیون ثبت می‌شود. هیچ DRM یا ورود حسابی دور زده نمی‌شود. دسترسی، کدک، محدودیت منطقه‌ای و نیاز به حساب هر سایت می‌تواند متفاوت باشد.
