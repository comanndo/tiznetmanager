# TizNet Manager

اپلیکیشن اندرویدی مدیریت حرفه‌ای پنل **هیدیفای** با رابط کاربری مدرن Material 3، با پشتیبانی کامل از
**نسخه‌های ۱۱ تا ۱۴ هیدیفای** و قابلیت مدیریت هم‌زمان چند پنل.

> این پروژه یک کلاینت غیررسمی برای Admin V2 REST API هیدیفای است و به پنل شما روی سرور شما متصل می‌شود.
> نام و لوگوی «هیدیفای» متعلق به سازنده اصلی است.

## ✨ امکانات

- **مدیریت چند پنلی**: افزودن نامحدود پنل، سوییچ سریع بین آن‌ها، ذخیره رمزنگاری‌شده رمزها (AES/GCM با Android Keystore)
- **داشبورد زنده**: آمار کاربران، نمودار مصرف و کاربران آنلاین، مصرف CPU/RAM/Disk، ترافیک شبکه، uptime و پردازش‌های پرمصرف
- **مدیریت کاربران**: ساخت، ویرایش، حذف، فعال/غیرفعال، جستجو و فیلتر، عملیات گروهی ( Bulk )، ساخت دسته‌ای، ریست مصرف/روزها، کیوآرکد و لینک اشتراک
- **ادمین‌ها**: درخت ادمین‌ها، نقش‌ها (super admin / admin / agent)، قابلیت‌ها، محدودیت‌ها و Alias
- **نودها**: لیست نودها، وضعیت آنلاین، پینگ، همگام‌سازی و ثبت نود جدید
- **دامنه‌ها**: مدیریت دامنه‌های direct/CDN/relay/worker، وضعیت TLS/Reality و گواهی‌ها
- **خروجی‌ها (Outbounds)**: WARP، Psiphon، Tor، SOCKS، مسدودسازی، کانفیگ‌های سفارشی هسته/Xray، ترتیب و خروجی پیش‌فرض
- **پروتکل‌ها**: فعال/غیرفعال‌سازی پروتکل‌های پنل
- **تنظیمات**: رندر داینامیک تمام فیلدهای تنظیمات سرور با پشتیبانی از جستجو و حالت پیشرفته
- **اعمال تغییرات**: apply / install / update / restart با لاگ زنده
- **پشتیبان‌گیری**: ساخت، دانلود، اشتراک‌گذاری، بازیابی آپلودی و ارسال به تلگرام
- **لاگ‌ها**: مشاهده فایل‌های لاگ سرور
- **پیام همگانی**: ارسال پیام تلگرام به مخاطبان مختلف
- **Clean IP / Reality**: تنظیمات Reality و اسکن شبکه
- **برچسب‌ها**: مدیریت تگ‌های رنگی کاربران
- پشتیبانی کامل از **RTL و زبان فارسی** (به‌همره انگلیسی)، حالت تیره/روشن و رنگ داینامیک اندروید ۱۲+

## 🏗 معماری

```
app/src/main/java/com/tiznet/manager
├── data
│   ├── db            # Room: ذخیره پنل‌ها
│   ├── model         # مدل‌های Serializable متناظر Admin V2 API
│   ├── prefs         # ترجیحات UI و پنل فعال
│   ├── remote        # Retrofit/OkHttp + HiddifyApi (تمام endpointها)
│   ├── repo          # PanelRepository
│   └── SessionManager# نشست پنل فعال، Bootstrap و اطلاعات ادمین
├── ui
│   ├── common        # BaseFragment، StateView، UiState
│   ├── widgets       # ProgressRing، BarChartView سفارشی
│   └── <feature>     # Fragment + ViewModel + Adapter هر بخش
├── util              # رمزنگاری، QR، فرمت‌ها، اکستنشن‌ها
└── TizNetApp         # Service Locator سطح اپلیکیشن
```

- **UI:** View Binding + Material Components 3 + Navigation Component + Drawer/Bottom Navigation
- **Network:** Retrofit 2 + OkHttp 4 + kotlinx.serialization
- **Async:** Kotlin Coroutines + Flow + ViewModel
- **Storage:** Room (KSP) + EncryptedSharedPreferences-style Keystore encryption
- **minSdk 26 / targetSdk 35 / Kotlin 2.1 / AGP 8.7**

## 🔐 احراز هویت

