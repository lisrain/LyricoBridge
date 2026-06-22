package com.hook.musictag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {

    private static final String TAG = "LyricoBridge";
    private static final String SALT_PKG = "com.salt.music";
    private static final String OLD_PKG = "com.xjcheng.musictageditor";
    private static final String OLD_CLASS = "com.xjcheng.musictageditor.SongDetailActivity";
    private static final String NEW_PKG = "com.lonx.lyrico";
    private static final String NEW_CLASS = "com.lonx.lyrico.MainActivity";

    private static final int GRANT_URI_PERMISSION = 0x00000001;

    private static final String OLD_APP_NAME = "音乐标签";
    private static final String NEW_APP_NAME = "Lyrico";

    public HookEntry() {
        Log.i(TAG, "HookEntry constructor called");
    }

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        Log.i(TAG, "onModuleLoaded: " + param.getProcessName());
        Log.i(TAG, "API version: " + getApiVersion());
        Log.i(TAG, "Framework: " + getFrameworkName() + " " + getFrameworkVersion());
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        Log.i(TAG, "onPackageLoaded: " + param.getPackageName());

        if (!SALT_PKG.equals(param.getPackageName())) {
            return;
        }

        Log.i(TAG, "Target package matched, installing hooks...");
        ClassLoader cl = param.getDefaultClassLoader();

        // Hook 1: ComponentActivity.startActivityForResult
        hookStartActivityForResult(cl);

        // Hook 2: Context.getString(int) - 修改资源字符串
        hookGetString(cl);

        // Hook 3: vo3 toast 方法 - 修改硬编码 toast
        hookToast(cl);
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!SALT_PKG.equals(param.getPackageName())) {
            return;
        }
        Log.i(TAG, "onPackageReady: " + param.getPackageName());
    }

    private void hookStartActivityForResult(ClassLoader cl) {
        try {
            Class<?> clazz = Class.forName("androidx.activity.ComponentActivity", false, cl);

            Method m1 = clazz.getMethod("startActivityForResult", Intent.class, int.class);
            Method m2 = clazz.getMethod("startActivityForResult", Intent.class, int.class, android.os.Bundle.class);

            hook(m1).intercept(chain -> {
                Log.d(TAG, ">>> Intercepted startActivityForResult(Intent, int)");
                handleIntent(chain);
                return chain.proceed();
            });

            hook(m2).intercept(chain -> {
                Log.d(TAG, ">>> Intercepted startActivityForResult(Intent, int, Bundle)");
                handleIntent(chain);
                return chain.proceed();
            });

            Log.i(TAG, "Hook installed: ComponentActivity.startActivityForResult");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook ComponentActivity: " + e.getMessage(), e);
            try {
                Method m1 = android.app.Activity.class.getMethod("startActivityForResult", Intent.class, int.class);
                Method m2 = android.app.Activity.class.getMethod("startActivityForResult", Intent.class, int.class, android.os.Bundle.class);

                hook(m1).intercept(chain -> {
                    handleIntent(chain);
                    return chain.proceed();
                });

                hook(m2).intercept(chain -> {
                    handleIntent(chain);
                    return chain.proceed();
                });

                Log.i(TAG, "Fallback hook installed: Activity.startActivityForResult");
            } catch (Throwable e2) {
                Log.e(TAG, "Fallback also failed: " + e2.getMessage(), e2);
            }
        }
    }

    private void hookGetString(ClassLoader cl) {
        try {
            // Hook Context.getString(int) - 检查结果是否包含 "音乐标签"
            Method getString = android.content.Context.class.getMethod("getString", int.class);
            hook(getString).intercept(chain -> {
                String result = (String) chain.proceed();
                if (result != null && result.contains(OLD_APP_NAME)) {
                    String replaced = result.replace(OLD_APP_NAME, NEW_APP_NAME);
                    Log.d(TAG, ">>> getString replaced: " + result + " -> " + replaced);
                    return replaced;
                }
                return result;
            });

            // Hook Context.getString(int, Object...) - 带格式化参数的版本
            Method getStringVarargs = android.content.Context.class.getMethod("getString", int.class, Object[].class);
            hook(getStringVarargs).intercept(chain -> {
                String result = (String) chain.proceed();
                if (result != null && result.contains(OLD_APP_NAME)) {
                    String replaced = result.replace(OLD_APP_NAME, NEW_APP_NAME);
                    Log.d(TAG, ">>> getStringVarargs replaced: " + result + " -> " + replaced);
                    return replaced;
                }
                return result;
            });

            Log.i(TAG, "Hook installed: Context.getString (content-based)");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook getString: " + e.getMessage(), e);
        }
    }

    private void hookToast(ClassLoader cl) {
        try {
            // Hook vo3 的静态 toast 方法 (showToast / showMessage)
            // 根据分析，vo3.m6473(String) 是显示 toast 的方法
            Class<?> vo3Class = Class.forName("androidx.obf.vo3", false, cl);

            // 找到接受 String 参数的静态方法
            for (Method m : vo3Class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class
                        && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    hook(m).intercept(chain -> {
                        Object arg = chain.getArgs().get(0);
                        if (arg instanceof String) {
                            String text = (String) arg;
                            if (text.contains(OLD_APP_NAME)) {
                                String replaced = text.replace(OLD_APP_NAME, NEW_APP_NAME);
                                Log.d(TAG, ">>> Toast replaced: " + text + " -> " + replaced);
                                return chain.proceedWith(chain.getThisObject(), new Object[]{replaced});
                            }
                        }
                        return chain.proceed();
                    });
                    Log.i(TAG, "Hook installed: vo3." + m.getName() + "(String)");
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook vo3 toast: " + e.getMessage(), e);
        }
    }

    private void handleIntent(Chain chain) {
        List<Object> args = chain.getArgs();
        if (args == null || args.isEmpty()) return;

        Object arg0 = args.get(0);
        if (!(arg0 instanceof Intent)) return;

        Intent intent = (Intent) arg0;
        ComponentName component = intent.getComponent();

        if (component == null) return;

        String pkg = component.getPackageName();
        String cls = component.getClassName();
        Log.d(TAG, "Intent component: " + pkg + "/" + cls);

        if (!OLD_PKG.equals(pkg)) return;
        if (!OLD_CLASS.equals(cls)) return;

        Object thisObj = chain.getThisObject();
        if (!(thisObj instanceof Context)) return;

        Context context = (Context) thisObj;
        PackageManager pm = context.getPackageManager();

        boolean oldAppInstalled;
        try {
            pm.getPackageInfo(OLD_PKG, 0);
            oldAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            oldAppInstalled = false;
        }

        if (oldAppInstalled) {
            Log.i(TAG, OLD_PKG + " is installed, keeping original");
            return;
        }

        Log.i(TAG, "REDIRECTING: " + OLD_PKG + " -> " + NEW_PKG);
        Log.i(TAG, "  Action: " + intent.getAction());
        Log.i(TAG, "  Data: " + intent.getData());
        Log.i(TAG, "  Type: " + intent.getType());
        Log.i(TAG, "  Flags: " + intent.getFlags());

        intent.setComponent(new ComponentName(NEW_PKG, NEW_CLASS));

        if (intent.getData() != null) {
            intent.setDataAndType(intent.getData(), "audio/*");
        }

        intent.addFlags(GRANT_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.i(TAG, "  After redirect - Action: " + intent.getAction());
        Log.i(TAG, "  After redirect - Data: " + intent.getData());
        Log.i(TAG, "  After redirect - Type: " + intent.getType());
        Log.i(TAG, "  After redirect - Component: " + intent.getComponent());
    }
}
