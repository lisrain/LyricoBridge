# LyricoBridge

Xposed 模块，将 SaltPlayer（盐音乐）的「使用音乐标签应用编辑」功能重定向到 Lyrico。

## 功能

- 将 SaltPlayer 中「使用音乐标签应用编辑」的跳转目标从 `com.xjcheng.musictageditor` 重定向到 `com.lonx.lyrico`
- 当音乐标签编辑器未安装时自动跳转 Lyrico，已安装时保持原行为
- 自动补全 intent 的 MIME type（`audio/*`）和 URI 读取权限，确保 Lyrico 能正确打开音频文件

## 环境要求

- Android 设备已 Root
- LSPosed 2.0+（支持 libxposed API 102）
- SaltPlayer（盐音乐）已安装
- Lyrico 已安装

## 安装

1. 从 [Releases](https://github.com/lisrain/LyricoBridge/releases) 或 [Actions](https://github.com/lisrain/LyricoBridge/actions) 下载 APK
2. 安装后在 LSPosed 中启用模块
3. 勾选作用域：`com.salt.music`
4. 重启 SaltPlayer

## 构建

```bash
# 需要 JDK 21 + Gradle 9.5.1
gradle assembleRelease
```

或直接 fork 本仓库，GitHub Actions 会自动编译。

## 许可证

MIT