هیدیفای مدرن (۱۱–۱4) احراز هویت فرم‌محور با کوکی session دارد؛ Basic Auth همراه رمز عبور
در نسخه‌های جدید پذیرفته نمی‌شود (ستون legacy `username` برای ادمین‌ها خالی است و super admin اصلاً آن را ندارد).
بنابراین اپ دقیقاً مثل مرورگر عمل می‌کند:

1. آدرس پنل و Proxy Path گرفته می‌شود (مسیر خالی با `__admin_v2_bootstrap` یا ریدایرکت لاگین خودکار کشف می‌شود)
2. صفحه لاگین `/<proxy_path>/` GET و توکن CSRF استخراج می‌شود
3. فیلدهای `username`/`password` (و نام‌های قدیمی `secret_textbox`/`password_textbox`) POST می‌شوند؛
   شناسه می‌تواند UUID یا Alias ادمین باشد
4. کوکی session در یک CookieJar درون‌حافظه‌ای نگه داشته می‌شود و همه درخواست‌های Admin V2 با آن ارسال می‌شوند
5. روی پاسخ ۴۰۱، handshake یک‌بار تکرار و در پنل‌های قدیمی‌تر به Basic Auth / هدر `Hiddify-API-Key` fallback می‌شود
6. رمز عبور با کلید غیرقابل استخراج Android Keystore (AES-256/GCM) رمز شده و فقط محلی ذخیره می‌شود
7. ترافیک Cleartext (HTTP) برای پنل‌های شبکه داخلی مجاز است؛ HTTPS در صورت داشتن دامنه توصیه می‌شود

## 🔌 نقشه Endpointها

| بخش اپ | Endpoint نمونه |
|---|---|
| داشبورد | `GET dashboard/?days=30` |
| کاربران | `GET/POST users/` · `PATCH/DELETE users/{uuid}/` · `users/bulk/` · `users/bulk-create/` |
| ادمین‌ها | `GET/POST admins/` · `PATCH/DELETE admins/{uuid}/` |
| نودها | `GET/POST nodes/` · `nodes/{id}/ping/` · `nodes/{id}/sync/` |
| دامنه‌ها | `GET/POST domains-page/` · `PATCH domains-page/{id}/` |
| خروجی‌ها | `GET/POST outbounds/` · `outbounds/order/` · `outbounds/{id}/default/` |
| پروتکل‌ها | `GET/PUT protocols/` |
| تنظیمات | `GET/PUT settings/` |
| اعمال | `GET apply/` · `POST apply/{action}/` · `GET apply/log/` |
| بکاپ | `GET/POST backup/` · `backup/download/` · `backup/restore/` |
| لاگ‌ها | `POST log/` |
| پیام همگانی | `GET/POST broadcast/` |
| Clean IP | `GET/PUT clean-ip/reality/` · `POST clean-ip/token/` |
| برچسب‌ها | `GET/POST/PATCH/DELETE tags/` |

## 🚀 ساخت پروژه

پیش‌نیاز: JDK 17 و Android SDK با API 35.

```bash
./gradlew assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

برای نسخه انتشار:

```bash
./gradlew assembleRelease
```

## 🧩 تولید منابع

آیکون‌ها و دراوبل‌ها از روی لوگوی رسمی هیدیفای با اسکریپت‌های پایتونی ساخته شده‌اند:

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install pillow
python3 scripts/generate_icons.py     # آیکون لگسی/تطبیقی/مونوکروم و لوگوهای درون‌اپ
python3 scripts/generate_drawables.py # آیکون‌های وکتور متریال و shapeها
```

## 📁 ساختار منابع

- `assets/logo/` — لوگوی اصلی هیدیفای (منبع تولید آیکون)
- `app/src/main/res/mipmap-*` — آیکون لگسی و foreground تطبیقی
- `app/src/main/res/mipmap-anydpi-v26/` — Adaptive Icon (اندروید 8+)
- `app/src/main/res/values/strings.xml` — فارسی (پیش‌فرض) · `values-en/` — انگلیسی

## ⚠️ نکات

- این اپ هیچ داده‌ای را به جز سرور پنل شما جایی ارسال نمی‌کند.
- بکاپ‌گیری ابری از دیتابیس اپ به‌صورت صریح غیرفعال شده تا اطلاعات پنل خارج نشود.
- نسخه‌های قدیمی‌تر هیدیفای (کمتر از 11) ساختار API متفاوتی دارند و پشتیبانی نمی‌شوند.
