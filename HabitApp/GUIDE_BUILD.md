# 📱 دليل بناء تطبيق عاداتي Pro على Android Studio
## شرح تفصيلي خطوة بخطوة من الصفر حتى التشغيل

---

## 🏗️ المعمارية العامة للمشروع

```
عاداتي Pro
├── طبقة البيانات (Data Layer)
│   ├── Models      ← تعريف شكل البيانات (Kotlin data classes)
│   ├── DAOs        ← عمليات قاعدة البيانات (SQL queries)
│   └── AppDatabase ← نقطة الدخول الوحيدة (Singleton)
│
├── طبقة الواجهة (UI Layer)
│   ├── Fragments   ← شاشات داخل MainActivity
│   ├── Activities  ← شاشات مستقلة
│   ├── Adapters    ← ربط البيانات بالقوائم
│   └── BottomSheets← نوافذ منبثقة من أسفل
│
├── Workers (الخلفية)
│   ├── HabitReminderWorker  ← تذكير يومي 8 ص
│   └── TodoReminderWorker   ← تنبيهات المهام
│
└── Utils (مساعدات)
    ├── NotificationHelper   ← إدارة كل التنبيهات
    ├── AchievementEngine    ← محرك الإنجازات + XP
    ├── TodoReminderScheduler← جدولة تنبيهات المهام
    └── TaskTemplates        ← 126 مهمة جاهزة
```

---

## 📋 الخطوة 1: إنشاء المشروع في Android Studio

### 1.1 — فتح Android Studio وإنشاء مشروع جديد

1. افتح **Android Studio** (تأكد من الإصدار Hedgehog 2023.1.1 أو أحدث)
2. اضغط **New Project**
3. اختر **Empty Views Activity** (ليس Compose)
4. اضغط **Next** وأدخل:

| الحقل | القيمة |
|-------|--------|
| Name | HabitApp |
| Package name | com.yourname.habitapp |
| Save location | اختر مجلداً مناسباً |
| Language | **Kotlin** |
| Minimum SDK | **API 24 (Android 7.0)** |

5. اضغط **Finish** وانتظر حتى ينتهي Gradle Sync

---

## 📋 الخطوة 2: إنشاء هيكل المجلدات

### 2.1 — هيكل الملفات المطلوب

في Android Studio، انقر بالزر الأيمن على مجلد `java/com.yourname.habitapp/`
وأنشئ الحزم (Packages) التالية:

```
java/com.yourname.habitapp/
├── data/
│   ├── models/        ← انقر بالزر الأيمن → New → Package
│   ├── dao/
│   └── (AppDatabase.kt هنا)
├── ui/
│   ├── habits/
│   ├── todo/
│   ├── goals/
│   ├── achievements/
│   ├── stats/
│   └── share/
├── worker/
└── utils/
```

**طريقة الإنشاء:**
```
Right-click على habitapp → New → Package → اكتب "data"
Right-click على data → New → Package → اكتب "models"
... وهكذا لكل مجلد
```

---

## 📋 الخطوة 3: إعداد المكتبات (Dependencies)

### 3.1 — تعديل app/build.gradle

افتح ملف `app/build.gradle` واستبدل محتواه بما يلي:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'            // مطلوب لـ Room
}

