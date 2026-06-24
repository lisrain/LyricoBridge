# LyricoBridge

Xposed 模块，将椒盐音乐（SaltPlayer）的「使用音乐标签应用编辑」功能重定向到 Lyrico。

## 相关软件

- **椒盐音乐（SaltPlayer）** — 一款优秀的本地音乐播放器
  - 官网：https://moriafly.com/program/salt-player.html
  - 酷安：https://www.coolapk.com/apk/com.salt.music
  - GitHub：https://github.com/Moriafly/SaltPlayerSource
- **Lyrico** — 一款面向 Android 的开源本地音乐标签编辑与歌词管理工具
  - GitHub：https://github.com/Replica0110/Lyrico

## 功能

- 将椒盐音乐中「使用音乐标签应用编辑」的跳转目标重定向到 Lyrico
- 自动补全 intent 的 MIME type（`audio/*`）和 URI 读取权限，确保 Lyrico 能正确打开音频文件

## 环境要求

- Android 设备已 Root
- LSPosed 2.0+（支持 libxposed API 101）
- 椒盐音乐已安装
- Lyrico 已安装
- （嗯...Root和LSPosed其实不是必须项，但你得自行解决签名校验问题）

## 安装

1. 从 [Releases](https://github.com/lisrain/LyricoBridge/releases) 或 [Actions](https://github.com/lisrain/LyricoBridge/actions) 下载 APK
2. 安装后在 LSPosed 中启用模块
3. 勾选作用域：`com.salt.music`
4. 重启椒盐音乐

## 构建

```bash
# 需要 JDK 21 + Gradle 9.5.1
gradle assembleRelease
```

或直接 fork 本仓库，GitHub Actions 会自动编译。

## 致谢

- [frank-HLJX](https://github.com/frank-HLJX) — 提供部分 hook 点信息

## 许可证

MIT
