# Нативные библиотеки SDL2

Эта директория содержит нативные библиотеки SDL2 и SDL2_mixer для различных платформ.

## Структура:
- `linux-x64/` - Linux x86_64
- `linux-aarch64/` - Linux ARM64
- `windows-x64/` - Windows x86_64
- `windows-aarch64/` - Windows ARM64
- `macos-x64/` - macOS x86_64
- `macos-aarch64/` - macOS ARM64 (Apple Silicon)

## Автоматическая загрузка:
Библиотеки загружаются автоматически при запуске приложения через JNA.
