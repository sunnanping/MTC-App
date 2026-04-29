# MTC App

一款基于 Capacitor 框架构建的多站点浏览器 Android 应用，内置多个 AI 聊天服务。

## 功能特性

- **多站点浏览器**：一键切换多个 AI 聊天服务
- **原生 Android 体验**：全屏 WebView，无浏览器 UI
- **Edge-to-Edge 支持**：完美适配 Android 16 边到边显示
- **自定义网站**：支持添加、编辑、删除自定义网站
- **本地存储**：网站配置本地持久化保存
- **启动屏幕**：TRAE 品牌启动画面
- **云端构建**：通过 GitHub Actions 自动构建 APK

## 内置 AI 服务

| 服务 | 网址 |
|------|------|
| TRAE SOLO | https://solo.trae.cn |
| DeepSeek | https://chat.deepseek.com |
| 豆包 | https://www.doubao.com/chat |
| Kimi | https://www.kimi.com |
| NotebookLM | https://notebooklm.google.com |

## 技术栈

### 前端（Web 层）
| 技术 | 版本 | 用途 |
|------|------|------|
| HTML5 | - | UI 结构 |
| CSS3 | - | 样式与布局 |
| JavaScript (ES6+) | - | 业务逻辑 |
| localStorage | - | 数据持久化 |
| Vite | ^5.0.0 | 构建工具 |

### 移动端框架
| 技术 | 版本 | 用途 |
|------|------|------|
| Capacitor Core | ^8.3.0 | 跨平台运行时 |
| Capacitor Android | ^8.3.0 | Android 平台 |
| Capacitor Splash Screen | ^8.0.1 | 启动屏幕 |

### Android 原生
| 技术 | 版本 | 用途 |
|------|------|------|
| Android Gradle Plugin | 8.9.1 | 构建系统 |
| Gradle | 8.11.1 | 构建自动化 |
| Java | 21 | 运行时 |
| AndroidX WebKit | 1.14.0 | WebView 支持 |
| compileSdk | 36 | SDK 版本 |
| targetSdk | 36 | 目标版本 |
| minSdk | 24 | 最低版本 |

### CI/CD
| 技术 | 用途 |
|------|------|
| GitHub Actions | 自动构建与发布 |

## 项目结构

```
MTC-App/
├── .github/
│   └── workflows/
│       └── build.yml              # GitHub Actions APK 构建工作流
│
├── android/                       # Android 原生项目
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/cn/trae/mtc/
│   │   │   │   └── MainActivity.java    # 主 Activity
│   │   │   ├── res/
│   │   │   │   ├── drawable/            # 图标与启动图
│   │   │   │   ├── layout/              # 布局 XML 文件
│   │   │   │   ├── mipmap-anydpi-v26/   # 自适应图标
│   │   │   │   ├── values/              # 字符串、颜色、样式
│   │   │   │   └── xml/                 # 配置与文件路径
│   │   │   └── AndroidManifest.xml      # 应用清单
│   │   ├── build.gradle                  # 应用级 Gradle 配置
│   │   ├── capacitor.build.gradle        # Capacitor 生成配置
│   │   └── debug.keystore                # 调试签名密钥
│   ├── gradle/wrapper/
│   │   ├── gradle-wrapper.jar            # Gradle 包装器 JAR
│   │   └── gradle-wrapper.properties     # Gradle 版本配置
│   ├── capacitor-cordova-android-plugins/
│   ├── build.gradle                      # 项目级 Gradle 配置
│   ├── variables.gradle                  # SDK 与依赖版本
│   ├── settings.gradle                   # 项目设置
│   ├── gradle.properties                 # Gradle 属性
│   └── gradlew                           # Gradle 包装器脚本
│
├── www/                           # Web 资源（生产环境）
│   ├── index.html                 # 主应用入口
│   └── error.html                 # 错误页面
│
├── dist/                          # Vite 构建输出
│   └── index.html
│
├── index.html                     # 源 HTML（开发环境）
├── vite.config.ts                 # Vite 配置
├── capacitor.config.json          # Capacitor 配置
├── package.json                   # NPM 依赖
├── package-lock.json              # 依赖锁定文件
├── .gitignore                     # Git 忽略规则
└── README.md                      # 本文件
```

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│ Android 应用 (APK)                                       │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Android WebView（原生组件）                          │ │
│ │ ┌─────────────────────────────────────────────────┐ │ │
│ │ │ Capacitor Bridge（JS-原生桥接）                 │ │ │
│ │ │ ┌─────────────────────────────────────────────┐ │ │ │
│ │ │ │ MTC App 前端 UI                            │ │ │ │
│ │ │ │ - 导航栏（网站图标导航）                    │ │ │ │
│ │ │ │ - Iframe（内嵌浏览器）                     │ │ │ │
│ │ │ │ - 模态框（添加/编辑网站）                  │ │ │ │
│ │ │ │ - 右键菜单（编辑/删除）                    │ │ │ │
│ │ │ └─────────────────────────────────────────────┘ │ │ │
│ │ └─────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## WebView 能力

### 已开启权限
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查
- `ACCESS_WIFI_STATE` - WiFi 状态检查
- `usesCleartextTraffic` - 允许 HTTP 明文传输

### iframe 沙箱权限
- `allow-scripts` - 执行 JavaScript
- `allow-same-origin` - 同源访问
- `allow-forms` - 表单提交
- `allow-popups` - 弹窗
- `allow-top-navigation` - 顶级页面导航

### 网络访问
- `<access origin="*" />` - 允许访问所有域名

## 安装方式

### 下载 APK
1. 访问 [Releases](../../releases) 页面
2. 下载最新的 `MTC-App-v1.0.x.apk`
3. 在 Android 设备上安装（如需请开启"未知来源"）

## 本地开发

```bash
# 克隆仓库
git clone https://github.com/sunnanping/MTC-App.git
cd MTC-App

# 安装依赖
npm install

# 构建前端
npm run build

# 同步 Capacitor
npx cap sync android

# 在 Android Studio 中打开
npx cap open android
```

## 构建 APK

### 自动构建（GitHub Actions）
推送到 `main` 分支或手动触发工作流：
1. 访问 Actions 页面
2. 选择 "Build Android APK" 工作流
3. 点击 "Run workflow"

### 手动构建
```bash
cd android
./gradlew assembleDebug
# APK 位置：android/app/build/outputs/apk/debug/app-debug.apk
```

## 版本兼容性

| AGP 版本 | 最低 Gradle | 最低 JDK |
|----------|-------------|----------|
| 8.9.1    | 8.11.1      | 17       |

## 致谢

- 使用 [Capacitor](https://capacitorjs.com/) 构建
- 由 [TRAE SOLO](https://solo.trae.cn) 提供支持

## 许可证

MIT License