android {
    namespace 'com.yourname.habitapp'
    compileSdk 34

    defaultConfig {
        applicationId "com.yourname.habitapp"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = '1.8' }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.cardview:cardview:1.0.0'

    // Room Database
    implementation "androidx.room:room-runtime:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"

    // Lifecycle
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    // WorkManager
    implementation "androidx.work:work-runtime-ktx:2.9.0"

    // Navigation
    implementation "androidx.navigation:navigation-fragment-ktx:2.7.7"
    implementation "androidx.navigation:navigation-ui-ktx:2.7.7"

    // MPAndroidChart (الرسوم البيانية)
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
```

### 3.2 — تعديل settings.gradle

أضف JitPack (مطلوب لـ MPAndroidChart):

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }   // ← أضف هذا السطر
    }
}
```

### 3.3 — Sync المكتبات

اضغط **Sync Now** في الشريط الأصفر الذي يظهر، أو:
`File → Sync Project with Gradle Files`

انتظر حتى ترى: **BUILD SUCCESSFUL** في نافذة Build

---

## 📋 الخطوة 4: نسخ ملفات Kotlin

### ترتيب النسخ المهم

انسخ الملفات بهذا الترتيب لتجنب أخطاء الاعتمادية:

#### المرحلة أ — نماذج البيانات (أولاً)

```
data/models/Habit.kt
data/models/TodoItem.kt
data/models/YearGoal.kt      (يحتوي على GoalStep أيضاً)
data/models/Achievement.kt
```

#### المرحلة ب — قاعدة البيانات

```
data/dao/HabitDao.kt
data/dao/TodoDao.kt
data/dao/YearGoalDao.kt      (يحتوي على AchievementDao)
data/AppDatabase.kt
```

#### المرحلة ج — Utils والـ Workers

```
utils/NotificationHelper.kt
utils/AchievementEngine.kt
utils/TaskTemplates.kt
utils/TodoReminderScheduler.kt
worker/HabitReminderWorker.kt
worker/TodoReminderWorker.kt
```

#### المرحلة د — الواجهات

```
ui/MainActivity.kt
ui/habits/HabitsFragment.kt
ui/habits/HabitAdapter.kt
ui/habits/AddHabitActivity.kt
ui/todo/TodoFragment.kt
ui/todo/TodoAdapter.kt
ui/todo/AddTodoBottomSheet.kt
ui/goals/YearGoalsFragment.kt
ui/goals/YearGoalAdapter.kt
ui/goals/AddGoalActivity.kt
ui/achievements/AchievementsFragment.kt
ui/achievements/AchievementAdapter.kt
ui/achievements/AchievementDetailDialog.kt
```

---

## 📋 الخطوة 5: إنشاء ملفات الواجهة (XML Layouts)

في مجلد `res/layout/` أنشئ الملفات التالية:

### 5.1 — activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- حاوية الـ Fragments -->
    <FrameLayout
        android:id="@+id/fragmentContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/bottomNavigation"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- شريط التنقل السفلي -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavigation"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:menu="@menu/bottom_nav_menu"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 5.2 — fragment_habits.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- شريط التقدم اليومي -->
    <TextView android:id="@+id/tvProgress"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="0 / 0 مكتملة"
        android:textSize="16sp"
        android:gravity="end"
        android:padding="8dp" />

    <com.google.android.material.progressindicator.LinearProgressIndicator
        android:id="@+id/progressBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp" />

    <!-- قائمة العادات -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerHabits"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- زر الإضافة -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAddHabit"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end"
        android:layout_margin="16dp"
        android:src="@android:drawable/ic_input_add"
        android:contentDescription="إضافة عادة" />

</LinearLayout>
```

### 5.3 — item_habit.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="6dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="3dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">

        <!-- الأيقونة -->
        <TextView android:id="@+id/tvHabitIcon"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:textSize="24sp"
            android:gravity="center" />

        <!-- اسم العادة + التكرار -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="8dp">

            <TextView android:id="@+id/tvHabitName"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView android:id="@+id/tvFrequency"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="#888888" />
        </LinearLayout>

        <!-- Streak -->
        <TextView android:id="@+id/tvStreak"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:layout_marginEnd="8dp" />

        <!-- CheckBox الإتمام -->
        <CheckBox android:id="@+id/checkHabitDone"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content" />

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 5.4 — item_todo.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="6dp"
    app:cardCornerRadius="10dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <!-- شريط الأولوية الملوّن -->
        <View android:id="@+id/viewPriorityBar"
            android:layout_width="6dp"
            android:layout_height="match_parent"
            android:background="#FF4444" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:padding="12dp">

            <TextView android:id="@+id/tvTodoTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="15sp"
                android:textStyle="bold" />

            <TextView android:id="@+id/tvTodoTime"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="#666666"
                android:visibility="gone" />

            <TextView android:id="@+id/tvPriority"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="11sp" />
        </LinearLayout>

        <CheckBox android:id="@+id/checkTodoDone"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="8dp" />

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 5.5 — menu/bottom_nav_menu.xml

أنشئ مجلد `res/menu/` ثم:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">

    <item android:id="@+id/nav_habits"
        android:icon="@android:drawable/star_on"
        android:title="العادات" />

    <item android:id="@+id/nav_todo"
        android:icon="@android:drawable/ic_menu_agenda"
        android:title="المهام" />

    <item android:id="@+id/nav_goals"
        android:icon="@android:drawable/ic_menu_compass"
        android:title="الأهداف" />

    <item android:id="@+id/nav_achievements"
        android:icon="@android:drawable/ic_menu_upload"
        android:title="الإنجازات" />

</menu>
```

---

## 📋 الخطوة 6: تعديل AndroidManifest.xml

استبدل محتوى `AndroidManifest.xml` بما يلي:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="عاداتي"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.HabitApp">

        <activity android:name=".ui.MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".ui.habits.AddHabitActivity" />
        <activity android:name=".ui.goals.AddGoalActivity" />

    </application>
</manifest>
```

---

## 📋 الخطوة 7: إنشاء أيقونة التنبيه

في Android Studio:
1. `res/drawable/` ← Right-click
2. **New → Vector Asset**
3. اختر أي أيقونة بسيطة (مثل `ic_notifications`)
4. سمّها `ic_notification`
5. اضغط **Finish**

---

## 📋 الخطوة 8: تشغيل المشروع

### 8.1 — على المحاكي (Emulator)

1. اضغط **Tools → Device Manager**
2. اضغط **Create Device**
3. اختر **Pixel 6** ← **API 34** ← **Finish**
4. اضغط ▶️ (Run) في شريط الأدوات العلوي

### 8.2 — على هاتف حقيقي

1. على الهاتف: **الإعدادات → حول الهاتف → رقم الإصدار** (اضغط 7 مرات)
2. **الإعدادات → خيارات المطور → تصحيح USB → تفعيل**
3. وصّل الهاتف بالكمبيوتر
4. في Android Studio اختر اسم هاتفك من القائمة المنسدلة
5. اضغط ▶️

---

## 🐛 أخطاء شائعة وحلولها

| الخطأ | السبب | الحل |
|-------|-------|------|
| `Unresolved reference: R` | ملف XML به خطأ | تحقق من كل ملفات الـ layout |
| `KAPT error` | نسيت `id 'kotlin-kapt'` | أضفه في plugins بـ build.gradle |
| `Cannot find symbol: HabitDao` | Room لم يُولّد الكود | Build → Clean → Rebuild |
| `BUILD FAILED: duplicate class` | تعارض في الإصدارات | تحقق من dependency واحد لكل مكتبة |
| `Room Schema Error` | غيّرت الموديل دون تغيير version | أزل التطبيق من المحاكي وأعد التشغيل |
| `WorkManager لا يعمل` | نقص POST_NOTIFICATIONS | تحقق من صلاحيات الـ Manifest |
| `MPAndroidChart: Could not resolve` | JitPack غير مضاف | أضف `maven { url 'https://jitpack.io' }` لـ settings.gradle |
| `java.lang.NullPointerException` | View لم يُجد في layout | تأكد من تطابق id في XML والكود |

---

## 🔄 تدفق البيانات في التطبيق

```
المستخدم يضغط CheckBox
    ↓
HabitsFragment.onHabitCompleted()
    ↓
db.habitDao().updateHabitStreak()       ← تحديث قاعدة البيانات
    ↓
NotificationHelper.showHabitComplete()  ← تنبيه إتمام
    ↓
AchievementEngine.addXP()              ← إضافة XP
    ↓
AchievementEngine.checkAndUnlock()     ← فحص الإنجازات
    ↓
LiveData تُشعر الـ Observer
    ↓
adapter.submitList()                   ← تحديث الواجهة تلقائياً
```

---

## 🚀 النشر على Google Play

### الخطوات:
1. **Build → Generate Signed Bundle → Android App Bundle**
2. أنشئ Keystore جديد (احتفظ به في مكان آمن للأبد!)
3. ارفع ملف `.aab` على [play.google.com/console](https://play.google.com/console)
4. أكمل معلومات المتجر (صور + وصف + سياسة الخصوصية)
5. انتظر مراجعة Google (1-3 أيام)

### متطلبات صفحة المتجر:
| العنصر | المواصفات |
|--------|-----------|
| أيقونة | 512×512 PNG |
| لقطات شاشة | 2+ لقطة 1080×1920 |
| وصف قصير | 80 حرف |
| سياسة الخصوصية | رابط مجاني (Google Sites أو GitHub Pages) |

---

## 💰 استراتيجية الربح

### AdMob:
1. سجّل على [admob.google.com](https://admob.google.com)
2. أضف `implementation 'com.google.android.gms:play-services-ads:23.0.0'`
3. أضف Banner Ad في أسفل الشاشة الرئيسية

### الاشتراك Premium:
1. أنشئ اشتراكاً في Google Play Console
2. استخدم Google Play Billing Library لإدارة الاشتراكات

---

## ✅ قائمة التحقق النهائية قبل النشر

- [ ] اختبرت على جهازين مختلفين
- [ ] اختبرت التنبيهات (تأكد أنها تصل)
- [ ] اختبرت إغلاق التطبيق وإعادة فتحه
- [ ] تأكدت من عمل Room بعد تحديث البيانات
- [ ] اختبرت الترتيب RTL (عربي)
- [ ] أضفت سياسة الخصوصية
- [ ] غيّرت `com.yourname` لاسمك الحقيقي
- [ ] رفعت الكود على GitHub (احتياطي)

---

**🎉 مبروك! تطبيقك جاهز للنشر!**
