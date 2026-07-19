# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-07-19

### Added
- 刷新触发：从 Lyrico 返回后自动刷新歌曲元数据
  - Hook sg1.parseResult 强制 resultCode = RESULT_OK
  - 触发 r01 协程更新数据库、刷新 UI、通知播放器
  - 动态方法扫描处理 R8 混淆
- Gradle Wrapper 支持本地编译
- 技术文档 (TECHNICAL.md)

### Changed
- Gradle 升级到 9.5.1
- CI/CD 改为双轨制：日常编译 + 手动发版

### Fixed
- 修复从外部编辑器返回后不刷新的问题
- 修复 chain.getArgs() 返回 UnmodifiableList 的问题

## [1.0.2] - 2026-06-22

### Added
- Intent 重定向到 Lyrico
- UI 文本替换（音乐标签 → Lyrico）
- Toast 替换
- 异常处理和错误提示

## [1.0.1] - 2026-06-22

### Fixed
- 修复 Toast 显示问题
- 修复 startActivity 异常处理

## [1.0.0] - 2026-06-22

### Added
- 初始版本
- 基本的 Intent 重定向功能
