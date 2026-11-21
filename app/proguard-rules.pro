# Keep DreamService, Application, and Activity entry points instantiated by the Android framework
-keep class dev.neonlauncher.NeonLauncherApp { *; }
-keep class dev.neonlauncher.MainActivity { *; }
-keep class dev.neonlauncher.dream.NeonDreamService { *; }

# Keep DataStore protobuf runtime if generated/used internally
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
