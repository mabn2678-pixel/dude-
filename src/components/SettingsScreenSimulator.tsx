import React, { useState } from 'react';
import {
  Settings,
  Globe,
  Moon,
  Sun,
  Palette,
  Copy,
  Folder,
  FileCode,
  FileText,
  Sliders,
  Layers,
  CloudDownload,
  Wifi,
  Gauge,
  Clock,
  Radio,
  Database,
  Trash2,
  HardDrive,
  RefreshCw,
  Cookie,
  Shield,
  BatteryCharging,
  Share2,
  RotateCcw,
  Info,
  Check,
  ChevronRight,
  AlertTriangle,
  X
} from 'lucide-react';

export function SettingsScreenSimulator() {
  // 1. General State
  const [language, setLanguage] = useState<'system' | 'ar' | 'en'>('ar');
  const [themeMode, setThemeMode] = useState<'system' | 'light' | 'dark'>('dark');
  const [dynamicColor, setDynamicColor] = useState<boolean>(true);
  const [clipboardDetection, setClipboardDetection] = useState<boolean>(true);

  // 2. Downloads State
  const [saveDir, setSaveDir] = useState<string>('Downloads/VidGet');
  const [namingTemplate, setNamingTemplate] = useState<string>('{title} - {quality}.{ext}');
  const [conflictResolution, setConflictResolution] = useState<string>('AUTO_RENAME');
  const [subfolderOrg, setSubfolderOrg] = useState<string>('NONE');
  const [defaultQuality, setDefaultQuality] = useState<string>('ALWAYS_ASK');
  const [defaultFormat, setDefaultFormat] = useState<'MP4' | 'MKV'>('MP4');
  const [connectionsCount, setConnectionsCount] = useState<number>(8);
  const [maxConcurrent, setMaxConcurrent] = useState<number>(3);
  const [embedMetadata, setEmbedMetadata] = useState<boolean>(true);

  // 3. Network State
  const [wifiOnly, setWifiOnly] = useState<boolean>(false);
  const [dataSaver, setDataSaver] = useState<boolean>(false);
  const [speedLimit, setSpeedLimit] = useState<string>('NO_LIMIT');
  const [scheduleEnabled, setScheduleEnabled] = useState<boolean>(false);
  const [scheduleHours, setScheduleHours] = useState<string>('00:00 – 06:00');
  const [blockRoaming, setBlockRoaming] = useState<boolean>(true);

  // 4. Storage State
  const [cacheSizeMB, setCacheSizeMB] = useState<number>(64.8);
  const [orphanFilesCount, setOrphanFilesCount] = useState<number>(4);
  const [orphanFilesMB, setOrphanFilesMB] = useState<number>(142.5);
  const [freeSpaceGB, setFreeSpaceGB] = useState<number>(18.5);
  const [totalSpaceGB] = useState<number>(64.0);

  // 5. Advanced State
  const [ytDlpVersion, setYtDlpVersion] = useState<string>('2025.01.26');
  const [ytDlpChannel, setYtDlpChannel] = useState<'STABLE' | 'NIGHTLY'>('STABLE');
  const [ytDlpAutoUpdate, setYtDlpAutoUpdate] = useState<boolean>(true);
  const [isUpdatingYtDlp, setIsUpdatingYtDlp] = useState<boolean>(false);
  const [cookiesFileName, setCookiesFileName] = useState<string | null>('cookies.txt');
  const [proxyEnabled, setProxyEnabled] = useState<boolean>(false);
  const [proxyHost, setProxyHost] = useState<string>('127.0.0.1');
  const [proxyPort, setProxyPort] = useState<number>(8080);
  const [proxyType, setProxyType] = useState<'HTTP' | 'SOCKS5'>('HTTP');

  // UI Dialog / Toast State
  const [activeModal, setActiveModal] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleUpdateYtDlp = () => {
    setIsUpdatingYtDlp(true);
    setTimeout(() => {
      setIsUpdatingYtDlp(false);
      setYtDlpVersion('2025.02.14');
      showToast('تم تحديث محرك yt-dlp بنجاح إلى إصدار 2025.02.14');
    }, 1500);
  };

  const handleClearCache = () => {
    const cleared = cacheSizeMB;
    setCacheSizeMB(0);
    showToast(`تم مسح ذاكرة التخزين المؤقت وتحرير ${cleared.toFixed(1)} MB`);
  };

  const handleDeleteOrphans = () => {
    const freed = orphanFilesMB;
    const count = orphanFilesCount;
    setOrphanFilesCount(0);
    setOrphanFilesMB(0);
    showToast(`تم حذف ${count} ملفات جزئية يتيمة وتحرير ${freed.toFixed(1)} MB`);
  };

  const handleResetDefaults = () => {
    setLanguage('ar');
    setThemeMode('dark');
    setDynamicColor(true);
    setClipboardDetection(true);
    setSaveDir('Downloads/VidGet');
    setNamingTemplate('{title} - {quality}.{ext}');
    setConflictResolution('AUTO_RENAME');
    setSubfolderOrg('NONE');
    setDefaultQuality('ALWAYS_ASK');
    setDefaultFormat('MP4');
    setConnectionsCount(8);
    setMaxConcurrent(3);
    setEmbedMetadata(true);
    setWifiOnly(false);
    setDataSaver(false);
    setSpeedLimit('NO_LIMIT');
    setScheduleEnabled(false);
    setBlockRoaming(true);
    setYtDlpChannel('STABLE');
    setYtDlpAutoUpdate(true);
    setCookiesFileName(null);
    setProxyEnabled(false);
    setActiveModal(null);
    showToast('تمت استعادة كافة إعدادات التطبيق الافتراضية بنجاح');
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* Left Column: Android Phone Screen Mockup */}
      <div className="lg:col-span-7 flex flex-col items-center">
        <div className="w-full max-w-md bg-slate-900 border border-slate-700/80 rounded-3xl shadow-2xl overflow-hidden flex flex-col relative min-h-[720px] max-h-[850px]">
          {/* Status Bar */}
          <div className="bg-slate-950 px-5 py-2 flex items-center justify-between text-[11px] text-slate-400 font-mono">
            <span>12:00</span>
            <div className="flex items-center gap-1.5">
              <Wifi className="w-3.5 h-3.5" />
              <Gauge className="w-3.5 h-3.5 text-emerald-400" />
              <span>90%</span>
            </div>
          </div>

          {/* TopAppBar */}
          <div className="p-4 bg-slate-900 border-b border-slate-800 flex items-center justify-between sticky top-0 z-10 shadow-sm">
            <div className="flex items-center gap-2">
              <Settings className="w-5 h-5 text-emerald-400" />
              <h3 className="font-bold text-lg text-white">الإعدادات (Settings)</h3>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
              حفظ فوري في DataStore
            </span>
          </div>

          {/* Scrollable Settings Content */}
          <div className="flex-1 overflow-y-auto p-4 space-y-5 text-right font-sans">
            {/* 1. عام (General) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">1. عام</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* Language */}
                <div
                  onClick={() => setActiveModal('language')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Globe className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">اللغة</div>
                      <div className="text-[11px] text-slate-400">لغة واجهة التطبيق</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-emerald-400 font-bold">
                    <span>{language === 'ar' ? 'العربية' : language === 'en' ? 'English' : 'تلقائي'}</span>
                    <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                  </div>
                </div>

                {/* Theme */}
                <div
                  onClick={() => setActiveModal('theme')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Moon className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">المظهر</div>
                      <div className="text-[11px] text-slate-400">وضع العرض لواجهة التطبيق</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-emerald-400 font-bold">
                    <span>{themeMode === 'dark' ? 'داكن' : themeMode === 'light' ? 'فاتح' : 'تلقائي'}</span>
                    <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                  </div>
                </div>

                {/* Dynamic Color */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Palette className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">الألوان الديناميكية</div>
                      <div className="text-[11px] text-slate-400">استخراج الألوان من خلفية الشاشة (Material You)</div>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      setDynamicColor(!dynamicColor);
                      showToast(!dynamicColor ? 'تم تفعيل الألوان الديناميكية' : 'تم تعطيل الألوان الديناميكية');
                    }}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      dynamicColor ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        dynamicColor ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Clipboard Detection */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Copy className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">رصد الحافظة</div>
                      <div className="text-[11px] text-slate-400">التعرف التلقائي على روابط الفيديو المنسوخة فور فتح التطبيق</div>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      setClipboardDetection(!clipboardDetection);
                      showToast(!clipboardDetection ? 'تم تفعيل رصد الحافظة' : 'تم تعطيل رصد الحافظة');
                    }}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      clipboardDetection ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        clipboardDetection ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </div>

            {/* 2. التحميل (Downloads) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">2. التحميل</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* Save Folder */}
                <div
                  onClick={() => showToast(`مجلد الحفظ الحالي: ${saveDir}`)}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Folder className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">مجلد الحفظ</div>
                      <div className="text-[11px] text-slate-400">المجلد المخصص لحفظ الوسائط المكتملة</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-slate-300 font-mono text-[11px]">
                    <span>{saveDir}</span>
                    <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                  </div>
                </div>

                {/* Naming Template */}
                <div
                  onClick={() => setActiveModal('naming')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <FileText className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">قالب التسمية</div>
                      <div className="text-[11px] text-slate-400">النمط المتبع لتسمية ملفات التنزيل</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-mono text-[11px]">{namingTemplate}</span>
                </div>

                {/* Conflict Resolution */}
                <div
                  onClick={() => setActiveModal('conflict')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Sliders className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">سلوك تعارض الأسماء</div>
                      <div className="text-[11px] text-slate-400">الإجراء المتّبع عند وجود ملف يحمل نفس الاسم</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-bold">
                    {conflictResolution === 'AUTO_RENAME'
                      ? 'إعادة تسمية تلقائية (1)'
                      : conflictResolution === 'OVERWRITE'
                      ? 'استبدال'
                      : 'سؤال كل مرة'}
                  </span>
                </div>

                {/* Folder Organization */}
                <div
                  onClick={() => setActiveModal('organization')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Layers className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تنظيم المجلدات</div>
                      <div className="text-[11px] text-slate-400">تقسيم التنزيلات تلقائيًا إلى مجلدات فرعية</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-bold">
                    {subfolderOrg === 'NONE' ? 'بدون (مباشر)' : subfolderOrg === 'PLATFORM' ? 'حسب المنصة' : 'حسب التاريخ'}
                  </span>
                </div>

                {/* Default Quality */}
                <div
                  onClick={() => setActiveModal('quality')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <CloudDownload className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">الجودة الافتراضية</div>
                      <div className="text-[11px] text-slate-400">الجودة المختارة مسبقًا عند فحص الروابط</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-bold">
                    {defaultQuality === 'ALWAYS_ASK'
                      ? 'دائمًا اسأل'
                      : defaultQuality === 'BEST'
                      ? 'الأفضل'
                      : defaultQuality === '1080p'
                      ? '1080p'
                      : defaultQuality === 'AUDIO_ONLY'
                      ? 'صوت فقط'
                      : defaultQuality}
                  </span>
                </div>

                {/* Default Format (MP4 / MKV) */}
                <div
                  onClick={() => {
                    const next = defaultFormat === 'MP4' ? 'MKV' : 'MP4';
                    setDefaultFormat(next);
                    showToast(`تم تعيين الصيغة الافتراضية إلى ${next}`);
                  }}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <FileCode className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">الصيغة الافتراضية</div>
                      <div className="text-[11px] text-slate-400">الحاوية المعتمدة لدمج الفيديو والصوت</div>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-mono font-bold">
                    {defaultFormat}
                  </span>
                </div>

                {/* Connection Threads Slider (1-16) */}
                <div className="p-3.5 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="font-bold text-white">عدد الاتصالات (aria2c)</div>
                    <span className="text-emerald-400 font-mono font-bold">{connectionsCount} مسارات</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="16"
                    value={connectionsCount}
                    onChange={(e) => setConnectionsCount(parseInt(e.target.value))}
                    className="w-full accent-emerald-500 cursor-pointer"
                  />
                  <div className="flex justify-between text-[10px] text-slate-500 font-mono">
                    <span>1</span>
                    <span>8 (الموصى به)</span>
                    <span>16</span>
                  </div>
                </div>

                {/* Concurrent Downloads Slider (1-5) */}
                <div className="p-3.5 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="font-bold text-white">التحميلات المتزامنة</div>
                    <span className="text-emerald-400 font-mono font-bold">{maxConcurrent} مهام</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="5"
                    value={maxConcurrent}
                    onChange={(e) => setMaxConcurrent(parseInt(e.target.value))}
                    className="w-full accent-emerald-500 cursor-pointer"
                  />
                </div>

                {/* Embed Metadata & Thumbnail */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Layers className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تضمين الغلاف والميتاداتا</div>
                      <div className="text-[11px] text-slate-400">دمج الغلاف المصغر والعنوان والوسوم داخل الملف</div>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      setEmbedMetadata(!embedMetadata);
                      showToast(!embedMetadata ? 'تم تفعيل تضمين الميتاداتا' : 'تم تعطيل تضمين الميتاداتا');
                    }}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      embedMetadata ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        embedMetadata ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </div>

            {/* 3. الشبكة (Network) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">3. الشبكة</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* Wi-Fi Only */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Wifi className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">Wi-Fi فقط</div>
                      <div className="text-[11px] text-slate-400">إيقاف التنزيل مؤقتًا عند الاتصال عبر بيانات الهاتف</div>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      setWifiOnly(!wifiOnly);
                      showToast(!wifiOnly ? 'تم تفعيل وضع Wi-Fi فقط' : 'تم تعطيل وضع Wi-Fi فقط');
                    }}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      wifiOnly ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        wifiOnly ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Data Saver */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Gauge className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">توفير البيانات</div>
                      <div className="text-[11px] text-slate-400">تقليل استهلاك البيانات والمسارات المتعددة على الشبكات الخلوية</div>
                    </div>
                  </div>
                  <button
                    onClick={() => setDataSaver(!dataSaver)}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      dataSaver ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        dataSaver ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Speed Limit */}
                <div
                  onClick={() => setActiveModal('speedLimit')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Gauge className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">حد السرعة</div>
                      <div className="text-[11px] text-slate-400">الحد الأقصى لسرعة التنزيل</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-bold">{speedLimit === 'NO_LIMIT' ? 'بلا حد' : speedLimit}</span>
                </div>

                {/* Allowed Download Hours */}
                <div
                  onClick={() => setActiveModal('schedule')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Clock className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">ساعات التحميل المسموحة</div>
                      <div className="text-[11px] text-slate-400">
                        {scheduleEnabled ? scheduleHours : 'غير مقيد (طوال اليوم)'}
                      </div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-mono text-[11px]">
                    {scheduleEnabled ? scheduleHours : 'معطل'}
                  </span>
                </div>

                {/* Block on Roaming */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Radio className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">منع التحميل أثناء التجوال</div>
                      <div className="text-[11px] text-slate-400">إيقاف التحميل بالكامل أثناء التجوال الدولي لتجنب الرسوم</div>
                    </div>
                  </div>
                  <button
                    onClick={() => setBlockRoaming(!blockRoaming)}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      blockRoaming ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        blockRoaming ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>
            </div>

            {/* 4. التخزين (Storage) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">4. التخزين</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* Clear Cache */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Database className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">مسح الكاش</div>
                      <div className="text-[11px] text-slate-400">
                        الصور المصغرة وسجلات الاستخراج المؤقتة: {cacheSizeMB.toFixed(1)} MB
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={handleClearCache}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-red-400 font-bold border border-slate-700 transition cursor-pointer"
                  >
                    مسح
                  </button>
                </div>

                {/* Delete Orphaned Temp Files */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Trash2 className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">حذف المؤقتات اليتيمة</div>
                      <div className="text-[11px] text-slate-400">
                        ملفات جزئية أقدم من 24 ساعة: {orphanFilesCount} ملف ({orphanFilesMB.toFixed(1)} MB)
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={handleDeleteOrphans}
                    className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-red-400 font-bold border border-slate-700 transition cursor-pointer"
                  >
                    تنظيف
                  </button>
                </div>

                {/* Free Space */}
                <div className="p-3.5 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <HardDrive className="w-4 h-4 text-emerald-400" />
                      <span className="font-bold text-white">المساحة الحرة على الجهاز</span>
                    </div>
                    <span className="text-emerald-400 font-bold font-mono">
                      {freeSpaceGB.toFixed(1)} GB / {totalSpaceGB} GB
                    </span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-emerald-500 to-teal-400"
                      style={{ width: `${(freeSpaceGB / totalSpaceGB) * 100}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* 5. متقدم (Advanced) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">5. متقدم</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* yt-dlp Update */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <RefreshCw className={`w-4 h-4 text-emerald-400 shrink-0 ${isUpdatingYtDlp ? 'animate-spin' : ''}`} />
                    <div>
                      <div className="font-bold text-white">تحديث محرك yt-dlp</div>
                      <div className="text-[11px] text-slate-400">
                        الإصدار: <span className="font-mono text-emerald-400">{ytDlpVersion}</span> • قناة {ytDlpChannel}
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={handleUpdateYtDlp}
                    disabled={isUpdatingYtDlp}
                    className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold transition disabled:opacity-50 cursor-pointer"
                  >
                    {isUpdatingYtDlp ? 'جارٍ الفحص…' : 'تحديث الآن'}
                  </button>
                </div>

                {/* yt-dlp Channel Toggle */}
                <div
                  onClick={() => {
                    const next = ytDlpChannel === 'STABLE' ? 'NIGHTLY' : 'STABLE';
                    setYtDlpChannel(next);
                    showToast(`تم تغيير قناة التحديث إلى ${next}`);
                  }}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Sliders className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">قناة تحديث yt-dlp</div>
                      <div className="text-[11px] text-slate-400">الاختيار بين الإصدار المستقر والتجريبي</div>
                    </div>
                  </div>
                  <span className="text-emerald-400 font-bold">{ytDlpChannel}</span>
                </div>

                {/* yt-dlp Auto Update */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <RefreshCw className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تحديث تلقائي لـ yt-dlp</div>
                      <div className="text-[11px] text-slate-400">فحص وتحديث دوري كل 24 ساعة عبر Wi-Fi</div>
                    </div>
                  </div>
                  <button
                    onClick={() => setYtDlpAutoUpdate(!ytDlpAutoUpdate)}
                    className={`w-11 h-6 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                      ytDlpAutoUpdate ? 'bg-emerald-500' : 'bg-slate-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition-transform ${
                        ytDlpAutoUpdate ? '-translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Cookies File */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Cookie className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">ملف cookies (Netscape)</div>
                      <div className="text-[11px] text-slate-400">
                        {cookiesFileName ? `الملف النشط: ${cookiesFileName}` : 'لا يوجد ملف مستورد'}
                      </div>
                    </div>
                  </div>
                  {cookiesFileName ? (
                    <button
                      onClick={() => {
                        setCookiesFileName(null);
                        showToast('تم حذف ملف الكوكيز');
                      }}
                      className="px-2.5 py-1 text-red-400 hover:text-red-300 font-bold"
                    >
                      حذف
                    </button>
                  ) : (
                    <button
                      onClick={() => {
                        setCookiesFileName('cookies.txt');
                        showToast('تم استيراد ملف الكوكيز بنجاح');
                      }}
                      className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold border border-slate-700"
                    >
                      استيراد
                    </button>
                  )}
                </div>

                {/* Network Proxy */}
                <div
                  onClick={() => setActiveModal('proxy')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Shield className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">بروكسي (Proxy)</div>
                      <div className="text-[11px] text-slate-400">
                        {proxyEnabled ? `${proxyType}://${proxyHost}:${proxyPort}` : 'معطل'}
                      </div>
                    </div>
                  </div>
                  <span className={`font-bold ${proxyEnabled ? 'text-emerald-400' : 'text-slate-500'}`}>
                    {proxyEnabled ? 'مفعّل' : 'معطّل'}
                  </span>
                </div>

                {/* Battery Optimization */}
                <div
                  onClick={() => showToast('فتح شاشة تجاهل تحسين البطارية (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <BatteryCharging className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تجاهل تحسين البطارية</div>
                      <div className="text-[11px] text-slate-400">السماح للتطبيق بالعمل في الخلفية دون انقطاع التحميل</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                </div>

                {/* Export Diagnostic Log */}
                <div
                  onClick={() => showToast('تم تجهيز ومشاركة سجل التشخيص (ACTION_SEND)')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Share2 className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تصدير سجل التشخيص</div>
                      <div className="text-[11px] text-slate-400">مشاركة معلومات الجهاز والبيئة لتشخيص المشاكل الفنية</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                </div>

                {/* Reset Defaults */}
                <div
                  onClick={() => setActiveModal('reset')}
                  className="p-3.5 flex items-center justify-between hover:bg-red-500/10 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <RotateCcw className="w-4 h-4 text-red-400 shrink-0" />
                    <div>
                      <div className="font-bold text-red-400">إعادة الضبط للإعدادات الافتراضية</div>
                      <div className="text-[11px] text-slate-400">استعادة كافة خيارات التطبيق لقيم التثبيت الأصلية</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-red-500" />
                </div>
              </div>
            </div>

            {/* 6. حول (About) */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-emerald-400 px-1">6. حول</span>
              <div className="rounded-2xl bg-slate-950/70 border border-slate-800 divide-y divide-slate-800/80 overflow-hidden text-xs">
                {/* Version */}
                <div className="p-3.5 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Info className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">إصدار التطبيق</div>
                      <div className="text-[11px] text-slate-400">VidGet 1.0.0 (بنية 1)</div>
                    </div>
                  </div>
                  <span className="text-slate-400 font-mono text-[11px]">v1.0.0</span>
                </div>

                {/* Privacy Policy */}
                <div
                  onClick={() => setActiveModal('privacy')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <Shield className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">سياسة الخصوصية</div>
                      <div className="text-[11px] text-slate-400">لا جمع للبيانات، بدون تتبع، كافة العمليات تجري على هاتفك</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                </div>

                {/* Legal Notice */}
                <div
                  onClick={() => setActiveModal('legal')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <AlertTriangle className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">إشعار قانوني</div>
                      <div className="text-[11px] text-slate-400">تطبيق VidGet مخصص للاستخدام للمحتوى المسموح بتحميله قانونيًا</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                </div>

                {/* Open Source Licenses */}
                <div
                  onClick={() => setActiveModal('licenses')}
                  className="p-3.5 flex items-center justify-between hover:bg-slate-800/50 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3">
                    <FileCode className="w-4 h-4 text-emerald-400 shrink-0" />
                    <div>
                      <div className="font-bold text-white">تراخيص المكتبات المفتوحة</div>
                      <div className="text-[11px] text-slate-400">تراخيص البرمجيات مفتوحة المصدر (AboutLibraries)</div>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 rotate-180 text-slate-500" />
                </div>
              </div>
            </div>
          </div>

          {/* Android Navigation Bar */}
          <div className="bg-slate-950 py-2.5 flex items-center justify-around border-t border-slate-800 text-slate-500">
            <div className="w-3 h-3 border-2 border-slate-600 rounded-sm" />
            <div className="w-3.5 h-3.5 border-2 border-slate-600 rounded-full" />
            <div className="w-3 h-3 border-r-2 border-b-2 border-slate-600 rotate-45" />
          </div>

          {/* Floating Toast Notification */}
          {toastMessage && (
            <div className="absolute bottom-14 left-4 right-4 bg-emerald-600/95 text-white px-4 py-2.5 rounded-xl text-xs font-bold shadow-xl flex items-center justify-between z-20 backdrop-blur animate-fade-in">
              <span>{toastMessage}</span>
              <Check className="w-4 h-4 text-white" />
            </div>
          )}
        </div>
      </div>

      {/* Right Column: Live DataStore Preferences Inspector & Architecture Info */}
      <div className="lg:col-span-5 space-y-4 text-right">
        {/* Architecture Card */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <h3 className="font-bold text-sm text-white flex items-center gap-2">
            <Settings className="w-4 h-4 text-emerald-400" />
            مواصفات SettingsScreen وSettingsRepository المحققة
          </h3>
          <ul className="space-y-2 text-xs text-slate-300">
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 font-bold">✓</span>
              <span><strong>الأقسام الستة الثابتة:</strong> عام، التحميل، الشبكة، التخزين، متقدم، حول بدون أي عناصر زائدة.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 font-bold">✓</span>
              <span><strong>مستودع واحد SettingsRepository:</strong> يوفر Flow منفصل لكل مفتاح في DataStore.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 font-bold">✓</span>
              <span><strong>تطبيق فوري بدون زر حفظ:</strong> كل Switch أو Slider أو Dialog يحدّث الـ DataStore فورًا.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 font-bold">✓</span>
              <span><strong>أدوات التخزين ومقاييس النظام:</strong> مسح الكاش، حذف المؤقتات اليتيمة، وفحص المساحة بـ StatFs.</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 font-bold">✓</span>
              <span><strong>تحديث yt-dlp وإدارة الكوكيز:</strong> دعم قناتي Stable/Nightly واستيراد ملفات Netscape وبروكسي مخصص.</span>
            </li>
          </ul>
        </div>

        {/* Live DataStore JSON Inspector */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3 font-mono">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-white flex items-center gap-2">
              <Database className="w-4 h-4 text-teal-400" />
              مراقب DataStore التفاعلي (Live Preferences State)
            </h4>
            <span className="text-[10px] px-2 py-0.5 rounded bg-teal-500/20 text-teal-300 border border-teal-500/30">
              Flow StateFlow
            </span>
          </div>

          <pre className="text-[11px] text-emerald-400 bg-slate-950 p-4 rounded-xl border border-slate-800 max-h-80 overflow-auto leading-relaxed text-left" dir="ltr">
            {JSON.stringify(
              {
                general: {
                  language,
                  themeMode,
                  dynamicColor,
                  clipboardDetection
                },
                downloads: {
                  saveDir,
                  namingTemplate,
                  conflictResolution,
                  subfolderOrg,
                  defaultQuality,
                  defaultFormat,
                  connectionsCount,
                  maxConcurrent,
                  embedMetadata
                },
                network: {
                  wifiOnly,
                  dataSaver,
                  speedLimit,
                  scheduleEnabled,
                  scheduleHours,
                  blockRoaming
                },
                storageMetrics: {
                  cacheSizeMB: `${cacheSizeMB.toFixed(1)} MB`,
                  orphanFiles: `${orphanFilesCount} files (${orphanFilesMB.toFixed(1)} MB)`,
                  freeSpace: `${freeSpaceGB.toFixed(1)} GB / ${totalSpaceGB} GB`
                },
                advanced: {
                  ytDlpVersion,
                  ytDlpChannel,
                  ytDlpAutoUpdate,
                  cookiesFileName,
                  proxy: {
                    enabled: proxyEnabled,
                    type: proxyType,
                    host: proxyHost,
                    port: proxyPort
                  }
                }
              },
              null,
              2
            )}
          </pre>
        </div>
      </div>

      {/* Interactive Modal Dialogs */}
      {activeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm">
          <div className="w-full max-w-sm bg-slate-900 border border-slate-700 rounded-3xl p-6 shadow-2xl space-y-4 text-right">
            {activeModal === 'language' && (
              <>
                <h4 className="font-bold text-white text-base">اختيار لغة التطبيق</h4>
                <div className="space-y-2 text-xs">
                  {[
                    { key: 'ar', label: 'العربية' },
                    { key: 'en', label: 'English' },
                    { key: 'system', label: 'تلقائي (لغة النظام)' }
                  ].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => {
                        setLanguage(item.key as any);
                        setActiveModal(null);
                        showToast(`تم تعيين اللغة إلى: ${item.label}`);
                      }}
                      className={`w-full p-3 rounded-xl flex items-center justify-between border cursor-pointer ${
                        language === item.key
                          ? 'border-emerald-500 bg-emerald-500/10 text-emerald-400 font-bold'
                          : 'border-slate-800 bg-slate-950 text-slate-300'
                      }`}
                    >
                      <span>{item.label}</span>
                      {language === item.key && <Check className="w-4 h-4 text-emerald-400" />}
                    </button>
                  ))}
                </div>
              </>
            )}

            {activeModal === 'theme' && (
              <>
                <h4 className="font-bold text-white text-base">اختيار المظهر (Theme)</h4>
                <div className="space-y-2 text-xs">
                  {[
                    { key: 'dark', label: 'داكن (Dark)' },
                    { key: 'light', label: 'فاتح (Light)' },
                    { key: 'system', label: 'تلقائي (حسب نظام الهاتف)' }
                  ].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => {
                        setThemeMode(item.key as any);
                        setActiveModal(null);
                        showToast(`تم تعيين المظهر إلى: ${item.label}`);
                      }}
                      className={`w-full p-3 rounded-xl flex items-center justify-between border cursor-pointer ${
                        themeMode === item.key
                          ? 'border-emerald-500 bg-emerald-500/10 text-emerald-400 font-bold'
                          : 'border-slate-800 bg-slate-950 text-slate-300'
                      }`}
                    >
                      <span>{item.label}</span>
                      {themeMode === item.key && <Check className="w-4 h-4 text-emerald-400" />}
                    </button>
                  ))}
                </div>
              </>
            )}

            {activeModal === 'quality' && (
              <>
                <h4 className="font-bold text-white text-base">الجودة الافتراضية للتحميل</h4>
                <div className="space-y-2 text-xs">
                  {[
                    { key: 'ALWAYS_ASK', label: 'دائمًا اسأل' },
                    { key: 'BEST', label: 'الأفضل المتاح' },
                    { key: '1080p', label: '1080p (عالية الدقة)' },
                    { key: '720p', label: '720p (متوسطة)' },
                    { key: '480p', label: '480p (منخفضة)' },
                    { key: 'AUDIO_ONLY', label: 'صوت فقط' }
                  ].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => {
                        setDefaultQuality(item.key);
                        setActiveModal(null);
                        showToast(`تم تعيين الجودة الافتراضية إلى: ${item.label}`);
                      }}
                      className={`w-full p-3 rounded-xl flex items-center justify-between border cursor-pointer ${
                        defaultQuality === item.key
                          ? 'border-emerald-500 bg-emerald-500/10 text-emerald-400 font-bold'
                          : 'border-slate-800 bg-slate-950 text-slate-300'
                      }`}
                    >
                      <span>{item.label}</span>
                      {defaultQuality === item.key && <Check className="w-4 h-4 text-emerald-400" />}
                    </button>
                  ))}
                </div>
              </>
            )}

            {activeModal === 'conflict' && (
              <>
                <h4 className="font-bold text-white text-base">سلوك تعارض الأسماء</h4>
                <div className="space-y-2 text-xs">
                  {[
                    { key: 'AUTO_RENAME', label: 'إعادة تسمية تلقائية: "video (1).mp4"' },
                    { key: 'OVERWRITE', label: 'استبدال الملف الموجود' },
                    { key: 'SKIP', label: 'تخطي التنزيل' },
                    { key: 'ASK', label: 'سؤال المستخدم كل مرة' }
                  ].map((item) => (
                    <button
                      key={item.key}
                      onClick={() => {
                        setConflictResolution(item.key);
                        setActiveModal(null);
                        showToast(`تم تعيين سلوك التعارض: ${item.label}`);
                      }}
                      className={`w-full p-3 rounded-xl flex items-center justify-between border cursor-pointer ${
                        conflictResolution === item.key
                          ? 'border-emerald-500 bg-emerald-500/10 text-emerald-400 font-bold'
                          : 'border-slate-800 bg-slate-950 text-slate-300'
                      }`}
                    >
                      <span>{item.label}</span>
                      {conflictResolution === item.key && <Check className="w-4 h-4 text-emerald-400" />}
                    </button>
                  ))}
                </div>
              </>
            )}

            {activeModal === 'proxy' && (
              <>
                <h4 className="font-bold text-white text-base">إعداد البروكسي (Proxy)</h4>
                <div className="space-y-3 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-300">تفعيل البروكسي:</span>
                    <button
                      onClick={() => setProxyEnabled(!proxyEnabled)}
                      className={`w-10 h-5 rounded-full transition-colors cursor-pointer relative p-0.5 ${
                        proxyEnabled ? 'bg-emerald-500' : 'bg-slate-700'
                      }`}
                    >
                      <div
                        className={`w-4 h-4 rounded-full bg-white transition-transform ${
                          proxyEnabled ? '-translate-x-5' : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  <div className="flex gap-2">
                    <button
                      onClick={() => setProxyType('HTTP')}
                      className={`flex-1 py-1.5 rounded-lg border font-bold ${
                        proxyType === 'HTTP' ? 'bg-emerald-600 border-emerald-500 text-white' : 'border-slate-800 bg-slate-950 text-slate-400'
                      }`}
                    >
                      HTTP
                    </button>
                    <button
                      onClick={() => setProxyType('SOCKS5')}
                      className={`flex-1 py-1.5 rounded-lg border font-bold ${
                        proxyType === 'SOCKS5' ? 'bg-emerald-600 border-emerald-500 text-white' : 'border-slate-800 bg-slate-950 text-slate-400'
                      }`}
                    >
                      SOCKS5
                    </button>
                  </div>

                  <input
                    type="text"
                    value={proxyHost}
                    onChange={(e) => setProxyHost(e.target.value)}
                    placeholder="عنوان الخادم (e.g. 127.0.0.1)"
                    className="w-full p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white font-mono text-xs text-left"
                    dir="ltr"
                  />
                  <input
                    type="number"
                    value={proxyPort}
                    onChange={(e) => setProxyPort(parseInt(e.target.value) || 8080)}
                    placeholder="المنفذ (e.g. 8080)"
                    className="w-full p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white font-mono text-xs text-left"
                    dir="ltr"
                  />
                </div>
              </>
            )}

            {activeModal === 'reset' && (
              <>
                <div className="flex items-center gap-3 text-red-400">
                  <AlertTriangle className="w-6 h-6" />
                  <h4 className="font-bold text-white text-base">إعادة ضبط الإعدادات؟</h4>
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  سيتم إعادة ضبط كافة التفضيلات ومجلدات الحفظ وخيارات التنزيل إلى الحالة الأولية. لن يتم حذف مقاطع الفيديو المحمّلة مسبقًا.
                </p>
                <div className="flex justify-end gap-2 pt-2">
                  <button
                    onClick={() => setActiveModal(null)}
                    className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold"
                  >
                    إلغاء
                  </button>
                  <button
                    onClick={handleResetDefaults}
                    className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold"
                  >
                    إعادة الضبط الآن
                  </button>
                </div>
              </>
            )}

            {activeModal === 'privacy' && (
              <>
                <h4 className="font-bold text-white text-base flex items-center gap-2">
                  <Shield className="w-5 h-5 text-emerald-400" />
                  سياسة الخصوصية
                </h4>
                <p className="text-xs text-slate-300 leading-relaxed">
                  تطبيق VidGet لا يجمع أي بيانات شخصية، لا يتضمن أي مكتبات إعلانية أو أدوات تتبع (Zero Analytics)، وكافة عمليات تحليل الروابط والتحميل والتشفير تجري محليًا بالكامل على جهازك.
                </p>
              </>
            )}

            {activeModal === 'legal' && (
              <>
                <h4 className="font-bold text-white text-base flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-amber-400" />
                  إشعار قانوني
                </h4>
                <p className="text-xs text-slate-300 leading-relaxed">
                  تطبيق VidGet مخصص حصريًا للاستخدام الشخصي ولتنزيل المحتوى الذي تملكه أو المحتوى المرخص للنشر والاستخدام الحر. لا يشجع التطبيق على انتهاك حقوق الملكية الفكرية أو شروط خدمة المنصات.
                </p>
              </>
            )}

            {activeModal === 'licenses' && (
              <>
                <h4 className="font-bold text-white text-base flex items-center gap-2">
                  <FileCode className="w-5 h-5 text-emerald-400" />
                  تراخيص المكتبات المفتوحة
                </h4>
                <div className="space-y-1.5 text-xs text-slate-300 font-mono max-h-48 overflow-y-auto">
                  <p>• yt-dlp & youtubedl-android (Unlicense)</p>
                  <p>• FFmpeg (LGPL / GPL)</p>
                  <p>• Aria2c (GPL v2)</p>
                  <p>• Google Media3 ExoPlayer (Apache 2.0)</p>
                  <p>• Jetpack Compose & DataStore (Apache 2.0)</p>
                  <p>• OkHttp & Coil (Apache 2.0)</p>
                </div>
              </>
            )}

            {activeModal !== 'reset' && (
              <div className="flex justify-end pt-2">
                <button
                  onClick={() => setActiveModal(null)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-bold hover:bg-slate-700"
                >
                  إغلاق
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
