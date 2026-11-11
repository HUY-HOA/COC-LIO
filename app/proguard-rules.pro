-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepnames class com.huyhoa.coclio.MainActivity
-keepnames class com.huyhoa.coclio.UpgradeWidgetConfigActivity
-keepnames class com.huyhoa.coclio.UpgradeWidgetListService
-keepnames class com.huyhoa.coclio.UpgradeWidgetProvider

-keepnames class com.huyhoa.coclio.UpgradeReminderWorker
-keepclassmembers class com.huyhoa.coclio.UpgradeReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keepnames class com.huyhoa.coclio.WidgetRefreshWorker
-keepclassmembers class com.huyhoa.coclio.WidgetRefreshWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keepnames class com.huyhoa.coclio.WidgetIconPreloadWorker
-keepclassmembers class com.huyhoa.coclio.WidgetIconPreloadWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
