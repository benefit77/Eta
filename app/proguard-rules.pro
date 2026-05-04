# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Xposed 通过 META-INF/xposed/java_init.list 中的类名字符串加载模块入口；
# 这里仅保留入口类当前承担模块启动职责的必要成员，避免 release 混淆或裁剪后模块失效。
-keep class fuck.andes.ModuleMain {
    void <init>();
    fuck.andes.ModuleLogger logger;
    boolean systemServerInstalled;
    boolean systemUiInstalled;
    boolean googleInstalled;
    boolean colorDirectInstalled;
    void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
    void onSystemServerStarting(io.github.libxposed.api.XposedModuleInterface$SystemServerStartingParam);
    void onPackageReady(io.github.libxposed.api.XposedModuleInterface$PackageReadyParam);
}
