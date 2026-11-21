# Clash 开机助手（ADB 版）

此项目是 Android 9 / API 28 电视的开机助手源码。它在 BOOT_COMPLETED 后等待 30 秒，通过 `127.0.0.1:5555` 执行 ADB shell：
1. 调用 Clash 的 `START_CLASH` action；
2. 后台启动现有 `/data/local/tmp/clash-watch.sh`。

## 重要限制
- 这是源码项目 ZIP，不是已编译 APK。
- 当前只验证了 TCP 端口可连接、`ro.adb.secure=0`；尚未在目标电视上验证 Java ADB 协议握手。必须安装后查看日志确认。
- 如果电视上的 adbd 不接受本机 socket ADB 握手，助手会记录错误，不会获得 shell 权限。
- APK 不包含 Watcher 脚本；它依赖你已经创建的 `/data/local/tmp/clash-watch.sh`。
- 不 Root、不改系统文件、不关闭电视管家。
- 目标设备的 adbd 可能因厂商修改而不兼容本项目内置的简化 ADB client。

## 云端构建
1. 在 GitHub 新建一个空仓库。
2. 将本项目文件上传到仓库根目录，确保 `.github/workflows/build.yml` 也上传。
3. 打开仓库 Actions，运行 `Build APK`（或 push 到 main 自动构建）。
4. 在运行记录的 Artifacts 下载 `ClashBootHelper-debug`，解压得到 `app-debug.apk`。
5. 把 APK 传到电视安装。

## 安装后检查
电视重启后检查：
```sh
cat /data/local/tmp/clash-boot-helper.log
tail -n 10 /data/local/tmp/clash-watch.log
```
若出现 `ADB shell result` 且 Watcher 日志持续更新，才算验证成功。若出现 `ADB handshake expected CNXN` 或 socket 错误，把日志发回分析。

## 电视端安装前提
确保 Clash 已至少启动过一次并完成 VPN 授权；现有 `/data/local/tmp/clash-watch.sh` 文件仍在。
