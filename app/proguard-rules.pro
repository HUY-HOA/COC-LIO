-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepnames class com.huyhoa.coclio.MainActivity
-keepnames class com.huyhoa.coclio.UpgradeWidgetConfigActivity
-keepnames class com.huyhoa.coclio.UpgradeWidgetListService
-keepnames class com.huyhoa.coclio.UpgradeWidgetProvider

-keep class com.huyhoa.coclio.** extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
