package com.hook.musictag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {

    private static final String TAG = "LyricoBridge";
    private static final String SALT_PKG = "com.salt.music";
    private static final String OLD_PKG = "com.xjcheng.musictageditor";
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

        // Hook 4: sg1.mo4958 (parseResult) - 强制 RESULT_OK 触发刷新
        hookParseResult(cl);
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
                boolean redirected = handleIntent(chain);
                try {
                    return chain.proceed();
                } catch (Throwable e) {
                    if (redirected) {
                        showRedirectFailedToast(chain.getThisObject());
                        return null;
                    }
                    throw e;
                }
            });

            hook(m2).intercept(chain -> {
                Log.d(TAG, ">>> Intercepted startActivityForResult(Intent, int, Bundle)");
                boolean redirected = handleIntent(chain);
                try {
                    return chain.proceed();
                } catch (Throwable e) {
                    if (redirected) {
                        showRedirectFailedToast(chain.getThisObject());
                        return null;
                    }
                    throw e;
                }
            });

            Log.i(TAG, "Hook installed: ComponentActivity.startActivityForResult");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook ComponentActivity: " + e.getMessage(), e);
            try {
                Method m1 = android.app.Activity.class.getMethod("startActivityForResult", Intent.class, int.class);
                Method m2 = android.app.Activity.class.getMethod("startActivityForResult", Intent.class, int.class, android.os.Bundle.class);

                hook(m1).intercept(chain -> {
                    boolean redirected = handleIntent(chain);
                    try {
                        return chain.proceed();
                    } catch (Throwable e2) {
                        if (redirected) {
                            showRedirectFailedToast(chain.getThisObject());
                            return null;
                        }
                        throw e2;
                    }
                });

                hook(m2).intercept(chain -> {
                    boolean redirected = handleIntent(chain);
                    try {
                        return chain.proceed();
                    } catch (Throwable e2) {
                        if (redirected) {
                            showRedirectFailedToast(chain.getThisObject());
                            return null;
                        }
                        throw e2;
                    }
                });

                Log.i(TAG, "Fallback hook installed: Activity.startActivityForResult");
            } catch (Throwable e2) {
                Log.e(TAG, "Fallback also failed: " + e2.getMessage(), e2);
            }
        }
    }

    private void showRedirectFailedToast(Object activity) {
        if (activity instanceof android.content.Context) {
            android.widget.Toast.makeText(
                    (android.content.Context) activity,
                    "未找到【" + NEW_APP_NAME + "】应用，请前往GitHub下载",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void hookGetString(ClassLoader cl) {
        try {
            // Hook Context.getString(int)
            Method m1 = android.content.Context.class.getMethod("getString", int.class);
            hook(m1).intercept(chain -> replaceIfNeeded(chain.proceed()));

            // Hook Context.getString(int, Object...)
            Method m2 = android.content.Context.class.getMethod("getString", int.class, Object[].class);
            hook(m2).intercept(chain -> replaceIfNeeded(chain.proceed()));

            // Hook Resources.getString(int)
            Method m3 = android.content.res.Resources.class.getMethod("getString", int.class);
            hook(m3).intercept(chain -> replaceIfNeeded(chain.proceed()));

            // Hook Resources.getString(int, Object...)
            Method m4 = android.content.res.Resources.class.getMethod("getString", int.class, Object[].class);
            hook(m4).intercept(chain -> replaceIfNeeded(chain.proceed()));

            // Hook Resources.getText(int)
            Method m5 = android.content.res.Resources.class.getMethod("getText", int.class);
            hook(m5).intercept(chain -> replaceIfNeeded(chain.proceed()));

            // Hook TextView.setText(CharSequence) - fallback for non-Compose views
            Method setText = android.widget.TextView.class.getMethod("setText", CharSequence.class);
            hook(setText).intercept(chain -> {
                Object arg = chain.getArgs().get(0);
                if (arg instanceof CharSequence) {
                    String str = arg.toString();
                    String replaced = doReplace(str);
                    if (replaced != null) {
                        Log.d(TAG, ">>> TextView.setText replaced: " + str + " -> " + replaced);
                        return chain.proceedWith(chain.getThisObject(), new Object[]{replaced});
                    }
                }
                return chain.proceed();
            });

            Log.i(TAG, "Hook installed: getString/getText + TextView.setText");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook getString: " + e.getMessage(), e);
        }
    }

    private String doReplace(String str) {
        if (!str.contains(OLD_APP_NAME)) return null;
        return str.replace(OLD_APP_NAME + "应用", NEW_APP_NAME)
                .replaceAll("[\u2026.]+$", "") + "...";
    }

    private Object replaceIfNeeded(Object result) {
        if (result instanceof String) {
            String str = (String) result;
            String replaced = doReplace(str);
            if (replaced != null) {
                Log.d(TAG, ">>> String replaced: " + str + " -> " + replaced);
                return replaced;
            }
        } else if (result instanceof CharSequence) {
            String str = result.toString();
            String replaced = doReplace(str);
            if (replaced != null) {
                Log.d(TAG, ">>> CharSequence replaced: " + str + " -> " + replaced);
                return replaced;
            }
        }
        return result;
    }

    private void hookToast(ClassLoader cl) {
        try {
            Class<?> vo3Class = Class.forName("androidx.obf.vo3", false, cl);
            for (Method m : vo3Class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class
                        && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    hook(m).intercept(chain -> {
                        String text = (String) chain.getArgs().get(0);
                        String replaced = doReplaceToast(text);
                        if (replaced != null) {
                            Log.d(TAG, ">>> Toast replaced: " + text + " -> " + replaced);
                            return chain.proceedWith(chain.getThisObject(), new Object[]{replaced});
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

    private String doReplaceToast(String text) {
        if (text.contains(OLD_APP_NAME)) {
            return text.replace(OLD_APP_NAME, NEW_APP_NAME);
        }
        return null;
    }

    /**
     * Hook sg1.mo4958 (ActivityResultContract.parseResult)
     * 强制 resultCode = -1 (RESULT_OK)，触发封面刷新
     *
     * 原逻辑：if (resultCode != -1 || intent == null) return false;
     *         else launch r01 coroutine (refresh cover)
     *
     * Hook 后：每次调用都把 resultCode 改成 -1，条件自然通过
     */
    private void hookParseResult(ClassLoader cl) {
        try {
            Class<?> sg1Class = Class.forName("androidx.obf.sg1", false, cl);

            // 动态查找 (int, Intent) -> Object 方法
            Method targetMethod = null;
            for (Method m : sg1Class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 && params[0] == int.class && params[1] == Intent.class
                        && m.getReturnType() == Object.class) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod == null) {
                Log.e(TAG, "Could not find parseResult method in sg1");
                for (Method m : sg1Class.getDeclaredMethods()) {
                    Log.d(TAG, "  sg1 method: " + m.getName() + " params=" + java.util.Arrays.toString(m.getParameterTypes()));
                }
                return;
            }

            targetMethod.setAccessible(true);
            final Class<?> finalSg1Class = sg1Class;
            final Method parseResult = targetMethod;
            final ClassLoader finalCl = cl;

            hook(parseResult).intercept(chain -> {
                try {
                    int resultCode = (int) chain.getArgs().get(0);
                    // 只在 resultCode 不是 -1 时修改
                    if (resultCode != -1) {
                        Log.d(TAG, ">>> sg1." + parseResult.getName() + ": resultCode=" + resultCode + " -> forcing to -1");
                        // 直接调用原始方法，传入修改后的参数
                        Object result = parseResult.invoke(chain.getThisObject(), -1, chain.getArgs().get(1) != null ? chain.getArgs().get(1) : new Intent());
                        Log.d(TAG, ">>> sg1." + parseResult.getName() + " returned: " + result);
                        return result;
                    }
                    // resultCode 已经是 -1，正常执行
                    return chain.proceed();
                } catch (Throwable e) {
                    Log.e(TAG, ">>> sg1 THREW: " + e.getClass().getName() + ": " + e.getMessage());
                    Log.e(TAG, ">>> STACKTRACE: " + android.util.Log.getStackTraceString(e));
                    return Boolean.TRUE;
                }
            });

            Log.i(TAG, "Hook installed: sg1." + parseResult.getName() + " (force RESULT_OK + scan)");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to hook sg1.parseResult: " + e.getMessage(), e);
        }
    }

    private boolean handleIntent(Chain chain) {
        List<Object> args = chain.getArgs();
        if (args == null || args.isEmpty()) return false;

        Object arg0 = args.get(0);
        if (!(arg0 instanceof Intent)) return false;

        Intent intent = (Intent) arg0;
        ComponentName component = intent.getComponent();

        if (component == null) return false;

        String pkg = component.getPackageName();
        String cls = component.getClassName();
        Log.d(TAG, "Intent component: " + pkg + "/" + cls);

        if (!OLD_PKG.equals(pkg)) return false;

        Log.i(TAG, "REDIRECTING to " + NEW_PKG);
        intent.setComponent(new ComponentName(NEW_PKG, NEW_CLASS));



        if (intent.getData() != null) {
            intent.setDataAndType(intent.getData(), "audio/*");
        }

        intent.addFlags(GRANT_URI_PERMISSION);

        return true;
    }
}
