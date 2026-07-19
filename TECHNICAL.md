# LyricoBridge 技术文档

## 概述

LyricoBridge 是一个 Xposed/LSPosed 模块，用于将椒盐音乐（SaltPlayer）的「使用音乐标签应用编辑」功能重定向到 Lyrico。

## 核心功能

### 1. Intent 重定向
- Hook `ComponentActivity.startActivityForResult`
- 检测目标包名 `com.xjcheng.musictageditor`
- 重定向到 `com.lonx.lyrico.MainActivity`
- 补充 MIME type 和 URI 读取权限

### 2. UI 文本替换
- Hook `Context.getString` / `Resources.getString` / `TextView.setText`
- 将「音乐标签」替换为「Lyrico」

### 3. Toast 替换
- Hook `vo3` 类的静态 String 参数方法
- 替换硬编码的 Toast 文本

### 4. 刷新触发（核心）
- Hook `sg1.parseResult` 方法（ActivityResultContract.parseResult）
- 强制 `resultCode = -1` (RESULT_OK) 触发刷新

## 刷新机制详解

### 问题背景
Lyrico 修改音频文件标签后：
- 不调用 `MediaScannerConnection.scanFile()` 更新 MediaStore
- 不调用 `setResult(RESULT_OK)` 通知 SaltPlayer
- 导致 SaltPlayer 数据库中的元数据未更新

### 解决方案
Hook `sg1.parseResult` 方法，在 `resultCode != -1` 时强制改为 `-1`：

```java
hook(parseResult).intercept(chain -> {
    int resultCode = (int) chain.getArgs().get(0);
    if (resultCode != -1) {
        // 直接调用原始方法，传入修改后的参数
        Object result = parseResult.invoke(chain.getThisObject(), -1, 
            chain.getArgs().get(1) != null ? chain.getArgs().get(1) : new Intent());
        return result;
    }
    return chain.proceed();
});
```

### 触发的刷新流程
```
sg1.parseResult(resultCode=-1)
  → r01 协程启动
    → qs.ޗ() 从文件系统查找歌曲最新元数据
    → SongExtensionsKt.copyKeepId() 合并数据
    → SongRepo.update() 更新数据库
    → ඵ.Ϳ() 刷新 StateFlow（UI 更新）
    → MusicController.߾() 通知播放器
```

### 关键类映射（jadx → 运行时）
| jadx 名 | 运行时名 | 说明 |
|---------|---------|------|
| `sg1` | `androidx.obf.sg1` | ActivityResultContract 子类 |
| `r21` | `androidx.obf.r21` | ActivityResultContract 基类 |
| `r01` | `androidx.obf.r01` | 刷新协程 |
| `C1237` | `androidx.obf.ො` | ActivityResultRegistry |
| `mo4958` | `ސ` | parseResult 方法 |

### R8 混淆处理
- 方法名被混淆为 Unicode 字符（`ސ`）
- 使用**动态方法扫描**按签名 `(int, Intent) -> Object` 查找
- 使用**反射调用**绕过 `final` 方法限制

## 技术难点

### 1. chain.getArgs() 返回不可修改列表
```java
// 报错：UnsupportedOperationException
chain.getArgs().set(0, -1);  // ❌ UnmodifiableList

// 解决：直接调用原始方法
parseResult.invoke(chain.getThisObject(), -1, intent);  // ✅
```

### 2. R8 混淆导致类名/方法名不可预测
```java
// 解决：按方法签名动态查找
for (Method m : sg1Class.getDeclaredMethods()) {
    Class<?>[] params = m.getParameterTypes();
    if (params.length == 2 && params[0] == int.class && params[1] == Intent.class) {
        targetMethod = m;
        break;
    }
}
```

### 3. onActivityResult 在 Activity 切换时就被调用
```java
// 问题：hook onActivityResult 太早
// 解决：hook sg1.parseResult，在真正处理结果时修改
```

## 本地编译

### 环境要求
- JDK 21+
- Android SDK（local.properties 配置路径）

### 编译命令
```bash
# Debug 版本
.\gradlew.bat assembleDebug

# Release 版本
.\gradlew.bat assembleRelease
```

### 输出路径
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 测试验证

### 日志查看
```bash
adb logcat -s LyricoBridge
```

### 预期日志
```
I LyricoBridge: Hook installed: ComponentActivity.startActivityForResult
I LyricoBridge: Hook installed: getString/getText + TextView.setText
I LyricoBridge: Hook installed: sg1.ސ (force RESULT_OK + scan)
...
I LyricoBridge: REDIRECTING to com.lonx.lyrico
...
D LyricoBridge: >>> sg1.ސ: resultCode=0 -> forcing to -1
D LyricoBridge: >>> sg1.ސ returned: true
```

### 测试步骤
1. 安装 APK → 启用模块 → 重启椒盐音乐
2. 选择歌曲 → "使用 Lyrico 编辑..."
3. 在 Lyrico 修改标签 → 保存 → 返回
4. 确认封面/元数据已刷新
