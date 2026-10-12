# VidGet - Video Downloader (Android)

مشروع أندرويد احترافي لتحميل مقاطع الفيديو مبني وفق معمارية **Clean Architecture + MVVM** مع تقسيم معتمد على وحدات Gradle المتعددة (Multi-Module Architecture).

---

## 1. هيكل الوحدات واتجاه الاعتماد (Module Dependency Graph)

```
       ┌───────────┐
       │    app    │ (تجميع التطبيق، Application Class، MainActivity)
       └─────┬─────┘
             │
     ┌───────┼────────────────────────┐
     │       │                        │
     ▼       ▼                        ▼
  ┌──────┐ ┌──────┐                ┌──────┐
  │  ui  │ │ data │                │ core │
  └──┬───┘ └──┬───┘                └──────┘
     │        │                       ▲
     ▼        ▼                       │ (تعتمد عليه كافة الوحدات المشتركة)
  ┌──────────────┐                    │
  │    domain    │                    │
  └──────────────┘                    │
                                      │
  ┌────────────────┐                  │
  │   downloader   │──────────────────┘
  └────────────────┘ (وحدة مستقلة بذاتها لمحرك التحميل yt-dlp / FFmpeg / aria2c)
```

### اتجاه الاعتماد وقواعد المعمارية:
1. **`domain` (الطبقة المركزية - Business Logic)**:
   - طبقة خالصة تمثل قواعد العمل وحالات الاستخدام (Use Cases) والنماذج المجردة (Domain Models) وواجهات المستودعات (Repository Interfaces).
   - لا تعتمد على طبقة `ui` ولا `data` ولا `downloader`.

2. **`ui` (عرض الواجهات - Presentation)**:
   - تعتمد على: `domain` و `core`.
   - مبنية بـ Jetpack Compose + Material 3.
   - تعرض الشاشات وحالات العرض (UI States) وتتعامل فقط مع حالات استخدام `domain` عبر ViewModels.

3. **`data` (البيانات والتخزين - Data & Infrastructure)**:
   - تعتمد على: `domain` و `core` و `downloader`.
   - مسؤولة عن تنفيذ واجهات `domain` (Repository Implementations).
   - تدير قواعد بيانات Room، وتفضيلات DataStore، وطلبات OkHttp / Jsoup، ومهام WorkManager.

4. **`downloader` (محرك التحميل المستقل)**:
   - وحدة مستقلة تمامًا عن طبقة العرض `ui` وقواعد بيانات `data`.
   - تعتمد فقط على `core`.
   - تغلّف مكتبات `youtubedl-android` (yt-dlp) و `ffmpeg` و `aria2c`.

5. **`core` (الوظائف المشتركة - Common)**:
   - توفر أدوات الـ Dispatchers (Dispatchers.IO, Main)، ونماذج النتائج (ResultWrapper)، والتسلسل المشترك (Kotlinx Serialization).

6. **`app` (نقطة الربط الرئيسية - Main Entrypoint)**:
   - تجمع الوحدات، وتعرّف `VidGetApp` مع حقن Hilt (`@HiltAndroidApp`).
   - تدير إعدادات ABI splits، وتفعيل R8 / ProGuard، وإعدادات `useLegacyPackaging = true`.

---

## 2. التقنيات والمكتبات المستخدمة

- **اللغة**: Kotlin 2.0.21
- **الواجهات**: Jetpack Compose + Material 3 (Compose BOM 2024.11.00)
- **حقن التبعيات (DI)**: Dagger Hilt 2.52 + KSP 2.0.21-1.0.28
- **محرك التنزيل**: `io.github.junkfood02.youtubedl-android` (library, ffmpeg, aria2c v0.17.2)
- **التخزين**: Room 2.6.1 + DataStore Preferences 1.1.1
- **المهام بالخلفية**: WorkManager 2.10.0 + Foreground Service
- **الشبكة وتحليل HTML**: OkHttp 4.12.0 + Jsoup 1.18.1 + kotlinx-serialization 1.7.3
- **الصور**: Coil Compose 2.7.0
- **مشغل الوسائط**: AndroidX Media3 (ExoPlayer) 1.5.0
- **إصدارات SDK**: compileSdk 35، targetSdk 35، minSdk 24

---

## 3. إعدادات البناء الحساسة

- **JNI Packaging**: تم تفعيل `packaging.jniLibs.useLegacyPackaging = true` لتمكين محرك yt-dlp ومكتبات FFmpeg من فك الضغط والتنفيذ بسلاسة على بيئات Android الحديثة.
- **ABI Splits**: مدعومة لمعماريات `arm64-v8a` و `armeabi-v7a` و `x86_64` مع إنتاج Universal APK كامل.
- **R8 / ProGuard**: قواعد كاملة لحماية وتأمين استدعاءات JNI والمكتبات الحيوية من الحذف عند التجميع لنسخة الإنتاج.
