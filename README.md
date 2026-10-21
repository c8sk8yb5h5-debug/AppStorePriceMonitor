# 价格哨兵（Android APK）

针对 AppStorePrice.org 的原生 Android 价格监控客户端。当前为 MVP 源码，尚未在真实 Android / HarmonyOS 兼容环境完成端到端测试。

## 为什么不直接照搬 appstore-discounts

开发前检查了 [appstore-discounts/appstore-discounts](https://github.com/appstore-discounts/appstore-discounts)。该项目采用 MIT 许可证，使用 GitHub Actions 定时执行，追踪 Apple App Store 应用和 App 内购买项目价格，并提供 RSS、Telegram、钉钉通知。其数据获取路径是 Apple iTunes Search API 与 App Store 应用详情页，不是 AppStorePrice.org 的地区订阅价格表，因此本项目只借鉴定时检查、历史价格比较的架构，不直接复用其数据采集器。

## 当前功能

- 默认监控 ChatGPT Plus：`https://appstoreprice.org/zh/apps/6448311069`
- 默认监控 Google AI Pro：`https://appstoreprice.org/zh/apps/6477489729`
- 可添加其他 AppStorePrice.org `/zh/apps/<数字ID>` 页面
- Android WorkManager 在有网络时约每 24 小时检查一次；可手动触发检查
- 默认重大变动阈值为 5%，可在界面修改
- 首次成功读取只建立基线，不发送通知
- 以当地货币和国家/地区匹配价格；人民币换算波动不单独触发提醒
- 价格基线保存在本机 SharedPreferences，不上传个人数据
- Android 13 及以上申请系统通知权限

## 重要限制

1. AppStorePrice.org 没有在本项目中确认可供第三方稳定使用的公开 API；当前通过 Jsoup 解析页面中的国家价格表。如果页面结构、按钮文本或渲染方式改变，解析可能失效。
2. 目前以网页价格表格的行结构为基础解析，仍需在真实页面响应上测试国家、币种和套餐映射，不能视为生产级数据采集器。
3. 页面可能有多个套餐。应用详情页默认显示哪个套餐，取决于网站页面默认选择；当前版本没有独立的套餐选择器，也不能保证同一页面中所有订阅套餐都会被同时监控。
4. 仅比较当地币种金额，阈值默认 5%；小于阈值的价格变化不通知。涨价和降价都会通知，正文会标注方向。
5. Android WorkManager 的周期任务不是精确闹钟；部分 Android 系统会限制后台运行。需允许后台活动并测试通知权限。
6. 鸿蒙 NEXT 的 APK 安装与运行取决于设备系统版本、兼容环境及具体 APK；本项目需通过卓易通等可用兼容方式实机验证，不能保证纯鸿蒙设备上所有后台功能均可用。

## 构建 APK

推荐在 GitHub 上创建私有仓库并上传整个项目，然后进入 **Actions → Build Android APK → Run workflow**。成功后下载 `price-sentinel-debug-apk` artifact，其中包含调试版 APK。也可用 Android Studio 打开项目，使用 JDK 17、Android SDK 35 和 Gradle 8.9 构建。

## 上游项目

- [appstore-discounts/appstore-discounts](https://github.com/appstore-discounts/appstore-discounts) — MIT License；仅借鉴其架构思想，本项目没有直接复制其源文件。
