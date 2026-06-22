package com.hook.musictag;

import android.app.Activity;
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
            Method m1 = Activity.class.getMethod("startActivity", Intent.class);
            Method m2 = Activity.class.getMethod("startActivityForResult", Intent.class, int.class);
            Method m3 = Activity.class.getMethod("startActivityForResult", Intent.class, int.class, android.os.Bundle.class);

            hook(m1).intercept(chain -> {
                handleIntent(chain);
                return chain.proceed();
            });

            hook(m2).intercept(chain -> {
                handleIntent(chain);
                return chain.proceed();
            });

            hook(m3).intercept(chain -> {
                handleIntent(chain);
                return chain.proceed();
            });

            log(Log.INFO, TAG, "Hook installed successfully on Activity methods");
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "Failed to find method: " + e.getMessage());
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
            log(Log.INFO, TAG, OLD_PKG + " is installed, using original intent");
            return;
        }

        log(Log.INFO, TAG, OLD_PKG + " not found, redirecting to " + NEW_PKG + "/" + NEW_CLASS);

        intent.setComponent(new ComponentName(NEW_PKG, NEW_CLASS));

        if (intent.getData() != null && intent.getType() == null) {
            intent.setType("audio/*");
        }

        intent.addFlags(GRANT_URI_PERMISSION);
    }
}
