package com.hook.musictag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class HookEntry extends XposedModule implements XposedModuleInterface {

    private static final String TAG = "LyricoBridge";
    private static final String SALT_PKG = "com.salt.music";
    private static final String OLD_PKG = "com.xjcheng.musictageditor";
    private static final String OLD_CLASS = "com.xjcheng.musictageditor.SongDetailActivity";
    private static final String NEW_PKG = "com.lonx.lyrico";
    private static final String NEW_CLASS = "com.lonx.lyrico.MainActivity";

    private static final int GRANT_URI_PERMISSION = 0x00000001;

    public HookEntry() {
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!SALT_PKG.equals(param.getPackageName())) {
            return;
        }

        log(Log.INFO, TAG, "Hooking SaltPlayer: " + param.getPackageName());

        try {
            Class<?> componentActivityClass = Class.forName(
                    "androidx.activity.ComponentActivity",
                    false,
                    param.getDefaultClassLoader()
            );

            hookMethod(componentActivityClass, "startActivityForResult", Intent.class, int.class);
            hookMethod(componentActivityClass, "startActivityForResult", Intent.class, int.class, android.os.Bundle.class);

            log(Log.INFO, TAG, "Hook installed on ComponentActivity");
        } catch (Throwable e) {
            log(Log.ERROR, TAG, "Failed to hook ComponentActivity: " + e.getMessage());
            try {
                hookMethod(android.app.Activity.class, "startActivityForResult", Intent.class, int.class);
                hookMethod(android.app.Activity.class, "startActivityForResult", Intent.class, int.class, android.os.Bundle.class);
                log(Log.INFO, TAG, "Fallback: hook installed on Activity");
            } catch (Throwable e2) {
                log(Log.ERROR, TAG, "Fallback also failed: " + e2.getMessage());
            }
        }
    }

    private void hookMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = clazz.getMethod(methodName, paramTypes);
        hook(method).intercept(chain -> {
            log(Log.DEBUG, TAG, "Intercepted: " + chain.getExecutable().getName());
            handleIntent(chain);
            return chain.proceed();
        });
    }

    private void handleIntent(Chain chain) {
        List<Object> args = chain.getArgs();
        if (args == null || args.isEmpty()) return;

        Object arg0 = args.get(0);
        if (!(arg0 instanceof Intent)) return;

        Intent intent = (Intent) arg0;
        log(Log.DEBUG, TAG, "Intent: " + intent);

        ComponentName component = intent.getComponent();
        if (component == null) {
            log(Log.DEBUG, TAG, "No component in intent");
            return;
        }

        log(Log.DEBUG, TAG, "Component: " + component.flattenToString());

        if (!OLD_PKG.equals(component.getPackageName())) return;
        if (!OLD_CLASS.equals(component.getClassName())) return;

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
            log(Log.INFO, TAG, OLD_PKG + " is installed, keeping original");
            return;
        }

        log(Log.INFO, TAG, "Redirecting: " + OLD_PKG + " -> " + NEW_PKG);

        intent.setComponent(new ComponentName(NEW_PKG, NEW_CLASS));

        if (intent.getData() != null && intent.getType() == null) {
            intent.setType("audio/*");
        }

        intent.addFlags(GRANT_URI_PERMISSION);
    }
}
