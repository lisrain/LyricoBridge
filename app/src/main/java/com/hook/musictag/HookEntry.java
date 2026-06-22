package com.hook.musictag;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MusicTagHook";
    private static final String SALT_PKG = "com.salt.music";
    private static final String OLD_PKG = "com.xjcheng.musictageditor";
    private static final String OLD_CLASS = "com.xjcheng.musictageditor.SongDetailActivity";
    private static final String NEW_PKG = "com.lonx.lyrico";
    private static final String NEW_CLASS = "com.lonx.lyrico.MainActivity";

    private static final int GRANT_URI_PERMISSION = 0x00000001;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(SALT_PKG)) {
            return;
        }

        Log.i(TAG, "Hooking SaltPlayer: " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "startActivity",
                Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        handleIntent(param);
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                lpparam.classLoader,
                "startActivity",
                Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        handleIntent(param);
                    }
                }
        );

        Log.i(TAG, "Hook installed successfully");
    }

    private void handleIntent(MethodHookParam param) {
        Intent intent = (Intent) param.args[0];
        if (intent == null) return;

        ComponentName component = intent.getComponent();
        if (component == null) return;

        if (!OLD_PKG.equals(component.getPackageName())) return;
        if (!OLD_CLASS.equals(component.getClassName())) return;

        Context context = null;
        if (param.thisObject instanceof Context) {
            context = (Context) param.thisObject;
        }
        if (context == null) return;

        PackageManager pm = context.getPackageManager();
        boolean oldAppInstalled;
        try {
            pm.getPackageInfo(OLD_PKG, 0);
            oldAppInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            oldAppInstalled = false;
        }

        if (oldAppInstalled) {
            Log.i(TAG, OLD_PKG + " is installed, using original intent");
            return;
        }

        Log.i(TAG, OLD_PKG + " not found, redirecting to " + NEW_PKG);

        intent.setComponent(new ComponentName(NEW_PKG, NEW_CLASS));

        if (intent.getData() != null && intent.getType() == null) {
            intent.setType("audio/*");
        }

        intent.addFlags(GRANT_URI_PERMISSION);
    }
}
